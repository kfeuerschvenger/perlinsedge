package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundPassTest {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final float AMPLITUDE = 0.05F;
    private static final float TOLERANCE = 1e-6F;
    private static final float BOUNDARY = 0.2F;
    private static final float CLIMATE = 0.5F;

    private final GenerationContext context = TestContexts.of(WIDTH, HEIGHT);

    /** A rule that returns hills above the boundary and sand below, and records nothing else. */
    private static final GroundRule SPLIT = (float height, float temperature, float moisture) ->
            height >= BOUNDARY ? TileType.HILL : TileType.SAND;

    private void fillFields(float height) {
        Arrays.fill(context.defineField(NoiseField.HEIGHT), height);
        Arrays.fill(context.defineField(NoiseField.TEMPERATURE), CLIMATE);
        Arrays.fill(context.defineField(NoiseField.MOISTURE), CLIMATE);
    }

    @Test
    void withoutDitherEveryTileIsClassifiedByItsOwnValues() {
        fillFields(0F);
        final var heights = context.field(NoiseField.HEIGHT);
        heights[context.index(3, 4)] = BOUNDARY + 0.1F;

        new GroundPass(new GroundParams(SPLIT, 0F)).apply(context);

        assertEquals(TileType.HILL, context.ground()[context.index(3, 4)]);
        assertEquals(TileType.SAND, context.ground()[context.index(0, 0)]);
        assertTrue(Arrays.stream(context.ground()).noneMatch((TileType type) -> type == null));
    }

    @Test
    void theRuleSeesTheClimateOfTheSameTile() {
        fillFields(0F);
        context.field(NoiseField.TEMPERATURE)[context.index(2, 2)] = 0.1F;
        context.field(NoiseField.MOISTURE)[context.index(2, 2)] = 0.9F;
        final GroundRule byClimate = (float height, float temperature, float moisture) ->
                temperature < moisture ? TileType.SWAMP : TileType.GRASS;

        new GroundPass(new GroundParams(byClimate, 0F)).apply(context);

        assertEquals(TileType.SWAMP, context.ground()[context.index(2, 2)]);
        assertEquals(TileType.GRASS, context.ground()[context.index(2, 3)]);
    }

    @Test
    void ditherNudgesTheHeightByAtMostTheAmplitudeAndNeverWritesItBack() {
        fillFields(BOUNDARY);
        final var largestNudge = new float[1];
        final GroundRule recorder = (float height, float temperature, float moisture) -> {
            largestNudge[0] = Math.max(largestNudge[0], Math.abs(height - BOUNDARY));
            return TileType.SAND;
        };

        new GroundPass(new GroundParams(recorder, AMPLITUDE)).apply(context);

        assertTrue(largestNudge[0] <= AMPLITUDE + TOLERANCE, "nudge " + largestNudge[0]);
        assertTrue(largestNudge[0] > 0F);
        for (var value : context.field(NoiseField.HEIGHT)) {
            assertEquals(BOUNDARY, value);
        }
    }

    @Test
    void ditherSplitsTilesThatSitOnTheBoundaryAndRepeatsForTheSameSeed() {
        fillFields(BOUNDARY);
        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(context);
        final var first = context.ground().clone();
        final var hills = Arrays.stream(first).filter((TileType type) -> type == TileType.HILL).count();

        final var again = TestContexts.of(WIDTH, HEIGHT);
        Arrays.fill(again.defineField(NoiseField.HEIGHT), BOUNDARY);
        Arrays.fill(again.defineField(NoiseField.TEMPERATURE), CLIMATE);
        Arrays.fill(again.defineField(NoiseField.MOISTURE), CLIMATE);
        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(again);

        assertTrue(hills > 0 && hills < first.length, "hills " + hills + " of " + first.length);
        assertTrue(Arrays.equals(first, again.ground()));
    }

    @Test
    void theDitherDependsOnTheSeed() {
        fillFields(BOUNDARY);
        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(context);
        final var other = TestContexts.of(context.type(), context.seed() + 1, WIDTH, HEIGHT, TestContexts.FLAT);
        Arrays.fill(other.defineField(NoiseField.HEIGHT), BOUNDARY);
        Arrays.fill(other.defineField(NoiseField.TEMPERATURE), CLIMATE);
        Arrays.fill(other.defineField(NoiseField.MOISTURE), CLIMATE);

        new GroundPass(new GroundParams(SPLIT, AMPLITUDE)).apply(other);

        assertFalse(Arrays.equals(context.ground(), other.ground()));
    }

    @Test
    void aMissingFieldIsReportedByName() {
        Arrays.fill(context.defineField(NoiseField.HEIGHT), 0F);
        final var pass = new GroundPass(new GroundParams(SPLIT, 0F));

        final var error = assertThrows(IllegalStateException.class, () -> pass.apply(context));

        assertTrue(error.getMessage().contains("TEMPERATURE"));
    }

    @Test
    void paramsRefuseAMissingRuleAndABadAmplitude() {
        assertThrows(NullPointerException.class, () -> new GroundParams(null, 0F));
        assertThrows(IllegalArgumentException.class, () -> new GroundParams(SPLIT, -0.1F));
        assertThrows(IllegalArgumentException.class, () -> new GroundParams(SPLIT, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new GroundParams(SPLIT, Float.POSITIVE_INFINITY));
    }

    @Test
    void namesItselfForTheTimings() {
        assertEquals("ground", new GroundPass(new GroundParams(SPLIT, 0F)).name());
    }
}
