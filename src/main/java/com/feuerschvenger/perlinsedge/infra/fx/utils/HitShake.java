package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * The short sideways shake of a resource that was just struck.
 * The offset is a pure function of the time since the hit: a few quick swings that fade out over a fraction of a second.
 */
public final class HitShake {

    /** Seconds the shake lasts. */
    public static final double DURATION_SECONDS = 0.15;

    /** Largest swing to either side, in art pixels: two screen pixels at the default zoom. */
    public static final double AMPLITUDE_ART_PIXELS = 1.0;

    /** Full left and right swings within the duration. */
    static final double SWINGS = 3.0;

    private HitShake() {}

    /**
     * Returns the horizontal offset of a resource that was struck some time ago.
     *
     * @param secondsSinceHit seconds since the last hit, infinite when it was never hit
     * @return the offset in art pixels, positive to the right, zero before the hit and after the shake ends
     */
    public static double offset(double secondsSinceHit) {
        return ShakeWave.offset(secondsSinceHit, DURATION_SECONDS, AMPLITUDE_ART_PIXELS, SWINGS);
    }

    /**
     * Returns the offset in screen pixels, snapped to whole pixels so the sprite stays crisp.
     *
     * @param secondsSinceHit seconds since the last hit, infinite when it was never hit
     * @param artScale        screen pixels per art pixel
     * @return the offset in screen pixels
     */
    public static double screenOffset(double secondsSinceHit, double artScale) {
        return ShakeWave.toScreen(offset(secondsSinceHit), artScale);
    }
}
