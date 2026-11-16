package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FxShadowHelperTest {

    private static final double MARGIN = 96.0;
    private static final double BOTTOM = 20.0;

    @Test
    void shadowsInsideTheCanvasCanReachIt() {
        assertTrue(FxShadowHelper.canReach(400, 300, 800, 600, MARGIN, BOTTOM));
        assertTrue(FxShadowHelper.canReach(1, 1, 800, 600, MARGIN, BOTTOM));
    }

    @Test
    void aShadowJustOutsideTheCanvasStillReachesIt() {
        assertTrue(FxShadowHelper.canReach(-40, 300, 800, 600, MARGIN, BOTTOM));
        assertTrue(FxShadowHelper.canReach(400, 610, 800, 600, MARGIN, BOTTOM));
    }

    @Test
    void theReachMarginIsTheShadowLengthPlusHalfTheWidthInScreenPixels() {
        // A 48 high tree with a 16 half width, shadow 2 times its height, at art scale 2
        assertEquals((48 * 2.0 + 16) * 2, FxShadowHelper.reachMargin(48, 16, 2.0, 2));
        assertTrue(FxShadowHelper.reachMargin(48, 16, 2.0, 2) > FxShadowHelper.reachMargin(48, 16, 0.6, 2));
        assertTrue(FxShadowHelper.reachMargin(48, 16, 1.0, 4) > FxShadowHelper.reachMargin(48, 16, 1.0, 2));
    }

    @Test
    void theBottomMarginIsHalfTheHalfWidthPlusOneCellInScreenPixels() {
        assertEquals((16 / 2.0 + 1) * 2, FxShadowHelper.bottomMargin(16, 2));
        assertEquals((16 / 2.0 + 1) * 4, FxShadowHelper.bottomMargin(16, 4));
        assertTrue(FxShadowHelper.bottomMargin(16, 2) < FxShadowHelper.reachMargin(48, 16, 0.6, 2));
    }

    @Test
    void anOffScreenTreeCastsWhenItsShadowReachesTheViewAndNotWhenItIsTooFar() {
        final double margin = FxShadowHelper.reachMargin(48, 16, 2.0, 2);
        final double bottom = FxShadowHelper.bottomMargin(16, 2);

        // Just inside the margin to the left of an 800 wide canvas: a low morning sun throws it across the view
        assertTrue(FxShadowHelper.canReach(-margin + 1, 300, 800, 600, margin, bottom));
        assertTrue(FxShadowHelper.canReach(800 + margin - 1, 300, 800, 600, margin, bottom));
        // Above the canvas the shadow falls into it, so the whole margin counts
        assertTrue(FxShadowHelper.canReach(400, -margin + 1, 800, 600, margin, bottom));
        // Beyond it, nothing can reach
        assertFalse(FxShadowHelper.canReach(-margin - 1, 300, 800, 600, margin, bottom));
        assertFalse(FxShadowHelper.canReach(800 + margin + 1, 300, 800, 600, margin, bottom));
        assertFalse(FxShadowHelper.canReach(400, -margin - 1, 800, 600, margin, bottom));
        // The same tree is skipped when the sun is high and its shadow short
        final double highSun = FxShadowHelper.reachMargin(48, 16, 0.6, 2);
        assertFalse(FxShadowHelper.canReach(-margin + 1, 300, 800, 600, highSun, bottom));
    }

    @Test
    void aCasterBelowTheBottomEdgeIsOnlyCastWithinTheSmallBottomMargin() {
        final double margin = FxShadowHelper.reachMargin(48, 16, 2.0, 2);
        final double bottom = FxShadowHelper.bottomMargin(16, 2);

        assertTrue(FxShadowHelper.canReach(400, 600 + bottom - 1, 800, 600, margin, bottom));
        assertFalse(FxShadowHelper.canReach(400, 600 + bottom + 1, 800, 600, margin, bottom),
                "a shadow falls down the screen, so it cannot come back up from far below");
        assertFalse(FxShadowHelper.canReach(400, 600 + margin - 1, 800, 600, margin, bottom));
    }

    @Test
    void theRingBeyondTheBoundsIsThinnerOnTheSidesThatFaceTheBottomOfTheScreen() {
        final double halfTileHeight = 16;
        final double margin = FxShadowHelper.reachMargin(48, 16, 2.0, 2);
        final double bottom = FxShadowHelper.bottomMargin(16, 2);

        final int near = FxShadowHelper.nearRingTiles(margin, halfTileHeight);
        final int far = FxShadowHelper.farRingTiles(margin, bottom, halfTileHeight);

        assertEquals((int) Math.ceil(margin / halfTileHeight), near);
        assertTrue(far < near, "far " + far + " near " + near);
        assertTrue(far > 0);
    }

    @Test
    void shadowsFarOutsideTheCanvasAreSkipped() {
        assertFalse(FxShadowHelper.canReach(-500, 300, 800, 600, MARGIN, BOTTOM));
        assertFalse(FxShadowHelper.canReach(1400, 300, 800, 600, MARGIN, BOTTOM));
        assertFalse(FxShadowHelper.canReach(400, -500, 800, 600, MARGIN, BOTTOM));
        assertFalse(FxShadowHelper.canReach(400, 1400, 800, 600, MARGIN, BOTTOM));
    }
}
