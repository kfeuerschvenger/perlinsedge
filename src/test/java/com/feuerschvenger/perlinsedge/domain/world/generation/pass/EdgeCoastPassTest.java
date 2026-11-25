package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The height starts flat at zero, so the coast is the only thing that changes it. */
class EdgeCoastPassTest {

    private static final int SIDE = 40;
    private static final int DEPTH = 10;
    private static final float DROP = 0.8F;
    private static final float TOLERANCE = 1e-5F;
    private static final long SEED = 5L;
    private static final long SEEDS = 24L;
    private static final int EDGES = 4;
    private static final int MIDDLE = SIDE / 2;
    private static final int LAST = SIDE - 1;
    private static final float SMOOTHSTEP_HALF = 0.5F;

    private static GenerationContext flat(long seed) {
        final var context = TestContexts.of(MapType.MEDITERRANEAN, seed, SIDE, SIDE, TestContexts.FLAT);
        TestContexts.fillField(context, NoiseField.HEIGHT, 0F);
        return context;
    }

    private static void apply(GenerationContext context, int depth) {
        new EdgeCoastPass(new EdgeCoastParams(depth, DROP)).apply(context);
    }

    /** Returns the middle tiles of the top, bottom, left and right edges, in that order. */
    private static float[] edgeMiddles(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        return new float[] {heights[context.index(MIDDLE, 0)], heights[context.index(MIDDLE, LAST)],
            heights[context.index(0, MIDDLE)], heights[context.index(LAST, MIDDLE)]};
    }

    /** Returns which edge was lowered, the one whose middle tile dropped by the full drop. */
    private static int loweredEdge(GenerationContext context) {
        final var middles = edgeMiddles(context);
        var found = -1;
        for (var edge = 0; edge < EDGES; edge++) {
            if (Math.abs(middles[edge] + DROP) < TOLERANCE) {
                assertEquals(-1, found, "more than one edge was lowered");
                found = edge;
            }
        }
        return found;
    }

    @Test
    void exactlyOneEdgeIsLoweredByTheFullDropAndTheOthersAreUntouched() {
        final var context = flat(SEED);

        apply(context, DEPTH);

        final var lowered = loweredEdge(context);
        assertTrue(lowered >= 0, "one edge drops by the drop");
        final var middles = edgeMiddles(context);
        for (var edge = 0; edge < EDGES; edge++) {
            if (edge != lowered) {
                assertEquals(0F, middles[edge], TOLERANCE, "edge " + edge);
            }
        }
    }

    @Test
    void theLoweringFadesSmoothlyToNothingAtTheDepth() {
        final var context = flat(SEED);

        apply(context, DEPTH);

        // Walk in from the lowered edge along the line through the middle of the map
        final var heights = context.field(NoiseField.HEIGHT);
        final var edge = loweredEdge(context);
        var previous = -DROP - 1F;
        for (var distance = 0; distance <= DEPTH; distance++) {
            final var height = heights[indexAt(context, edge, distance)];
            assertTrue(height >= previous, "the height rises with the distance, at " + distance);
            previous = height;
        }
        assertEquals(0F, heights[indexAt(context, edge, DEPTH)], TOLERANCE);
        assertEquals(-DROP * SMOOTHSTEP_HALF, heights[indexAt(context, edge, DEPTH / 2)], TOLERANCE);
    }

    private static int indexAt(GenerationContext context, int edge, int distance) {
        return switch (edge) {
            case 0 -> context.index(MIDDLE, distance);
            case 1 -> context.index(MIDDLE, LAST - distance);
            case 2 -> context.index(distance, MIDDLE);
            default -> context.index(LAST - distance, MIDDLE);
        };
    }

    @Test
    void seedsPickDifferentEdges() {
        final var edges = new HashSet<Integer>();
        for (var seed = 1L; seed <= SEEDS; seed++) {
            final var context = flat(seed);
            apply(context, DEPTH);
            edges.add(loweredEdge(context));
        }

        assertEquals(EDGES, edges.size());
    }

    @Test
    void theSameSeedLowersTheSameEdgeTheSameWay() {
        final var first = flat(SEED);
        final var second = flat(SEED);

        apply(first, DEPTH);
        apply(second, DEPTH);

        assertArrayEquals(first.field(NoiseField.HEIGHT), second.field(NoiseField.HEIGHT));
    }

    @Test
    void aCoastDeeperThanTheMapStillLowersEveryTileOfItWithoutFailing() {
        final var context = flat(SEED);

        apply(context, SIDE * 2);

        final var heights = context.field(NoiseField.HEIGHT);
        for (var index = 0; index < heights.length; index++) {
            assertTrue(heights[index] <= 0F);
        }
        assertEquals(-DROP, Math.min(Math.min(edgeMiddles(context)[0], edgeMiddles(context)[1]),
                Math.min(edgeMiddles(context)[2], edgeMiddles(context)[3])), TOLERANCE);
    }

    @Test
    void theParametersRefuseNumbersThatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(0, DROP));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(DEPTH, 0F));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(DEPTH, -DROP));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(DEPTH, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(DEPTH, Float.POSITIVE_INFINITY));
        assertThrows(NullPointerException.class, () -> new EdgeCoastPass(null));
    }

    @Test
    void thePassIsNamed() {
        assertEquals("edge-coast", new EdgeCoastPass(new EdgeCoastParams(DEPTH, DROP)).name());
    }
}
