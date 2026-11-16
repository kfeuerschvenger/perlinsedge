package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Lays the ambient tint of the time of day over the whole canvas, so the palette of the world shifts toward warm at dawn and
 * dusk and toward a cool blue at night. Nothing is drawn at midday, when the tint has no strength. The colour is kept
 * with the values it was built from, so a sky that holds the same tint from one frame to the next reuses the colour and
 * allocates nothing.
 */
public final class FxSkyTint {

    /** The colour laid over the canvas, rebuilt only when the tint it was built from changes. */
    private Color colour = Color.TRANSPARENT;
    /** The red of the tint the colour was built from. */
    private double red = Double.NaN;
    /** The green of the tint the colour was built from. */
    private double green = Double.NaN;
    /** The blue of the tint the colour was built from. */
    private double blue = Double.NaN;
    /** The clamped strength the colour was built from, which is its opacity. */
    private double opacity = Double.NaN;

    /**
     * Fills the canvas with the tint of the moment.
     *
     * @param gc     the graphics context, with no transform
     * @param sky    the clock of the sky
     * @param width  canvas width
     * @param height canvas height
     */
    public void draw(GraphicsContext gc, DayCycle sky, double width, double height) {
        final double strength = Math.min(1.0, sky.skyTintStrength());
        if (strength < FxShadowHelper.MIN_VISIBLE_STRENGTH) {
            return;
        }
        final Rgba tint = sky.skyTint();
        if (tint.r() != red || tint.g() != green || tint.b() != blue || strength != opacity) {
            red = tint.r();
            green = tint.g();
            blue = tint.b();
            opacity = strength;
            colour = Color.color(red, green, blue, opacity);
        }
        gc.setFill(colour);
        gc.fillRect(0, 0, width, height);
    }

    /** @return the colour the last frame laid over the canvas, the same instance while the tint does not change */
    Color colour() {
        return colour;
    }
}
