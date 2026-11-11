package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;

/**
 * The visibility test the tile passes share, so a tile skipped by one pass is skipped by all of them.
 */
final class ScreenCulling {

    /** Art pixels the deepest bed lies below the land tops, so its block still reaches into view from above. */
    static final int MAX_SINK_ART_PIXELS = TileDepths.sinkPixels(TileType.DEEP_WATER);

    /** Not instantiable: the class only holds the shared test. */
    private ScreenCulling() {}

    /**
     * Checks whether the bounding box of a tile is within the visible canvas.
     * The top test allows for the sunk beds of water, which are drawn lower than the land top the coordinates refer to.
     *
     * @param screenX    X-coordinate of the tile top vertex
     * @param screenY    Y-coordinate of the tile top vertex
     * @param tileWidth  zoomed tile width
     * @param tileHeight zoomed tile height
     * @param canvasWidth  canvas width
     * @param canvasHeight canvas height
     * @param artScale   screen pixels per art pixel
     * @return true when any part of the tile may be on screen
     */
    static boolean isTileVisible(double screenX, double screenY, double tileWidth, double tileHeight,
                                 double canvasWidth, double canvasHeight, double artScale) {
        return screenX + tileWidth > 0 &&
                screenX - tileWidth < canvasWidth &&
                screenY + tileHeight * 2 + MAX_SINK_ART_PIXELS * artScale > 0 &&
                screenY - tileHeight * 3 < canvasHeight;
    }
}
