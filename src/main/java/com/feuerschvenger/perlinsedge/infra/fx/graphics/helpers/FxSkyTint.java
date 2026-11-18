package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.utils.ColourChannels;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.LightLayer;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.PixelSink;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Arrays;

/**
 * Lays the ambient tint of the time of day over the whole canvas, so the palette of the world shifts toward warm at dawn
 * and dusk and toward a cool blue at night. Nothing is drawn at midday, when the tint has no strength. The colour is the
 * plain cell of the {@link LightLayer}, rounded to 8 bits per channel, and it is kept until that cell changes, so a sky
 * that holds the same tint from one frame to the next reuses the colour and allocates nothing. The fill with no lights
 * and the fill around the lit regions are the same colour, so switching a light on or off never steps the tint.
 * <p>
 * Where there are lights, the tint is turned toward the colour of each light and eased around it. The regions of the
 * canvas that hold the lights are drawn from the cells of a {@link LightLayer}, at the integer art scale with smoothing
 * off so every cell of the gradient is a crisp block, and the rest of the canvas is filled with the plain tint, in the
 * colour the layer gives a cell no light reaches, so the fill and the cells meet without a seam. A light costs only the
 * area it lights, and a chain of lights that overlap one another merges into one region that costs the box around all
 * of them. With no light in view the whole canvas is one fill, as it is without lights at all, and a tint with no
 * strength draws nothing whatever the lights are.
 */
public final class FxSkyTint {

    /**
     * How far into a cell, as a share of a screen pixel per art pixel, the source of the cells is moved, for the same
     * reason as in {@link FxShadowPass}: the software pipeline samples a scaled source a hair short.
     */
    private static final double SAMPLE_NUDGE = 0.25;

    /** Numbers kept for each region box: left, top, right and bottom. */
    private static final int BOX_FIELDS = 4;
    /** Position of the left side in a box. */
    private static final int BOX_LEFT = 0;
    /** Position of the top side in a box. */
    private static final int BOX_TOP = 1;
    /** Position of the right side in a box. */
    private static final int BOX_RIGHT = 2;
    /** Position of the bottom side in a box. */
    private static final int BOX_BOTTOM = 3;
    /** Rows where a band starts that each box adds: its top and its bottom. */
    private static final int EDGES_PER_BOX = 2;
    /** Rows where a band starts that the canvas adds: its top and its bottom. */
    private static final int CANVAS_EDGES = 2;
    /** Regions the buffers hold before they first grow. */
    private static final int INITIAL_REGIONS = 4;
    /** Bits the red channel is shifted up in a packed colour. */
    private static final int RED_SHIFT = 16;
    /** Bits the green channel is shifted up in a packed colour. */
    private static final int GREEN_SHIFT = 8;
    /** Largest value of an 8 bit channel, as a double. */
    private static final double CHANNEL_MAX = 255.0;

    /**
     * The tint of the frame as the sky reports it, copied into one object that this class owns so a frame allocates
     * nothing and no caller holds the state of the clock.
     */
    private static final class SkyColour implements ColourChannels {
        private double r;
        private double g;
        private double b;

        private void read(DayCycle sky) {
            r = sky.skyTintRed();
            g = sky.skyTintGreen();
            b = sky.skyTintBlue();
        }

        @Override
        public double r() {
            return r;
        }

        @Override
        public double g() {
            return g;
        }

        @Override
        public double b() {
            return b;
        }
    }

    /** The colour of the sky in the frame being drawn. */
    private final SkyColour skyColour = new SkyColour();
    /** How every light is drawn. */
    private final LightStyle style;
    /** The cells of the lit regions: the sky tint with a warm wash toward the light, the plain tint where none reaches. */
    private final LightLayer layer = new LightLayer();
    /** The image that mirrors the cells of the light layer for drawing. */
    private WritableImage image;
    /** The plain tint as the layer rounds it, as premultiplied ARGB, which every fill of the plain tint comes from. */
    private int plainCell;
    /** The colour of the plain tint, the same tint the unlit cells of the layer hold, rebuilt only when it changes. */
    private Color plainColour = Color.TRANSPARENT;
    /** The regions held to the canvas as left, top, right and bottom in screen pixels, four numbers for each. */
    private double[] clipped = new double[INITIAL_REGIONS * BOX_FIELDS];
    /** The rows where a band of the canvas around the regions starts, sorted. */
    private double[] edges = new double[INITIAL_REGIONS * EDGES_PER_BOX + CANVAS_EDGES];
    /** The regions that cover the band being filled, in the order of their left side. */
    private int[] order = new int[INITIAL_REGIONS];
    /** The region being uploaded as left, top, right and bottom, kept so an upload allocates nothing. */
    private final int[] uploadBox = new int[BOX_FIELDS];
    /** Takes the cells the layer lends and uploads the regions from them, made once so an upload allocates nothing. */
    private final PixelSink regionUploader = this::uploadRegions;

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
     * @param view     the canvas of the frame: the art scale, the camera origin the art grid is aligned to and the size
     */
    public void draw(GraphicsContext gc, DayCycle sky, LightRegistry lights, double gameTime, CanvasView view) {
        // The fill and the alpha the tint sets are the tint's own, so what is drawn after it starts from the state it had
        gc.save();
        try {
            layTint(gc, sky, lights, gameTime, view);
        } finally {
            gc.restore();
        }
    }

