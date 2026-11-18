package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.utils.Doubles;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowLayer;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.util.Arrays;

/**
 * The shadow pass of a frame: every shadow is cast at full black, and the result is laid over the canvas at the shadow
 * opacity times the strength of the day. Each cell keeps the strongest alpha of the shadows that cover it instead of
 * their sum, so two overlapping shadows are exactly as dark as one.
 * <p>
 * The layers have one cell per art pixel and are drawn at the integer art scale with smoothing off, so every shadow
 * pixel is a crisp square block at every zoom. The grid is aligned to the camera, so the shadows of the tiles sit on the
 * same grid as the tile art. The shadow of a sprite is anchored on the base of the sprite as it is drawn, see
 * {@link ShadowAnchor}, so the two move together.
 * <p>
 * The shadows come in two lists that are cast into two layers. The static ones, of the resources and buildings on the
 * tiles, are given in world coordinates, which are screen coordinates with the camera offset taken off, and are cast
 * into a window of the world that is larger than the canvas by a margin of cells on every side, a quarter of the canvas
 * or at least {@link #WINDOW_MARGIN_CELLS}. The window stays where it is while the camera pans inside it, and the camera
 * offset is applied when the layer is drawn, so walking casts no static shadow at all: the layer is cast again only when
 * the window has to move, which places it again around the camera, the sun moves a step, the world changes or the list
 * of static shadows does. The dynamic ones, of the player and the enemies, are given in screen
 * coordinates and change with every step, so they are cast into a layer of the size of the canvas, aligned to the
 * camera. Each layer is mirrored into an image of its own, and only the area that changed is uploaded.
 * <p>
 * The two images are drawn one after the other. To keep two overlapping shadows as dark as one, the cells of the
 * dynamic image that a static shadow already covers are lowered by exactly what the static shadow already darkens, so
 * the two draws add up to the stronger of the two. A frame with no visible shadows draws nothing.
 */
public final class FxShadowPass {

    /**
     * How far into a cell, as a share of a screen pixel per art pixel, the source of the composite is moved. The software
     * pipeline walks the source in fixed point, so a step of one third of a cell is stored a hair short and the first
     * pixel of every third cell samples the cell before it. A quarter of a screen pixel is far from every cell edge for
     * a sampler that reads at the corner of a pixel and for one that reads at its centre alike.
     */
    private static final double SAMPLE_NUDGE = 0.25;

    /** The cells of pad an image has past the last column and the last row of its layer, see {@link #newPaddedImage}. */
    private static final int EDGE_PAD = 1;

    /**
     * The fewest cells the static window reaches past the canvas on every side when it is placed. A canvas that is
     * large in cells gets a margin of {@link #WINDOW_MARGIN_SHARE} of its size instead, see {@link #marginFor}.
     */
    public static final int WINDOW_MARGIN_CELLS = 40;

    /**
     * The share of the canvas, across or down, that the static window reaches past it on every side when it is placed.
     * The camera can pan this far either way before the window has to move, and a recast of the static layer is what a
     * pan of that distance is paid with, so a margin that grows with the canvas keeps a camera that follows the player
     * from casting again every few tiles.
     */
    public static final double WINDOW_MARGIN_SHARE = 0.25;

    /** The cells of border a canvas grid has past the cells the canvas shows. */
    private static final int BORDER_CELLS = 2;
    /** The sides of the canvas the window margin is added to, one on each side. */
    private static final int MARGIN_SIDES = 2;
    /** The margin of the window plus the distance the camera may pan before it moves, which is as far as the margin. */
    private static final double OVERHANG_MARGIN_FACTOR = 2.0;
    /** The cells of rounding the overhang of the window allows for, one on each side. */
    private static final double OVERHANG_ROUNDING_CELLS = 2.0;

    /** The numbers a box of cells is given by: left, top, right and bottom. */
    private static final int BOX_VALUES = 4;
    /** Position of the left side in a box. */
    private static final int BOX_LEFT = 0;
    /** Position of the top side in a box. */
    private static final int BOX_TOP = 1;
    /** Position of the right side in a box, which is exclusive. */
    private static final int BOX_RIGHT = 2;
    /** Position of the bottom side in a box, which is exclusive. */
    private static final int BOX_BOTTOM = 3;

    /** Bits the alpha is shifted up in a packed ARGB cell. */
    private static final int ALPHA_SHIFT = 24;
    /** The largest alpha of a cell. */
    private static final int MAX_ALPHA = 255;
    /** The largest alpha of a cell as a double, the factor between an alpha and a share of opacity. */
    private static final double ALPHA_RANGE = MAX_ALPHA;

