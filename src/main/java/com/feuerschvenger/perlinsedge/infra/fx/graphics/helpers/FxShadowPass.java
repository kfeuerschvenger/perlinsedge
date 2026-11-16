package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowLayer;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * The shadow pass of a frame: every shadow is cast at full black, and the result is laid over the canvas once, at the
 * shadow opacity times the strength of the day. Each cell keeps the strongest alpha of the shadows that cover it instead
 * of their sum, so two overlapping shadows are exactly as dark as one.
 * <p>
 * The layer has one cell per art pixel and is drawn at the integer art scale with smoothing off, so every shadow pixel
 * is a crisp square block at every zoom. Its grid is aligned to the camera, so the shadows of the tiles sit on the same
 * grid as the tile art. The shadow of a sprite is anchored on the base of the sprite as it is drawn, see
 * {@link ShadowAnchor}, so the two move together.
 * <p>
 * The shadows come in two lists that are cast into two layers. The static ones, of the resources and buildings on the
 * tiles, change only when the camera, the sun or the world does. The dynamic ones, of the player and the enemies, change
 * with every step. Keeping them apart means a frame in which only a creature moved casts nothing static again: the
 * dynamic layer is cast anew and the two are combined, cell by cell with the stronger alpha winning, only over the
 * area the dynamic shadows touched before and touch now. That area is the only part of the image that is uploaded.
 * <p>
 * Each layer depends only on the camera grid, the art scale, the size of the canvas, the sun and its own list of
 * shadows, so when none of them changed since the last frame the layer is kept as it is. The strength of the day only
 * scales the composite and does not take part. A frame with no visible shadows draws nothing.
 */
public final class FxShadowPass {

    /**
     * How far into a cell, as a share of a screen pixel per art pixel, the source of the composite is moved. The software
     * pipeline walks the source in fixed point, so a step of one third of a cell is stored a hair short and the first
     * pixel of every third cell samples the cell before it. A quarter of a screen pixel is far from every cell edge for
     * a sampler that reads at the corner of a pixel and for one that reads at its centre alike.
     */
    private static final double SAMPLE_NUDGE = 0.25;

    /** The shadows of the things that stay where they are, cast into their own layer. */
    private final ShadowLayer staticLayer = new ShadowLayer();
    /** The shadows of the things that move, cast into their own layer. */
    private final ShadowLayer dynamicLayer = new ShadowLayer();
    /** The cells of both layers combined, the stronger alpha of the two in every cell, which the image mirrors. */
    private int[] combined = new int[0];
    /** The image that mirrors the combined cells for drawing. */
    private WritableImage image;

    /** The static shadows the static layer was last cast from, so an unchanged list is recognised. */
    private final ShadowList castStatic = new ShadowList();
    /** The dynamic shadows the dynamic layer was last cast from, so an unchanged list is recognised. */
    private final ShadowList castDynamic = new ShadowList();
    /** Whether the layers were cast at all last time, which is false while the shadows are too faint to see. */
    private boolean castVisible;
    /** Whether there is a last frame to compare with. */
    private boolean hasLastFrame;
    /** The number of cells across the layers were last cast at. */
    private int lastCellsWide;
    /** The number of cells down the layers were last cast at. */
    private int lastCellsHigh;
    /** The horizontal phase of the camera grid the layers were last cast at. */
    private double lastPhaseX;
    /** The vertical phase of the camera grid the layers were last cast at. */
    private double lastPhaseY;
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
    /** How many times the image was uploaded to, which a frame that kept the layers does not count. */
    private int uploads;
    /** The area of the last upload as left, top, right and bottom, with the last two exclusive. */
    private final int[] lastUpload = new int[4];
    /** The area the current frame changed, which is combined and uploaded, empty when right is not past left. */
    private final int[] region = new int[4];

