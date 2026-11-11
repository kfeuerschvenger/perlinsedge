package com.feuerschvenger.perlinsedge.infra.fx.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * The pre-drawn grey diamonds of the debug height view, built once on first use.
 * Drawing one image per tile is far cheaper than filling and stroking a polygon per tile every frame, and the images
 * are drawn at the integer art scale like every other tile.
 */
public final class DebugTileSprites {

    /** The outline keeps this share of the grey of its tile, so it stays a thin dark line rather than a black frame. */
    private static final double OUTLINE_KEPT = 0.35;

    private static final class Holder {
        static final Image[] IMAGES = bake();
    }

    private DebugTileSprites() {}

    /**
     * Returns the diamond of a grey level.
     *
     * @param level the level from {@link GrayscaleTiles#levelFor}
     * @return the shared image
     */
    public static Image get(int level) {
        return Holder.IMAGES[level];
    }

    private static Image[] bake() {
        final Image[] images = new Image[GrayscaleTiles.LEVELS];
        for (int level = 0; level < images.length; level++) {
            final int grey = GrayscaleTiles.greyOf(level);
            final int fill = 0xFF000000 | grey << 16 | grey << 8 | grey;
            final int edge = (int) Math.round(grey * OUTLINE_KEPT);
            final int outline = 0xFF000000 | edge << 16 | edge << 8 | edge;
            final WritableImage image = new WritableImage(GrayscaleTiles.WIDTH, GrayscaleTiles.HEIGHT);
            final PixelWriter writer = image.getPixelWriter();
            for (int y = 0; y < GrayscaleTiles.HEIGHT; y++) {
                for (int x = 0; x < GrayscaleTiles.WIDTH; x++) {
                    if (GrayscaleTiles.insideDiamond(x, y)) {
                        writer.setArgb(x, y, GrayscaleTiles.isOutline(x, y) ? outline : fill);
                    }
                }
            }
            images[level] = image;
        }
        return images;
    }
}
