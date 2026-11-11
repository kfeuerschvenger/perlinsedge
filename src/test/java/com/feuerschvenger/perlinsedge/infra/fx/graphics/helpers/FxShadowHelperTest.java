package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

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
    void shadowsFarOutsideTheCanvasAreSkipped() {
        assertFalse(FxShadowHelper.isOnScreen(-500, 300, 800, 600));
        assertFalse(FxShadowHelper.isOnScreen(1400, 300, 800, 600));
        assertFalse(FxShadowHelper.isOnScreen(400, -500, 800, 600));
        assertFalse(FxShadowHelper.isOnScreen(400, 1400, 800, 600));
    }
}
