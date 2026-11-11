package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/**
 * Chooses which translucent water sheet a water tile is drawn with.
 * Deep water that touches shallow or coast water uses an intermediate tone, so the colour grades from the shallows to the
 * open water instead of jumping. It is purely cosmetic: the tile keeps its type, and the choice is a pure function of the
 * tile and its eight neighbours.
 */
public final class WaterTones {

    private WaterTones() {}

    /**
     * Returns the water art of a tile.
     *
     * @param type                  the tile type
     * @param touchesShallowWater   whether one of the eight neighbours is shallow or coast water
     * @return the art name, {@link AssetPaths#DEEP_EDGE_WATER} for deep water at the edge of the shallows, the plain art of the
     *         type otherwise, or null when the type is not water
     */
    public static String artFor(TileType type, boolean touchesShallowWater) {
        if (type == TileType.DEEP_WATER && touchesShallowWater) {
            return AssetPaths.DEEP_EDGE_WATER;
        }
        return AssetPaths.waterArt(type);
    }

    /**
     * Tells whether one of the eight neighbours of a tile is shallow or coast water.
     *
     * @param map the map
     * @param x   the tile column
     * @param y   the tile row
     * @return true when a neighbour inside the map is shallow or coast water
     */
    public static boolean touchesShallowWater(TileMap map, int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                final Tile neighbour = map.getTile(x + dx, y + dy);
                if (neighbour != null && isShallow(neighbour.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isShallow(TileType type) {
        return type == TileType.SHALLOW_WATER || type == TileType.COAST;
    }
}
