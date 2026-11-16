package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FxShadowHelperTest {

    @Test
    void shadowsInsideTheCanvasAreOnScreen() {
        assertTrue(FxShadowHelper.isOnScreen(400, 300, 800, 600));
        assertTrue(FxShadowHelper.isOnScreen(0, 0, 800, 600));
    }

    @Test
    void aShadowJustOutsideTheCanvasStillReachesIt() {
        assertTrue(FxShadowHelper.isOnScreen(-40, 300, 800, 600));
        assertTrue(FxShadowHelper.isOnScreen(400, 640, 800, 600));
    }

    @Test
    void theReachMarginIsTheShadowLengthPlusHalfTheWidthInScreenPixels() {
        // A 48 high tree with a 16 half width, shadow 2 times its height, at art scale 2
        assertEquals((48 * 2.0 + 16) * 2, FxShadowHelper.reachMargin(48, 16, 2.0, 2));
        assertTrue(FxShadowHelper.reachMargin(48, 16, 2.0, 2) > FxShadowHelper.reachMargin(48, 16, 0.6, 2));
        assertTrue(FxShadowHelper.reachMargin(48, 16, 1.0, 4) > FxShadowHelper.reachMargin(48, 16, 1.0, 2));
    }

    @Test
    void anOffScreenTreeCastsWhenItsShadowReachesTheViewAndNotWhenItIsTooFar() {
        final double margin = FxShadowHelper.reachMargin(48, 16, 2.0, 2);

        // Just inside the margin to the left of an 800 wide canvas: a low morning sun throws it across the view
        assertTrue(FxShadowHelper.canReach(-margin + 1, 300, 800, 600, margin));
        assertTrue(FxShadowHelper.canReach(800 + margin - 1, 300, 800, 600, margin));
        assertTrue(FxShadowHelper.canReach(400, 600 + margin - 1, 800, 600, margin));
        // Beyond it, nothing can reach
        assertFalse(FxShadowHelper.canReach(-margin - 1, 300, 800, 600, margin));
        assertFalse(FxShadowHelper.canReach(800 + margin + 1, 300, 800, 600, margin));
        assertFalse(FxShadowHelper.canReach(400, -margin - 1, 800, 600, margin));
        // The same tree is skipped when the sun is high and its shadow short
        final double highSun = FxShadowHelper.reachMargin(48, 16, 0.6, 2);
        assertFalse(FxShadowHelper.canReach(-margin + 1, 300, 800, 600, highSun));
    }

    @Test
    void shadowsFarOutsideTheCanvasAreSkipped() {
        assertFalse(FxShadowHelper.isOnScreen(-500, 300, 800, 600));
        assertFalse(FxShadowHelper.isOnScreen(1400, 300, 800, 600));
        assertFalse(FxShadowHelper.isOnScreen(400, -500, 800, 600));
        assertFalse(FxShadowHelper.isOnScreen(400, 1400, 800, 600));
    }
}
