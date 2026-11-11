package com.feuerschvenger.perlinsedge.infra.fx.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * The pre-baked shadow ellipses, built once on first use.
 * Each is a black ellipse at the shadow opacity, one image per size of {@link GroundShadow}, made of hard pixels so it
 * stays crisp when the renderer scales it by the integer art scale with smoothing off.
 */
public final class ShadowSprites {

    private static final int SHADOW_ARGB = (int) Math.round(GroundShadow.OPACITY * 255) << 24;

    /** Builds the images on the first lookup, which the class loader makes thread safe. */
    private static final class Holder {
        static final Image[] IMAGES = bake();
    }

    /** Not instantiable: the class only holds shared images. */
    private ShadowSprites() {}

    /**
     * Returns the ellipse of a size.
     *
     * @param bucket the size index from {@link GroundShadow#bucketFor}
     * @return the shared image
     */
    public static Image get(int bucket) {
        return Holder.IMAGES[bucket];
    }

    /** Draws one ellipse per size. */
    private static Image[] bake() {
        final Image[] images = new Image[GroundShadow.bucketCount()];
        for (int i = 0; i < images.length; i++) {
            final int width = GroundShadow.widthOf(i);
            final int height = GroundShadow.heightOf(width);
            final WritableImage image = new WritableImage(width, height);
            final PixelWriter writer = image.getPixelWriter();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.setArgb(x, y, GroundShadow.covers(width, height, x, y) ? SHADOW_ARGB : 0);
                }
            }
            images[i] = image;
        }
        return images;
    }
}
