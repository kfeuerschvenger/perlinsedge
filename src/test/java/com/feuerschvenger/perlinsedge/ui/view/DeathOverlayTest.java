package com.feuerschvenger.perlinsedge.ui.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

class DeathOverlayTest {

    private static boolean visibleAfter(boolean dead, double timer) {
        return FxToolkitSupport.onFx(() -> {
            DeathOverlay overlay = new DeathOverlay();
            overlay.update(dead, timer);
            return overlay.isVisible();
        });
    }

    @Test
    void theOverlayStartsHidden() {
        assertFalse(FxToolkitSupport.onFx(() -> new DeathOverlay().isVisible()));
    }

    @Test
    void theOverlayIsShownWhileThePlayerIsDead() {
        assertTrue(visibleAfter(true, 3.0));
    }

    @Test
    void theOverlayIsHiddenWhenThePlayerRespawns() {
        assertFalse(FxToolkitSupport.onFx(() -> {
            DeathOverlay overlay = new DeathOverlay();
            overlay.update(true, 3.0);
            overlay.update(false, 0.0);
            return overlay.isVisible();
        }));
    }

    @Test
    void leavingTheGameHidesTheOverlayAndTheNextDeathShowsTheCountdownAgain() {
        String text = FxToolkitSupport.onFx(() -> {
            DeathOverlay overlay = new DeathOverlay();
            overlay.update(true, 3.0);
            overlay.hide();
            assertFalse(overlay.isVisible());
            overlay.update(true, 3.0);
            assertTrue(overlay.isVisible());
            return ((Label) overlay.getChildren().get(1)).getText();
        });
        assertFalse(text.isEmpty());
        assertEquals(text, text.toUpperCase());
    }
}
