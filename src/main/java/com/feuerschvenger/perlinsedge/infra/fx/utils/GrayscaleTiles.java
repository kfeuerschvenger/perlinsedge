package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * The shades and the shape of the debug height view, where every tile is a grey diamond whose brightness is its height.
 * Heights are grouped into a fixed number of levels so one pre-drawn diamond per level serves every tile, and the pure
 * rules live here so they can be tested without a window.
 */
public final class GrayscaleTiles {

    /** The number of grey levels, one pre-drawn diamond each. */
    public static final int LEVELS = 64;

    /** Width of a diamond in pixels. */
    public static final int WIDTH = 32;

    /** Height of a diamond in pixels. */
    public static final int HEIGHT = 16;

    /** Heights run from minus one to one. */
    private static final double HEIGHT_NORMALIZATION = 2.0;

    /** Not instantiable: the class only holds static rules. */
    private GrayscaleTiles() {}

    /**
     * Maps a tile height to a brightness between zero and one, clamped.
     *
     * @param height the tile height, from minus one to one
     * @return zero for the lowest tile and one for the highest
     */
    public static double normalized(double height) {
        return Math.max(0.0, Math.min(1.0, (height + 1.0) / HEIGHT_NORMALIZATION));
    }

    /**
     * Maps a tile height to one of the grey levels.
     *
     * @param height the tile height, from minus one to one
     * @return the level, from zero to {@link #LEVELS} minus one
     */
    public static int levelFor(double height) {
        return Math.min(LEVELS - 1, (int) (normalized(height) * LEVELS));
    }

    /**
     * Returns the grey value of a level, used for the red, green and blue channels alike.
     *
     * @param level the level, from zero to {@link #LEVELS} minus one
     * @return a channel value from 0 to 255
     */
    public static int greyOf(int level) {
        return (int) Math.round(255.0 * level / (LEVELS - 1));
    }

    /**
     * Tells whether a pixel lies on the diamond: rows grow by two pixels each side down to the middle, then shrink.
     *
     * @param x pixel column
     * @param y pixel row
     * @return true when the pixel is part of the diamond
     */
    public static boolean insideDiamond(int x, int y) {
        if (y < 0 || y >= HEIGHT) {
            return false;
        }
        final int half = 2 * (y < HEIGHT / 2 ? y + 1 : HEIGHT - y);
        return x >= WIDTH / 2 - half && x < WIDTH / 2 + half;
    }

    /**
     * Tells whether a pixel of the diamond touches the outside, which is drawn black like the outline of the tile.
     *
     * @param x pixel column
     * @param y pixel row
     * @return true when the pixel is on the diamond and one of its four neighbours is not
     */
    public static boolean isOutline(int x, int y) {
        return insideDiamond(x, y)
                && !(insideDiamond(x - 1, y) && insideDiamond(x + 1, y) && insideDiamond(x, y - 1) && insideDiamond(x, y + 1));
    }
}
