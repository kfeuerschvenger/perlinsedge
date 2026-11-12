package com.feuerschvenger.perlinsedge.infra.fx.input;

import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxKeyMapperTest {

    /** A key pressed with no modifier. */
    private static Optional<GameKey> key(KeyCode code) {
        return FxKeyMapper.toGameKey(code, false, false, false, false);
    }

    /** A key pressed with Ctrl only, or with no modifier. */
    private static Optional<GameKey> key(KeyCode code, boolean ctrlDown) {
        return FxKeyMapper.toGameKey(code, ctrlDown, false, false, false);
    }

    @Test
    void mapsGameplayKeys() {
        assertEquals(Optional.of(GameKey.ESCAPE), key(KeyCode.ESCAPE));
        assertEquals(Optional.of(GameKey.SPACE), key(KeyCode.SPACE));
        assertEquals(Optional.of(GameKey.I), key(KeyCode.I));
        assertEquals(Optional.of(GameKey.B), key(KeyCode.B));
        assertEquals(Optional.of(GameKey.C), key(KeyCode.C));
    }

    @Test
    void mapsDebugKeys() {
        assertEquals(Optional.of(GameKey.F1), key(KeyCode.F1));
        assertEquals(Optional.of(GameKey.F5), key(KeyCode.F5));
        assertEquals(Optional.of(GameKey.F6), key(KeyCode.F6));
    }

    @Test
    void mapsTheHotbarPageKeys() {
        assertEquals(Optional.of(GameKey.PAGE_UP), key(KeyCode.PAGE_UP));
        assertEquals(Optional.of(GameKey.PAGE_DOWN), key(KeyCode.PAGE_DOWN));
    }

    @Test
    void qThrowsOneItemAndCtrlQThrowsTheStack() {
        assertEquals(Optional.of(GameKey.DROP_ONE), key(KeyCode.Q));
        assertEquals(Optional.of(GameKey.DROP_ONE), key(KeyCode.Q, false));
        assertEquals(Optional.of(GameKey.DROP_STACK), key(KeyCode.Q, true));
    }

    @Test
    void qWithShiftAltOrMetaIsNotADropKey() {
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Q, false, true, false, false).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Q, false, false, true, false).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Q, false, false, false, true).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Q, true, true, false, false).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Q, true, false, true, false).isEmpty(), "AltGr is Ctrl and Alt");
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Q, true, false, false, true).isEmpty());
    }

    @Test
    void qAndCtrlQStillMapWithNoOtherModifier() {
        assertEquals(Optional.of(GameKey.DROP_ONE), FxKeyMapper.toGameKey(KeyCode.Q, false, false, false, false));
        assertEquals(Optional.of(GameKey.DROP_STACK), FxKeyMapper.toGameKey(KeyCode.Q, true, false, false, false));
    }

    @Test
    void eStaysUnmappedSoItIsFreeForOtherActions() {
        assertTrue(key(KeyCode.E).isEmpty());
        assertTrue(key(KeyCode.E, true).isEmpty());
    }

    @Test
    void ctrlChangesNothingForTheKeysOtherThanQ() {
        assertEquals(Optional.of(GameKey.I), key(KeyCode.I, true));
        assertEquals(Optional.of(GameKey.DIGIT_4), key(KeyCode.DIGIT4, true));
        assertTrue(key(KeyCode.Z, true).isEmpty());
        assertTrue(key(null, true).isEmpty());
    }

    @Test
    void mapsDigitsOneToNineWithTheirNumber() {
        for (int n = 1; n <= 9; n++) {
            GameKey key = key(KeyCode.valueOf("DIGIT" + n)).orElseThrow();

            assertTrue(key.isDigit());
            assertEquals(n, key.getDigit());
        }
    }

    @Test
    void ignoresKeysTheGameDoesNotUse() {
        assertTrue(key(KeyCode.DIGIT0).isEmpty());
        assertTrue(key(KeyCode.NUMPAD1).isEmpty());
        assertTrue(key(KeyCode.Z).isEmpty());
        assertTrue(key(null).isEmpty());
    }

    @Test
    void nonDigitKeysCarryNoNumber() {
        assertEquals(-1, GameKey.SPACE.getDigit());
    }
}