    /**
     * The art grid a layer is cast on.
     *
     * @param artScale screen pixels per art pixel
     * @param phaseX   the horizontal phase of the grid
     * @param phaseY   the vertical phase of the grid
     * @param offsetX  what the list cast adds to a world X to get the screen X: the camera offset for the static list,
     *                 zero for the dynamic one, which is given in screen coordinates
     * @param offsetY  the same along Y
     */
    private record GridFrame(double artScale, double phaseX, double phaseY, double offsetX, double offsetY) {}

    /** The shadows of the things that stay where they are, cast into their own layer, a window of the world. */
    private final ShadowLayer staticLayer = new ShadowLayer();
    /** The shadows of the things that move, cast into their own layer, which is the size of the canvas. */
    private final ShadowLayer dynamicLayer = new ShadowLayer();
    /** The cells of the dynamic layer as they are drawn: lowered where a static shadow already darkens them. */
    private int[] dynamicShown = new int[0];
    /** The image that mirrors the cells of the static layer. */
    private WritableImage staticImage;
    /** The image that mirrors the cells of the dynamic layer as they are drawn. */
    private WritableImage dynamicImage;

    /** The version that says the caller does not keep one, see {@link #draw(GraphicsContext, ShadowList, long, ShadowList, CanvasView, SunShadow)}. */
    public static final long NO_VERSION = -1L;
    /** The static shadows the static layer was last cast from, so an unchanged list is recognised. */
    private final ShadowList castStatic = new ShadowList();
    /** The version of the static list the static layer was last cast from, or {@link Long#MIN_VALUE} for none. */
    private long castStaticVersion = Long.MIN_VALUE;
    /** The version of the static list of the frame being drawn. */
    private long pendingStaticVersion = NO_VERSION;
    /** The dynamic shadows the dynamic layer was last cast from, so an unchanged list is recognised. */
    private final ShadowList castDynamic = new ShadowList();
    /** Whether the layers were cast at all last time, which is false while the shadows are too faint to see. */
    private boolean castVisible;
    /** Whether there is a last frame to compare with. */
    private boolean hasLastFrame;

    /** The number of cells across the canvas grid of the frame. */
    private int cellsWide;
    /** The number of cells down the canvas grid of the frame. */
    private int cellsHigh;
    /** The camera offset of the frame in whole screen pixels along X. */
    private double snappedX;
    /** The camera offset of the frame in whole screen pixels along Y. */
    private double snappedY;
    /** The horizontal phase of the camera grid of the frame, the part of the offset that is not a whole art pixel. */
    private double phaseX;
    /** The vertical phase of the camera grid of the frame. */
    private double phaseY;

    /** Whether a window has been placed. */
    private boolean windowPlaced;
    /** The art scale the window was placed at. */
    private double windowScale;
    /** The number of canvas cells across the window was placed for. */
    private int windowCanvasWide;
    /** The number of canvas cells down the window was placed for. */
    private int windowCanvasHigh;
    /** The world column of the first cell of the window, in cells, a world cell being an art pixel of the world. */
    private int windowCellX;
    /** The world row of the first cell of the window, in cells. */
    private int windowCellY;
    /** The cells the window reaches past the canvas on the left and on the right. */
    private int windowMarginX;
    /** The cells the window reaches past the canvas above and below. */
    private int windowMarginY;
    /** The number of cells across the window. */
    private int windowCellsWide;
    /** The number of cells down the window. */
    private int windowCellsHigh;
    /** Whether the window was placed anew since the static layer was last cast, which casts it again. */
    private boolean windowFresh;
    /** Cells to add to a column of the static layer to get its column on the canvas grid. */
    private int shiftX;
    /** Cells to add to a row of the static layer to get its row on the canvas grid. */
    private int shiftY;
    /** Whether the last cast of the static layer uploaded cells to the static image. */
    private boolean lastStaticUploaded;
    /** The shift the dynamic cells were last lowered against, so a pan of whole cells is recognised. */
    private int shownShiftX;
    /** The shift the dynamic cells were last lowered against, along Y. */
    private int shownShiftY;
    /** The share of the shadow opacity the dynamic cells were last lowered at. */
    private double shownOpacity = Double.NaN;

