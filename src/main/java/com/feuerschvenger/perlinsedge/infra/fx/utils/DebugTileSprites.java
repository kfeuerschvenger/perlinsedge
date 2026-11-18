package com.feuerschvenger.perlinsedge.infra.fx.utils;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

/**
 * The pre-drawn grey diamonds of the debug height view, built once on first use.
 * Drawing one image per tile is far cheaper than filling and stroking a polygon per tile every frame, and the images
 * are drawn at the integer art scale like every other tile.
 */
public final class DebugTileSprites {

    /** The outline keeps this share of the grey of its tile, so it stays a thin dark line rather than a black frame. */
    private static final double OUTLINE_KEPT = 0.35;

    /** Builds the images on the first lookup, which the class loader makes thread safe. */
    private static final class Holder {
        private static final Image[] IMAGES = bake();

        /** Draws one grey diamond per level. */
        private static Image[] bake() {
            final var images = new Image[GrayscaleTiles.LEVELS];
            for (var level = 0; level < images.length; level++) {
                images[level] = diamond(level);
            }
            return images;
        }

        /** Draws the grey diamond of one level, with its darker outline. */
        private static Image diamond(int level) {
            final var grey = GrayscaleTiles.greyOf(level);
            final var fill = 0xFF000000 | grey << 16 | grey << 8 | grey;
            final var edge = (int) Math.round(grey * OUTLINE_KEPT);
            final var outline = 0xFF000000 | edge << 16 | edge << 8 | edge;
            final var image = new WritableImage(GrayscaleTiles.WIDTH, GrayscaleTiles.HEIGHT);
            final var writer = image.getPixelWriter();
            for (var y = 0; y < GrayscaleTiles.HEIGHT; y++) {
                for (var x = 0; x < GrayscaleTiles.WIDTH; x++) {
                    if (GrayscaleTiles.insideDiamond(x, y)) {
                        writer.setArgb(x, y, GrayscaleTiles.isOutline(x, y) ? outline : fill);
                    }
                }
            }
            return image;
        }
    }

    /** Not instantiable: the class only holds shared images. */
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
}
