package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Draws the faint red glow along the screen edges that follows a hit on the player.
 * The gradient is built once for a canvas size and its opacity is applied through the global alpha, so a frame of the
 * effect allocates nothing.
 */
public final class FxHurtGlow {

    /** Share of the distance from the centre to the corner where the glow starts. */
    private static final double INNER_RADIUS_SHARE = 0.55;

    private static final Color GLOW_COLOUR = Color.rgb(255, 30, 30, 1.0);

    private static RadialGradient gradient;
    private static double gradientWidth;
    private static double gradientHeight;

    /** Not instantiable: the class only holds static drawing code. */
    private FxHurtGlow() {}

    /**
     * Draws the glow over the whole canvas.
     *
     * @param gc      the graphics context
     * @param width   canvas width
     * @param height  canvas height
     * @param opacity glow opacity, see {@code HurtReaction.glowOpacity}; nothing is drawn at zero
     */
    public static void draw(GraphicsContext gc, double width, double height, double opacity) {
        if (opacity <= 0.0) {
            return;
        }
        if (gradient == null || gradientWidth != width || gradientHeight != height) {
            final double radius = Math.hypot(width, height) / 2.0;
            gradient = new RadialGradient(0, 0, width / 2.0, height / 2.0, radius, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.TRANSPARENT),
                    new Stop(INNER_RADIUS_SHARE, Color.TRANSPARENT),
                    new Stop(1.0, GLOW_COLOUR));
            gradientWidth = width;
            gradientHeight = height;
        }
        gc.save();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.setGlobalAlpha(opacity);
        gc.setFill(gradient);
        gc.fillRect(0, 0, width, height);
        gc.restore();
    }
}
