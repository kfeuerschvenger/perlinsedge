package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatTilesTest {

    private static final int GREEN = FlatTiles.argbOf(Rgba.rgb(60, 120, 60));

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    @Test
    void aColourPacksIntoAnOpaqueArgb() {
        assertEquals(0xFF3C783C, GREEN);
        assertEquals(0xFF000000, FlatTiles.argbOf(Rgba.rgb(0, 0, 0)));
        assertEquals(0xFFFFFFFF, FlatTiles.argbOf(Rgba.rgb(255, 255, 255)));
    }

    @Test
    void scalingDarkensEachChannelAndKeepsTheColourOpaque() {
        int darker = FlatTiles.scale(0xFF646464, 0.5);
        assertEquals(0xFF323232, darker);
        assertEquals(0xFF000000, FlatTiles.scale(0xFF010101, 0.1) & 0xFF000000);
    }

    @Test
    void theBlockIsTheTopDiamondPushedDownByTheFaceHeight() {
        assertTrue(FlatTiles.insideBlock(16, 0));
        assertTrue(FlatTiles.insideBlock(16, FlatTiles.HEIGHT - 1));
        assertFalse(FlatTiles.insideBlock(0, 0));
        assertFalse(FlatTiles.insideBlock(16, FlatTiles.HEIGHT));
        assertEquals(21, FlatTiles.HEIGHT);
        int top = 0;
        int block = 0;
        for (int y = 0; y < FlatTiles.HEIGHT; y++) {
            for (int x = 0; x < FlatTiles.WIDTH; x++) {
                top += GrayscaleTiles.insideDiamond(x, y) ? 1 : 0;
                block += FlatTiles.insideBlock(x, y) ? 1 : 0;
                assertEquals(FlatTiles.insideBlock(x, y), FlatTiles.pixel(x, y, GREEN, false) != 0, x + "," + y);
            }
        }
        assertTrue(block > top);
    }

    @Test
    void landHasTheTileColourOnTopAndDarkerFacesBelow() {
        assertEquals(GREEN, FlatTiles.pixel(16, 8, GREEN, false));
        int left = FlatTiles.pixel(3, 13, GREEN, false);
        int right = FlatTiles.pixel(28, 13, GREEN, false);
        assertTrue(red(left) < red(GREEN));
        assertTrue(red(right) < red(left));
        assertEquals(FlatTiles.scale(GREEN, FlatTiles.DARKER), left);
        assertEquals(FlatTiles.scale(GREEN, FlatTiles.DARKER * FlatTiles.DARKER), right);
    }

    @Test
    void landOutlinesItsTopInADarkerShade() {
        assertEquals(FlatTiles.scale(GREEN, FlatTiles.DARKER), FlatTiles.pixel(16, 0, GREEN, false));
        assertNotEquals(GREEN, FlatTiles.pixel(16, 0, GREEN, false));
    }

    @Test
    void waterIsOneFlatColourWithNoOutlineOrShading() {
        for (int y = 0; y < FlatTiles.HEIGHT; y++) {
            for (int x = 0; x < FlatTiles.WIDTH; x++) {
                int pixel = FlatTiles.pixel(x, y, GREEN, true);
                assertEquals(FlatTiles.insideBlock(x, y) ? GREEN : 0, pixel, x + "," + y);
            }
        }
    }
}
