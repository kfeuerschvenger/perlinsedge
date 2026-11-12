package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.utils.TileDepths;

/**
 * Finds the tile under a screen point, the one function behind both the hover cursor and the clicks.
 * Land tops are drawn at the tile position and the water sheet half a depth step lower, so the diamond a water tile
 * shows is lower than the diamond of the same grid cell on land. The picker looks at the two cells that can be under
 * the point, the one in the land layer and the one in the water layer, and returns what the player sees on top: a land
 * tile whose top covers the point wins over the water behind it, because land in front is drawn after the water.
 * A point on the side face a land tile shows above lower water belongs to that land tile.
 * Nothing is allocated, the result is written into a caller supplied array.
 */
public final class TilePicker {

    /** Not instantiable: the class only holds static rules. */
    private TilePicker() {}

    /**
     * Picks the visible tile at a screen point.
     *
     * @param map            the map, which says whether each candidate cell is land or water
     * @param screenX        X of the point on the canvas
     * @param screenY        Y of the point on the canvas
     * @param cameraX        X of the camera, where the top vertex of tile 0,0 is drawn
     * @param cameraY        Y of the camera, where the top vertex of tile 0,0 is drawn
     * @param zoom           camera zoom
     * @param halfTileWidth  half the width of a tile at zoom 1
     * @param halfTileHeight half the height of a tile at zoom 1
     * @param out            receives the tile column and row, at least two elements
     * @return true when a tile of the map is under the point, false when the point is outside the map
     */
    public static boolean pick(TileMap map, double screenX, double screenY, double cameraX, double cameraY,
                               double zoom, double halfTileWidth, double halfTileHeight, int[] out) {
        final double scaledHalfWidth = halfTileWidth * zoom;
        final double scaledHalfHeight = halfTileHeight * zoom;
        final double sheetDrop = TileDepths.SURFACE_PIXELS * ZoomSteps.artScale(zoom);

        final double worldX = screenX - cameraX;
        final double worldY = screenY - cameraY;
        final int landX = cellX(worldX, worldY, scaledHalfWidth, scaledHalfHeight);
        final int landY = cellY(worldX, worldY, scaledHalfWidth, scaledHalfHeight);
        final int waterX = cellX(worldX, worldY - sheetDrop, scaledHalfWidth, scaledHalfHeight);
        final int waterY = cellY(worldX, worldY - sheetDrop, scaledHalfWidth, scaledHalfHeight);

        final Tile land = map.getTile(landX, landY);
        final Tile water = map.getTile(waterX, waterY);
        final boolean landShowsTop = land != null && !land.getType().isWater();

        // Land wins whenever its top covers the point, also when the water sheet shows there too. The water sheet
        // sits lower, so a land top that covers a point is in front of it, and the renderer draws the land after the
        // water: the two cells are never ordered by their position in the grid.
        if (landShowsTop) {
            return write(out, landX, landY);
        }
        if (water != null) {
            // The water diamond, or the side face of the land tile above lower water
            return write(out, waterX, waterY);
        }
        if (land != null) {
            // The land layer holds a water tile and the water layer is off the map, which happens on the top edge:
            // the point is on the margin above the sheet of an edge tile, with nothing else under it. The nearest
            // tile drawn there is that water tile, so the hover stays on the map instead of dropping out at the edge.
            return write(out, landX, landY);
        }
        return false;
    }

    private static boolean write(int[] out, int x, int y) {
        out[0] = x;
        out[1] = y;
        return true;
    }

    /** Column of the exact diamond cell that holds a point given relative to the camera. */
    private static int cellX(double worldX, double worldY, double halfWidth, double halfHeight) {
        return (int) Math.floor((worldX / halfWidth + worldY / halfHeight) / 2);
    }

    /** Row of the exact diamond cell that holds a point given relative to the camera. */
    private static int cellY(double worldX, double worldY, double halfWidth, double halfHeight) {
        return (int) Math.floor((worldY / halfHeight - worldX / halfWidth) / 2);
    }
}
