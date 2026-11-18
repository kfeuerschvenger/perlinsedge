package com.feuerschvenger.perlinsedge.infra.fx.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The silhouettes of sprites, made the first time a sprite casts a shadow and kept after that.
 * A silhouette is baked at the logical size of its sprite and keyed by the identity of the image, as the tinted copies of
 * {@link TintedSprites} are, so a sprite that the sprite manager hands out again maps to the same silhouette. The cache
 * is only touched from the render thread.
 * <p>
 * An image with no pixel reader or no size, such as one that has not loaded or failed to load, has no shape to bake. It
 * gets an empty silhouette that is not cached, so the image is read again once it can be.
 */
public final class SilhouetteSprites {

    /** The silhouette of an image that has no pixels to read, which casts no shadow. */
    private static final Silhouette EMPTY = new Silhouette(0, 0, new int[0]);

    /**
     * The silhouette of every sprite seen so far, keyed by the identity of the image. Its keys are the images held by
     * the cached sprite manager, which hands out a fixed set of them, so the cache does not grow past that set and it is
     * not meant for images that are made anew every frame.
     */
    private static final Map<Image, Silhouette> CACHE = new IdentityHashMap<>();

    /** Not instantiable: the class only holds the cache. */
    private SilhouetteSprites() {}

    /**
     * Returns the silhouette of a sprite.
     *
     * @param source the sprite
     * @return the shared silhouette, or an empty one that is not cached when the image has no pixel reader or no size
     */
    public static Silhouette of(Image source) {
        final Silhouette cached = CACHE.get(source);
        if (cached != null) {
            return cached;
        }
        final var reader = source.getPixelReader();
        if (reader == null || (int) source.getWidth() <= 0 || (int) source.getHeight() <= 0) {
            return EMPTY;
        }
        final Silhouette baked = bake(source, reader);
        CACHE.put(source, baked);
        return baked;
    }

    /** Reads the pixels of a sprite once and keeps only the shape. */
    private static Silhouette bake(Image source, PixelReader reader) {
        final int width = (int) source.getWidth();
        final int height = (int) source.getHeight();
        final var pixels = new int[width * height];
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                pixels[y * width + x] = reader.getArgb(x, y);
            }
        }
        return new Silhouette(width, height, pixels);
    }
}
