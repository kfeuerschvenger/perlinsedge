package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The sea level runs over small maps whose height rises with the index of the tile, so the lowest tiles are known. */
class SeaLevelPassTest {

    private static final int SIDE = 20;
    private static final int TILES = SIDE * SIDE;
    private static final float WATER = 0.3F;
    private static final float LEVEL = -0.2F;
    private static final float TOLERANCE = 2F;
    private static final float FLOOR = 7F;
    private static final float EPSILON = 1e-4F;
    private static final float FLAT = 0.4F;

    /** Builds a map whose height rises with the tile index, from the given floor up to the floor plus one. */
    private static GenerationContext rising(float floor) {
        final var context = TestContexts.of(SIDE, SIDE);
        final var heights = context.defineField(NoiseField.HEIGHT);
        for (var index = 0; index < TILES; index++) {
            heights[index] = floor + index / (float) TILES;
        }
        return context;
    }

    private static int below(GenerationContext context, float level) {
        final var heights = context.field(NoiseField.HEIGHT);
        var count = 0;
        for (var height : heights) {
            count += height < level ? 1 : 0;
        }
        return count;
    }

    private static void apply(GenerationContext context, float waterShare) {
        new SeaLevelPass(new SeaLevelParams(waterShare, LEVEL)).apply(context);
    }

    @Test
    void theWaterShareOfTheMapEndsUpBelowTheLevel() {
        final var context = rising(0F);

        apply(context, WATER);

        assertEquals(TILES * WATER, (float) below(context, LEVEL), TOLERANCE);
    }

    @Test
    void theShareHoldsWhereverTheNoiseHappenedToSitInItsRange() {
        final var high = rising(FLOOR);
        final var low = rising(-FLOOR);

        apply(high, WATER);
        apply(low, WATER);

        assertEquals(TILES * WATER, (float) below(high, LEVEL), TOLERANCE);
        assertEquals(TILES * WATER, (float) below(low, LEVEL), TOLERANCE);
    }

    @Test
    void everyHeightMovesByTheSameAmountSoTheShapeOfTheLandIsKept() {
        final var context = rising(0F);
        final var before = context.field(NoiseField.HEIGHT).clone();

        apply(context, WATER);

        final var after = context.field(NoiseField.HEIGHT);
        final var shift = after[0] - before[0];
        for (var index = 0; index < TILES; index++) {
            assertEquals(before[index] + shift, after[index], EPSILON, "tile " + index);
        }
    }

    @Test
    void noWaterLeavesEveryTileAtOrAboveTheLevel() {
        final var context = rising(0F);

        apply(context, 0F);

        assertEquals(0, below(context, LEVEL));
        assertEquals(LEVEL, context.field(NoiseField.HEIGHT)[0], EPSILON);
    }

    @Test
    void aFlatMapIsLeftAlone() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillField(context, NoiseField.HEIGHT, FLAT);

        apply(context, WATER);

        for (var height : context.field(NoiseField.HEIGHT)) {
            assertEquals(FLAT, height, EPSILON);
        }
    }

    @Test
    void theParametersRefuseAShareOfOneOrMoreAndALevelThatIsNotANumber() {
        assertThrows(IllegalArgumentException.class, () -> new SeaLevelParams(1F, LEVEL));
        assertThrows(IllegalArgumentException.class, () -> new SeaLevelParams(-WATER, LEVEL));
        assertThrows(IllegalArgumentException.class, () -> new SeaLevelParams(Float.NaN, LEVEL));
        assertThrows(IllegalArgumentException.class, () -> new SeaLevelParams(WATER, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new SeaLevelParams(WATER, Float.POSITIVE_INFINITY));
        assertThrows(NullPointerException.class, () -> new SeaLevelPass(null));
    }

    @Test
    void thePassIsNamedForTheTimings() {
        assertEquals("sea-level", new SeaLevelPass(new SeaLevelParams(WATER, LEVEL)).name());
    }
}
