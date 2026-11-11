package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScreenCullingTest {

    private static final double TILE_WIDTH = 64;
    private static final double TILE_HEIGHT = 32;
    private static final double ART_SCALE = 2;

    private static boolean visible(double screenY) {
        return ScreenCulling.isTileVisible(100, screenY, TILE_WIDTH, TILE_HEIGHT, 800, 600, ART_SCALE);
    }

    @Test
    void aTileInTheMiddleIsVisible() {
        assertTrue(visible(300));
    }

    @Test
    void aTileFarAboveOrBelowTheCanvasIsCulled() {
        assertFalse(visible(-1000));
        assertFalse(visible(2000));
    }

    @Test
    void aDeepBedWhoseLandTopIsAboveTheEdgeStillReachesIntoView() {
        // The deep bed is drawn 20 art pixels below the land top, plus the block of 21: 82 screen pixels at this scale
        double landTop = -(TILE_HEIGHT * 2) - 10;
        assertTrue(landTop + TILE_HEIGHT * 2 < 0, "the old test culled this tile");
        assertTrue(visible(landTop));
    }

    @Test
    void theMarginEndsWhereTheBedCanNoLongerBeSeen() {
        double blockBottom = (ScreenCulling.MAX_SINK_ART_PIXELS + 21) * ART_SCALE;
        assertTrue(visible(-blockBottom + 1));
        assertFalse(visible(-(TILE_HEIGHT * 2 + ScreenCulling.MAX_SINK_ART_PIXELS * ART_SCALE) - 1));
    }
}
