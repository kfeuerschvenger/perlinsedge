package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Decides which shadows are worth casting. The drawing itself is done by {@link FxShadowPass}.
 */
public final class FxShadowHelper {

    /**
     * The share of full opacity below which a shadow or a sky tint cannot be seen, so nothing is cast or laid over the
     * world for it.
     */
    public static final double MIN_VISIBLE_STRENGTH = 0.004;

    /** Not instantiable: the class only holds static rules. */
    private FxShadowHelper() {}

    /**
     * Returns how far outside the canvas a caster can stand and still throw its shadow into it. A shadow reaches the
     * height of the caster times the length factor, and the width of the caster adds to it sideways.
     *
     * @param heightArt     height in art pixels of the tallest caster
     * @param halfWidthArt  half the width in art pixels of the widest caster
     * @param lengthFactor  how long shadows are as a multiple of the height of what casts them
     * @param artScale      screen pixels per art pixel
     * @return the margin in screen pixels
     */
    public static double reachMargin(double heightArt, double halfWidthArt, double lengthFactor, double artScale) {
        return (heightArt * lengthFactor + halfWidthArt) * artScale;
    }

    /**
     * Tells whether a caster standing on a point can throw a shadow into the canvas.
     *
     * @param x            screen X of the ground contact point
     * @param y            screen Y of the ground contact point
     * @param canvasWidth  canvas width
     * @param canvasHeight canvas height
     * @param margin       how far outside the canvas a caster can stand, see {@link #reachMargin}
     * @return true when the shadow may reach the canvas
     */
    public static boolean canReach(double x, double y, double canvasWidth, double canvasHeight, double margin) {
        return x > -margin && x < canvasWidth + margin && y > -margin && y < canvasHeight + margin;
    }
}