    /**
     * Casts the shadows of a frame, only as far as they differ from the last frame, and lays them over the canvas.
     *
     * @param gc             the graphics context, with image smoothing off and no transform
     * @param staticShadows  the shadows of the resources and buildings, which stand still
     * @param dynamicShadows the shadows of the player and the enemies, which move
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
        final int cellsWide = (int) Math.ceil(canvasW / artScale) + 2;
        final int cellsHigh = (int) Math.ceil(canvasH / artScale) + 2;
        final double snappedX = Math.rint(originX);
        final double snappedY = Math.rint(originY);
        final double phaseX = snappedX - Math.floor(snappedX / artScale) * artScale;
        final double phaseY = snappedY - Math.floor(snappedY / artScale) * artScale;
        final boolean visible = strength >= FxShadowHelper.MIN_VISIBLE_STRENGTH;

        // What both layers share: a change in any of it casts both again. While the shadows are too faint to see nothing
        // is cast, so the lists do not matter
        final boolean everythingChanged = !hasLastFrame || visible != castVisible || cellsWide != lastCellsWide
                || cellsHigh != lastCellsHigh || phaseX != lastPhaseX || phaseY != lastPhaseY
                || artScale != lastArtScale || angle != lastAngle || length != lastLength;
        final boolean staticChanged = everythingChanged || (visible && !staticShadows.sameShadowsAs(castStatic));
        final boolean dynamicChanged = everythingChanged || (visible && !dynamicShadows.sameShadowsAs(castDynamic));
        if (staticChanged || dynamicChanged) {
            cast(staticShadows, dynamicShadows, staticChanged, dynamicChanged, visible, cellsWide, cellsHigh, phaseX,
                    phaseY, artScale, angle, length);
        }

        if (!visible) {
            return;
        }
        final boolean hasStatic = staticLayer.hasShadows();
        final boolean hasDynamic = dynamicLayer.hasShadows();
        if (!hasStatic && !hasDynamic) {
            return;
        }
        final int left = Math.min(hasStatic ? staticLayer.stampedLeft() : Integer.MAX_VALUE,
                hasDynamic ? dynamicLayer.stampedLeft() : Integer.MAX_VALUE);
        final int top = Math.min(hasStatic ? staticLayer.stampedTop() : Integer.MAX_VALUE,
                hasDynamic ? dynamicLayer.stampedTop() : Integer.MAX_VALUE);
        final int right = Math.max(hasStatic ? staticLayer.stampedRight() : Integer.MIN_VALUE,
                hasDynamic ? dynamicLayer.stampedRight() : Integer.MIN_VALUE);
        final int bottom = Math.max(hasStatic ? staticLayer.stampedBottom() : Integer.MIN_VALUE,
                hasDynamic ? dynamicLayer.stampedBottom() : Integer.MIN_VALUE);
        final int wide = right - left;
        final int high = bottom - top;
        gc.setGlobalAlpha(GroundShadow.OPACITY * Math.min(1.0, strength));
        final double nudge = SAMPLE_NUDGE / artScale;
        gc.drawImage(image, left + nudge, top + nudge, wide, high, phaseX + (left - 1) * artScale, phaseY + (top - 1) * artScale,
                wide * artScale, high * artScale);
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Casts the layers that changed, combines them over the area that changed and uploads that area.
     *
     * @param staticChanged  whether the static layer is cast again
     * @param dynamicChanged whether the dynamic layer is cast again
     * @param visible        whether the shadows are strong enough to be cast at all
     */
    private void cast(ShadowList staticShadows, ShadowList dynamicShadows, boolean staticChanged,
                      boolean dynamicChanged, boolean visible, int cellsWide, int cellsHigh, double phaseX,
                      double phaseY, double artScale, double angle, double length) {
        final int scale = (int) Math.round(artScale);
        region[0] = 0;
        region[1] = 0;
        region[2] = 0;
        region[3] = 0;
        if (combined.length != cellsWide * cellsHigh) {
            combined = new int[cellsWide * cellsHigh];
        }
        if (staticChanged) {
            castLayer(staticLayer, staticShadows, castStatic, visible, cellsWide, cellsHigh, phaseX, phaseY, artScale,
                    angle, length, scale);
            staticRecasts++;
        }
        if (dynamicChanged) {
            castLayer(dynamicLayer, dynamicShadows, castDynamic, visible, cellsWide, cellsHigh, phaseX, phaseY,
                    artScale, angle, length, scale);
        }
        combineRegion(cellsWide);
        upload(cellsWide, cellsHigh);
        hasLastFrame = true;
        castVisible = visible;
        lastCellsWide = cellsWide;
        lastCellsHigh = cellsHigh;
        lastPhaseX = phaseX;
        lastPhaseY = phaseY;
        lastArtScale = artScale;
        lastAngle = angle;
        lastLength = length;
        recasts++;
    }

