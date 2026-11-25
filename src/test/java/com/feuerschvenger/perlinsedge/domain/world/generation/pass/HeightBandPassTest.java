package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The band runs over small maps whose height rises with the index of the tile, so the highest tiles are known. */
class HeightBandPassTest {

    private static final int SIDE = 10;
    private static final int TILES = SIDE * SIDE;
    /** The lowest half of the map is water in the test of the share of the land. */
    private static final int WATER_TILES = 50;
    private static final float QUARTER = 0.25F;
    private static final float HALF = 0.5F;
    private static final float ALL = 1F;
    private static final float NO_SHARE = 0F;
    private static final float OVER = 1.5F;
    private static final int TOLERANCE = 1;
    private static final float FLAT = 0.3F;
    private static final float PEAK = 9F;

    /** Builds a map of grass whose height rises with the tile index, from 0 up to just below 1. */
    private static GenerationContext rising() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        final var heights = context.defineField(NoiseField.HEIGHT);
        for (var index = 0; index < TILES; index++) {
            heights[index] = index / (float) TILES;
        }
        return context;
    }

    private static void apply(GenerationContext context, float share) {
        new HeightBandPass(new HeightBandParams(share, TileType.MOUNTAIN)).apply(context);
    }

    @Test
    void theHighestShareOfTheLandBecomesTheBand() {
        final var context = rising();

        apply(context, QUARTER);

        final var band = TestContexts.countGround(context, TileType.MOUNTAIN);
        assertEquals(TILES * QUARTER, band, TOLERANCE);
        assertEquals(TileType.MOUNTAIN, context.groundAt(TILES - 1));
        assertEquals(TileType.GRASS, context.groundAt(0));
        var lowestBand = TILES;
        var highestGrass = -1;
        for (var index = 0; index < TILES; index++) {
            if (context.groundAt(index) == TileType.MOUNTAIN) {
                lowestBand = Math.min(lowestBand, index);
            } else {
                highestGrass = Math.max(highestGrass, index);
            }
        }
        assertTrue(highestGrass < lowestBand, "every tile of the band is higher than every tile left");
    }

    @Test
    void theShareIsOfTheLandAndWaterNeverJoinsTheBand() {
        final var context = rising();
        for (var index = 0; index < WATER_TILES; index++) {
            context.setGround(index, TileType.DEEP_WATER);
        }

        apply(context, HALF);

        assertEquals((TILES - WATER_TILES) * HALF, TestContexts.countGround(context, TileType.MOUNTAIN), TOLERANCE);
        assertEquals(WATER_TILES, TestContexts.countGround(context, TileType.DEEP_WATER));
    }

    @Test
    void aReservedTileKeepsItsGroundEvenWhenItIsTheHighest() {
        final var context = rising();
        context.reserve(TILES - 1);

        apply(context, QUARTER);

        assertEquals(TileType.GRASS, context.groundAt(TILES - 1));
        assertNotEquals(0, TestContexts.countGround(context, TileType.MOUNTAIN));
    }

    @Test
    void aShareOfAllTheLandTurnsEveryLandTileAndAMapWithoutLandIsLeftAlone() {
        final var everything = rising();
        final var sea = rising();
        TestContexts.fillGround(sea, TileType.DEEP_WATER);

        apply(everything, ALL);
        apply(sea, QUARTER);

        assertEquals(TILES, TestContexts.countGround(everything, TileType.MOUNTAIN));
        assertEquals(TILES, TestContexts.countGround(sea, TileType.DEEP_WATER));
    }

    @Test
    void aFlatMapGetsNoBandUnlessTheShareIsAll() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        TestContexts.fillField(context, NoiseField.HEIGHT, FLAT);

        apply(context, QUARTER);

        assertEquals(0, TestContexts.countGround(context, TileType.MOUNTAIN));
    }

    @Test
    void theBandFollowsTheHeightWhateverItsRangeIs() {
        final var context = rising();
        final var heights = context.field(NoiseField.HEIGHT);
        for (var index = 0; index < TILES; index++) {
            heights[index] = heights[index] * PEAK + PEAK;
        }

        apply(context, QUARTER);

        assertEquals(TILES * QUARTER, TestContexts.countGround(context, TileType.MOUNTAIN), TOLERANCE);
        assertEquals(TileType.MOUNTAIN, context.groundAt(TILES - 1));
    }

    @Test
    void theParametersRefuseAShareThatIsNotAShareOfTheLandAndABandThatIsNotLand() {
        assertThrows(IllegalArgumentException.class, () -> new HeightBandParams(NO_SHARE, TileType.MOUNTAIN));
        assertThrows(IllegalArgumentException.class, () -> new HeightBandParams(OVER, TileType.MOUNTAIN));
        assertThrows(IllegalArgumentException.class, () -> new HeightBandParams(Float.NaN, TileType.MOUNTAIN));
        assertThrows(IllegalArgumentException.class, () -> new HeightBandParams(QUARTER, TileType.DEEP_WATER));
        assertThrows(NullPointerException.class, () -> new HeightBandParams(QUARTER, null));
        assertThrows(NullPointerException.class, () -> new HeightBandPass(null));
    }

    @Test
    void thePassIsNamedForTheTimings() {
        assertEquals("height-band", new HeightBandPass(new HeightBandParams(QUARTER, TileType.MOUNTAIN)).name());
    }
}
