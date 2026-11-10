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
