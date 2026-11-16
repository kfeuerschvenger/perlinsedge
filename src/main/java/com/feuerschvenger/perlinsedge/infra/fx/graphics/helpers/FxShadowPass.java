package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowLayer;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * The shadow pass of a frame: every shadow is cast into one {@link ShadowLayer} at full black, and the layer is laid over
 * the canvas once, at the shadow opacity times the strength of the day. Because the layer holds the strongest alpha of
 * the shadows that cover a cell instead of their sum, two overlapping shadows are exactly as dark as one.
 * <p>
 * The layer has one cell per art pixel and is drawn at the integer art scale with smoothing off, so every shadow pixel
 * is a crisp square block at every zoom. Its grid is aligned to the camera, so the shadows of the tiles sit on the same
 * grid as the tile art. The shadow of a sprite is anchored on the base of the sprite as it is drawn, see
 * {@link ShadowAnchor}, so the two move together.
 * <p>
 * The layer and the image that mirrors it are created once and reused. What the layer holds depends only on the camera
 * grid, the art scale, the size of the canvas, the sun and the list of shadows, so when none of them changed since the
 * last frame the layer and the image are kept as they are and only the composite is drawn again. The strength of the day
 * only scales that composite and does not take part. When something did change, only the area that frame touched is
 * cleared and uploaded. A frame with no visible shadows draws nothing.
 */
public final class FxShadowPass {

    /** The shadow layer, which keeps the cells of the last frame that was cast. */
    private final ShadowLayer layer = new ShadowLayer();
    /** The image that mirrors the layer for drawing. */
    private WritableImage image;

    /** The shadows the layer was last cast from, so an unchanged list is recognised. */
    private final ShadowList castShadows = new ShadowList();
    /** Whether the layer was cast at all last time, which is false while the shadows are too faint to see. */
    private boolean castVisible;
    /** Whether there is a last frame to compare with. */
    private boolean hasLastFrame;
    /** The number of cells across the layer was last cast at. */
    private int lastCellsWide;
    /** The number of cells down the layer was last cast at. */
    private int lastCellsHigh;
    /** The horizontal phase of the camera grid the layer was last cast at. */
    private double lastPhaseX;
    /** The vertical phase of the camera grid the layer was last cast at. */
    private double lastPhaseY;
    /** The art scale the layer was last cast at. */
    private double lastArtScale;
    /** The shadow direction the layer was last cast at. */
    private double lastAngle;
    /** The shadow length factor the layer was last cast at. */
    private double lastLength;
    /** How many times the shadows were cast into the layer, which a frame that kept the layer does not count. */
    private int recasts;
    /** How many times the image was uploaded to, which a frame that kept the layer does not count. */
    private int uploads;

    /**
     * Casts the shadows of a frame, unless the last frame already did, and lays them over the canvas.
     *
     * @param gc        the graphics context, with image smoothing off and no transform
     * @param shadows   the shadows to draw
     * @param artScale  the integer number of screen pixels per art pixel
     * @param originX   screen X of the camera origin, which the art grid is aligned to
     * @param originY   screen Y of the camera origin
     * @param canvasW   canvas width
     * @param canvasH   canvas height
     * @param angle     direction of the shadows on the screen in radians
     * @param length    how long shadows are as a multiple of the height of what casts them
     * @param strength  how dark shadows are as a share of their full opacity, 0 to 1
     */
    public void draw(GraphicsContext gc, ShadowList shadows, double artScale, double originX, double originY,
                     double canvasW, double canvasH, double angle, double length, double strength) {
        final int cellsWide = (int) Math.ceil(canvasW / artScale) + 2;
        final int cellsHigh = (int) Math.ceil(canvasH / artScale) + 2;
        final double snappedX = Math.rint(originX);
        final double snappedY = Math.rint(originY);
        final double phaseX = snappedX - Math.floor(snappedX / artScale) * artScale;
        final double phaseY = snappedY - Math.floor(snappedY / artScale) * artScale;
        final boolean visible = strength >= FxShadowHelper.MIN_VISIBLE_STRENGTH;

        if (needsCast(shadows, visible, cellsWide, cellsHigh, phaseX, phaseY, artScale, angle, length)) {
            cast(shadows, visible, cellsWide, cellsHigh, phaseX, phaseY, artScale, angle, length);
        }

        if (!visible || !layer.hasShadows()) {
            return;
        }
        final int left = layer.stampedLeft();
        final int top = layer.stampedTop();
        final int wide = layer.stampedRight() - left;
        final int high = layer.stampedBottom() - top;
        gc.setGlobalAlpha(GroundShadow.OPACITY * Math.min(1.0, strength));
        gc.drawImage(image, left, top, wide, high, phaseX + (left - 1) * artScale, phaseY + (top - 1) * artScale,
                wide * artScale, high * artScale);
        gc.setGlobalAlpha(1.0);
    }

    /** Tells whether anything the layer depends on differs from the frame it was last cast for. */
    private boolean needsCast(ShadowList shadows, boolean visible, int cellsWide, int cellsHigh, double phaseX,
                              double phaseY, double artScale, double angle, double length) {
        if (!hasLastFrame || visible != castVisible || cellsWide != lastCellsWide || cellsHigh != lastCellsHigh
                || phaseX != lastPhaseX || phaseY != lastPhaseY || artScale != lastArtScale || angle != lastAngle
                || length != lastLength) {
            return true;
        }
        // While the shadows are too faint to see nothing is cast, so the list does not matter
        return visible && !shadows.sameShadowsAs(castShadows);
    }

    /** Clears the layer, casts the shadows into it and uploads the cells that changed. */
    private void cast(ShadowList shadows, boolean visible, int cellsWide, int cellsHigh, double phaseX, double phaseY,
                      double artScale, double angle, double length) {
        layer.prepare(cellsWide, cellsHigh, angle, length, (int) Math.round(artScale));
        if (visible) {
            for (int i = 0, count = shadows.size(); i < count; i++) {
                castOne(shadows, i, phaseX, phaseY, artScale);
            }
            castShadows.copyFrom(shadows);
        }
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

    /** Casts one shadow of the list, anchored the way its object is drawn. */
    private void castOne(ShadowList shadows, int i, double phaseX, double phaseY, double artScale) {
        final int alpha = (int) Math.round(shadows.fade(i) * 255.0);
        final Silhouette sprite = shadows.silhouette(i);
        if (sprite != null) {
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

    /** Copies the cells that changed into the image, creating the image when the canvas has a new size. */
    private void upload(int cellsWide, int cellsHigh) {
        if (image == null || (int) image.getWidth() != cellsWide || (int) image.getHeight() != cellsHigh) {
            image = new WritableImage(cellsWide, cellsHigh);
        }
        final int[] box = layer.changedBox();
        if (box[2] > box[0] && box[3] > box[1]) {
            image.getPixelWriter().setPixels(box[0], box[1], box[2] - box[0], box[3] - box[1],
                    PixelFormat.getIntArgbPreInstance(), layer.pixels(), box[1] * cellsWide + box[0], cellsWide);
            uploads++;
        }
        layer.handedOn();
    }

    /** @return how many times the shadows were cast into the layer so far */
    int recasts() {
        return recasts;
    }

    /** @return how many times cells were uploaded to the image so far */
    int uploads() {
        return uploads;
    }
}
