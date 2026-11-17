package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Tinted copies of building sprites for the placement ghost: green where the piece can be placed, red where the tile
 * refuses it and amber where the resources are missing. A copy keeps the shape of the sprite and nothing around it, and
 * is made the first time a tint is needed and kept after that. The cache is only touched from the render thread.
 */
public final class GhostSprites {
    /** Share of each painted pixel that is replaced by the tint, so the piece stays recognisable under the colour. */
    public static final double TINT_SHARE = 0.45;

    private static final Map<Image, Map<Rgba, Image>> CACHE = new IdentityHashMap<>();

    /** Not instantiable: the class only holds the cache. */
    private GhostSprites() {
    }

    /**
     * Returns a tinted copy of a sprite.
     *
     * @param source the sprite
     * @param tint   the colour of the ghost, its opacity is ignored
     * @return the shared copy
     */
    public static Image of(Image source, Rgba tint) {
        final Rgba key = new Rgba(tint.r(), tint.g(), tint.b(), 1.0);
        return CACHE.computeIfAbsent(source, image -> new HashMap<>()).computeIfAbsent(key, k -> bake(source, k));
    }

    /**
     * Mixes a pixel toward a tint, keeping its opacity, so a transparent pixel stays transparent.
     *
     * @param argb the pixel
     * @param tint the colour to mix toward
     * @return the tinted pixel
     */
    public static int tint(int argb, Rgba tint) {
        final int alpha = argb >>> 24;
        if (alpha == 0) {
            return argb;
        }
        return (alpha << 24)
                | (mix((argb >> 16) & 0xFF, tint.r()) << 16)
                | (mix((argb >> 8) & 0xFF, tint.g()) << 8)
                | mix(argb & 0xFF, tint.b());
    }

    private static int mix(int channel, double toward) {
        return (int) Math.round(channel * (1.0 - TINT_SHARE) + toward * 255.0 * TINT_SHARE);
    }

    private static Image bake(Image source, Rgba tint) {
        final int width = (int) source.getWidth();
        final int height = (int) source.getHeight();
        final PixelReader reader = source.getPixelReader();
        final WritableImage copy = new WritableImage(width, height);
        final PixelWriter writer = copy.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setArgb(x, y, tint(reader.getArgb(x, y), tint));
            }
        }
        return copy;
    }
}
