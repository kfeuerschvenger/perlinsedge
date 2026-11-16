package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Lays the ambient tint of the time of day over the whole canvas, so the palette of the world shifts toward warm at dawn and
 * dusk and toward a cool blue at night. Nothing is drawn at midday, when the tint has no strength. The colour of the last
 * frame is kept, so a steady sky allocates no colour.
 */
public final class FxSkyTint {

    /** Tints weaker than this opacity are not visible, so they are not laid over the world. */
    private static final double MIN_STRENGTH = 0.004;

    private Color colour = Color.TRANSPARENT;

    /**
     * Fills the canvas with the tint of the moment.
     *
     * @param gc     the graphics context, with no transform
     * @param sky    the clock of the sky
     * @param width  canvas width
     * @param height canvas height
     */
    public void draw(GraphicsContext gc, DayCycle sky, double width, double height) {
        final double strength = sky.skyTintStrength();
        if (strength < MIN_STRENGTH) {
            return;
        }
        final Rgba tint = sky.skyTint();
        if (colour.getRed() != tint.r() || colour.getGreen() != tint.g() || colour.getBlue() != tint.b()
                || colour.getOpacity() != strength) {
            colour = Color.color(tint.r(), tint.g(), tint.b(), Math.min(1.0, strength));
        }
        gc.setFill(colour);
        gc.fillRect(0, 0, width, height);
    }
}
