package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * The black outline of a sprite at its logical size: every painted pixel is opaque black and every other pixel is
 * transparent. A shadow is cast from this shape, so it has the form of the object and none of its colours. The class
 * holds plain pixel data and no framework type, so it can be built and tested without a window.
 */
public final class Silhouette {

    /** A painted pixel of a silhouette: black and fully opaque. */
    public static final int SOLID = 0xFF000000;

    /** Source pixels with at least this alpha count as painted, so a faint edge does not widen the shadow. */
    private static final int PAINTED_ALPHA = 0x80;

    private final int width;
    private final int height;
    private final int paintedBottom;
    private final int[] argb;

    /**
     * Builds a silhouette from the pixels of a sprite.
     *
     * @param width  sprite width in pixels
     * @param height sprite height in pixels
     * @param source the sprite pixels as ARGB, row by row
     */
    public Silhouette(int width, int height, int[] source) {
        this.width = width;
        this.height = height;
        this.argb = new int[width * height];
        int bottom = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((source[y * width + x] >>> 24) >= PAINTED_ALPHA) {
                    argb[y * width + x] = SOLID;
                    bottom = y + 1;
                }
            }
        }
        this.paintedBottom = bottom;
    }

    /** @return the width in pixels */
    public int width() {
        return width;
    }

    /** @return the height in pixels */
    public int height() {
        return height;
    }

    /** @return the number of rows from the top down to and including the lowest painted row, zero for an empty sprite */
    public int paintedBottom() {
        return paintedBottom;
    }

    /**
     * Returns one pixel.
     *
     * @param x column
     * @param y row
     * @return {@link #SOLID} for a painted pixel, zero for a transparent one
     */
    public int argbAt(int x, int y) {
        return argb[y * width + x];
    }

    /**
     * Tells whether a pixel is painted.
     *
     * @param x column, inside the sprite
     * @param y row, inside the sprite
     * @return true for a painted pixel
     */
    public boolean isSolid(int x, int y) {
        return argb[y * width + x] != 0;
    }
}
