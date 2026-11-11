package com.feuerschvenger.perlinsedge.domain.events;

/**
 * Framework-free keys the game reacts to. The input layer maps raw key codes to these values,
 * keys the game does not use are never delivered.
 */
public enum GameKey {
    ESCAPE,
    SPACE,
    I,
    B,
    C,
    /** Previous page of the building hotbar. */
    PAGE_UP,
    /** Next page of the building hotbar. */
    PAGE_DOWN,
    F1,
    F2,
    F3,
    F4,
    F5,
    /** Toggles the FPS overlay, which is independent of the debug mode. */
    F6,
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
