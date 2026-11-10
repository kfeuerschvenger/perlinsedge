package com.feuerschvenger.perlinsedge.domain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class IsometricUtilsTest {

    private static final double HALF_W = 32.0;
    private static final double HALF_H = 16.0;
    private static final double DELTA = 1e-9;

    @Test
    void tileToScreenMatchesKnownValues() {
        assertArrayEquals(new double[]{100, 50},
                IsometricUtils.tileToScreen(0, 0, 100, 50, 1, HALF_W, HALF_H), DELTA);
        assertArrayEquals(new double[]{132, 98},
                IsometricUtils.tileToScreen(2, 1, 100, 50, 1, HALF_W, HALF_H), DELTA);
    }

    @Test
    void tileToScreenScalesWithZoom() {
        assertArrayEquals(new double[]{64, 96},
                IsometricUtils.tileToScreen(2, 1, 0, 0, 2, HALF_W, HALF_H), DELTA);
    }

    @Test
    void screenToTileFindsKnownTile() {
        assertArrayEquals(new int[]{2, 1},
                IsometricUtils.screenToTile(132, 98, 100, 50, 1, HALF_W, HALF_H));
    }

    @Test
    void fractionalRoundTripReturnsOriginalTile() {
        double[] screen = IsometricUtils.tileToScreen(7.25, 3.5, 40, -20, 1.5, HALF_W, HALF_H);
        double[] tile = IsometricUtils.screenToTileFractional(screen[0], screen[1], 40, -20, 1.5, HALF_W, HALF_H);
        assertArrayEquals(new double[]{7.25, 3.5}, tile, DELTA);
    }

    @Test
    void screenToTileFloorsNegativeCoordinatesInsteadOfTruncating() {
        // One pixel left of the origin sits in tile (-1, 0); truncation toward zero would give (0, 0)
        assertArrayEquals(new int[]{-1, 0},
                IsometricUtils.screenToTile(-1, 0, 0, 0, 1, HALF_W, HALF_H));
        assertArrayEquals(new int[]{-1, -1},
                IsometricUtils.screenToTile(0, -1, 0, 0, 1, HALF_W, HALF_H));
    }

    @Test
    void screenToTileHandlesNegativeTileCoordinates() {
        double[] screen = IsometricUtils.tileToScreen(-3, -2, 0, 0, 1, HALF_W, HALF_H);
        assertArrayEquals(new double[]{-32, -80}, screen, DELTA);
        assertArrayEquals(new int[]{-3, -2},
                IsometricUtils.screenToTile(screen[0], screen[1], 0, 0, 1, HALF_W, HALF_H));
    }

    @Test
    void screenToTileHandlesNegativeCameraOffset() {
        assertArrayEquals(new int[]{2, 1},
                IsometricUtils.screenToTile(-68, -2, -100, -50, 1, HALF_W, HALF_H));
    }

    @Test
    void screenToTileMapsTileVertexToThatTileAndJustBeforeItToTheNeighbour() {
        // The top vertex of tile (2, 1) is exactly on its boundary and belongs to it
        assertArrayEquals(new int[]{2, 1},
                IsometricUtils.screenToTile(32, 48, 0, 0, 1, HALF_W, HALF_H));
        // A hair above that vertex lies in tile (1, 0)
        assertArrayEquals(new int[]{1, 0},
                IsometricUtils.screenToTile(32, 47.99, 0, 0, 1, HALF_W, HALF_H));
        // A hair below it is inside tile (2, 1)
        assertArrayEquals(new int[]{2, 1},
                IsometricUtils.screenToTile(32, 48.01, 0, 0, 1, HALF_W, HALF_H));
    }

    @Test
    void screenToTileMapsOriginVertexToTileZero() {
        assertArrayEquals(new int[]{0, 0},
                IsometricUtils.screenToTile(0, 0, 0, 0, 1, HALF_W, HALF_H));
    }

    @Test
    void screenToTileFractionalKeepsNegativeValues() {
        assertArrayEquals(new double[]{-1.5, 0},
                IsometricUtils.screenToTileFractional(-48, -24, 0, 0, 1, HALF_W, HALF_H), DELTA);
    }

    @Test
    void tileToScreenHandlesNegativeTileCoordinates() {
        assertArrayEquals(new double[]{32, -16},
                IsometricUtils.tileToScreen(-1, 0, 64, 0, 1, HALF_W, HALF_H), DELTA);
    }

    @Test
    void integerRoundTripReturnsOriginalTile() {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                // Sample the middle of the tile so floor() cannot land on a neighbour
                double[] screen = IsometricUtils.tileToScreen(x + 0.5, y + 0.5, 0, 0, 1.25, HALF_W, HALF_H);
                int[] tile = IsometricUtils.screenToTile(screen[0], screen[1], 0, 0, 1.25, HALF_W, HALF_H);
                assertArrayEquals(new int[]{x, y}, tile, "tile " + x + "," + y);
            }
        }
    }
}
