package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.ShadowSprites;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Draws the ground shadow pass: every shadow of the frame as a pre-baked ellipse at the integer art scale, centred on
 * its ground contact point and snapped to whole pixels. Nothing is allocated per shadow.
 */
public final class FxShadowHelper {

    /** Shadows whose centre is this far outside the canvas, in pixels, are skipped. */
    private static final double MARGIN = 96.0;

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

    /**
     * Draws the shadows of a frame.
     *
     * @param gc      the graphics context, with image smoothing off
     * @param shadows the shadows to draw
     * @param zoom    the camera zoom
     */
    public static void draw(GraphicsContext gc, ShadowList shadows, double zoom) {
        final double scale = ZoomSteps.artScale(zoom);
        double alpha = 1.0;
        for (int i = 0, count = shadows.size(); i < count; i++) {
            final Image ellipse = ShadowSprites.get(GroundShadow.bucketFor(shadows.widthArt(i)));
            final double width = ellipse.getWidth() * scale;
            final double height = ellipse.getHeight() * scale;
            final double fade = shadows.fade(i);
            if (fade != alpha) {
                gc.setGlobalAlpha(fade);
                alpha = fade;
            }
            gc.drawImage(ellipse, Math.rint(shadows.centreX(i) - width / 2), Math.rint(shadows.centreY(i) - height / 2),
                    width, height);
        }
        if (alpha != 1.0) {
            gc.setGlobalAlpha(1.0);
        }
    }
}
