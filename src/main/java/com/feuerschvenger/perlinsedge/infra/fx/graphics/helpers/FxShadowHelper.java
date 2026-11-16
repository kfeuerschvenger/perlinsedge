package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Decides which shadows are worth casting. The drawing itself is done by {@link FxShadowPass}.
 */
public final class FxShadowHelper {

    /** Shadows whose centre is this far outside the canvas, in pixels, are skipped. */
    private static final double MARGIN = 96.0;

    /** Not instantiable: the class only holds static rules. */
    private FxShadowHelper() {}

    /**
     * Tells whether a shadow centred on a point can reach the canvas.
     *
     * @param x            screen X of the shadow centre
     * @param y            screen Y of the shadow centre
     * @param canvasWidth  canvas width
     * @param canvasHeight canvas height
     * @return true when the shadow may be on screen
     */
    public static boolean isOnScreen(double x, double y, double canvasWidth, double canvasHeight) {
        return x > -MARGIN && x < canvasWidth + MARGIN && y > -MARGIN && y < canvasHeight + MARGIN;
    }
}