    /** The horizontal phase of the camera grid the dynamic layer was last cast at. */
    private double lastPhaseX;
    /** The vertical phase of the camera grid the dynamic layer was last cast at. */
    private double lastPhaseY;
    /** The number of cells across the layers were last cast at. */
    private int lastCellsWide;
    /** The number of cells down the layers were last cast at. */
    private int lastCellsHigh;
    /** The art scale the layers were last cast at. */
    private double lastArtScale;
    /** The shadow direction the layers were last cast at. */
    private double lastAngle;
    /** The shadow length factor the layers were last cast at. */
    private double lastLength;
    /** How many frames cast shadows into a layer, which a frame that kept both layers does not count. */
    private int recasts;
    /** How many times the static layer was cast, which a frame that only moved the dynamic shadows does not count. */
    private int staticRecasts;
    /** How many frames uploaded to an image, which a frame that kept both layers does not count. */
    private int uploads;
    /** The area of the last upload to the dynamic image as left, top, right and bottom, with the last two exclusive. */
    private final int[] lastUpload = new int[BOX_VALUES];
    /** The area of the dynamic layer the current frame changed, empty when right is not past left. */
    private final int[] region = new int[BOX_VALUES];
    /** The area a layer reports as changed, kept so asking for it allocates nothing. */
    private final int[] changedScratch = new int[BOX_VALUES];

    /**
     * Works out where the canvas grid of a frame lies and places the static window when the frame needs a new one. A
     * caller that collects the static shadows asks for this first, so it knows which part of the world they must cover,
     * see {@link #windowLeft()}; {@link #draw} does the same, and doing it twice in a frame changes nothing.
     *
     * @param view the canvas of the frame: the art scale, the camera origin the art grid is aligned to and the size
     */
    public void beginFrame(CanvasView view) {
        final double artScale = view.artScale();
        cellsWide = (int) Math.ceil(view.width() / artScale) + BORDER_CELLS;
        cellsHigh = (int) Math.ceil(view.height() / artScale) + BORDER_CELLS;
        snappedX = Math.rint(view.originX());
        snappedY = Math.rint(view.originY());
        // The whole art pixels the camera offset spans, rounded down
        final int wholeCellsX = (int) Math.floor(snappedX / artScale);
        final int wholeCellsY = (int) Math.floor(snappedY / artScale);
        phaseX = snappedX - wholeCellsX * artScale;
        phaseY = snappedY - wholeCellsY * artScale;
        final int marginX = marginFor(cellsWide);
        final int marginY = marginFor(cellsHigh);
        // The canvas shows the window shifted by the whole cells of the camera offset plus where the window was placed;
        // it stays valid while the canvas lies inside the window, and a window that has to move is placed again around
        // the camera of this frame
        final boolean sameCanvas = windowPlaced && Doubles.areEqual(artScale, windowScale) && cellsWide == windowCanvasWide
                && cellsHigh == windowCanvasHigh;
        final boolean insideWindow = Math.abs(windowCellX + wholeCellsX + marginX) <= marginX
                && Math.abs(windowCellY + wholeCellsY + marginY) <= marginY;
        final boolean fits = sameCanvas && insideWindow;
        if (!fits) {
            windowMarginX = marginX;
            windowMarginY = marginY;
            windowCellX = -marginX - wholeCellsX;
            windowCellY = -marginY - wholeCellsY;
            windowCellsWide = cellsWide + MARGIN_SIDES * marginX;
            windowCellsHigh = cellsHigh + MARGIN_SIDES * marginY;
            windowScale = artScale;
            windowCanvasWide = cellsWide;
            windowCanvasHigh = cellsHigh;
            windowPlaced = true;
            windowFresh = true;
        }
        shiftX = windowCellX + wholeCellsX;
        shiftY = windowCellY + wholeCellsY;
    }

    /**
     * Returns the margin of the static window along one axis of the canvas grid.
     *
     * @param canvasCells the number of cells across the canvas grid along the axis
     * @return the larger of {@link #WINDOW_MARGIN_CELLS} and {@link #WINDOW_MARGIN_SHARE} of the canvas, in cells
     */
    static int marginFor(int canvasCells) {
        return Math.max(WINDOW_MARGIN_CELLS, (int) Math.ceil(canvasCells * WINDOW_MARGIN_SHARE));
    }

    /**
     * Returns how far the static window can reach past the canvas to the left or to the right at most, in screen
     * pixels: the margin it is placed with plus the distance the camera may pan before the window moves, and a cell of
     * rounding on each. It describes the window of the frame, so it is asked after {@link #beginFrame}.
     *
     * @return the horizontal overhang in screen pixels
     */
    public double windowOverhangXPixels() {
        return (OVERHANG_MARGIN_FACTOR * windowMarginX + OVERHANG_ROUNDING_CELLS) * windowScale;
    }

