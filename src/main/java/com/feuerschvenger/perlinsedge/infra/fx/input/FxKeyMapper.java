package com.feuerschvenger.perlinsedge.infra.fx.input;

import com.feuerschvenger.perlinsedge.domain.events.GameKey;
import javafx.scene.input.KeyCode;

import java.util.Optional;

/**
 * Maps JavaFX key codes to the framework-free {@link GameKey} values.
 */
public final class FxKeyMapper {

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
            case ESCAPE -> GameKey.ESCAPE;
            case SPACE -> GameKey.SPACE;
            case I -> GameKey.I;
            case F1 -> GameKey.F1;
            case F2 -> GameKey.F2;
            case F3 -> GameKey.F3;
            case F4 -> GameKey.F4;
            case F5 -> GameKey.F5;
            case F7 -> GameKey.F7;
            case DIGIT1 -> GameKey.DIGIT_1;
            case DIGIT2 -> GameKey.DIGIT_2;
            case DIGIT3 -> GameKey.DIGIT_3;
            case DIGIT4 -> GameKey.DIGIT_4;
            case DIGIT5 -> GameKey.DIGIT_5;
            case DIGIT6 -> GameKey.DIGIT_6;
            case DIGIT7 -> GameKey.DIGIT_7;
            case DIGIT8 -> GameKey.DIGIT_8;
            case DIGIT9 -> GameKey.DIGIT_9;
            default -> null;
        });
    }

    /** E uses the item in hand only on its own, so a chord such as Ctrl+E or AltGr+E is left to the system. */
    private static GameKey useKey(boolean ctrlDown, boolean shiftDown, boolean altDown, boolean metaDown) {
        return ctrlDown || shiftDown || altDown || metaDown ? null : GameKey.USE_ITEM;
    }

    private static GameKey dropKey(boolean ctrlDown, boolean shiftDown, boolean altDown, boolean metaDown) {
        if (shiftDown || altDown || metaDown) {
            return null;
        }
        return ctrlDown ? GameKey.DROP_STACK : GameKey.DROP_ONE;
    }
}
