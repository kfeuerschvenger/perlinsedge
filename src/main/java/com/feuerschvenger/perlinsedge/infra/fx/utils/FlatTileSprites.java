package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.EnumMap;
import java.util.Map;

/**
 * The pre-drawn solid colour blocks of the tile types, built once on first use.
 * The colour of a tile comes from its type, so one image per type serves every tile and a lookup allocates nothing.
 * Drawing an image per tile is far cheaper than filling and stroking polygons per tile every frame, and the images are
 * drawn at the integer art scale like every other tile.
 */
public final class FlatTileSprites {

    /** Builds the images on the first lookup, which the class loader makes thread safe. */
    private static final class Holder {
        static final Map<TileType, Image> IMAGES = bake();
    }

    /** Not instantiable: the class only holds shared images. */
    private FlatTileSprites() {}

    /**
     * Returns the block of a tile type.
     *
     * @param type the tile type
     * @return the shared image
     */
    public static Image get(TileType type) {
        return Holder.IMAGES.get(type);
    }

    /** Draws one block per tile type. */
    private static Map<TileType, Image> bake() {
        final Map<TileType, Image> images = new EnumMap<>(TileType.class);
        for (TileType type : TileType.values()) {
            final int argb = FlatTiles.argbOf(type.getColor());
            final WritableImage image = new WritableImage(FlatTiles.WIDTH, FlatTiles.HEIGHT);
            final PixelWriter writer = image.getPixelWriter();
            for (int y = 0; y < FlatTiles.HEIGHT; y++) {
                for (int x = 0; x < FlatTiles.WIDTH; x++) {
                    final int pixel = FlatTiles.pixel(x, y, argb, type.isWater());
                    if (pixel != 0) {
                        writer.setArgb(x, y, pixel);
                    }
                }
            }
            images.put(type, image);
        }
        return images;
    }
}
