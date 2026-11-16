package com.feuerschvenger.perlinsedge.infra.fx.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilhouetteTest {

    private static final int RED_OPAQUE = 0xFFFF0000;
    private static final int GREEN_FAINT = 0x2000FF00;
    private static final int GREEN_HALF = 0x8000FF00;

    @Test
    void opaquePixelsBecomeBlackOpaqueAndTransparentOnesStayTransparent() {
        final Silhouette silhouette = new Silhouette(2, 2, new int[]{RED_OPAQUE, 0, 0x00123456, 0xFF336699});

        assertEquals(Silhouette.SOLID, silhouette.argbAt(0, 0));
        assertEquals(0xFF000000, silhouette.argbAt(0, 0));
        assertEquals(0, silhouette.argbAt(1, 0));
        assertEquals(0, silhouette.argbAt(0, 1), "colour under zero alpha must not leak into the shape");
        assertEquals(Silhouette.SOLID, silhouette.argbAt(1, 1));
        assertTrue(silhouette.isSolid(0, 0));
        assertFalse(silhouette.isSolid(1, 0));
    }

    @Test
    void aFaintEdgeIsDroppedAndAHalfOpaquePixelIsKept() {
        final Silhouette silhouette = new Silhouette(2, 1, new int[]{GREEN_FAINT, GREEN_HALF});

        assertFalse(silhouette.isSolid(0, 0));
        assertTrue(silhouette.isSolid(1, 0));
    }

    @Test
    void itKeepsTheSizeOfTheSprite() {
        final Silhouette silhouette = new Silhouette(3, 2, new int[6]);

        assertEquals(3, silhouette.width());
        assertEquals(2, silhouette.height());
    }

    @Test
    void thePaintedBottomIsTheRowAfterTheLowestPaintedPixel() {
        // Row 0 empty, row 1 painted, rows 2 and 3 empty padding
        final int[] pixels = new int[3 * 4];
        pixels[1 * 3 + 1] = RED_OPAQUE;

        assertEquals(2, new Silhouette(3, 4, pixels).paintedBottom());
        assertEquals(0, new Silhouette(3, 4, new int[12]).paintedBottom());
    }
}
