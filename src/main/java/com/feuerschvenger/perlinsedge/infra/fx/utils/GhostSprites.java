package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tinted copies of building sprites for the placement ghost: green where the piece can be placed, red where the tile
 * refuses it and amber where the resources are missing. A copy keeps the shape of the sprite and nothing around it, and
 * is made the first time a tint is needed and kept after that, for as long as the sprite it was made from is alive. A tint is
 * told apart by its three 8-bit channels, so asking for a copy allocates nothing once it exists. The cache is only touched from
 * the render thread.
 */
public final class GhostSprites {
    /** Share of each painted pixel that is replaced by the tint, so the piece stays recognisable under the colour. */
    public static final double TINT_SHARE = 0.45;

    /** Bit positions of the alpha, red and green channels in a packed ARGB value, the blue channel sits at zero. */
    private static final int ALPHA_SHIFT = 24;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    /** The largest value of an 8-bit colour channel. */
    private static final double CHANNEL_MAX = 255.0;

    /** The mask of one 8-bit channel. */
    private static final int CHANNEL_MASK = 0xFF;

    /** One baked copy of a sprite and the tint it was baked with, in a chain of the copies of the same sprite. */
    private record Baked(int tintKey, Image image, Baked next) { }

    /** The copies of each sprite, held by the sprite weakly: the copy does not refer to the sprite it was made from. */
    private static final Map<Image, Baked> CACHE = new WeakHashMap<>();

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
        final int key = keyOf(tint);
        final var first = CACHE.get(source);
        for (var baked = first; baked != null; baked = baked.next()) {
            if (baked.tintKey() == key) {
                return baked.image();
            }
        }
        final var image = bake(source, tintOf(key));
        CACHE.put(source, new Baked(key, image, first));
        return image;
    }

    /** Forgets every copy, for when the sprites are loaded again. */
    public static void clear() {
        CACHE.clear();
    }

    /** Packs the three channels of a tint, each rounded to 8 bits, into one number. */
    private static int keyOf(Rgba tint) {
        return (channel(tint.r()) << RED_SHIFT) | (channel(tint.g()) << GREEN_SHIFT) | channel(tint.b());
    }

    private static int channel(double value) {
        return (int) Math.round(Math.clamp(value, 0.0, 1.0) * CHANNEL_MAX);
    }

    private static Rgba tintOf(int key) {
        return new Rgba(((key >> RED_SHIFT) & CHANNEL_MASK) / CHANNEL_MAX,
                ((key >> GREEN_SHIFT) & CHANNEL_MASK) / CHANNEL_MAX, (key & CHANNEL_MASK) / CHANNEL_MAX, 1.0);
    }

    /**
     * Mixes a pixel toward a tint, keeping its opacity, so a transparent pixel stays transparent.
     *
     * @param argb the pixel
     * @param tint the colour to mix toward
     * @return the tinted pixel
     */
    public static int tint(int argb, Rgba tint) {
        final var alpha = argb >>> ALPHA_SHIFT;
        if (alpha == 0) {
            return argb;
        }
        return (alpha << ALPHA_SHIFT)
                | (mix((argb >> RED_SHIFT) & 0xFF, tint.r()) << RED_SHIFT)
                | (mix((argb >> GREEN_SHIFT) & 0xFF, tint.g()) << GREEN_SHIFT)
                | mix(argb & 0xFF, tint.b());
    }

    private static int mix(int channel, double toward) {
        return (int) Math.round(channel * (1.0 - TINT_SHARE) + toward * CHANNEL_MAX * TINT_SHARE);
    }

    private static Image bake(Image source, Rgba tint) {
        final var width = (int) source.getWidth();
        final var height = (int) source.getHeight();
        final var reader = source.getPixelReader();
        final var copy = new WritableImage(width, height);
        final var writer = copy.getPixelWriter();
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                writer.setArgb(x, y, tint(reader.getArgb(x, y), tint));
            }
        }
        return copy;
    }
}