    /**
     * Returns how far the static window can reach past the canvas above or below at most, in screen pixels, see
     * {@link #windowOverhangXPixels()}.
     *
     * @return the vertical overhang in screen pixels
     */
    public double windowOverhangYPixels() {
        return (OVERHANG_MARGIN_FACTOR * windowMarginY + OVERHANG_ROUNDING_CELLS) * windowScale;
    }

    /** @return the camera offset of the frame along X in whole screen pixels, which the static shadows are given relative to */
    public double snappedOffsetX() {
        return snappedX;
    }

    /** @return the camera offset of the frame along Y in whole screen pixels, which the static shadows are given relative to */
    public double snappedOffsetY() {
        return snappedY;
    }

    /** @return the screen X of the left edge of the static window of the frame, see {@link #beginFrame} */
    public double windowLeft() {
        return snappedX + (windowCellX - 1) * windowScale;
    }

    /** @return the screen Y of the top edge of the static window of the frame */
    public double windowTop() {
        return snappedY + (windowCellY - 1) * windowScale;
    }

    /** @return the width of the static window of the frame in screen pixels */
    public double windowWidth() {
        return windowCellsWide * windowScale;
    }

    /** @return the height of the static window of the frame in screen pixels */
    public double windowHeight() {
        return windowCellsHigh * windowScale;
    }

    /**
     * Casts the shadows of a frame, only as far as they differ from the last frame, and lays them over the canvas.
     *
     * @param gc             the graphics context, with image smoothing off and no transform
     * @param staticShadows  the shadows of the resources and buildings, which stand still, in world coordinates: the
     *                       screen position with the whole pixel camera offset taken off
     * @param dynamicShadows the shadows of the player and the enemies, which move, in screen coordinates
     * @param view           the canvas of the frame: the art scale, the camera origin the art grid is aligned to and
     *                       the size
     * @param sunShadow            the direction, length and strength of the shadows
     */
    public void draw(GraphicsContext gc, ShadowList staticShadows, ShadowList dynamicShadows, CanvasView view,
                     SunShadow sunShadow) {
        draw(gc, staticShadows, NO_VERSION, dynamicShadows, view, sunShadow);
    }

    /**
     * Casts the shadows of a frame like {@link #draw(GraphicsContext, ShadowList, ShadowList, CanvasView, SunShadow)},
     * telling whether the static list changed by a version the caller keeps instead of comparing every shadow of the
     * list with the last cast, which a list of thousands of shadows would cost on every frame.
     *
     * @param gc             the graphics context, with image smoothing off and no transform
     * @param staticShadows  the shadows of the resources and buildings, in world coordinates
     * @param staticVersion  a number the caller changes whenever the static list changes, and keeps while it does not;
     *                       {@link #NO_VERSION} compares the lists shadow by shadow
     * @param dynamicShadows the shadows of the player and the enemies, in screen coordinates
     * @param view           the canvas of the frame: the art scale, the camera origin the art grid is aligned to and
     *                       the size
     * @param sunShadow            the direction, length and strength of the shadows
     */
    public void draw(GraphicsContext gc, ShadowList staticShadows, long staticVersion, ShadowList dynamicShadows,
                     CanvasView view, SunShadow sunShadow) {
        pendingStaticVersion = staticVersion;
        beginFrame(view);
        final boolean visible = sunShadow.visible();
        final double opacity = sunShadow.opacity();

        // What both layers share: a change in any of it casts both again. While the shadows are too faint to see nothing
        // is cast, so the lists do not matter
        final boolean everythingChanged = everythingChanged(view, sunShadow);
        // The static layer does not depend on the phase of the camera grid, only the dynamic one does
        final boolean staticChanged = staticLayerChanged(everythingChanged, visible, staticShadows, staticVersion);
        final boolean dynamicChanged = dynamicLayerChanged(everythingChanged, visible, dynamicShadows);
        // The dynamic cells are lowered against the static layer, so they are worked out again when it moved against
        // them or when the opacity, which the lowering depends on, changed
        final boolean loweringChanged = staticChanged || loweringShifted(opacity);
        if (staticChanged || dynamicChanged || loweringChanged) {
            cast(staticShadows, dynamicShadows, staticChanged, dynamicChanged, view.artScale(), sunShadow);
        }
        if (visible) {
            layOver(gc, view.artScale(), opacity);
        }
    }

