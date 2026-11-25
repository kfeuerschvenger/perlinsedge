package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Writes a path found by {@link LeastCostPath} onto the map as a stroke of one or two tiles. A diagonal step also
 * carves the tile beside it, so the carved tiles are 4-connected: a player can walk the stroke and water can flow along
 * it. A stroke of two tiles stamps a 2x2 block that extends to the right and below each path tile.
 */
final class Carve {

    /** The thinnest stroke. */
    public static final int MIN_STROKE = 1;
    /** The widest stroke. */
    public static final int MAX_STROKE = 2;

    private Carve() {
    }

    /**
     * Carves a path.
     *
     * @param width  the number of columns of the grid
     * @param height the number of rows of the grid
     * @param path   the tiles of the path, in order, each one a step from the one before
     * @param length how many tiles of the array form the path
     * @param stroke the width of the stroke, {@value #MIN_STROKE} or {@value #MAX_STROKE}
     * @param action what to do to each carved tile, called once or more for a tile that several steps reach
     * @throws IllegalArgumentException when the grid is empty, the stroke is outside its range, the length exceeds the
     *                                  array or a tile of the path is outside the grid
     */
    static void path(int width, int height, int[] path, int length, int stroke, IntConsumer action) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(action, "action");
        validate(width, height, path, length, stroke);
        var previousX = 0;
        var previousY = 0;
        for (var position = 0; position < length; position++) {
            final var y = path[position] / width;
            final var x = path[position] - y * width;
            if (position > 0 && x != previousX && y != previousY) {
                stamp(width, height, x, previousY, stroke, action);
            }
            stamp(width, height, x, y, stroke, action);
            previousX = x;
            previousY = y;
        }
    }

    /** Refuses a grid, a stroke, a length or a path tile that the carving cannot honour. */
    private static void validate(int width, int height, int[] path, int length, int stroke) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("the grid needs at least one tile: " + width + "x" + height);
        }
        if (stroke < MIN_STROKE || stroke > MAX_STROKE) {
            throw new IllegalArgumentException(
                    "the stroke must be " + MIN_STROKE + " or " + MAX_STROKE + ": " + stroke);
        }
        if (length < 0 || length > path.length) {
            throw new IllegalArgumentException("the length " + length + " does not fit a path of " + path.length);
        }
        requireTilesOnGrid((long) width * height, path, length);
    }

    /** Refuses a path tile that lies outside a grid of the given number of tiles. */
    private static void requireTilesOnGrid(long tiles, int[] path, int length) {
        for (var position = 0; position < length; position++) {
            if (path[position] < 0 || path[position] >= tiles) {
                throw new IllegalArgumentException(
                        "the path tile " + path[position] + " at " + position + " is outside the grid of " + tiles);
            }
        }
    }

    /** Applies the action to the square of the stroke whose upper left tile is given, as far as it lies on the grid. */
    private static void stamp(int width, int height, int x, int y, int stroke, IntConsumer action) {
        final var lastY = Math.min(height - 1, y + stroke - 1);
        final var lastX = Math.min(width - 1, x + stroke - 1);
        for (var ny = y; ny <= lastY; ny++) {
            for (var nx = x; nx <= lastX; nx++) {
                action.accept(ny * width + nx);
            }
        }
    }
}
