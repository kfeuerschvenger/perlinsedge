package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.util.List;
import java.util.Map;

/**
 * The colour each tile has on the minimap, which is the average colour of the sprite the tile is drawn from, so the
 * minimap looks like the world. It is computed once when the art is loaded and read as plain integers afterwards.
 * <p>
 * The average covers the opaque pixels of the top diamond of a tile sprite, over all the variants of its art; the side
 * faces are left out because they are not what a tile looks like from above. A water tile is its sand bed seen
 * through the translucent water sheet, so the sheet colour is composited over the bed average with the opacity of the
 * sheet. A tile type whose art is missing keeps the flat colour of the type.
 */
public final class TilePalette {

    /** Rows of a tile sprite that hold the top diamond. */
    static final int TOP_ROWS = 16;

    /** The alpha of a fully opaque pixel. */
    private static final int OPAQUE = 0xFF;

    /** Layout of a packed 0xAARRGGBB pixel. */
    private static final int ALPHA_SHIFT = 24;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int CHANNEL_MASK = 0xFF;
    private static final double MAX_CHANNEL = 255.0;

    /** Slots of a running sum: the pixel count, then the red, green, blue and alpha totals. */
    private static final int COUNT = 0;
    private static final int RED = 1;
    private static final int GREEN = 2;
    private static final int BLUE = 3;
    private static final int ALPHA = 4;
    private static final int SUM_SLOTS = 5;

    /** The colour of every tile type on every map type, indexed by the ordinals. */
    private final int[][] colours;

    private TilePalette(int[][] colours) {
        this.colours = colours;
    }

    /**
     * Builds the palette of every map type from the loaded tile art.
     *
     * @param tileArts   the variants of every tile art that has files, by art name
     * @param waterSheet the translucent water sheet, or null when there is none
     * @return the palette
     */
    public static TilePalette of(Map<String, List<Image>> tileArts, Image waterSheet) {
        final var water = waterSheet == null ? 0 : overlayOf(waterSheet);
        final var colours = new int[MapType.values().length][TileType.values().length];
        for (MapType mapType : MapType.values()) {
            for (TileType type : TileType.values()) {
                colours[mapType.ordinal()][type.ordinal()] = colourOf(mapType, type, tileArts, water);
            }
        }
        return new TilePalette(colours);
    }

    /**
     * Returns the minimap colour of a tile type on a map type.
     *
     * @param mapType the map type, which picks the biome art
     * @param type    the tile type
     * @return the colour in 0xAARRGGBB layout, opaque
     */
    public int argb(MapType mapType, TileType type) {
        return colours[mapType.ordinal()][type.ordinal()];
    }

    /**
     * Packs a colour into the 0xAARRGGBB integer layout of a pixel buffer.
     *
     * @param color colour with components from 0.0 to 1.0
     * @return the packed colour
     */
    public static int toArgb(Rgba color) {
        return (channel(color.a()) << ALPHA_SHIFT) | (channel(color.r()) << RED_SHIFT)
                | (channel(color.g()) << GREEN_SHIFT) | channel(color.b());
    }

    /**
     * Averages the opaque pixels of the top diamond of a tile art over its variants.
     *
     * @param variants the variants of the art
     * @return the average in 0xAARRGGBB layout, or 0 when no pixel is opaque
     */
    static int averageTop(List<Image> variants) {
        final var sum = sumOf(variants, OPAQUE, true);
        return sum[COUNT] == 0 ? 0 : pack(sum, OPAQUE);
    }

    /**
     * Composites a translucent colour over an opaque one, the way a layer of water covers its bed.
     *
     * @param base    the opaque colour below
     * @param overlay the translucent colour above, whose alpha is its opacity
     * @return the opaque result in 0xAARRGGBB layout
     */
    static int composite(int base, int overlay) {
        final var alpha = (overlay >>> ALPHA_SHIFT) / MAX_CHANNEL;
        final var red = mix((base >>> RED_SHIFT) & CHANNEL_MASK, (overlay >>> RED_SHIFT) & CHANNEL_MASK, alpha);
        final var green = mix((base >>> GREEN_SHIFT) & CHANNEL_MASK, (overlay >>> GREEN_SHIFT) & CHANNEL_MASK, alpha);
        final var blue = mix(base & CHANNEL_MASK, overlay & CHANNEL_MASK, alpha);
        return (OPAQUE << ALPHA_SHIFT) | (red << RED_SHIFT) | (green << GREEN_SHIFT) | blue;
    }

    /** The colour of one tile type on one map type: its art, seen through the water for a water type. */
    private static int colourOf(MapType mapType, TileType type, Map<String, List<Image>> tileArts, int water) {
        final var variants = tileArts.get(AssetPaths.tileArt(mapType, type));
        final var average = variants == null || variants.isEmpty() ? 0 : averageTop(variants);
        if (average == 0) {
            return toArgb(type.getColor());
        }
        return AssetPaths.waterArt(type) != null && water != 0 ? composite(average, water) : average;
    }

    /** The average colour of the painted pixels of the water sheet with their mean alpha, or 0 when none is painted. */
    private static int overlayOf(Image sheet) {
        final var sum = sumOf(List.of(sheet), 1, false);
        return sum[COUNT] == 0 ? 0 : pack(sum, (int) Math.round(sum[ALPHA] / (double) sum[COUNT]));
    }

    /** Adds up the pixels painted at least as much as the given alpha, in the top rows of the images or in all rows. */
    private static long[] sumOf(List<Image> images, int minAlpha, boolean topOnly) {
        final var sum = new long[SUM_SLOTS];
        for (Image image : images) {
            final var reader = image.getPixelReader();
            final var rows = topOnly ? Math.min(TOP_ROWS, (int) image.getHeight()) : (int) image.getHeight();
            for (var y = 0; y < rows; y++) {
                addRow(sum, reader, y, (int) image.getWidth(), minAlpha);
            }
        }
        return sum;
    }

    /** Adds the painted pixels of one row to a running sum. */
    private static void addRow(long[] sum, PixelReader reader, int y, int width, int minAlpha) {
        for (var x = 0; x < width; x++) {
            final var argb = reader.getArgb(x, y);
            final var alpha = argb >>> ALPHA_SHIFT;
            if (alpha >= minAlpha && alpha > 0) {
                sum[COUNT]++;
                sum[RED] += (argb >>> RED_SHIFT) & CHANNEL_MASK;
                sum[GREEN] += (argb >>> GREEN_SHIFT) & CHANNEL_MASK;
                sum[BLUE] += argb & CHANNEL_MASK;
                sum[ALPHA] += alpha;
            }
        }
    }

    /** Packs the average colour of a running sum with the given alpha. */
    private static int pack(long[] sum, int alpha) {
        final var red = Math.round(sum[RED] / (double) sum[COUNT]);
        final var green = Math.round(sum[GREEN] / (double) sum[COUNT]);
        final var blue = Math.round(sum[BLUE] / (double) sum[COUNT]);
        return (alpha << ALPHA_SHIFT) | ((int) red << RED_SHIFT) | ((int) green << GREEN_SHIFT) | (int) blue;
    }

    /** Mixes two channel values by the weight of the second. */
    private static int mix(int below, int above, double weight) {
        return (int) Math.round(below * (1 - weight) + above * weight);
    }

    private static int channel(double component) {
        return (int) Math.round(Math.clamp(component, 0.0, 1.0) * MAX_CHANNEL);
    }
}
