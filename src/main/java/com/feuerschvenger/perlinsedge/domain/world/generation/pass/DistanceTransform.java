package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * The Chebyshev distance of every tile to the nearest selected tile, in two linear sweeps over an {@code int[]}: a
 * forward sweep from the upper left and a backward sweep from the lower right, each looking at the four neighbours
 * already swept. Chebyshev distance counts a diagonal step as one, so the tiles within distance {@code r} of a tile
 * form the square of side {@code 2r + 1} around it. A pass that needs "within reach of water" or "clear of rock"
 * reads it from one transform instead of scanning a square around each tile.
 */
final class DistanceTransform {

    /** The distance of a tile when no tile is selected. */
    public static final int UNREACHABLE = Integer.MAX_VALUE / 2;

    private DistanceTransform() {
    }

    /**
     * Computes the distance of every tile to the nearest selected tile.
     *
     * @param width  the number of columns
     * @param height the number of rows
     * @param source the tiles at distance 0
     * @return a new array indexed like the grid, with {@link #UNREACHABLE} everywhere when no tile is selected
     */
    static int[] chebyshev(int width, int height, IntPredicate source) {
        Objects.requireNonNull(source, "source");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("the grid needs at least one tile: " + width + "x" + height);
        }
        final var distance = new int[width * height];
        for (var index = 0; index < distance.length; index++) {
            distance[index] = source.test(index) ? 0 : UNREACHABLE;
        }
        sweepForward(distance, width, height);
        sweepBackward(distance, width, height);
        return distance;
    }

    /** Takes the distance through the west, north west, north and north east neighbours. */
    private static void sweepForward(int[] distance, int width, int height) {
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                final var index = y * width + x;
                var best = distance[index];
                if (x > 0) {
                    best = Math.min(best, distance[index - 1] + 1);
                }
                if (y > 0) {
                    best = Math.min(best, nearestInRow(distance, index - width, x, width));
                }
                distance[index] = Math.min(best, UNREACHABLE);
            }
        }
    }

    /** Takes the distance through the east, south east, south and south west neighbours. */
    private static void sweepBackward(int[] distance, int width, int height) {
        for (var y = height - 1; y >= 0; y--) {
            for (var x = width - 1; x >= 0; x--) {
                final var index = y * width + x;
                var best = distance[index];
                if (x < width - 1) {
                    best = Math.min(best, distance[index + 1] + 1);
                }
                if (y < height - 1) {
                    best = Math.min(best, nearestInRow(distance, index + width, x, width));
                }
                distance[index] = Math.min(best, UNREACHABLE);
            }
        }
    }

    /** Returns one more than the lowest distance of the up to three tiles of the row at {@code rowIndex} around x. */
    private static int nearestInRow(int[] distance, int rowIndex, int x, int width) {
        var best = distance[rowIndex];
        if (x > 0) {
            best = Math.min(best, distance[rowIndex - 1]);
        }
        if (x < width - 1) {
            best = Math.min(best, distance[rowIndex + 1]);
        }
        return best + 1;
    }
}