    /** Tells whether the static layer has to be cast again: it does not depend on the phase of the camera grid. */
    private boolean staticLayerChanged(boolean everythingChanged, boolean visible, ShadowList staticShadows,
                                       long staticVersion) {
        return everythingChanged || windowFresh || (visible && staticListChanged(staticShadows, staticVersion));
    }

    /** Tells whether the dynamic layer has to be cast again: it also depends on the phase of the camera grid. */
    private boolean dynamicLayerChanged(boolean everythingChanged, boolean visible, ShadowList dynamicShadows) {
        return everythingChanged || phaseChanged() || (visible && !dynamicShadows.sameShadowsAs(castDynamic));
    }

    /** Tells whether the phase of the camera grid is not the one the last cast was made at. */
    private boolean phaseChanged() {
        return Doubles.differs(phaseX, lastPhaseX) || Doubles.differs(phaseY, lastPhaseY);
    }

    /** Tells whether the static layer moved against the dynamic one, or the opacity is not the one shown. */
    private boolean loweringShifted(double opacity) {
        return shiftX != shownShiftX || shiftY != shownShiftY || Doubles.differs(opacity, shownOpacity);
    }

    /** Tells whether something that both layers share changed since the last cast. */
    private boolean everythingChanged(CanvasView view, SunShadow sunShadow) {
        return !hasLastFrame || sunShadow.visible() != castVisible || gridChanged(view) || sunChanged(sunShadow);
    }

    /** Tells whether the canvas grid has another size or art scale than the last cast was made at. */
    private boolean gridChanged(CanvasView view) {
        return cellsWide != lastCellsWide || cellsHigh != lastCellsHigh || Doubles.differs(view.artScale(), lastArtScale);
    }

    /** Tells whether the direction or the length of the shadows is not what the last cast was made at. */
    private boolean sunChanged(SunShadow sunShadow) {
        return Doubles.differs(sunShadow.angle(), lastAngle) || Doubles.differs(sunShadow.length(), lastLength);
    }

    /** Tells whether the static list differs from the one the static layer was last cast from. */
    private boolean staticListChanged(ShadowList staticShadows, long staticVersion) {
        if (staticVersion == NO_VERSION) {
            return !staticShadows.sameShadowsAs(castStatic);
        }
        return staticVersion != castStaticVersion;
    }

    /** Lays the layers that hold shadows over the canvas at the opacity of the shadows. */
    private void layOver(GraphicsContext gc, double artScale, double opacity) {
        final boolean hasStatic = staticLayer.hasShadows();
        final boolean hasDynamic = dynamicLayer.hasShadows();
        if (!hasStatic && !hasDynamic) {
            return;
        }
        gc.setGlobalAlpha(opacity);
        final double nudge = SAMPLE_NUDGE / artScale;
        if (hasStatic) {
            drawStaticLayer(gc, artScale, nudge);
        }
        if (hasDynamic) {
            final int left = dynamicLayer.stampedLeft();
            final int top = dynamicLayer.stampedTop();
            final int wide = dynamicLayer.stampedRight() - left;
            final int high = dynamicLayer.stampedBottom() - top;
            gc.drawImage(dynamicImage, left + nudge, top + nudge, wide, high, phaseX + (left - 1) * artScale,
                    phaseY + (top - 1) * artScale, wide * artScale, high * artScale);
        }
        gc.setGlobalAlpha(1.0);
    }

    /** Draws the part of the static layer that the canvas shows, at the place the camera puts it. */
    private void drawStaticLayer(GraphicsContext gc, double artScale, double nudge) {
        final int left = Math.max(staticLayer.stampedLeft(), -shiftX);
        final int top = Math.max(staticLayer.stampedTop(), -shiftY);
        final int right = Math.min(staticLayer.stampedRight(), cellsWide - shiftX);
        final int bottom = Math.min(staticLayer.stampedBottom(), cellsHigh - shiftY);
        if (right > left && bottom > top) {
            final double wide = (double) right - left;
            final double high = (double) bottom - top;
            gc.drawImage(staticImage, left + nudge, top + nudge, wide, high,
                    phaseX + (left + shiftX - 1) * artScale,
                    phaseY + (top + shiftY - 1) * artScale, wide * artScale, high * artScale);
        }
    }

