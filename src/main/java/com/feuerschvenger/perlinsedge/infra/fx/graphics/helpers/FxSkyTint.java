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

import java.util.Arrays;

/**
 * Lays the ambient tint of the time of day over the whole canvas, so the palette of the world shifts toward warm at dawn
 * and dusk and toward a cool blue at night. Nothing is drawn at midday, when the tint has no strength. The colour is
 * kept with the values it was built from, so a sky that holds the same tint from one frame to the next reuses the colour
 * and allocates nothing.
 * <p>
 * Where there are lights, the tint is turned toward the colour of each light and eased around it. The regions of the
 * canvas that hold the lights are drawn from the cells of a {@link LightLayer}, at the integer art scale with smoothing
 * off so every cell of the gradient is a crisp block, and the rest of the canvas is filled with the plain tint, in the
 * colour the layer gives a cell no light reaches, so the fill and the cells meet without a seam. A light costs only the
 * area it lights. With no light in view the whole canvas is one fill, as it is without lights at all, and a tint with
 * no strength draws nothing whatever the lights are.
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
    /** The cells of the lit regions: the sky tint with a warm wash toward the light, the plain tint where none reaches. */
    private final LightLayer layer = new LightLayer();
    /** The image that mirrors the cells of the light layer for drawing. */
    private WritableImage image;
    /** The plain tint as the layer holds it, as premultiplied ARGB, which the colour around the regions comes from. */
    private int plainCell;
    /** The colour of the plain tint around the regions, the same tint the unlit cells of the layer hold. */
    private Color plainColour = Color.TRANSPARENT;
    /** The regions held to the canvas as left, top, right and bottom in screen pixels, four numbers for each. */
    private double[] clipped = new double[16];
    /** The rows where a band of the canvas around the regions starts, sorted. */
    private double[] edges = new double[10];
    /** The regions that cover the band being filled, in the order of their left side. */
    private int[] order = new int[4];

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
     * Lays the tint of the moment over the canvas, eased and warmed around the lights.
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
        // The plain tint around the regions is the cell the layer holds where no light reaches
        if (layer.plainCell() != plainCell) {
            plainCell = layer.plainCell();
            plainColour = colourOf(plainCell);
        }
        gc.setFill(plainColour);
        final int regions = layer.regionCount();
        final int boxes = clipRegions(regions, artScale, width, height);
        fillAround(gc, boxes, width, height);
        final double nudge = SAMPLE_NUDGE / artScale;
        for (int region = 0; region < regions; region++) {
            final int left = layer.regionLeft(region);
            final int top = layer.regionTop(region);
            final int right = layer.regionRight(region);
            final int bottom = layer.regionBottom(region);
            // The cells have a border cell that lies outside the canvas
            gc.drawImage(image, left + nudge, top + nudge, FxShadowPass.sourceSpan(left, right, layer.width(), nudge),
                    FxShadowPass.sourceSpan(top, bottom, layer.height(), nudge),
                    layer.phaseX() + (left - 1) * artScale, layer.phaseY() + (top - 1) * artScale,
                    (right - left) * artScale, (bottom - top) * artScale);
        }
    }

    /**
     * Holds the regions to the canvas in screen pixels.
     *
     * @return how many regions have a part inside the canvas, the first that many of the clipped boxes
     */
    private int clipRegions(int regions, double artScale, double width, double height) {
        if (clipped.length < regions * 4) {
            clipped = new double[regions * 4];
            edges = new double[regions * 2 + 2];
            order = new int[regions];
        }
        int boxes = 0;
        for (int region = 0; region < regions; region++) {
            final double screenLeft = layer.phaseX() + (layer.regionLeft(region) - 1) * artScale;
            final double screenTop = layer.phaseY() + (layer.regionTop(region) - 1) * artScale;
            final double screenRight = layer.phaseX() + (layer.regionRight(region) - 1) * artScale;
            final double screenBottom = layer.phaseY() + (layer.regionBottom(region) - 1) * artScale;
            final double clipLeft = Math.max(0.0, Math.min(width, screenLeft));
            final double clipTop = Math.max(0.0, Math.min(height, screenTop));
            final double clipRight = Math.max(0.0, Math.min(width, screenRight));
            final double clipBottom = Math.max(0.0, Math.min(height, screenBottom));
            if (clipRight > clipLeft && clipBottom > clipTop) {
                clipped[boxes * 4] = clipLeft;
                clipped[boxes * 4 + 1] = clipTop;
                clipped[boxes * 4 + 2] = clipRight;
                clipped[boxes * 4 + 3] = clipBottom;
                boxes++;
            }
        }
        return boxes;
    }

    /**
     * Fills the canvas with the fill of the context everywhere the boxes are not, in rectangles that do not overlap. The
     * canvas is cut into bands at the top and the bottom of every box, and in each band the stretches between the boxes
     * that cross it are filled, which is the four rectangles around a single box.
     */
    private void fillAround(GraphicsContext gc, int boxes, double width, double height) {
        int edgeCount = 0;
        edges[edgeCount++] = 0.0;
        edges[edgeCount++] = height;
        for (int box = 0; box < boxes; box++) {
            edges[edgeCount++] = clipped[box * 4 + 1];
            edges[edgeCount++] = clipped[box * 4 + 3];
        }
        Arrays.sort(edges, 0, edgeCount);
        for (int edge = 0; edge + 1 < edgeCount; edge++) {
            final double bandTop = edges[edge];
            final double bandBottom = edges[edge + 1];
            if (!(bandBottom > bandTop)) {
                continue;
            }
            int across = 0;
            for (int box = 0; box < boxes; box++) {
                if (clipped[box * 4 + 1] <= bandTop && clipped[box * 4 + 3] >= bandBottom) {
                    int at = across++;
                    while (at > 0 && clipped[order[at - 1] * 4] > clipped[box * 4]) {
                        order[at] = order[at - 1];
                        at--;
                    }
                    order[at] = box;
                }
            }
            double cursor = 0.0;
            for (int k = 0; k < across; k++) {
                final int box = order[k];
                if (clipped[box * 4] > cursor) {
                    gc.fillRect(cursor, bandTop, clipped[box * 4] - cursor, bandBottom - bandTop);
                }
                cursor = Math.max(cursor, clipped[box * 4 + 2]);
            }
            if (width > cursor) {
                gc.fillRect(cursor, bandTop, width - cursor, bandBottom - bandTop);
            }
        }
    }

    /** Returns the straight colour that, laid over the canvas, gives what a premultiplied cell gives when it is drawn. */
    private static Color colourOf(int premultiplied) {
        final int alpha = premultiplied >>> 24;
        if (alpha == 0) {
            return Color.TRANSPARENT;
        }
        return Color.color(Math.min(1.0, ((premultiplied >> 16) & 0xFF) / (double) alpha),
                Math.min(1.0, ((premultiplied >> 8) & 0xFF) / (double) alpha),
                Math.min(1.0, (premultiplied & 0xFF) / (double) alpha), alpha / 255.0);
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

    /** Copies the regions of the light layer into the image when the layer worked its cells out again. */
    private void upload() {
        final boolean fresh = image == null || (int) image.getWidth() != layer.width()
                || (int) image.getHeight() != layer.height();
        if (fresh) {
            image = new WritableImage(layer.width(), layer.height());
        }
        if (!layer.changed() && !fresh) {
            return;
        }
        for (int region = 0, regions = layer.regionCount(); region < regions; region++) {
            final int left = layer.regionLeft(region);
            final int top = layer.regionTop(region);
            image.getPixelWriter().setPixels(left, top, layer.regionRight(region) - left,
                    layer.regionBottom(region) - top, PixelFormat.getIntArgbPreInstance(), layer.pixels(),
                    top * layer.width() + left, layer.width());
        }
    }

    /** @return the colour the last frame laid over the canvas, the same instance while the tint does not change */
    Color colour() {
        return colour;
    }

    /** @return the layer that holds the lit cells, for tests */
    LightLayer layer() {
        return layer;
    }
}
