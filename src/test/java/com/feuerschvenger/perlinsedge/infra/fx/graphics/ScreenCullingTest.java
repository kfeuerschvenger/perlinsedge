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
        // The deep bed is drawn the deepest sink below the land top, so the tile reaches into view from further up
        double landTop = -(TILE_HEIGHT * 2) - 10;
        assertTrue(landTop + TILE_HEIGHT * 2 < 0, "a margin of the tile height alone would cull this tile");
        assertTrue(visible(landTop));
    }

    @Test
    void theMarginEndsWhereTheBedCanNoLongerBeSeen() {
        double margin = TILE_HEIGHT * 2 + ScreenCulling.MAX_SINK_ART_PIXELS * ART_SCALE;
        assertTrue(visible(-margin + 1));
        assertFalse(visible(-margin));
        assertFalse(visible(-margin - 1));
    }
}
