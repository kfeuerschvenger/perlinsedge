package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * The timing of the reaction to the player being hit: the sprite flashes between red and white, shakes sideways, and a faint
 * red glow fades in the screen edges.
 * Every value is a pure function of the time since the hit, so it needs no state of its own and can be tested without a
 * window. The flash and the shake are quick and end together; the glow lingers a little longer and starts strongest.
 */
public final class HurtReaction {

    /** No tint: the sprite is drawn as it is. */
    public static final int NO_FLASH = 0;

    /** The red half of the flash. */
    public static final int RED = 1;

    /** The white half of the flash. */
    public static final int WHITE = 2;

    /** Seconds the sprite flashes and shakes. */
    public static final double FLASH_SECONDS = 0.18;

    /** Seconds each colour of the flash lasts before the other takes over. */
    static final double PHASE_SECONDS = 0.045;

    /** Largest swing of the shake to either side, in art pixels. */
    public static final double SHAKE_AMPLITUDE_ART_PIXELS = 1.0;

    /** Full left and right swings of the shake within its duration. */
    static final double SWINGS = 3.0;

    /** Seconds the red glow at the screen edges takes to fade out. */
    public static final double GLOW_SECONDS = 0.35;

    /** Opacity of the glow at its strongest, kept low so it stays a hint. */
    public static final double GLOW_MAX_OPACITY = 0.30;

    /** Share of the flash colour mixed into each painted pixel of the sprite. */
    static final double TINT_SHARE = 0.7;

    private static final int RED_TARGET = 0xFFFF2828;
    private static final int WHITE_TARGET = 0xFFFFFFFF;

    private HurtReaction() {}

    /**
     * Returns which colour the sprite is tinted with.
     *
     * @param secondsSinceHurt seconds since the last hit, infinite when the player was never hit
     * @return {@link #RED}, {@link #WHITE}, or {@link #NO_FLASH} before the hit and after the flash ends
     */
    public static int flashPhase(double secondsSinceHurt) {
        if (secondsSinceHurt < 0 || secondsSinceHurt >= FLASH_SECONDS) {
            return NO_FLASH;
        }
        return ((int) (secondsSinceHurt / PHASE_SECONDS)) % 2 == 0 ? RED : WHITE;
    }

    /**
     * Returns the horizontal offset of the shaking sprite.
     *
     * @param secondsSinceHurt seconds since the last hit, infinite when the player was never hit
     * @return the offset in art pixels, positive to the right, zero before the hit and after the shake ends
     */
    public static double shakeOffset(double secondsSinceHurt) {
        if (secondsSinceHurt < 0 || secondsSinceHurt >= FLASH_SECONDS) {
            return 0.0;
        }
        final double progress = secondsSinceHurt / FLASH_SECONDS;
        return SHAKE_AMPLITUDE_ART_PIXELS * (1.0 - progress) * Math.sin(2.0 * Math.PI * SWINGS * progress);
    }

    /**
     * Returns the shake offset in screen pixels, snapped to whole pixels so the sprite stays crisp.
     *
     * @param secondsSinceHurt seconds since the last hit, infinite when the player was never hit
     * @param artScale         screen pixels per art pixel
     * @return the offset in screen pixels
     */
    public static double shakeScreenOffset(double secondsSinceHurt, double artScale) {
        final double art = shakeOffset(secondsSinceHurt);
        return art == 0.0 ? 0.0 : Math.rint(art * artScale);
    }

    /**
     * Returns the opacity of the red glow at the screen edges.
     *
     * @param secondsSinceHurt seconds since the last hit, infinite when the player was never hit
     * @return an opacity from zero to {@link #GLOW_MAX_OPACITY}, strongest at the hit and fading smoothly to zero
     */
    public static double glowOpacity(double secondsSinceHurt) {
        if (secondsSinceHurt < 0 || secondsSinceHurt >= GLOW_SECONDS) {
            return 0.0;
        }
        final double remaining = 1.0 - secondsSinceHurt / GLOW_SECONDS;
        return GLOW_MAX_OPACITY * remaining * remaining;
    }

    /**
     * Mixes a flash colour into a painted pixel, keeping its transparency.
     *
     * @param argb  the packed pixel
     * @param phase {@link #RED} or {@link #WHITE}; any other value leaves the pixel as it is
     * @return the packed pixel, unchanged when it is fully transparent
     */
    public static int tint(int argb, int phase) {
        final int target = phase == RED ? RED_TARGET : phase == WHITE ? WHITE_TARGET : argb;
        final int alpha = argb >>> 24;
        if (alpha == 0 || phase != RED && phase != WHITE) {
            return argb;
        }
        final int r = mix((argb >> 16) & 0xFF, (target >> 16) & 0xFF);
        final int g = mix((argb >> 8) & 0xFF, (target >> 8) & 0xFF);
        final int b = mix(argb & 0xFF, target & 0xFF);
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    private static int mix(int from, int to) {
        return (int) Math.round(from + (to - from) * TINT_SHARE);
    }
}
