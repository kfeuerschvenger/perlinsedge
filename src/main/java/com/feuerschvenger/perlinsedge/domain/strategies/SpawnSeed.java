package com.feuerschvenger.perlinsedge.domain.strategies;

/**
 * Turns the tile an enemy spawned on into a random seed, so the timing of its movement is the same on every run and
 * enemies on neighbouring tiles get unrelated sequences.
 */
public final class SpawnSeed {

    /** Layout and constants of the splitmix64 finalizer that turns a spawn tile into a seed. */
    private static final int X_SHIFT = 32;
    private static final long LOW_MASK = 0xFFFF_FFFFL;
    private static final long GAMMA = 0x9E37_79B9_7F4A_7C15L;
    private static final long MIX_FIRST = 0xBF58_476D_1CE4_E5B9L;
    private static final long MIX_SECOND = 0x94D0_49BB_1331_11EBL;
    private static final int SHIFT_FIRST = 30;
    private static final int SHIFT_SECOND = 27;
    private static final int SHIFT_FINAL = 31;

    private SpawnSeed() {
    }

    /**
     * Packs the tile and runs it through the splitmix64 finalizer, so the same tile always gives the same seed and
     * tiles that differ by one coordinate give unrelated seeds.
     *
     * @param tileX column of the spawn tile
     * @param tileY row of the spawn tile
     * @return the seed
     */
    public static long of(int tileX, int tileY) {
        var z = (((long) tileX << X_SHIFT) | (tileY & LOW_MASK)) + GAMMA;
        z = (z ^ (z >>> SHIFT_FIRST)) * MIX_FIRST;
        z = (z ^ (z >>> SHIFT_SECOND)) * MIX_SECOND;
        return z ^ (z >>> SHIFT_FINAL);
    }
}
