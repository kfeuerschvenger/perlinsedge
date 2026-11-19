package com.feuerschvenger.perlinsedge.domain.utils;

/**
 * Deterministic mixing of integers into well spread 64 bit values, for the places that need an unrelated but
 * repeatable number for each input, such as a seed for a tile or a chance that does not draw from a shared random
 * source.
 */
public final class Hashes {

    /** Layout and constants of the splitmix64 finalizer. */
    private static final int HIGH_SHIFT = 32;
    private static final long LOW_MASK = 0xFFFF_FFFFL;
    private static final long GAMMA = 0x9E37_79B9_7F4A_7C15L;
    private static final long MIX_FIRST = 0xBF58_476D_1CE4_E5B9L;
    private static final long MIX_SECOND = 0x94D0_49BB_1331_11EBL;
    private static final int SHIFT_FIRST = 30;
    private static final int SHIFT_SECOND = 27;
    private static final int SHIFT_FINAL = 31;

    /** Not instantiable: the class only holds static helpers. */
    private Hashes() {
    }

    /**
     * Packs two integers into one 64 bit value and runs it through the splitmix64 finalizer, so the same pair always
     * gives the same value and pairs that differ by one give unrelated values.
     *
     * @param high the integer placed in the upper 32 bits
     * @param low  the integer placed in the lower 32 bits
     * @return the mixed value
     */
    public static long mix(int high, int low) {
        var z = (((long) high << HIGH_SHIFT) | (low & LOW_MASK)) + GAMMA;
        z = (z ^ (z >>> SHIFT_FIRST)) * MIX_FIRST;
        z = (z ^ (z >>> SHIFT_SECOND)) * MIX_SECOND;
        return z ^ (z >>> SHIFT_FINAL);
    }
}
