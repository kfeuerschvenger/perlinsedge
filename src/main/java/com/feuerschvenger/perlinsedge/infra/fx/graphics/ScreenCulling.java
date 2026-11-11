package com.feuerschvenger.perlinsedge.infra.fx.graphics;

/**
 * The visibility test the tile passes share, so a tile skipped by one pass is skipped by all of them.
 */
final class ScreenCulling {

    private ScreenCulling() {}

    /**
     * Checks whether the bounding box of a tile is within the visible canvas.
     *
     * @param screenX    X-coordinate of the tile top vertex
     * @param screenY    Y-coordinate of the tile top vertex
     * @param tileWidth  zoomed tile width
     * @param tileHeight zoomed tile height
     * @param canvasWidth  canvas width
     * @param canvasHeight canvas height
     * @return true when any part of the tile may be on screen
     */
    static boolean isTileVisible(double screenX, double screenY, double tileWidth, double tileHeight,
                                 double canvasWidth, double canvasHeight) {
        return screenX + tileWidth > 0 &&
                screenX - tileWidth < canvasWidth &&
                screenY + tileHeight * 2 > 0 &&
                screenY - tileHeight * 3 < canvasHeight;
    }
}
