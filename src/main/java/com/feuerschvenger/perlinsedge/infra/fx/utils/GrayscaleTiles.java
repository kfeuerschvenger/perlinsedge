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

    /** Pixels a diamond row grows by on each side, going down to the middle. */
    private static final int ROW_GROWTH_PER_SIDE = 2;

    /** The column at the middle of a diamond. */
    private static final int MIDDLE_COLUMN = WIDTH / 2;

    /** The row at the middle of a diamond, where the rows stop growing and start to shrink. */
    private static final int MIDDLE_ROW = HEIGHT / 2;

    /** The largest grey value of a channel. */
    private static final double GREY_MAX = 255.0;

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
        return Math.clamp((height + 1.0) / HEIGHT_NORMALIZATION, 0.0, 1.0);
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
        return (int) Math.round(GREY_MAX * level / (LEVELS - 1));
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
        final var half = ROW_GROWTH_PER_SIDE * (y < MIDDLE_ROW ? y + 1 : HEIGHT - y);
        return x >= MIDDLE_COLUMN - half && x < MIDDLE_COLUMN + half;
    }

    /**
     * Tells whether a pixel of the diamond touches the outside, which is drawn black like the outline of the tile.
     *
     * @param x pixel column
     * @param y pixel row
     * @return true when the pixel is on the diamond and one of its four neighbours is not
     */
    public static boolean isOutline(int x, int y) {
        return insideDiamond(x, y) && !hasAllNeighbours(x, y);
    }

    private static boolean hasAllNeighbours(int x, int y) {
        final var sides = insideDiamond(x - 1, y) && insideDiamond(x + 1, y);
        final var ends = insideDiamond(x, y - 1) && insideDiamond(x, y + 1);
        return sides && ends;
    }
}
