package com.feuerschvenger.perlinsedge.infra.fx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrayscaleTilesTest {

    @Test
    void heightsMapToABrightnessBetweenZeroAndOneAndAreClamped() {
        assertEquals(0.0, GrayscaleTiles.normalized(-1.0));
        assertEquals(0.5, GrayscaleTiles.normalized(0.0));
        assertEquals(1.0, GrayscaleTiles.normalized(1.0));
        assertEquals(0.0, GrayscaleTiles.normalized(-5.0));
        assertEquals(1.0, GrayscaleTiles.normalized(5.0));
    }

    @Test
    void levelsCoverTheWholeRangeAndNeverLeaveIt() {
        assertEquals(0, GrayscaleTiles.levelFor(-1.0));
        assertEquals(GrayscaleTiles.LEVELS - 1, GrayscaleTiles.levelFor(1.0));
        assertEquals(GrayscaleTiles.LEVELS - 1, GrayscaleTiles.levelFor(9.0));
        assertEquals(0, GrayscaleTiles.levelFor(-9.0));
        int previous = -1;
        for (double h = -1.0; h <= 1.0; h += 0.01) {
            int level = GrayscaleTiles.levelFor(h);
            assertTrue(level >= previous, "levels never decrease with height");
            previous = level;
        }
    }

    @Test
    void greysRunFromBlackToWhite() {
        assertEquals(0, GrayscaleTiles.greyOf(0));
        assertEquals(255, GrayscaleTiles.greyOf(GrayscaleTiles.LEVELS - 1));
        assertTrue(GrayscaleTiles.greyOf(10) < GrayscaleTiles.greyOf(11));
    }

    @Test
    void theDiamondIsTheTopFaceOfATile() {
        assertFalse(GrayscaleTiles.insideDiamond(0, 0));
        assertTrue(GrayscaleTiles.insideDiamond(16, 0));
        assertTrue(GrayscaleTiles.insideDiamond(0, 7));
        assertTrue(GrayscaleTiles.insideDiamond(31, 8));
        assertTrue(GrayscaleTiles.insideDiamond(16, 15));
        assertFalse(GrayscaleTiles.insideDiamond(16, 16));
        assertFalse(GrayscaleTiles.insideDiamond(-1, 8));
        assertFalse(GrayscaleTiles.insideDiamond(32, 8));
    }

    @Test
    void theOutlineHugsTheEdgeAndLeavesTheMiddleSolid() {
        assertTrue(GrayscaleTiles.isOutline(16, 0));
        assertTrue(GrayscaleTiles.isOutline(0, 7));
        assertFalse(GrayscaleTiles.isOutline(16, 8));
        assertFalse(GrayscaleTiles.isOutline(0, 0), "outside pixels are not outline");
    }
}
