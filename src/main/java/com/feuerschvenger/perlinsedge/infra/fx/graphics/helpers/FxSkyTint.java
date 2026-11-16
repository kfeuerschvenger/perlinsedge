package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.LightLayer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Lays the ambient tint of the time of day over the whole canvas, so the palette of the world shifts toward warm at dawn and
 * dusk and toward a cool blue at night. Nothing is drawn at midday, when the tint has no strength. The colour is kept
 * with the values it was built from, so a sky that holds the same tint from one frame to the next reuses the colour and
 * allocates nothing.
 * <p>
 * Where there are lights, the tint is pushed back around them. The part of the canvas that holds the lights is drawn
 * from the cells of a {@link LightLayer}, at the integer art scale with smoothing off so every cell of the gradient is a crisp block, and
 * the rest of the canvas is filled with the plain tint, so a light costs only the area it lights. With no light in view
 * the whole canvas is one fill, as it is without lights at all, and a tint with no strength draws nothing whatever the
 * lights are.
 */
public final class FxSkyTint {

    /**
     * How far into a cell, as a share of a screen pixel per art pixel, the source of the cells is moved, for the same
     * reason as in {@link FxShadowPass}: the software pipeline samples a scaled source a hair short.
     */
    private static final double SAMPLE_NUDGE = 0.25;

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

    /** How every light is drawn. */
    private final LightStyle style;
    /** The cells of the darkness around the lights. */
    private final LightLayer layer = new LightLayer();
    /** The image that mirrors the cells of the light layer for drawing. */
    private WritableImage image;

    /** Creates a sky tint that draws lights the way the configuration says. */
    public FxSkyTint() {
        this(AppConfig.getInstance().lights().style());
    }

    /**
     * Creates a sky tint with a chosen way to draw lights.
     *
     * @param style how every light is drawn
     */
    public FxSkyTint(LightStyle style) {
        this.style = style;
    }

    /**
     * Fills the canvas with the tint of the moment, with no lights.
     *
     * @param gc     the graphics context, with no transform
     * @param sky    the clock of the sky
     * @param width  canvas width
     * @param height canvas height
     */
    public void draw(GraphicsContext gc, DayCycle sky, double width, double height) {
        final double strength = prepare(sky);
        if (strength < 0.0) {
            return;
        }
        gc.setFill(colour);
        gc.fillRect(0, 0, width, height);
    }

    /**
     * Lays the tint of the moment over the canvas, pushed back around the lights.
     *
     * @param gc       the graphics context, with image smoothing off and no transform
     * @param sky      the clock of the sky
     * @param lights   the lights of the world
     * @param gameTime the game time in seconds, which the flicker of the lights runs on
     * @param artScale the integer number of screen pixels per art pixel
     * @param originX  screen X of the camera origin, which the art grid is aligned to
     * @param originY  screen Y of the camera origin
     * @param width    canvas width
     * @param height   canvas height
     */
    public void draw(GraphicsContext gc, DayCycle sky, LightRegistry lights, double gameTime, double artScale,
                     double originX, double originY, double width, double height) {
        final double strength = prepare(sky);
        if (strength < 0.0) {
            return;
        }
        gc.setFill(colour);
        final int wide = (int) Math.ceil(width / artScale) + 2;
        final int high = (int) Math.ceil(height / artScale) + 2;
        if (lights.isEmpty() || !layer.compose(lights, style, gameTime, sky.skyTint(), strength, artScale, originX,
                originY, wide, high)) {
            gc.fillRect(0, 0, width, height);
            return;
        }
        upload();
        final int left = layer.boxLeft();
        final int top = layer.boxTop();
        final int boxWide = layer.boxRight() - left;
        final int boxHigh = layer.boxBottom() - top;
        // The box in screen pixels, held to the canvas: the cells have a border cell that lies outside it
        final double screenLeft = layer.phaseX() + (left - 1) * artScale;
        final double screenTop = layer.phaseY() + (top - 1) * artScale;
        final double screenRight = screenLeft + boxWide * artScale;
        final double screenBottom = screenTop + boxHigh * artScale;
        final double clipLeft = Math.max(0.0, Math.min(width, screenLeft));
        final double clipTop = Math.max(0.0, Math.min(height, screenTop));
        final double clipRight = Math.max(0.0, Math.min(width, screenRight));
        final double clipBottom = Math.max(0.0, Math.min(height, screenBottom));
        // The plain tint around the box, in four rectangles that do not overlap
        gc.fillRect(0, 0, width, clipTop);
        gc.fillRect(0, clipBottom, width, height - clipBottom);
        gc.fillRect(0, clipTop, clipLeft, clipBottom - clipTop);
        gc.fillRect(clipRight, clipTop, width - clipRight, clipBottom - clipTop);
        final double nudge = SAMPLE_NUDGE / artScale;
        gc.drawImage(image, left + nudge, top + nudge, boxWide, boxHigh, screenLeft, screenTop, boxWide * artScale,
                boxHigh * artScale);
    }

    /**
     * Works out the colour of the tint of the moment.
     *
     * @return the clamped strength, or -1 when the tint is too weak to be seen and nothing is to be drawn
     */
    private double prepare(DayCycle sky) {
        final double strength = Math.min(1.0, sky.skyTintStrength());
        if (strength < DayCycle.MIN_VISIBLE_TINT_STRENGTH) {
            return -1.0;
        }
        final Rgba tint = sky.skyTint();
        if (tint.r() != red || tint.g() != green || tint.b() != blue || strength != opacity) {
            red = tint.r();
            green = tint.g();
            blue = tint.b();
            opacity = strength;
            colour = Color.color(red, green, blue, opacity);
        }
        return strength;
    }

    /** Copies the box of the light layer into the image when the layer worked its cells out again. */
    private void upload() {
        final boolean fresh = image == null || (int) image.getWidth() != layer.width()
                || (int) image.getHeight() != layer.height();
        if (fresh) {
            image = new WritableImage(layer.width(), layer.height());
        }
        if (!layer.changed() && !fresh) {
            return;
        }
        final int left = layer.boxLeft();
        final int top = layer.boxTop();
        image.getPixelWriter().setPixels(left, top, layer.boxRight() - left, layer.boxBottom() - top,
                PixelFormat.getIntArgbPreInstance(), layer.pixels(), top * layer.width() + left, layer.width());
    }

    /** @return the colour the last frame laid over the canvas, the same instance while the tint does not change */
    Color colour() {
        return colour;
    }

    /** @return the cells of the darkness around the lights, for tests */
    LightLayer layer() {
        return layer;
    }
}