    /**
     * Creates an image for the cells of a layer, with {@link #EDGE_PAD} more column and row than the layer has. The source
     * of a draw starts a nudge into its first cell, so a draw that reaches the last cell reads the nudge past it, and
     * what a pipeline samples outside an image is up to it. The extra column and row, which {@link #uploadPadded} fills
     * with the cells next to them, keep that source inside the image so the draw keeps its exact scale.
     *
     * @param cellsWide the number of cells of the layer across
     * @param cellsHigh the number of cells of the layer down
     * @return an empty image with room for the layer and its edge pad
     */
    static WritableImage newPaddedImage(int cellsWide, int cellsHigh) {
        return new WritableImage(cellsWide + EDGE_PAD, cellsHigh + EDGE_PAD);
    }

    /**
     * Tells whether an image made by {@link #newPaddedImage} fits a layer.
     *
     * @param image     the image, or null when there is none yet
     * @param cellsWide the number of cells of the layer across
     * @param cellsHigh the number of cells of the layer down
     * @return true when the image has the size {@link #newPaddedImage} gives for the layer
     */
    static boolean fitsPadded(WritableImage image, int cellsWide, int cellsHigh) {
        return image != null && (int) image.getWidth() == cellsWide + EDGE_PAD
                && (int) image.getHeight() == cellsHigh + EDGE_PAD;
    }

    /**
     * Copies a box of cells into an image made by {@link #newPaddedImage}, and where the box reaches the last column or
     * the last row of the layer repeats those cells into the pad past them. Nothing is allocated.
     *
     * @param image     the padded image
     * @param cellsWide the number of cells of the layer across, which is also the stride of the pixels
     * @param cellsHigh the number of cells of the layer down
     * @param box       the box of cells as left, top, right and bottom, the last two exclusive
     * @param pixels    the cells of the layer as premultiplied ARGB, row by row
     */
    static void uploadPadded(WritableImage image, int cellsWide, int cellsHigh, int[] box, int[] pixels) {
        final int left = box[BOX_LEFT];
        final int top = box[BOX_TOP];
        final int right = box[BOX_RIGHT];
        final int bottom = box[BOX_BOTTOM];
        final var writer = image.getPixelWriter();
        final var format = PixelFormat.getIntArgbPreInstance();
        writer.setPixels(left, top, right - left, bottom - top, format, pixels, top * cellsWide + left, cellsWide);
        final boolean lastColumn = right == cellsWide;
        final boolean lastRow = bottom == cellsHigh;
        if (lastColumn) {
            writer.setPixels(cellsWide, top, EDGE_PAD, bottom - top, format, pixels, top * cellsWide + cellsWide - 1,
                    cellsWide);
        }
        if (lastRow) {
            writer.setPixels(left, cellsHigh, right - left, EDGE_PAD, format, pixels,
                    (cellsHigh - 1) * cellsWide + left, cellsWide);
        }
        if (lastColumn && lastRow) {
            writer.setPixels(cellsWide, cellsHigh, EDGE_PAD, EDGE_PAD, format, pixels,
                    (cellsHigh - 1) * cellsWide + cellsWide - 1, cellsWide);
        }
    }

    /**
     * Casts the layers that changed, lowers the dynamic cells over the area that changed and uploads what changed.
     *
     * @param staticChanged  whether the static layer is cast again
     * @param dynamicChanged whether the dynamic layer is cast again
     * @param artScale       the integer number of screen pixels per art pixel
     * @param sunShadow            the direction, length and strength of the shadows, whose opacity the lowering of the
     *                       dynamic cells depends on
     */
    private void cast(ShadowList staticShadows, ShadowList dynamicShadows, boolean staticChanged,
                      boolean dynamicChanged, double artScale, SunShadow sunShadow) {
        final double opacity = sunShadow.opacity();
        final int scale = (int) Math.round(artScale);
        Arrays.fill(region, 0);
        if (dynamicShown.length != cellsWide * cellsHigh) {
            dynamicShown = new int[cellsWide * cellsHigh];
        }
        if (staticChanged) {
            castStaticLayer(staticShadows, sunShadow, artScale, scale);
            staticRecasts++;
        }
        if (dynamicChanged) {
            castDynamicLayer(dynamicShadows, sunShadow, artScale, scale);
        } else if (dynamicLayer.hasShadows()) {
            // The cells did not change, but what the static layer lowers them by did
            region[BOX_LEFT] = dynamicLayer.stampedLeft();
            region[BOX_TOP] = dynamicLayer.stampedTop();
            region[BOX_RIGHT] = dynamicLayer.stampedRight();
            region[BOX_BOTTOM] = dynamicLayer.stampedBottom();
        }
        final boolean uploadedStatic = staticChanged && lastStaticUploaded;
        lowerRegion(opacity);
        final boolean uploadedDynamic = uploadDynamic();
        if (uploadedStatic || uploadedDynamic) {
            uploads++;
        }
        shownShiftX = shiftX;
        shownShiftY = shiftY;
        shownOpacity = opacity;
        hasLastFrame = true;
        castVisible = sunShadow.visible();
        lastCellsWide = cellsWide;
        lastCellsHigh = cellsHigh;
        lastPhaseX = phaseX;
        lastPhaseY = phaseY;
        lastArtScale = artScale;
        lastAngle = sunShadow.angle();
        lastLength = sunShadow.length();
        if (staticChanged || dynamicChanged) {
            recasts++;
        }
    }

