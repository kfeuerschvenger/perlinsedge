package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * The decaying sine that every sideways shake is made of: a few quick swings whose size fades linearly to nothing.
 * The hit shake of a resource and the hurt shake of the player only differ in their numbers, so both use this one curve.
 */
final class ShakeWave {

    /** Not instantiable: the class only holds static rules. */
    private ShakeWave() {}

    /**
     * Returns the horizontal offset of a shake some time after it started.
     *
     * @param elapsedSeconds seconds since the shake started, infinite when it never did
     * @param durationSeconds seconds the shake lasts
     * @param amplitude       largest swing to either side
     * @param swings          full left and right swings within the duration
     * @return the offset in the unit of the amplitude, zero before the start and after the end
     */
    static double offset(double elapsedSeconds, double durationSeconds, double amplitude, double swings) {
        if (elapsedSeconds < 0 || elapsedSeconds >= durationSeconds) {
            return 0.0;
        }
        final double progress = elapsedSeconds / durationSeconds;
        return amplitude * (1.0 - progress) * Math.sin(2.0 * Math.PI * swings * progress);
    }

    /**
     * Snaps an offset in art pixels to whole screen pixels so the sprite stays crisp.
     *
     * @param artOffset the offset in art pixels
     * @param artScale  screen pixels per art pixel
     * @return the offset in screen pixels
     */
    static double toScreen(double artOffset, double artScale) {
        return artOffset == 0.0 ? 0.0 : Math.rint(artOffset * artScale);
    }
}
