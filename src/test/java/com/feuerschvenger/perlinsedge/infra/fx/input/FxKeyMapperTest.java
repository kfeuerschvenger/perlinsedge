package com.feuerschvenger.perlinsedge.infra.fx.input;

import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxKeyMapperTest {

    @Test
    void mapsGameplayKeys() {
        assertEquals(Optional.of(GameKey.ESCAPE), FxKeyMapper.toGameKey(KeyCode.ESCAPE));
        assertEquals(Optional.of(GameKey.SPACE), FxKeyMapper.toGameKey(KeyCode.SPACE));
        assertEquals(Optional.of(GameKey.I), FxKeyMapper.toGameKey(KeyCode.I));
        assertEquals(Optional.of(GameKey.B), FxKeyMapper.toGameKey(KeyCode.B));
        assertEquals(Optional.of(GameKey.C), FxKeyMapper.toGameKey(KeyCode.C));
    }

    @Test
    void mapsDebugKeys() {
        assertEquals(Optional.of(GameKey.F1), FxKeyMapper.toGameKey(KeyCode.F1));
        assertEquals(Optional.of(GameKey.F5), FxKeyMapper.toGameKey(KeyCode.F5));
        assertEquals(Optional.of(GameKey.F6), FxKeyMapper.toGameKey(KeyCode.F6));
    }

    @Test
    void mapsTheHotbarPageKeys() {
        assertEquals(Optional.of(GameKey.Q), FxKeyMapper.toGameKey(KeyCode.Q));
        assertEquals(Optional.of(GameKey.E), FxKeyMapper.toGameKey(KeyCode.E));
    }

    @Test
    void mapsDigitsOneToNineWithTheirNumber() {
        for (int n = 1; n <= 9; n++) {
            GameKey key = FxKeyMapper.toGameKey(KeyCode.valueOf("DIGIT" + n)).orElseThrow();

            assertTrue(key.isDigit());
            assertEquals(n, key.getDigit());
        }
    }

    @Test
    void ignoresKeysTheGameDoesNotUse() {
        assertTrue(FxKeyMapper.toGameKey(KeyCode.DIGIT0).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(KeyCode.NUMPAD1).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Z).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(null).isEmpty());
    }

    @Test
    void nonDigitKeysCarryNoNumber() {
        assertEquals(-1, GameKey.SPACE.getDigit());
    }
}