    /** Clears the static layer, casts the static shadows into the window and uploads what changed. */
    private void castStaticLayer(ShadowList shadows, SunShadow sunShadow, double artScale, int scale) {
        staticLayer.prepare(windowCellsWide, windowCellsHigh, sunShadow.angle(), sunShadow.length(), scale);
        if (sunShadow.visible()) {
            // The window grid starts at the world cell of its first column, with the cell of border the layers have
            final var grid = new GridFrame(artScale, windowCellX * artScale, windowCellY * artScale, snappedX,
                    snappedY);
            for (int i = 0, count = shadows.size(); i < count; i++) {
                castOne(staticLayer, shadows, i, grid);
            }
            // A caller that keeps a version says by it when the list changed, so the copy to compare with is for the
            // caller that does not
            castStaticVersion = pendingStaticVersion;
            if (pendingStaticVersion == NO_VERSION) {
                castStatic.copyFrom(shadows);
            }
        }
        if (!fitsPadded(staticImage, windowCellsWide, windowCellsHigh)) {
            staticImage = newPaddedImage(windowCellsWide, windowCellsHigh);
        }
        staticLayer.changedBoxInto(changedScratch);
        lastStaticUploaded = hasCells(changedScratch);
        if (lastStaticUploaded) {
            staticLayer.readPixels(
                    cells -> uploadPadded(staticImage, windowCellsWide, windowCellsHigh, changedScratch, cells));
        }
        staticLayer.handedOn();
        windowFresh = false;
    }

    /** Clears the dynamic layer, casts the dynamic shadows into it and takes the area it changed as the region. */
    private void castDynamicLayer(ShadowList shadows, SunShadow sunShadow, double artScale, int scale) {
        dynamicLayer.prepare(cellsWide, cellsHigh, sunShadow.angle(), sunShadow.length(), scale);
        if (sunShadow.visible()) {
            final var grid = new GridFrame(artScale, phaseX, phaseY, 0.0, 0.0);
            for (int i = 0, count = shadows.size(); i < count; i++) {
                castOne(dynamicLayer, shadows, i, grid);
            }
            castDynamic.copyFrom(shadows);
        }
        dynamicLayer.changedBoxInto(changedScratch);
        if (hasCells(changedScratch)) {
            System.arraycopy(changedScratch, 0, region, 0, BOX_VALUES);
        }
        dynamicLayer.handedOn();
    }

    /**
     * Sets every dynamic cell of the region to what is drawn of it. A cell no static shadow covers is drawn as it is.
     * Where a static shadow covers it, the two draws land on top of each other, so the cell is lowered to what must be
     * added to the static shadow for the stronger of the two to show: nothing when the static one is as strong, and
     * otherwise the difference, scaled for the way the two layers blend at the opacity they are drawn at.
     */
    private void lowerRegion(double opacity) {
        if (!hasCells(region)) {
            return;
        }
        for (int y = region[BOX_TOP]; y < region[BOX_BOTTOM]; y++) {
            for (int x = region[BOX_LEFT]; x < region[BOX_RIGHT]; x++) {
                final int i = y * cellsWide + x;
                dynamicShown[i] = shownAlpha(dynamicLayer.alphaAt(x, y), x, y, opacity) << ALPHA_SHIFT;
            }
        }
    }

    /** Returns the alpha a dynamic cell is drawn with, lowered by the static cell under it when there is one. */
    private int shownAlpha(int dynamicAlpha, int x, int y, double opacity) {
        final int staticColumn = x - shiftX;
        final int staticRow = y - shiftY;
        final boolean inWindow = staticColumn >= 0 && staticColumn < windowCellsWide && staticRow >= 0
                && staticRow < windowCellsHigh;
        if (dynamicAlpha == 0 || !inWindow) {
            return dynamicAlpha;
        }
        final int staticAlpha = staticLayer.alphaAt(staticColumn, staticRow);
        if (staticAlpha == 0) {
            return dynamicAlpha;
        }
        return dynamicAlpha <= staticAlpha ? 0 : lowered(dynamicAlpha, staticAlpha, opacity);
    }

