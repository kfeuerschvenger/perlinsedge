package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Pure geometry of the slime ground shadow. The shadow is anchored to the ground point, so its position never depends
 * on how high the slime is. Height only shrinks and fades it, the way a shadow thins out under something in the air.
 */
public final class SlimeShadow {

    /** Shadow width relative to the size it is derived from. */
    static final double WIDTH_FACTOR = 1.2;
    /** Shadow height relative to the size it is derived from. */
    static final double HEIGHT_FACTOR = 0.8;
    /** Horizontal shift of the shadow, relative to the size, so the light reads as coming from the upper left. */
    static final double SIDE_SHIFT_FACTOR = 0.1;
    /** Drop of the ellipse centre below the ground point, relative to the shadow height. The sprite bottom covers the upper half. */
    static final double GROUND_DROP_FACTOR = 0.25;
    /** Fraction of the size lost at the top of a jump. */
    static final double MAX_SHRINK = 0.3;
    /** Opacity of a shadow under a slime resting on the ground. */
    static final double BASE_OPACITY = 0.8;
    /** Fraction of the opacity lost at the top of a jump. */
    static final double MAX_FADE = 0.25;

    private SlimeShadow() {}

    /**
     * An ellipse, as offsets from the ground point.
     *
     * @param width   ellipse width
     * @param height  ellipse height
     * @param centreX horizontal offset of the ellipse centre from the ground point
     * @param centreY vertical offset of the ellipse centre from the ground point, positive is down
     * @param opacity alpha, 0 to 1
     */
    public record Geometry(double width, double height, double centreX, double centreY, double opacity) {}

    /**
     * Computes the shadow for a slime at a height above the ground.
     * The centre offsets depend only on the size, so the shadow stays where the slime took off. A negative height counts
     * as the ground and a height past the maximum counts as the top of the jump.
     *
     * @param size      the size the shadow is proportioned to, in pixels
     * @param height    how far the slime is above the ground, in pixels
     * @param maxHeight the height at the top of a jump, in pixels, above zero
     * @return the shadow geometry
     */
    public static Geometry at(double size, double height, double maxHeight) {
        final double lift = clamp01(maxHeight > 0 ? height / maxHeight : 0.0);
        final double scale = 1.0 - MAX_SHRINK * lift;
        final double shadowHeight = size * HEIGHT_FACTOR;
        return new Geometry(
                size * WIDTH_FACTOR * scale,
                shadowHeight * scale,
                size * SIDE_SHIFT_FACTOR,
                shadowHeight * GROUND_DROP_FACTOR,
                BASE_OPACITY * (1.0 - MAX_FADE * lift));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
