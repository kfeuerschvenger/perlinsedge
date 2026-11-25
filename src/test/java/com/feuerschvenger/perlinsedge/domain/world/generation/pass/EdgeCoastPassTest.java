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
    private static final float DEPTH_SHARE = 0.25F;
    private static final int DEPTH = 10;
    private static final int WIDE = 60;
    private static final int LOW = 20;
    private static final int WIDE_DEPTH = 15;
    private static final int LOW_DEPTH = 5;
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

    private static void apply(GenerationContext context, float depthShare) {
        new EdgeCoastPass(new EdgeCoastParams(depthShare, DROP)).apply(context);
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

        apply(context, DEPTH_SHARE);

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

        apply(context, DEPTH_SHARE);

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
            apply(context, DEPTH_SHARE);
            edges.add(loweredEdge(context));
        }

        assertEquals(EDGES, edges.size());
    }

    @Test
    void theSameSeedLowersTheSameEdgeTheSameWay() {
        final var first = flat(SEED);
        final var second = flat(SEED);

        apply(first, DEPTH_SHARE);
        apply(second, DEPTH_SHARE);

        assertArrayEquals(first.field(NoiseField.HEIGHT), second.field(NoiseField.HEIGHT));
    }

    @Test
    void aCoastAsDeepAsTheMapStillLowersEveryTileOfItWithoutFailing() {
        final var context = flat(SEED);

        apply(context, 1F);

        final var heights = context.field(NoiseField.HEIGHT);
        for (var index = 0; index < heights.length; index++) {
            assertTrue(heights[index] <= 0F);
        }
        assertEquals(-DROP, Math.min(Math.min(edgeMiddles(context)[0], edgeMiddles(context)[1]),
                Math.min(edgeMiddles(context)[2], edgeMiddles(context)[3])), TOLERANCE);
    }

    @Test
    void theDepthIsAShareOfTheMapAcrossTheEdgeSoBothSizesAreFloodedAlike() {
        final var wide = new EdgeCoastPass(new EdgeCoastParams(DEPTH_SHARE, DROP));

        assertEquals(DEPTH, wide.depthOf(SIDE));
        assertEquals(WIDE_DEPTH, wide.depthOf(WIDE));
        assertEquals(LOW_DEPTH, wide.depthOf(LOW));
        assertEquals(1, wide.depthOf(1));
    }

    @Test
    void onAMapThatIsNotSquareTheTopAndBottomUseTheHeightAndTheSidesUseTheWidth() {
        for (var seed = 1L; seed <= SEEDS; seed++) {
            final var context = TestContexts.of(MapType.MEDITERRANEAN, seed, WIDE, LOW, TestContexts.FLAT);
            TestContexts.fillField(context, NoiseField.HEIGHT, 0F);

            apply(context, DEPTH_SHARE);

            // The middle column crosses a lowered top or bottom, the middle row a lowered left or right side
            final var heights = context.field(NoiseField.HEIGHT);
            var columnHits = 0;
            for (var y = 0; y < LOW; y++) {
                columnHits += heights[context.index(WIDE / 2, y)] < 0F ? 1 : 0;
            }
            var rowHits = 0;
            for (var x = 0; x < WIDE; x++) {
                rowHits += heights[context.index(x, LOW / 2)] < 0F ? 1 : 0;
            }
            assertEquals(columnHits > 0 ? LOW_DEPTH : WIDE_DEPTH, columnHits + rowHits, "seed " + seed);
        }
    }

    @Test
    void theParametersRefuseNumbersThatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(0F, DROP));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(1.5F, DROP));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(Float.NaN, DROP));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(DEPTH_SHARE, 0F));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(DEPTH_SHARE, -DROP));
        assertThrows(IllegalArgumentException.class, () -> new EdgeCoastParams(DEPTH_SHARE, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new EdgeCoastParams(DEPTH_SHARE, Float.POSITIVE_INFINITY));
        assertThrows(NullPointerException.class, () -> new EdgeCoastPass(null));
    }

    @Test
    void thePassIsNamed() {
        assertEquals("edge-coast", new EdgeCoastPass(new EdgeCoastParams(DEPTH_SHARE, DROP)).name());
    }
}
