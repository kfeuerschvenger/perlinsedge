package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Pure geometry of the slime ground shadow, returned as plain numbers so a frame allocates nothing. The shadow is centred on the ground point, so its position never depends
 * on how high the slime is. Height only shrinks and fades it, the way a shadow thins out under something in the air.
 * The shadow pass casts it from the silhouette of the slime, narrowed by the width here, and the opacity here is the
 * share of the shadow opacity that is left.
 */
public final class SlimeShadow {

    /** Shadow width relative to the size it is derived from. */
    static final double WIDTH_FACTOR = 1.2;
    /** Fraction of the size lost at the top of a jump. */
    static final double MAX_SHRINK = 0.3;
    /** The shadow width factor is rounded to a multiple of one over this, so the shadow takes few distinct shapes. */
    static final int WIDTH_STEPS = 16;
    /** Share of the shadow opacity under a slime resting on the ground: all of it. */
    static final double BASE_OPACITY = 1.0;
    /** Fraction of the opacity lost at the top of a jump. */
    static final double MAX_FADE = 0.4;

    /** Not instantiable: the class only holds static rules. */
    private SlimeShadow() {}

    /**
     * Computes the width of the shadow ellipse for a slime at a height above the ground.
     * A negative height counts as the ground and a height past the maximum counts as the top of the jump.
     *
     * @param size      the size the shadow is proportioned to, in pixels
     * @param height    how far the slime is above the ground, in pixels
     * @param maxHeight the height at the top of a jump, in pixels, above zero
     * @return the ellipse width
     */
    public static double width(double size, double height, double maxHeight) {
        return size * WIDTH_FACTOR * (1.0 - MAX_SHRINK * lift(height, maxHeight));
    }

    /**
     * Computes the share of the shadow opacity for a slime at a height above the ground.
     * A negative height counts as the ground and a height past the maximum counts as the top of the jump.
     *
     * @param height    how far the slime is above the ground, in pixels
     * @param maxHeight the height at the top of a jump, in pixels, above zero
     * @return the opacity share, 0 to 1
     */
    public static double opacity(double height, double maxHeight) {
        return BASE_OPACITY * (1.0 - MAX_FADE * lift(height, maxHeight));
    }

    /**
     * Rounds the width factor of a shadow to a multiple of {@code 1 / WIDTH_STEPS}. The factor changes continuously
     * through a jump, and every distinct value is a different shape for the shadow layer to rasterise and keep, so a
     * jump only takes a handful of shapes instead of one per frame.
     *
     * @param factor the width of the shadow relative to its width at rest
     * @return the rounded factor
     */
    public static double quantiseFactor(double factor) {
        return Math.rint(factor * WIDTH_STEPS) / WIDTH_STEPS;
    }

    /** Returns how far into the jump the slime is, from zero on the ground to one at the top. */
    private static double lift(double height, double maxHeight) {
        return Math.max(0.0, Math.min(1.0, maxHeight > 0 ? height / maxHeight : 0.0));
    }
}
