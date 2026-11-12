package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorStyleTest {

    @Test
    void lightGroundsNeedADarkCursor() {
        for (String art : new String[]{"sand", "dune", "snow", "ice", "savanna"}) {
            assertTrue(CursorStyle.isLightGround(art), art);
        }
    }

    @Test
    void darkGroundsKeepTheLightCursor() {
        for (String art : new String[]{"grass", "forest", "hill", "mountain", "swamp", "bed", "bed-deep"}) {
            assertFalse(CursorStyle.isLightGround(art), art);
        }
        assertFalse(CursorStyle.isLightGround(null));
    }

    @Test
    void theArtOfEveryTileTypeOnEveryBiomeHasAnAnswer() {
        for (MapType mapType : MapType.values()) {
            for (TileType type : TileType.values()) {
                // Must not throw, and water never counts as light
                boolean light = CursorStyle.isLightGround(AssetPaths.tileArt(mapType, type));
                if (type.isWater() && type != TileType.ICE) {
                    assertFalse(light, mapType + " " + type);
                }
            }
        }
    }

    @Test
    void sandAndSnowGroundsAreLightOnTheBiomesThatDrawThem() {
        assertTrue(CursorStyle.isLightGround(AssetPaths.tileArt(MapType.DESERT, TileType.GRASS)), "dunes in the desert");
        assertTrue(CursorStyle.isLightGround(AssetPaths.tileArt(MapType.TUNDRA, TileType.GRASS)), "snow in the tundra");
        assertTrue(CursorStyle.isLightGround(AssetPaths.tileArt(null, TileType.BEACH)));
        assertFalse(CursorStyle.isLightGround(AssetPaths.tileArt(null, TileType.GRASS)));
    }

    @Test
    void theBreathStaysBetweenItsLowAndFullOpacity() {
        for (double t = 0; t < 10; t += 0.013) {
            double alpha = CursorStyle.breathAlpha(t);
            assertTrue(alpha >= CursorStyle.BREATH_MIN_ALPHA - 1e-9 && alpha <= 1 + 1e-9, "alpha " + alpha + " at " + t);
        }
    }

    @Test
    void theBreathRepeatsEveryPeriod() {
        assertEquals(CursorStyle.breathAlpha(0.4), CursorStyle.breathAlpha(0.4 + CursorStyle.BREATH_PERIOD_SECONDS), 1e-9);
        assertEquals(CursorStyle.breathAlpha(3.1), CursorStyle.breathAlpha(3.1 + 5 * CursorStyle.BREATH_PERIOD_SECONDS), 1e-9);
    }

    @Test
    void theBreathReachesBothEnds() {
        double low = Double.MAX_VALUE;
        double high = 0;
        for (double t = 0; t < CursorStyle.BREATH_PERIOD_SECONDS; t += 0.005) {
            low = Math.min(low, CursorStyle.breathAlpha(t));
            high = Math.max(high, CursorStyle.breathAlpha(t));
        }
        assertEquals(CursorStyle.BREATH_MIN_ALPHA, low, 1e-3);
        assertEquals(1.0, high, 1e-3);
    }

    @Test
    void theFlashFadesFromFullOpacity() {
        assertEquals(1.0, CursorStyle.flashAlpha(1.0), 1e-9);
        assertEquals(CursorStyle.FLASH_MIN_ALPHA, CursorStyle.flashAlpha(0.0), 1e-9);
        assertEquals(CursorStyle.FLASH_MIN_ALPHA, CursorStyle.flashAlpha(-3), 1e-9, "a strength below zero is clamped");
        assertEquals(1.0, CursorStyle.flashAlpha(7), 1e-9, "a strength above one is clamped");
    }
}
