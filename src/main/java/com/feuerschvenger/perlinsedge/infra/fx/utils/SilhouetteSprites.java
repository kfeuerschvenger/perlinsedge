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
 */
public final class SilhouetteSprites {

    private static final Map<Image, Silhouette> CACHE = new IdentityHashMap<>();

    /** Not instantiable: the class only holds the cache. */
    private SilhouetteSprites() {}

    /**
     * Returns the silhouette of a sprite.
     *
     * @param source the sprite
     * @return the shared silhouette
     */
    public static Silhouette of(Image source) {
        return CACHE.computeIfAbsent(source, SilhouetteSprites::bake);
    }

    /** Reads the pixels of a sprite once and keeps only the shape. */
    private static Silhouette bake(Image source) {
        final int width = (int) source.getWidth();
        final int height = (int) source.getHeight();
        final PixelReader reader = source.getPixelReader();
        final int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = reader.getArgb(x, y);
            }
        }
        return new Silhouette(width, height, pixels);
    }
}
