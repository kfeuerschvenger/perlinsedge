package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowLayer;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

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

    /**
     * The fewest cells the static window reaches past the canvas on every side when it is placed. A canvas that is
     * large in cells gets a margin of {@link #WINDOW_MARGIN_SHARE} of its size instead, see {@link #marginFor}.
     */
    static final int WINDOW_MARGIN_CELLS = 40;

    /**
     * The share of the canvas, across or down, that the static window reaches past it on every side when it is placed.
     * The camera can pan this far either way before the window has to move, and a recast of the static layer is what a
     * pan of that distance is paid with, so a margin that grows with the canvas keeps a camera that follows the player
     * from casting again every few tiles.
     */
    static final double WINDOW_MARGIN_SHARE = 0.25;

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

    /** The static shadows the static layer was last cast from, so an unchanged list is recognised. */
    private final ShadowList castStatic = new ShadowList();
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
    /** The whole art pixels the camera offset of the frame spans along X, rounded down. */
    private int wholeCellsX;
    /** The whole art pixels the camera offset of the frame spans along Y, rounded down. */
    private int wholeCellsY;
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
    private final int[] lastUpload = new int[4];
    /** The area of the dynamic layer the current frame changed, empty when right is not past left. */
    private final int[] region = new int[4];

    /**
     * Works out where the canvas grid of a frame lies and places the static window when the frame needs a new one. A
     * caller that collects the static shadows asks for this first, so it knows which part of the world they must cover,
     * see {@link #windowLeft()}; {@link #draw} does the same, and doing it twice in a frame changes nothing.
     *
     * @param artScale the integer number of screen pixels per art pixel
     * @param originX  screen X of the camera origin, which the art grid is aligned to
     * @param originY  screen Y of the camera origin
     * @param canvasW  canvas width
     * @param canvasH  canvas height
     */
    public void beginFrame(double artScale, double originX, double originY, double canvasW, double canvasH) {
        cellsWide = (int) Math.ceil(canvasW / artScale) + 2;
        cellsHigh = (int) Math.ceil(canvasH / artScale) + 2;
        snappedX = Math.rint(originX);
        snappedY = Math.rint(originY);
        wholeCellsX = (int) Math.floor(snappedX / artScale);
        wholeCellsY = (int) Math.floor(snappedY / artScale);
        phaseX = snappedX - wholeCellsX * artScale;
        phaseY = snappedY - wholeCellsY * artScale;
        final int marginX = marginFor(cellsWide);
        final int marginY = marginFor(cellsHigh);
        // The canvas shows the window shifted by the whole cells of the camera offset plus where the window was placed;
        // it stays valid while the canvas lies inside the window, and a window that has to move is placed again around
        // the camera of this frame
        final boolean fits = windowPlaced && artScale == windowScale && cellsWide == windowCanvasWide
                && cellsHigh == windowCanvasHigh && Math.abs(windowCellX + wholeCellsX + marginX) <= marginX
                && Math.abs(windowCellY + wholeCellsY + marginY) <= marginY;
        if (!fits) {
            windowMarginX = marginX;
            windowMarginY = marginY;
            windowCellX = -marginX - wholeCellsX;
            windowCellY = -marginY - wholeCellsY;
            windowCellsWide = cellsWide + 2 * marginX;
            windowCellsHigh = cellsHigh + 2 * marginY;
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
        return (2.0 * windowMarginX + 2.0) * windowScale;
    }

    /**
     * Returns how far the static window can reach past the canvas above or below at most, in screen pixels, see
     * {@link #windowOverhangXPixels()}.
     *
     * @return the vertical overhang in screen pixels
     */
    public double windowOverhangYPixels() {
        return (2.0 * windowMarginY + 2.0) * windowScale;
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
     * @param artScale       the integer number of screen pixels per art pixel
     * @param originX        screen X of the camera origin, which the art grid is aligned to
     * @param originY        screen Y of the camera origin
     * @param canvasW        canvas width
     * @param canvasH        canvas height
     * @param angle          direction of the shadows on the screen in radians
     * @param length         how long shadows are as a multiple of the height of what casts them
     * @param strength       how dark shadows are as a share of their full opacity, 0 to 1
     */
    public void draw(GraphicsContext gc, ShadowList staticShadows, ShadowList dynamicShadows, double artScale,
                     double originX, double originY, double canvasW, double canvasH, double angle, double length,
                     double strength) {
        beginFrame(artScale, originX, originY, canvasW, canvasH);
        final boolean visible = strength >= FxShadowHelper.MIN_VISIBLE_STRENGTH;
        final double opacity = GroundShadow.OPACITY * Math.min(1.0, strength);

        // What both layers share: a change in any of it casts both again. While the shadows are too faint to see nothing
        // is cast, so the lists do not matter
        final boolean everythingChanged = !hasLastFrame || visible != castVisible || cellsWide != lastCellsWide
                || cellsHigh != lastCellsHigh || artScale != lastArtScale || angle != lastAngle || length != lastLength;
        // The static layer does not depend on the phase of the camera grid, only the dynamic one does
        final boolean staticChanged = everythingChanged || windowFresh
                || (visible && !staticShadows.sameShadowsAs(castStatic));
        final boolean dynamicChanged = everythingChanged || phaseX != lastPhaseX || phaseY != lastPhaseY
                || (visible && !dynamicShadows.sameShadowsAs(castDynamic));
        // The dynamic cells are lowered against the static layer, so they are worked out again when it moved against
        // them or when the opacity, which the lowering depends on, changed
        final boolean loweringChanged = staticChanged || shiftX != shownShiftX || shiftY != shownShiftY
                || opacity != shownOpacity;
        if (staticChanged || dynamicChanged || loweringChanged) {
            cast(staticShadows, dynamicShadows, staticChanged, dynamicChanged, visible, opacity, artScale, angle,
                    length);
        }

        if (!visible) {
            return;
        }
        final boolean hasStatic = staticLayer.hasShadows();
        final boolean hasDynamic = dynamicLayer.hasShadows();
        if (!hasStatic && !hasDynamic) {
            return;
        }
        gc.setGlobalAlpha(opacity);
        final double nudge = SAMPLE_NUDGE / artScale;
        if (hasStatic) {
            // Only the part of the static layer that the canvas shows is drawn, at the place the camera puts it
            final int left = Math.max(staticLayer.stampedLeft(), -shiftX);
            final int top = Math.max(staticLayer.stampedTop(), -shiftY);
            final int right = Math.min(staticLayer.stampedRight(), cellsWide - shiftX);
            final int bottom = Math.min(staticLayer.stampedBottom(), cellsHigh - shiftY);
            if (right > left && bottom > top) {
                gc.drawImage(staticImage, left + nudge, top + nudge, sourceSpan(left, right, windowCellsWide, nudge),
                        sourceSpan(top, bottom, windowCellsHigh, nudge), phaseX + (left + shiftX - 1) * artScale,
                        phaseY + (top + shiftY - 1) * artScale, (right - left) * artScale, (bottom - top) * artScale);
            }
        }
        if (hasDynamic) {
            final int left = dynamicLayer.stampedLeft();
            final int top = dynamicLayer.stampedTop();
            final int wide = dynamicLayer.stampedRight() - left;
            final int high = dynamicLayer.stampedBottom() - top;
            gc.drawImage(dynamicImage, left + nudge, top + nudge, sourceSpan(left, left + wide, cellsWide, nudge),
                    sourceSpan(top, top + high, cellsHigh, nudge), phaseX + (left - 1) * artScale,
                    phaseY + (top - 1) * artScale, wide * artScale, high * artScale);
        }
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Returns how many cells of an image to read from a source rectangle that starts a nudge into its first cell. The
     * nudge moves the whole rectangle, so a rectangle that reaches the last cell of the image would read the nudge past
     * its edge, and it is shortened by the nudge there instead. What a pipeline samples outside an image is up to it, so
     * the source is kept inside.
     *
     * @param from       the first cell of the rectangle
     * @param to         the cell after the last of the rectangle
     * @param imageCells the number of cells of the image along the axis
     * @param nudge      how far into its first cell the rectangle starts, in cells
     * @return the span to read along the axis, in cells
     */
    static double sourceSpan(int from, int to, int imageCells, double nudge) {
        return to + nudge > imageCells ? (to - from) - nudge : to - from;
    }

    /**
     * Casts the layers that changed, lowers the dynamic cells over the area that changed and uploads what changed.
     *
     * @param staticChanged  whether the static layer is cast again
     * @param dynamicChanged whether the dynamic layer is cast again
     * @param visible        whether the shadows are strong enough to be cast at all
     * @param opacity        the opacity the layers are drawn at, which the lowering of the dynamic cells depends on
     */
    private void cast(ShadowList staticShadows, ShadowList dynamicShadows, boolean staticChanged,
                      boolean dynamicChanged, boolean visible, double opacity, double artScale, double angle,
                      double length) {
        final int scale = (int) Math.round(artScale);
        region[0] = 0;
        region[1] = 0;
        region[2] = 0;
        region[3] = 0;
        if (dynamicShown.length != cellsWide * cellsHigh) {
            dynamicShown = new int[cellsWide * cellsHigh];
        }
        if (staticChanged) {
            castStaticLayer(staticShadows, visible, artScale, angle, length, scale);
            staticRecasts++;
        }
        if (dynamicChanged) {
            castDynamicLayer(dynamicShadows, visible, artScale, angle, length, scale);
        } else if (dynamicLayer.hasShadows()) {
            // The cells did not change, but what the static layer lowers them by did
            region[0] = dynamicLayer.stampedLeft();
            region[1] = dynamicLayer.stampedTop();
            region[2] = dynamicLayer.stampedRight();
            region[3] = dynamicLayer.stampedBottom();
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
        castVisible = visible;
        lastCellsWide = cellsWide;
        lastCellsHigh = cellsHigh;
        lastPhaseX = phaseX;
        lastPhaseY = phaseY;
        lastArtScale = artScale;
        lastAngle = angle;
        lastLength = length;
        if (staticChanged || dynamicChanged) {
            recasts++;
        }
    }

    /** Clears the static layer, casts the static shadows into the window and uploads what changed. */
    private void castStaticLayer(ShadowList shadows, boolean visible, double artScale, double angle, double length,
                                 int scale) {
        staticLayer.prepare(windowCellsWide, windowCellsHigh, angle, length, scale);
        if (visible) {
            // The window grid starts at the world cell of its first column, with the cell of border the layers have
            final double worldPhaseX = windowCellX * artScale;
            final double worldPhaseY = windowCellY * artScale;
            for (int i = 0, count = shadows.size(); i < count; i++) {
                castOne(staticLayer, shadows, i, worldPhaseX, worldPhaseY, artScale, snappedX, snappedY);
            }
            castStatic.copyFrom(shadows);
        }
        if (staticImage == null || (int) staticImage.getWidth() != windowCellsWide
                || (int) staticImage.getHeight() != windowCellsHigh) {
            staticImage = new WritableImage(windowCellsWide, windowCellsHigh);
        }
        final int[] changed = staticLayer.changedBox();
        lastStaticUploaded = changed[2] > changed[0] && changed[3] > changed[1];
        if (lastStaticUploaded) {
            staticImage.getPixelWriter().setPixels(changed[0], changed[1], changed[2] - changed[0],
                    changed[3] - changed[1], PixelFormat.getIntArgbPreInstance(), staticLayer.pixels(),
                    changed[1] * windowCellsWide + changed[0], windowCellsWide);
        }
        staticLayer.handedOn();
        windowFresh = false;
    }

    /** Clears the dynamic layer, casts the dynamic shadows into it and takes the area it changed as the region. */
    private void castDynamicLayer(ShadowList shadows, boolean visible, double artScale, double angle, double length,
                                  int scale) {
        dynamicLayer.prepare(cellsWide, cellsHigh, angle, length, scale);
        if (visible) {
            for (int i = 0, count = shadows.size(); i < count; i++) {
                castOne(dynamicLayer, shadows, i, phaseX, phaseY, artScale, 0.0, 0.0);
            }
            castDynamic.copyFrom(shadows);
        }
        final int[] changed = dynamicLayer.changedBox();
        if (changed[2] > changed[0] && changed[3] > changed[1]) {
            System.arraycopy(changed, 0, region, 0, 4);
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
        if (!(region[2] > region[0] && region[3] > region[1])) {
            return;
        }
        final int[] moving = dynamicLayer.pixels();
        final int[] fixed = staticLayer.pixels();
        for (int y = region[1]; y < region[3]; y++) {
            final int staticRow = y - shiftY;
            for (int x = region[0]; x < region[2]; x++) {
                final int i = y * cellsWide + x;
                final int dynamicAlpha = moving[i] >>> 24;
                int shown = dynamicAlpha;
                final int staticColumn = x - shiftX;
                if (dynamicAlpha > 0 && staticColumn >= 0 && staticColumn < windowCellsWide && staticRow >= 0
                        && staticRow < windowCellsHigh) {
                    final int staticAlpha = fixed[staticRow * windowCellsWide + staticColumn] >>> 24;
                    if (staticAlpha > 0) {
                        shown = dynamicAlpha <= staticAlpha ? 0 : lowered(dynamicAlpha, staticAlpha, opacity);
                    }
                }
                dynamicShown[i] = shown << 24;
            }
        }
    }

    /**
     * Returns the alpha a dynamic cell is drawn with over a static cell that is fainter than it. With both drawn at the
     * same opacity {@code o}, the two blend to {@code 1 - (1 - o * s) * (1 - o * d')}, and that is {@code o * d} for the
     * stronger of the two when {@code d' = (d - s) / (1 - o * s)}.
     */
    private static int lowered(int dynamicAlpha, int staticAlpha, double opacity) {
        final double s = staticAlpha / 255.0;
        final double d = dynamicAlpha / 255.0;
        return (int) Math.min(255, Math.round(255.0 * (d - s) / (1.0 - opacity * s)));
    }

    /**
     * Casts one shadow of a list into a layer, anchored the way its object is drawn. A sprite is drawn with its edges
     * rounded in screen space, so the base is rounded there too: the shadow is moved to the screen by the offset its
     * list is given relative to, rounded the way the sprite is, and moved back. Where the rounding of a tie depends on
     * the position on the screen, which it does for a sprite an odd number of pixels wide, the shadow then falls where
     * the sprite does.
     *
     * @param offsetX what the list adds to a world X to get the screen X: the camera offset for the static list, zero
     *                for the dynamic one, which is given in screen coordinates
     * @param offsetY the same along Y
     */
    private void castOne(ShadowLayer layer, ShadowList shadows, int i, double gridPhaseX, double gridPhaseY,
                         double artScale, double offsetX, double offsetY) {
        final int alpha = (int) Math.round(shadows.fade(i) * 255.0);
        final Silhouette sprite = shadows.silhouette(i);
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
        if (dynamicImage == null || (int) dynamicImage.getWidth() != cellsWide
                || (int) dynamicImage.getHeight() != cellsHigh) {
            dynamicImage = new WritableImage(cellsWide, cellsHigh);
        }
        if (region[2] > region[0] && region[3] > region[1]) {
            dynamicImage.getPixelWriter().setPixels(region[0], region[1], region[2] - region[0],
                    region[3] - region[1], PixelFormat.getIntArgbPreInstance(), dynamicShown,
                    region[1] * cellsWide + region[0], cellsWide);
            System.arraycopy(region, 0, lastUpload, 0, 4);
            return true;
        }
        return false;
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
