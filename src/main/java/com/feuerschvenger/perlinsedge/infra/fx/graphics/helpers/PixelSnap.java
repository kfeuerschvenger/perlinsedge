package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * The one rounding that places a sprite on whole screen pixels and the shadow that follows it. A tie rounds up, so
 * {@code round(x + k) == round(x) + k} for every whole number k: a camera that pans by whole pixels moves the sprite by
 * exactly that many pixels. {@link Math#rint} rounds a tie to the even neighbour, which makes the result depend on the
 * parity of the position, so a shadow cast once in world coordinates would stand a pixel off the sprite after a pan by
 * an odd number of pixels. The sprites and the anchors of their shadows both round through this class, so the two can
 * not drift apart.
 */
public final class PixelSnap {

    /** The fraction at which a tie, and everything above it, rounds up to the next whole pixel. */
    private static final double TIE = 0.5;

    /** Not instantiable: the class only holds a static rule. */
    private PixelSnap() {}

    /**
     * Rounds a screen coordinate to the nearest whole pixel, a tie going up.
     *
     * @param value the coordinate in screen pixels
     * @return the whole pixel
     */
    public static double round(double value) {
        // The fraction is exact, where value + 0.5 is not: the one value just under a half, 0.49999999999999994,
        // gets 1.0 from the sum, which rounds up to the next whole number, and so the nearest whole pixel is 1 instead of 0
        final double whole = Math.floor(value);
        return value - whole >= TIE ? whole + 1.0 : whole;
    }
}