    private void layTint(GraphicsContext gc, DayCycle sky, LightRegistry lights, double gameTime, CanvasView view) {
        // The tint of the frame is read once and passed on to everything that needs it
        skyColour.read(sky);
        final double strength = Math.min(1.0, sky.skyTintStrength());
        if (strength < DayCycle.MIN_VISIBLE_TINT_STRENGTH) {
            return;
        }
        final ColourChannels tint = skyColour;
        useTint(tint, strength);
        gc.setFill(plainColour);
        final double artScale = view.artScale();
        final double width = view.width();
        final double height = view.height();
        final int wide = (int) Math.ceil(width / artScale) + 2;
        final int high = (int) Math.ceil(height / artScale) + 2;
        final var grid = new LightLayer.GridView(artScale, view.originX(), view.originY(), wide, high);
        if (lights.isEmpty() || !layer.compose(lights, style, gameTime, tint, strength, grid)) {
            gc.fillRect(0, 0, width, height);
            return;
        }
        upload();
        // The plain tint around the regions is the cell the layer holds where no light reaches, which is the fill
        // colour already set
        final int regions = layer.regionCount();
        final int boxes = clipRegions(regions, artScale, width, height);
        fillAround(gc, boxes, width, height);
        final double nudge = SAMPLE_NUDGE / artScale;
        for (var region = 0; region < regions; region++) {
            final int left = layer.regionLeft(region);
            final int top = layer.regionTop(region);
            final int right = layer.regionRight(region);
            final int bottom = layer.regionBottom(region);
            // The cells have a border cell that lies outside the canvas
            final double cellsWide = (double) right - left;
            final double cellsHigh = (double) bottom - top;
            gc.drawImage(image, left + nudge, top + nudge, cellsWide, cellsHigh,
                    layer.phaseX() + (left - 1) * artScale, layer.phaseY() + (top - 1) * artScale,
                    cellsWide * artScale, cellsHigh * artScale);
        }
    }

