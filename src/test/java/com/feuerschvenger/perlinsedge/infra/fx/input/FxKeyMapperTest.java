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
        assertEquals(Optional.of(GameKey.PAGE_UP), FxKeyMapper.toGameKey(KeyCode.PAGE_UP));
        assertEquals(Optional.of(GameKey.PAGE_DOWN), FxKeyMapper.toGameKey(KeyCode.PAGE_DOWN));
    }

    @Test
    void leavesQAndEUnmappedSoTheyStayFree() {
        assertTrue(FxKeyMapper.toGameKey(KeyCode.Q).isEmpty());
        assertTrue(FxKeyMapper.toGameKey(KeyCode.E).isEmpty());
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
