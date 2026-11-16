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
 * grid as the tile art. The layer and the image that mirrors it are created once and reused, and only the area a frame
 * touched is cleared and uploaded, so a steady frame allocates nothing. A frame with no shadows, as at night, uploads
 * what the last one left and draws nothing.
 */
public final class FxShadowPass {

    /** Strengths below this are not visible, so the pass casts nothing. */
    private static final double MIN_STRENGTH = 0.004;

    private final ShadowLayer layer = new ShadowLayer();
    private WritableImage image;

    /**
     * Casts the shadows of a frame and lays them over the canvas.
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

        layer.prepare(cellsWide, cellsHigh, angle, length);
        if (strength >= MIN_STRENGTH) {
            for (int i = 0, count = shadows.size(); i < count; i++) {
                // The layer grid has a cell of border before the first cell that lies on the canvas
                final double anchorX = Math.rint((shadows.centreX(i) - phaseX) / artScale) + 1;
                final double anchorY = Math.rint((shadows.centreY(i) - phaseY) / artScale) + 1;
                final int alpha = (int) Math.round(shadows.fade(i) * 255.0);
                final Silhouette sprite = shadows.silhouette(i);
                if (sprite != null) {
                    layer.castSprite(sprite, anchorX, anchorY, shadows.groundRow(i),
                            shadows.widthArt(i) / sprite.width(), alpha);
                } else {
                    layer.castEllipse(anchorX, anchorY, shadows.widthArt(i), alpha);
                }
            }
        }
        upload(cellsWide, cellsHigh);

        if (strength < MIN_STRENGTH || !layer.hasShadows()) {
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

    /** Copies the cells that changed into the image, creating the image when the canvas has a new size. */
    private void upload(int cellsWide, int cellsHigh) {
        if (image == null || (int) image.getWidth() != cellsWide || (int) image.getHeight() != cellsHigh) {
            image = new WritableImage(cellsWide, cellsHigh);
        }
        final int[] box = layer.changedBox();
        if (box[2] > box[0] && box[3] > box[1]) {
            image.getPixelWriter().setPixels(box[0], box[1], box[2] - box[0], box[3] - box[1],
                    PixelFormat.getIntArgbPreInstance(), layer.pixels(), box[1] * cellsWide + box[0], cellsWide);
        }
        layer.handedOn();
    }
}
