package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Where an item thrown by the player lands.
 * The item travels up to {@link #MAX_DISTANCE} tiles along the facing direction and stops on the last walkable
 * tile before the first one that is not: a wall, water, a resource or the edge of the map never receives an item and
 * is never flown over. When the very first tile is blocked the item lands on the thrower's own tile.
 */
public final class ItemThrow {
    /** Farthest a thrown item travels, in tiles. */
    public static final int MAX_DISTANCE = 2;

    /** Tile offsets of the eight facing directions, indexed by the {@code DIR_*} constants of {@link Entity}. */
    private static final int[][] DIRECTION_OFFSETS = {
            {0, -1},  // DIR_UP
            {1, -1},  // DIR_UP_RIGHT
            {1, 0},   // DIR_RIGHT
            {1, 1},   // DIR_DOWN_RIGHT
            {0, 1},   // DIR_DOWN
            {-1, 1},  // DIR_DOWN_LEFT
            {-1, 0},  // DIR_LEFT
            {-1, -1}  // DIR_UP_LEFT
    };

    private ItemThrow() {}

    /**
     * Tile offset of a facing direction.
     *
     * @param direction one of the {@code Entity.DIR_*} constants
     * @return {dx, dy}, which is {0, 0} for a value that is not a direction
     */
    public static int[] offsetOf(int direction) {
        if (direction < 0 || direction >= DIRECTION_OFFSETS.length) {
            return new int[]{0, 0};
        }
        return DIRECTION_OFFSETS[direction].clone();
    }

    /**
     * Finds the tile a thrown item lands on.
     *
     * @param map       the world, a null map leaves the item on the thrower's tile
     * @param fromX     thrower tile X
     * @param fromY     thrower tile Y
     * @param direction facing direction, one of the {@code Entity.DIR_*} constants
     * @return {x, y} of the landing tile
     */
    public static int[] landingTile(TileMap map, int fromX, int fromY, int direction) {
        int[] offset = offsetOf(direction);
        int landingX = fromX;
        int landingY = fromY;
        if (map == null) {
            return new int[]{landingX, landingY};
        }
        for (int step = 1; step <= MAX_DISTANCE; step++) {
            int x = fromX + offset[0] * step;
            int y = fromY + offset[1] * step;
            Tile tile = map.getTile(x, y);
            if (tile == null || !tile.isWalkable()) {
                break;
            }
            landingX = x;
            landingY = y;
        }
        return new int[]{landingX, landingY};
    }
}
