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

    /** Not instantiable: the class only holds a static rule. */
    private PixelSnap() {}

    /**
     * Rounds a screen coordinate to the nearest whole pixel, a tie going up.
     *
     * @param value the coordinate in screen pixels
     * @return the whole pixel
     */
    public static double round(double value) {
        return Math.floor(value + 0.5);
    }
}