    /**
     * Returns the alpha a dynamic cell is drawn with over a static cell that is fainter than it. With both drawn at the
     * same opacity {@code o}, the two blend to {@code 1 - (1 - o * s) * (1 - o * d')}, and that is {@code o * d} for the
     * stronger of the two when {@code d' = (d - s) / (1 - o * s)}.
     */
    private static int lowered(int dynamicAlpha, int staticAlpha, double opacity) {
        final double s = staticAlpha / ALPHA_RANGE;
        final double d = dynamicAlpha / ALPHA_RANGE;
        return (int) Math.min(MAX_ALPHA, Math.round(ALPHA_RANGE * (d - s) / (1.0 - opacity * s)));
    }

    /**
     * Casts one shadow of a list into a layer, anchored the way its object is drawn. A sprite is drawn with its edges
     * rounded in screen space, so the base is rounded there too: the shadow is moved to the screen by the offset its
     * list is given relative to, rounded the way the sprite is, and moved back. Where the rounding of a tie depends on
     * the position on the screen, which it does for a sprite an odd number of pixels wide, the shadow then falls where
     * the sprite does.
     *
     * @param grid the art grid the layer is cast on and the offset its list is given relative to
     */
    private static void castOne(ShadowLayer layer, ShadowList shadows, int i, GridFrame grid) {
        final double artScale = grid.artScale();
        final double gridPhaseX = grid.phaseX();
        final double gridPhaseY = grid.phaseY();
        final double offsetX = grid.offsetX();
        final double offsetY = grid.offsetY();
        final int alpha = (int) Math.round(shadows.fade(i) * ALPHA_RANGE);
        final var sprite = shadows.silhouette(i);
        if (sprite != null) {
            if (sprite.width() <= 0) {
                return;
            }
            // The layer grid has a cell of border before the first cell that lies on the canvas
            final double baseX = ShadowAnchor.baseX(shadows.centreX(i) + offsetX, shadows.drawnWidth(i), artScale)
                    - offsetX;
            final double baseY = ShadowAnchor.baseY(shadows.centreY(i) + offsetY, shadows.drawnHeight(i), artScale)
                    - offsetY;
            final double anchorX = ShadowAnchor.cell(baseX, gridPhaseX, artScale);
            final double anchorY = ShadowAnchor.cell(baseY, gridPhaseY, artScale);
            final double heightFactor = shadows.groundRow(i) > 0 ? shadows.drawnHeight(i) / shadows.groundRow(i) : 1.0;
            layer.castSprite(sprite, anchorX, anchorY, shadows.groundRow(i), shadows.widthArt(i) / sprite.width(),
                    heightFactor, alpha);
        } else {
            // An object drawn from shapes has no sprite to follow, so its ellipse sits on the art grid
            final double anchorX = Math.rint((shadows.centreX(i) - gridPhaseX) / artScale) + 1;
            final double anchorY = Math.rint((shadows.centreY(i) - gridPhaseY) / artScale) + 1;
            layer.castEllipse(anchorX, anchorY, shadows.widthArt(i), alpha);
        }
    }

    /**
     * Copies the dynamic cells of the region into the dynamic image, creating the image when the canvas has a new size.
     *
     * @return true when cells were uploaded
     */
    private boolean uploadDynamic() {
        if (!fitsPadded(dynamicImage, cellsWide, cellsHigh)) {
            dynamicImage = newPaddedImage(cellsWide, cellsHigh);
        }
        if (hasCells(region)) {
            uploadPadded(dynamicImage, cellsWide, cellsHigh, region, dynamicShown);
            System.arraycopy(region, 0, lastUpload, 0, BOX_VALUES);
            return true;
        }
        return false;
    }

    /** Tells whether a box of cells holds at least one cell. */
    private static boolean hasCells(int[] box) {
        return box[BOX_RIGHT] > box[BOX_LEFT] && box[BOX_BOTTOM] > box[BOX_TOP];
    }

    /** @return how many frames cast shadows into a layer so far */
    int recasts() {
        return recasts;
    }

    /** @return how many times the static layer was cast so far */
    int staticRecasts() {
        return staticRecasts;
    }

    /** @return how many frames uploaded cells to an image so far */
    int uploads() {
        return uploads;
    }

    /** @return a copy of the area of the last upload to the dynamic image as left, top, right and bottom */
    int[] lastUpload() {
        return lastUpload.clone();
    }
}
