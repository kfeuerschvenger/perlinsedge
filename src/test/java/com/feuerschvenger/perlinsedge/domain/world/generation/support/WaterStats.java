package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/** Questions the harness asks of the water, the rivers, the land masses and the spawn region of a generated map. */
public final class WaterStats {

    /** The smallest body of water that counts as one, so a puddle of one or two tiles is not a body. */
    public static final int MIN_BODY_TILES = 4;
    /** The smallest land mass that counts as an island or a component of land. */
    public static final int MIN_LAND_TILES = 20;

    private WaterStats() {
    }

    /**
     * Tells whether a terrain is water, which is the terrain that is not solid ground: ice counts as land.
     *
     * @param type the terrain
     * @return true for deep and shallow water and for rivers
     */
    public static boolean isWater(TileType type) {
        return !type.isLand();
    }

    /**
     * Measures the share of the map that is water.
     *
     * @param map the map
     * @return the water tiles over all tiles, from 0 to 1
     */
    public static double waterShare(TileMap map) {
        return 1.0 - MapStats.landTiles(map) / (double) (map.getWidth() * map.getHeight());
    }

    /**
     * Finds the 4-connected bodies of water, rivers included.
     *
     * @param map the map
     * @return the components of the water tiles
     */
    public static Components waterBodies(TileMap map) {
        return Components.of(map, WaterStats::isWater);
    }

    /**
     * Finds the 4-connected components of river tiles.
     *
     * @param map the map
     * @return the components of the river tiles
     */
    public static Components rivers(TileMap map) {
        return Components.of(map, type -> type == TileType.RIVER);
    }

    /**
     * Finds the 4-connected masses of land, ice included.
     *
     * @param map the map
     * @return the components of the land tiles
     */
    public static Components landMasses(TileMap map) {
        return Components.of(map, TileType::isLand);
    }

    /**
     * Measures how much of the land the player can reach on foot from the spawn, ignoring the resources on the way,
     * since a player can cut a tree. Water, rivers and mountains cut the land into parts and this is the share of the
     * land that lies in the part of the spawn.
     *
     * @param map the map, which has a spawn
     * @return the walkable tiles connected to the spawn over all land tiles, from 0 to 1
     */
    public static double spawnRegionShareOfLand(TileMap map) {
        final var walkable = Components.of(map, TileType::isWalkable);
        final var spawn = map.getSpawn().orElseThrow();
        final var label = walkable.labelAt(spawn.x(), spawn.y());
        return walkable.sizeOf(label) / (double) MapStats.landTiles(map);
    }

    /**
     * Measures the share of water along each side of the map and returns the highest, which tells how much of the
     * longest coast one side holds.
     *
     * @param map the map
     * @return the water tiles over the tiles of the outermost row or column, for the side with the most water
     */
    public static double wettestEdgeShare(TileMap map) {
        final var width = map.getWidth();
        final var height = map.getHeight();
        var top = 0;
        var bottom = 0;
        for (var x = 0; x < width; x++) {
            top += isWater(map.getTile(x, 0).getType()) ? 1 : 0;
            bottom += isWater(map.getTile(x, height - 1).getType()) ? 1 : 0;
        }
        var left = 0;
        var right = 0;
        for (var y = 0; y < height; y++) {
            left += isWater(map.getTile(0, y).getType()) ? 1 : 0;
            right += isWater(map.getTile(width - 1, y).getType()) ? 1 : 0;
        }
        return Math.max(Math.max(top, bottom) / (double) width, Math.max(left, right) / (double) height);
    }
}
