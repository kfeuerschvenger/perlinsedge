package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * The shape and the shades of the solid colour tiles drawn for a tile type that has no art, or while the art is hidden.
 * A tile is a block like the land sprites: a top diamond, a darker left face and a darker still right face. The pixels are
 * a pure function of the tile colour, so one block image per tile type is baked once and drawn for every tile, instead of
 * filling and stroking polygons per tile every frame. The rules live here so they can be tested without a window.
 */
public final class FlatTiles {

    /** Width of a block in pixels, the same as a land sprite. */
    public static final int WIDTH = GrayscaleTiles.WIDTH;

    /** Height of the top diamond in pixels. */
    public static final int TOP_HEIGHT = GrayscaleTiles.HEIGHT;

    /** Height of the side faces in pixels. */
    public static final int FACE_HEIGHT = 5;

    /** Height of a block in pixels. */
    public static final int HEIGHT = TOP_HEIGHT + FACE_HEIGHT;

    /** Brightness kept by the left face and by the outline of the top, the factor of the JavaFX darker colour. */
    static final double DARKER = 0.7;

    /** Not instantiable: the class only holds static rules. */
    private FlatTiles() {}

    /**
     * Converts a colour to an opaque packed ARGB value.
     *
     * @param colour the tile colour
     * @return the packed value
     */
    public static int argbOf(Rgba colour) {
        return 0xFF000000 | channel(colour.r()) << 16 | channel(colour.g()) << 8 | channel(colour.b());
    }

    /**
     * Scales the brightness of a packed colour, keeping it opaque.
     *
     * @param argb   the packed colour
     * @param factor the factor, below one to darken
     * @return the packed result
     */
    public static int scale(int argb, double factor) {
        final int r = (int) Math.round(((argb >> 16) & 0xFF) * factor);
        final int g = (int) Math.round(((argb >> 8) & 0xFF) * factor);
        final int b = (int) Math.round((argb & 0xFF) * factor);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * Tells whether a pixel lies on the block: the top diamond pushed down by the face height.
     *
     * @param x pixel column
     * @param y pixel row
     * @return true when the pixel is part of the block
     */
    public static boolean insideBlock(int x, int y) {
        for (int drop = 0; drop <= FACE_HEIGHT; drop++) {
            if (GrayscaleTiles.insideDiamond(x, y - drop)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the colour of one pixel of a block.
     * Land has a darker outline around its top, a left face and a still darker right face. Water is one flat colour with
     * no outline and the same colour on its faces, as the water surface has no thickness to shade.
     *
     * @param x     pixel column
     * @param y     pixel row
     * @param argb  the packed tile colour
     * @param water whether the tile is water
     * @return the packed colour of the pixel, or zero when the pixel is not on the block
     */
    public static int pixel(int x, int y, int argb, boolean water) {
        if (!insideBlock(x, y)) {
            return 0;
        }
        if (water) {
            return argb;
        }
        if (GrayscaleTiles.insideDiamond(x, y)) {
            return GrayscaleTiles.isOutline(x, y) ? scale(argb, DARKER) : argb;
        }
        if (GrayscaleTiles.insideDiamond(x, y - 1)) {
            return scale(argb, DARKER * DARKER);
        }
        return scale(argb, x < WIDTH / 2 ? DARKER : DARKER * DARKER);
    }

    /** Converts a colour component from zero to one into a byte, clamping values outside that range. */
    private static int channel(double component) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, component)) * 255.0);
    }
}