    /**
     * Holds the regions to the canvas in screen pixels.
     *
     * @return how many regions have a part inside the canvas, the first that many of the clipped boxes
     */
    private int clipRegions(int regions, double artScale, double width, double height) {
        if (clipped.length < regions * BOX_FIELDS) {
            clipped = new double[regions * BOX_FIELDS];
            edges = new double[regions * EDGES_PER_BOX + CANVAS_EDGES];
            order = new int[regions];
        }
        var boxes = 0;
        for (var region = 0; region < regions; region++) {
            final double screenLeft = layer.phaseX() + (layer.regionLeft(region) - 1) * artScale;
            final double screenTop = layer.phaseY() + (layer.regionTop(region) - 1) * artScale;
            final double screenRight = layer.phaseX() + (layer.regionRight(region) - 1) * artScale;
            final double screenBottom = layer.phaseY() + (layer.regionBottom(region) - 1) * artScale;
            final double clipLeft = Math.clamp(screenLeft, 0.0, width);
            final double clipTop = Math.clamp(screenTop, 0.0, height);
            final double clipRight = Math.clamp(screenRight, 0.0, width);
            final double clipBottom = Math.clamp(screenBottom, 0.0, height);
            if (clipRight > clipLeft && clipBottom > clipTop) {
                final int base = boxes * BOX_FIELDS;
                clipped[base + BOX_LEFT] = clipLeft;
                clipped[base + BOX_TOP] = clipTop;
                clipped[base + BOX_RIGHT] = clipRight;
                clipped[base + BOX_BOTTOM] = clipBottom;
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
        edges[0] = 0.0;
        edges[1] = height;
        var edgeCount = CANVAS_EDGES;
        for (var box = 0; box < boxes; box++) {
            edges[edgeCount] = clipped[box * BOX_FIELDS + BOX_TOP];
            edges[edgeCount + 1] = clipped[box * BOX_FIELDS + BOX_BOTTOM];
            edgeCount += EDGES_PER_BOX;
        }
        Arrays.sort(edges, 0, edgeCount);
        for (var edge = 0; edge + 1 < edgeCount; edge++) {
            final double bandTop = edges[edge];
            final double bandBottom = edges[edge + 1];
            if (bandBottom > bandTop) {
                fillBand(gc, boxes, bandTop, bandBottom, width);
            }
        }
    }

    /** Fills the stretches of one band of the canvas that the boxes crossing it leave out. */
    private void fillBand(GraphicsContext gc, int boxes, double bandTop, double bandBottom, double width) {
        final int across = orderCrossing(boxes, bandTop, bandBottom);
        var cursor = 0.0;
        for (var k = 0; k < across; k++) {
            final int box = order[k];
            final double boxLeft = clipped[box * BOX_FIELDS + BOX_LEFT];
            if (boxLeft > cursor) {
                gc.fillRect(cursor, bandTop, boxLeft - cursor, bandBottom - bandTop);
            }
            cursor = Math.max(cursor, clipped[box * BOX_FIELDS + BOX_RIGHT]);
        }
        if (width > cursor) {
            gc.fillRect(cursor, bandTop, width - cursor, bandBottom - bandTop);
        }
    }

    /**
     * Puts the boxes that cover a band in the order of their left side.
     *
     * @return how many boxes cover the band, the first that many of the order
     */
    private int orderCrossing(int boxes, double bandTop, double bandBottom) {
        var across = 0;
        for (var box = 0; box < boxes; box++) {
            if (clipped[box * BOX_FIELDS + BOX_TOP] <= bandTop && clipped[box * BOX_FIELDS + BOX_BOTTOM] >= bandBottom) {
                var at = across;
                across++;
                while (at > 0 && clipped[order[at - 1] * BOX_FIELDS + BOX_LEFT] > clipped[box * BOX_FIELDS + BOX_LEFT]) {
                    order[at] = order[at - 1];
                    at--;
                }
                order[at] = box;
            }
        }
        return across;
    }

    /** Returns the straight colour that, laid over the canvas, gives what a premultiplied cell gives when it is drawn. */
    private static Color colourOf(int premultiplied) {
        final int alpha = premultiplied >>> 24;
        if (alpha == 0) {
            return Color.TRANSPARENT;
        }
        return Color.color(Math.min(1.0, ((premultiplied >> RED_SHIFT) & 0xFF) / (double) alpha),
                Math.min(1.0, ((premultiplied >> GREEN_SHIFT) & 0xFF) / (double) alpha),
                Math.min(1.0, (premultiplied & 0xFF) / (double) alpha), alpha / CHANNEL_MAX);
    }

    /**
     * Works out the colour of the plain tint of the moment from the cell the layer rounds it to, and keeps the colour
     * until the cell changes.
     *
     * @param tint     the colour of the tint
     * @param strength the clamped strength of the tint, which is visible
     */
    private void useTint(ColourChannels tint, double strength) {
        final int cell = LightLayer.plainCellFor(tint, strength);
        if (cell != plainCell) {
            plainCell = cell;
            plainColour = colourOf(cell);
        }
    }

    /** Copies the regions of the light layer into the image when the layer worked its cells out again. */
    private void upload() {
        final boolean fresh = !FxShadowPass.fitsPadded(image, layer.width(), layer.height());
        if (fresh) {
            image = FxShadowPass.newPaddedImage(layer.width(), layer.height());
        }
        if (!layer.changed() && !fresh) {
            return;
        }
        layer.readPixels(regionUploader);
    }

    /** Copies every region of the layer, whose cells it lends, into the image. */
    private void uploadRegions(int[] cells) {
        for (int region = 0, regions = layer.regionCount(); region < regions; region++) {
            uploadBox[BOX_LEFT] = layer.regionLeft(region);
            uploadBox[BOX_TOP] = layer.regionTop(region);
            uploadBox[BOX_RIGHT] = layer.regionRight(region);
            uploadBox[BOX_BOTTOM] = layer.regionBottom(region);
            FxShadowPass.uploadPadded(image, layer.width(), layer.height(), uploadBox, cells);
        }
    }

    /** @return the colour the last frame laid over the canvas, the same instance while the tint does not change */
    Color colour() {
        return plainColour;
    }

    /** @return the layer that holds the lit cells, for tests */
    LightLayer layer() {
        return layer;
    }
}
