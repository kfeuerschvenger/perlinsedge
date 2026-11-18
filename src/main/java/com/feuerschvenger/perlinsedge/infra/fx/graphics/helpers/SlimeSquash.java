package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Pure rules for how a slime that stands still is stretched, returned as plain numbers so a frame allocates nothing.
 * A resting slime breathes with a gentle squash and stretch, and a slime about to strike squashes down and spreads
 * wider, more and more until the strike. The sprite keeps its feet on the ground point, so the ground shadow does not
 * move with either.
 */
public final class SlimeSquash {

    /** Seconds one breath takes. */
    public static final double BREATH_PERIOD_SECONDS = 2.4;
    /** Share of the height a breath adds or removes, which the width does the other way round. */
    public static final double BREATH_AMPLITUDE = 0.06;
    /** Share of the height lost at the moment of the strike. */
    public static final double WINDUP_SQUASH = 0.3;
    /** Share of the width gained at the moment of the strike. */
    public static final double WINDUP_SPREAD = 0.2;
    /** Full turns of the sine wave in one breath, written as the radians of a full circle. */
    private static final double FULL_CIRCLE = 2 * Math.PI;

    /** Not instantiable: the class only holds static rules. */
    private SlimeSquash() {}

    /**
     * Gives the breath of a slime at a time.
     *
     * @param clock  the game clock in seconds
     * @param offset where in the breath this slime is at clock zero, in breaths, so slimes do not breathe in step
     * @return -1 to 1, positive when the slime is drawn in tall and thin
     */
    public static double breath(double clock, double offset) {
        return Math.sin(FULL_CIRCLE * (clock / BREATH_PERIOD_SECONDS + offset));
    }

    /**
     * Gives the height factor of a slime standing still.
     *
     * @param windup how far the squash before a strike has gone, 0 when there is none, up to 1
     * @param breath the breath, from {@link #breath(double, double)}
     * @return the factor to multiply the sprite height by
     */
    public static double height(double windup, double breath) {
        if (windup > 0) {
            return 1.0 - WINDUP_SQUASH * Math.min(1.0, windup);
        }
        return 1.0 + BREATH_AMPLITUDE * breath;
    }

    /**
     * Gives the width factor of a slime standing still.
     *
     * @param windup how far the squash before a strike has gone, 0 when there is none, up to 1
     * @param breath the breath, from {@link #breath(double, double)}
     * @return the factor to multiply the sprite width by
     */
    public static double width(double windup, double breath) {
        if (windup > 0) {
            return 1.0 + WINDUP_SPREAD * Math.min(1.0, windup);
        }
        return 1.0 - BREATH_AMPLITUDE * breath;
    }
}
