package com.feuerschvenger.perlinsedge.infra.fx.input;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyRepeatFilterTest {

    private final KeyRepeatFilter filter = new KeyRepeatFilter();

    @Test
    void deliversOnlyTheFirstPressWhileTheKeyIsHeld() {
        assertTrue(filter.firstPress(KeyCode.E));
        assertFalse(filter.firstPress(KeyCode.E));
        assertFalse(filter.firstPress(KeyCode.E));
    }

    @Test
    void deliversAgainAfterTheKeyIsReleased() {
        filter.firstPress(KeyCode.Q);
        filter.release(KeyCode.Q);

        assertTrue(filter.firstPress(KeyCode.Q));
    }

    @Test
    void tracksEachPhysicalKeyOnItsOwn() {
        assertTrue(filter.firstPress(KeyCode.E));
        assertTrue(filter.firstPress(KeyCode.Q));
        assertFalse(filter.firstPress(KeyCode.E));
    }

    @Test
    void clearForgetsEveryHeldKey() {
        filter.firstPress(KeyCode.E);
        filter.firstPress(KeyCode.DIGIT1);

        filter.clear();

        assertTrue(filter.firstPress(KeyCode.E));
        assertTrue(filter.firstPress(KeyCode.DIGIT1));
    }

    @Test
    void thePagingKeysRepeatWhileHeld() {
        assertTrue(filter.firstPress(KeyCode.PAGE_UP));
        assertTrue(filter.firstPress(KeyCode.PAGE_UP));
        assertTrue(filter.firstPress(KeyCode.PAGE_DOWN));
        assertTrue(filter.firstPress(KeyCode.PAGE_DOWN));
    }

    @Test
    void ignoresAMissingKeyCode() {
        assertFalse(filter.firstPress(null));
        filter.release(null);
    }
}