    /** Clears a layer, casts a list of shadows into it and adds the area it changed to the area of the frame. */
    private void castLayer(ShadowLayer layer, ShadowList shadows, ShadowList kept, boolean visible, int cellsWide,
                           int cellsHigh, double phaseX, double phaseY, double artScale, double angle, double length,
                           int scale) {
        layer.prepare(cellsWide, cellsHigh, angle, length, scale);
        if (visible) {
            for (int i = 0, count = shadows.size(); i < count; i++) {
                castOne(layer, shadows, i, phaseX, phaseY, artScale);
            }
            kept.copyFrom(shadows);
        }
        final int[] changed = layer.changedBox();
        if (changed[2] > changed[0] && changed[3] > changed[1]) {
            if (region[2] > region[0] && region[3] > region[1]) {
                region[0] = Math.min(region[0], changed[0]);
                region[1] = Math.min(region[1], changed[1]);
                region[2] = Math.max(region[2], changed[2]);
                region[3] = Math.max(region[3], changed[3]);
            } else {
                System.arraycopy(changed, 0, region, 0, 4);
            }
        }
        layer.handedOn();
    }

    /** Sets every cell of the changed area to the stronger of the static and the dynamic cell. */
    private void combineRegion(int cellsWide) {
        final int[] fixed = staticLayer.pixels();
        final int[] moving = dynamicLayer.pixels();
        for (int y = region[1]; y < region[3]; y++) {
            for (int i = y * cellsWide + region[0], end = y * cellsWide + region[2]; i < end; i++) {
                combined[i] = (fixed[i] >>> 24) >= (moving[i] >>> 24) ? fixed[i] : moving[i];
            }
        }
    }

    /** Casts one shadow of a list into a layer, anchored the way its object is drawn. */
    private void castOne(ShadowLayer layer, ShadowList shadows, int i, double phaseX, double phaseY, double artScale) {
        final int alpha = (int) Math.round(shadows.fade(i) * 255.0);
        final Silhouette sprite = shadows.silhouette(i);
        if (sprite != null) {
            if (sprite.width() <= 0) {
                return;
            }
            // The layer grid has a cell of border before the first cell that lies on the canvas
            final double anchorX = ShadowAnchor.cell(
                    ShadowAnchor.baseX(shadows.centreX(i), shadows.drawnWidth(i), artScale), phaseX, artScale);
            final double anchorY = ShadowAnchor.cell(
                    ShadowAnchor.baseY(shadows.centreY(i), shadows.drawnHeight(i), artScale), phaseY, artScale);
            final double heightFactor = shadows.groundRow(i) > 0 ? shadows.drawnHeight(i) / shadows.groundRow(i) : 1.0;
            layer.castSprite(sprite, anchorX, anchorY, shadows.groundRow(i), shadows.widthArt(i) / sprite.width(),
                    heightFactor, alpha);
        } else {
            // An object drawn from shapes has no sprite to follow, so its ellipse sits on the art grid
            final double anchorX = Math.rint((shadows.centreX(i) - phaseX) / artScale) + 1;
            final double anchorY = Math.rint((shadows.centreY(i) - phaseY) / artScale) + 1;
            layer.castEllipse(anchorX, anchorY, shadows.widthArt(i), alpha);
        }
    }

    /** Copies the cells of the changed area into the image, creating the image when the canvas has a new size. */
    private void upload(int cellsWide, int cellsHigh) {
        if (image == null || (int) image.getWidth() != cellsWide || (int) image.getHeight() != cellsHigh) {
            image = new WritableImage(cellsWide, cellsHigh);
        }
        if (region[2] > region[0] && region[3] > region[1]) {
            image.getPixelWriter().setPixels(region[0], region[1], region[2] - region[0], region[3] - region[1],
                    PixelFormat.getIntArgbPreInstance(), combined, region[1] * cellsWide + region[0], cellsWide);
            System.arraycopy(region, 0, lastUpload, 0, 4);
            uploads++;
        }
    }

    /** @return how many frames cast shadows into a layer so far */
    int recasts() {
        return recasts;
    }

    /** @return how many times the static layer was cast so far */
    int staticRecasts() {
        return staticRecasts;
    }

    /** @return how many times cells were uploaded to the image so far */
    int uploads() {
        return uploads;
    }

    /** @return a copy of the area of the last upload as left, top, right and bottom, the last two exclusive */
    int[] lastUpload() {
        return lastUpload.clone();
    }
}
