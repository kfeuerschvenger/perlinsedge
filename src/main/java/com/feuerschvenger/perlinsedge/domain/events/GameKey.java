package com.feuerschvenger.perlinsedge.domain.events;

/**
 * Framework-free keys the game reacts to. The input layer maps raw key codes to these values,
 * keys the game does not use are never delivered.
 */
public enum GameKey {
    /** Escape: drops the selected building piece in build mode, otherwise opens the pause menu. */
    ESCAPE,
    /** Centres the camera on the player. */
    SPACE,
    I,
    /** R: turns the selected building piece a quarter turn clockwise while the hammer is in hand. */
    ROTATE,
    /** E: uses the item held in hand, such as a potion. */
    USE_ITEM,
    /** Q: throws one item out of the hovered or selected inventory slot. */
    DROP_ONE,
    /** Ctrl+Q: throws the whole stack out of the hovered or selected inventory slot. */
    DROP_STACK,
    F1,
    F2,
    F3,
    F4,
    /** Toggles the fast time of the day cycle, so the sun can be watched crossing the sky. */
    F5,
    DIGIT_1(1),
    DIGIT_2(2),
    DIGIT_3(3),
    DIGIT_4(4),
    DIGIT_5(5),
    DIGIT_6(6),
    DIGIT_7(7),
    DIGIT_8(8),
    DIGIT_9(9);

    private final int digit;

    GameKey() {
        this(-1);
    }

    GameKey(int digit) {
        this.digit = digit;
    }

    /**
     * Tells whether this key is a number key used for hotbar selection.
     *
     * @return true for DIGIT_1 to DIGIT_9
     */
    public boolean isDigit() {
        return digit > 0;
    }

    /**
     * Returns the number carried by a digit key.
     *
     * @return 1 to 9 for digit keys, or -1 for any other key
     */
    public int getDigit() {
        return digit;
    }
}
