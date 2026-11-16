package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowAnchorTest {

    private static final double[] CAMERA_OFFSETS = {0, 1, 2, 3, 7, -5, -64, 101, 333.0, -250};
    private static final double[] ART_SCALES = {1, 2, 3, 4, 6};
    private static final int[] WIDTHS = {9, 10, 11, 16, 17, 32};
    private static final int[] HEIGHTS = {7, 14, 16, 31, 48};

    /** The screen column of the middle of a resource sprite, from the rounding of the draw call. */
    private static double drawnBaseX(double tileCentreX, int spriteWidth, double scale) {
        final double left = Math.rint(tileCentreX - spriteWidth * scale / 2);
        return left + spriteWidth * scale / 2;
    }

    /** The screen row of the ground row of a resource sprite, from the rounding of the draw call. */
    private static double drawnBaseY(double tileTopY, double halfTileHeight, int spriteHeight, int groundRow,
                                     double scale) {
        final double top = Math.rint(tileTopY + halfTileHeight - spriteHeight * scale);
        return top + groundRow * scale;
    }

    @Test
    void theAnchorOfAResourceShadowIsTheBaseColumnOfItsSpriteForEveryCameraAndScale() {
        for (double scale : ART_SCALES) {
            final double halfTileWidth = 16 * scale;
            for (double offset : CAMERA_OFFSETS) {
                final double phase = Math.rint(offset) - Math.floor(Math.rint(offset) / scale) * scale;
                for (int tile = -4; tile <= 4; tile++) {
                    final double tileCentreX = tile * halfTileWidth + offset;
                    for (int width : WIDTHS) {
                        final double anchorCell = ShadowAnchor.cell(ShadowAnchor.baseX(tileCentreX, width, scale), phase,
                                scale);
                        assertEquals(drawnBaseX(tileCentreX, width, scale),
                                ShadowAnchor.screen(anchorCell, phase, scale), 1e-9,
                                "scale " + scale + " offset " + offset + " tile " + tile + " width " + width);
                    }
                }
            }
        }
    }

    @Test
    void theAnchorOfAResourceShadowIsTheBaseRowOfItsSpriteForEveryCameraAndScale() {
        for (double scale : ART_SCALES) {
            final double halfTileHeight = 8 * scale;
            for (double offset : CAMERA_OFFSETS) {
                final double phase = Math.rint(offset) - Math.floor(Math.rint(offset) / scale) * scale;
                for (int tile = -4; tile <= 4; tile++) {
                    final double tileTopY = tile * halfTileHeight + offset;
                    for (int height : HEIGHTS) {
                        // A tall sprite stands on its bottom row, a low one on the lowest painted row above it
                        for (int groundRow : new int[]{height, height - 3}) {
                            final double groundY = tileTopY + halfTileHeight - (height - groundRow) * scale;
                            final double anchorCell = ShadowAnchor.cell(ShadowAnchor.baseY(groundY, groundRow, scale),
                                    phase, scale);
                            assertEquals(drawnBaseY(tileTopY, halfTileHeight, height, groundRow, scale),
                                    ShadowAnchor.screen(anchorCell, phase, scale), 1e-9,
                                    "scale " + scale + " offset " + offset + " tile " + tile + " height " + height);
                        }
                    }
                }
            }
        }
    }

    @Test
    void aMovingSpriteAndItsShadowStepByWholePixelsTogether() {
        final double scale = 2.0;
        double previousSprite = Double.NaN;
        double previousShadow = Double.NaN;
        for (double x = 100.0; x < 110.0; x += 0.25) {
            final double sprite = drawnBaseX(x, 11, scale);
            final double shadow = ShadowAnchor.baseX(x, 11, scale);
            if (!Double.isNaN(previousSprite)) {
                assertEquals(sprite - previousSprite, shadow - previousShadow, 1e-9, "at " + x);
            }
            previousSprite = sprite;
            previousShadow = shadow;
        }
    }
}
