package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Pure geometry of the slime ground shadow. The shadow is centred on the ground point, so its position never depends
 * on how high the slime is. Height only shrinks and fades it, the way a shadow thins out under something in the air.
 * The ground shadow pass draws it from a pre-baked ellipse that already carries the shadow opacity, so the opacity
 * here is the share of it that is left.
 */
public final class SlimeShadow {

    /** Shadow width relative to the size it is derived from. */
    static final double WIDTH_FACTOR = 1.2;
    /** Fraction of the size lost at the top of a jump. */
    static final double MAX_SHRINK = 0.3;
    /** Share of the baked shadow opacity under a slime resting on the ground. */
    static final double BASE_OPACITY = 1.0;
    /** Fraction of the opacity lost at the top of a jump. */
    static final double MAX_FADE = 0.4;

    private SlimeShadow() {}

    /**
     * A shadow ellipse centred on the ground point.
     *
     * @param width   ellipse width
     * @param opacity share of the baked shadow opacity, 0 to 1
     */
    public record Geometry(double width, double opacity) {}

    /**
     * Computes the shadow for a slime at a height above the ground.
     * A negative height counts as the ground and a height past the maximum counts as the top of the jump.
     *
     * @param size      the size the shadow is proportioned to, in pixels
     * @param height    how far the slime is above the ground, in pixels
     * @param maxHeight the height at the top of a jump, in pixels, above zero
     * @return the shadow geometry
     */
    public static Geometry at(double size, double height, double maxHeight) {
        final double lift = clamp01(maxHeight > 0 ? height / maxHeight : 0.0);
        return new Geometry(
                size * WIDTH_FACTOR * (1.0 - MAX_SHRINK * lift),
                BASE_OPACITY * (1.0 - MAX_FADE * lift));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
