package com.feuerschvenger.perlinsedge.infra.fx.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Red and white copies of sprites for the hurt flash, made the first time a frame is flashed and kept after that.
 * A tinted copy keeps the shape of the sprite and nothing around it, which a rectangle drawn over the sprite on the shared
 * canvas could not. The cache is only touched from the render thread.
 */
public final class TintedSprites {

    private static final Map<Image, Image> RED = new IdentityHashMap<>();
    private static final Map<Image, Image> WHITE = new IdentityHashMap<>();

    /** Not instantiable: the class only holds the cache. */
    private TintedSprites() {}

    /**
     * Returns a tinted copy of a sprite.
     *
     * @param source the sprite
     * @param phase  {@link HurtReaction#RED} or {@link HurtReaction#WHITE}
     * @return the shared copy, or the sprite itself for any other phase
     */
    public static Image of(Image source, int phase) {
        if (phase == HurtReaction.RED) {
            return RED.computeIfAbsent(source, image -> bake(image, HurtReaction.RED));
        }
        if (phase == HurtReaction.WHITE) {
            return WHITE.computeIfAbsent(source, image -> bake(image, HurtReaction.WHITE));
        }
        return source;
    }

    /** Copies a sprite with every painted pixel mixed toward the colour of the phase. */
    private static Image bake(Image source, int phase) {
        final int width = (int) source.getWidth();
        final int height = (int) source.getHeight();
        final PixelReader reader = source.getPixelReader();
        final WritableImage copy = new WritableImage(width, height);
        final PixelWriter writer = copy.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setArgb(x, y, HurtReaction.tint(reader.getArgb(x, y), phase));
            }
        }
        return copy;
    }
}
