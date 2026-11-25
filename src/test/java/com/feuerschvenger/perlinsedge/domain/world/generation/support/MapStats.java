package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/** Questions the harness asks of a generated map. */
public final class MapStats {

    private MapStats() {
    }

    /**
     * Tells whether a tile is walkable ground with no resource on it, which is what a player can stand on and walk
     * through.
     *
     * @param tile the tile
     * @return true when the tile is free
     */
    public static boolean isFree(Tile tile) {
        return tile.isWalkable() && !tile.hasResource();
    }

    /**
     * Tells whether a terrain is the rocky ground that iron ore and crystals lie in.
     *
     * @param type the terrain
     * @return true for rocky ground only, because a mountain cannot be walked onto
     */
    public static boolean isRocky(TileType type) {
        return type == TileType.ROCKY_GROUND;
    }

    /**
     * Counts the tiles that hold a resource.
     *
     * @param map  the map
     * @param type the resource
     * @return the number of tiles
     */
    public static int count(TileMap map, ResourceType type) {
        var count = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var resource = map.getTile(x, y).getWorldResource();
                count += resource != null && resource.getType() == type ? 1 : 0;
            }
        }
        return count;
    }

    /**
     * Counts the tiles of rocky ground.
     *
     * @param map the map
     * @return the number of rocky ground tiles
     */
    public static int rockyTiles(TileMap map) {
        var count = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                count += isRocky(map.getTile(x, y).getType()) ? 1 : 0;
            }
        }
        return count;
    }

    /**
     * Counts the tiles of land, the ground something can stand on, ice included.
     *
     * @param map the map
     * @return the number of land tiles
     */
    public static int landTiles(TileMap map) {
        var count = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                count += map.getTile(x, y).getType().isLand() ? 1 : 0;
            }
        }
        return count;
    }

    /**
     * Counts the tiles of walkable ground, with or without resources.
     *
     * @param map the map
     * @return the number of walkable tiles
     */
    public static int walkableTiles(TileMap map) {
        var count = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                count += map.getTile(x, y).getType().isWalkable() ? 1 : 0;
            }
        }
        return count;
    }

    /**
     * Counts the free tiles, walkable ground with no resource on it.
     *
     * @param map the map
     * @return the number of free tiles
     */
    public static int freeTiles(TileMap map) {
        var count = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                count += isFree(map.getTile(x, y)) ? 1 : 0;
            }
        }
        return count;
    }

    /**
     * Counts the tiles of a terrain.
     *
     * @param map  the map
     * @param type the terrain
     * @return the number of tiles
     */
    public static int count(TileMap map, TileType type) {
        var count = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                count += map.getTile(x, y).getType() == type ? 1 : 0;
            }
        }
        return count;
    }

    /**
     * Works out, for every tile, the size of the 4-connected region of free tiles it belongs to.
     *
     * @param map the map
     * @return an array indexed {@code y * width + x} with the size of the region of each tile, 0 for a tile that is not
     *         free
     */
    public static int[] regionSizes(TileMap map) {
        final var width = map.getWidth();
        final var height = map.getHeight();
        final var sizes = new int[width * height];
        final var seen = new boolean[sizes.length];
        final var queue = new int[sizes.length];
        for (var start = 0; start < sizes.length; start++) {
            if (!seen[start] && isFree(map.getTile(start % width, start / width))) {
                final var size = flood(map, seen, queue, start);
                for (var i = 0; i < size; i++) {
                    sizes[queue[i]] = size;
                }
            }
        }
        return sizes;
    }

    /** Visits the free tiles connected to a start tile and returns how many, which are the head of the queue. */
    private static int flood(TileMap map, boolean[] seen, int[] queue, int start) {
        final var width = map.getWidth();
        var head = 0;
        var tail = 0;
        seen[start] = true;
        queue[tail++] = start;
        while (head < tail) {
            final var index = queue[head++];
            final var x = index % width;
            final var y = index / width;
            tail = enqueue(map, seen, queue, tail, x - 1, y);
            tail = enqueue(map, seen, queue, tail, x + 1, y);
            tail = enqueue(map, seen, queue, tail, x, y - 1);
            tail = enqueue(map, seen, queue, tail, x, y + 1);
        }
        return tail;
    }

    private static int enqueue(TileMap map, boolean[] seen, int[] queue, int tail, int x, int y) {
        if (!map.isValidCoordinate(x, y)) {
            return tail;
        }
        final var index = y * map.getWidth() + x;
        if (seen[index] || !isFree(map.getTile(x, y))) {
            return tail;
        }
        seen[index] = true;
        queue[tail] = index;
        return tail + 1;
    }
}
