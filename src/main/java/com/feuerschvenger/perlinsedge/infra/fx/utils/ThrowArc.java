package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * The height of a thrown item above the ground during its flight: a parabola that is zero at the throw and at the
 * landing and highest halfway. The item slides along the ground tiles while this lifts it on the screen.
 */
public final class ThrowArc {
    /** Height of the top of the arc, in screen pixels at zoom 1. */
    public static final double PEAK_PIXELS = 18.0;

    private ThrowArc() {}

    /**
     * Fraction of the peak height reached at a point of the flight.
     *
     * @param progress flight progress, 0 at the throw and 1 at the landing, values outside are clamped
     * @return 0 at both ends and 1 halfway
     */
    public static double heightFraction(double progress) {
        double p = Math.max(0.0, Math.min(1.0, progress));
        return 4.0 * p * (1.0 - p);
    }

    /**
     * Lift of an item above its path on the screen.
     *
     * @param progress flight progress, 0 at the throw and 1 at the landing
     * @param zoom     current camera zoom
     * @return pixels to draw the item higher by, never negative
     */
    public static double liftPixels(double progress, double zoom) {
        return PEAK_PIXELS * zoom * heightFraction(progress);
    }
}
