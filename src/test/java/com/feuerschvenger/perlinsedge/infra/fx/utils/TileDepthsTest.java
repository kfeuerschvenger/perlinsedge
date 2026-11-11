package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileDepthsTest {

    @Test
    void landIsAtTheTopAndShallowCoastAndRiverWaterOneStepDownAndDeepWaterTwo() {
        for (TileType type : TileType.values()) {
            int expected = !type.isWater() ? 0 : type == TileType.DEEP_WATER ? 2 : 1;
            assertEquals(expected, TileDepths.level(type), type.name());
            assertEquals(expected * 10, TileDepths.sinkPixels(type), type.name());
        }
    }

    @Test
    void theWaterSheetLiesHalfAStepBelowTheLandTopsSoItIsBetweenLandAndBed() {
        assertEquals(5, TileDepths.SURFACE_PIXELS);
        assertTrue(TileDepths.SURFACE_PIXELS > 0);
        assertTrue(TileDepths.SURFACE_PIXELS < TileDepths.sinkPixels(TileType.SHALLOW_WATER));
    }

    @Test
    void aStepIsAWholeNumberOfFaceStripsPlusTheFaceOfTheSprite() {
        assertEquals(0, (TileDepths.STEP_PIXELS - TileDepths.FACE_PIXELS) % TileDepths.FACE_PIXELS);
    }

    @Test
    void aNeighbourAtTheSameLevelOrHigherExposesNothing() {
        assertEquals(0, TileDepths.stripsExposed(TileType.GRASS, TileType.SAND));
        assertEquals(0, TileDepths.stripsExposed(TileType.SHALLOW_WATER, TileType.COAST));
        assertEquals(0, TileDepths.stripsExposed(TileType.SHALLOW_WATER, TileType.GRASS));
        assertEquals(0, TileDepths.stripsExposed(TileType.DEEP_WATER, TileType.SHALLOW_WATER));
        assertEquals(0, TileDepths.stripsExposed(TileType.GRASS, null));
    }

    @Test
    void aLowerNeighbourExposesTheDropMinusTheFaceTheSpriteAlreadyHas() {
        // Land to shallow is a 10 pixel drop, 5 of them in the sprite
        assertEquals(1, TileDepths.stripsExposed(TileType.GRASS, TileType.SHALLOW_WATER));
        assertEquals(1, TileDepths.stripsExposed(TileType.SAND, TileType.RIVER));
        // Shallow to deep is the step in the water
        assertEquals(1, TileDepths.stripsExposed(TileType.SHALLOW_WATER, TileType.DEEP_WATER));
        // Land straight to deep water is a 20 pixel drop
        assertEquals(3, TileDepths.stripsExposed(TileType.MOUNTAIN, TileType.DEEP_WATER));
    }

    @Test
    void theBankFaceReachesExactlyTheBedOfTheNeighbour() {
        for (TileType self : TileType.values()) {
            for (TileType other : TileType.values()) {
                int drop = TileDepths.sinkPixels(other) - TileDepths.sinkPixels(self);
                int shown = drop > 0 ? TileDepths.FACE_PIXELS * (1 + TileDepths.stripsExposed(self, other)) : 0;
                if (drop > TileDepths.FACE_PIXELS) {
                    assertEquals(drop, shown, self + " over " + other);
                }
            }
        }
    }
}
