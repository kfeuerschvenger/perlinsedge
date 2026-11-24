package com.feuerschvenger.perlinsedge.infra.fx.input;

import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import javafx.scene.input.KeyCode;

import java.util.Map;
import java.util.Optional;

/**
 * Maps JavaFX key codes to the framework-free {@link GameKey} values.
 */
public final class FxKeyMapper {

    /** The keys that map to one game key whatever modifiers are held. */
    private static final Map<KeyCode, GameKey> PLAIN_KEYS = Map.ofEntries(
            Map.entry(KeyCode.ESCAPE, GameKey.ESCAPE),
            Map.entry(KeyCode.SPACE, GameKey.SPACE),
            Map.entry(KeyCode.I, GameKey.I),
            Map.entry(KeyCode.R, GameKey.ROTATE),
            Map.entry(KeyCode.F1, GameKey.F1),
            Map.entry(KeyCode.F2, GameKey.F2),
            Map.entry(KeyCode.F3, GameKey.F3),
            Map.entry(KeyCode.F4, GameKey.F4),
            Map.entry(KeyCode.F5, GameKey.F5),
            Map.entry(KeyCode.DIGIT1, GameKey.DIGIT_1),
            Map.entry(KeyCode.DIGIT2, GameKey.DIGIT_2),
            Map.entry(KeyCode.DIGIT3, GameKey.DIGIT_3),
            Map.entry(KeyCode.DIGIT4, GameKey.DIGIT_4),
            Map.entry(KeyCode.DIGIT5, GameKey.DIGIT_5),
            Map.entry(KeyCode.DIGIT6, GameKey.DIGIT_6),
            Map.entry(KeyCode.DIGIT7, GameKey.DIGIT_7),
            Map.entry(KeyCode.DIGIT8, GameKey.DIGIT_8),
            Map.entry(KeyCode.DIGIT9, GameKey.DIGIT_9));

    private FxKeyMapper() {
    }

    /**
     * Translates a JavaFX key code. Only Q and E read the modifiers. Q throws one item alone, and the whole stack with
     * Ctrl alone. Q with Shift, Alt or Meta, and Ctrl combined with any of them (AltGr reports Ctrl and Alt), is not a
     * drop key and maps to nothing. E uses the item in hand and only without any modifier.
     *
     * @param code      the key code from a key event
     * @param ctrlDown  true when Ctrl is held
     * @param shiftDown true when Shift is held
     * @param altDown   true when Alt is held
     * @param metaDown  true when Meta is held
     * @return the matching game key, or empty when the game does not use the key
     */
    public static Optional<GameKey> toGameKey(KeyCode code, boolean ctrlDown, boolean shiftDown, boolean altDown,
                                              boolean metaDown) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(switch (code) {
            case Q -> dropKey(ctrlDown, shiftDown, altDown, metaDown);
            case E -> useKey(ctrlDown, shiftDown, altDown, metaDown);
            default -> PLAIN_KEYS.get(code);
        });
    }

    /** E uses the item in hand only on its own, so a chord such as Ctrl+E or AltGr+E is left to the system. */
    private static GameKey useKey(boolean ctrlDown, boolean shiftDown, boolean altDown, boolean metaDown) {
        final boolean anyModifier = ctrlDown || shiftDown || altDown || metaDown;
        return anyModifier ? null : GameKey.USE_ITEM;
    }

    private static GameKey dropKey(boolean ctrlDown, boolean shiftDown, boolean altDown, boolean metaDown) {
        if (shiftDown || altDown || metaDown) {
            return null;
        }
        return ctrlDown ? GameKey.DROP_STACK : GameKey.DROP_ONE;
    }
}
