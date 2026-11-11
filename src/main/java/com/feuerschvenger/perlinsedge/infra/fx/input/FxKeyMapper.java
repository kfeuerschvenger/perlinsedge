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
     * Translates a JavaFX key code pressed without a modifier.
     *
     * @param code the key code from a key event
     * @return the matching game key, or empty when the game does not use the key
     */
    public static Optional<GameKey> toGameKey(KeyCode code) {
        return toGameKey(code, false);
    }

    /**
     * Translates a JavaFX key code. Only Q reads the Ctrl state: it throws one item, and the whole stack with Ctrl.
     *
     * @param code     the key code from a key event
     * @param ctrlDown true when Ctrl is held
     * @return the matching game key, or empty when the game does not use the key
     */
    public static Optional<GameKey> toGameKey(KeyCode code, boolean ctrlDown) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(switch (code) {
            case Q -> ctrlDown ? GameKey.DROP_STACK : GameKey.DROP_ONE;
            case ESCAPE -> GameKey.ESCAPE;
            case SPACE -> GameKey.SPACE;
            case I -> GameKey.I;
            case B -> GameKey.B;
            case C -> GameKey.C;
            case PAGE_UP -> GameKey.PAGE_UP;
            case PAGE_DOWN -> GameKey.PAGE_DOWN;
            case F1 -> GameKey.F1;
            case F2 -> GameKey.F2;
            case F3 -> GameKey.F3;
            case F4 -> GameKey.F4;
            case F5 -> GameKey.F5;
            case F6 -> GameKey.F6;
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
}
