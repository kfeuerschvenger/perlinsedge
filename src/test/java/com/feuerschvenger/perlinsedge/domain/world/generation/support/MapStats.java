package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * What a generated map holds, counted once: tiles per terrain type, resources per kind, and how many tiles are land.
 *
 * @param tiles     tiles per terrain type
 * @param resources resources per kind
 * @param land      tiles a player can walk on
 * @param total     all tiles of the map
 */
public record MapStats(Map<TileType, Integer> tiles, Map<ResourceType, Integer> resources, int land, int total) {

    /**
     * Counts a map.
     *
     * @param map the map
     * @return its statistics
     */
    public static MapStats of(TileMap map) {
        final var tiles = new EnumMap<TileType, Integer>(TileType.class);
        final var resources = new EnumMap<ResourceType, Integer>(ResourceType.class);
        var land = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                tiles.merge(tile.getType(), 1, Integer::sum);
                land += tile.getType().isWalkable() ? 1 : 0;
                if (tile.hasResource()) {
                    resources.merge(tile.getWorldResource().getType(), 1, Integer::sum);
                }
            }
        }
        return new MapStats(tiles, resources, land, map.getWidth() * map.getHeight());
    }

    /**
     * Counts one terrain type.
     *
     * @param type the type
     * @return how many tiles have it
     */
    public int count(TileType type) {
        return tiles.getOrDefault(type, 0);
    }

    /**
     * Counts one resource kind.
     *
     * @param type the kind
     * @return how many resources of it the map holds
     */
    public int count(ResourceType type) {
        return resources.getOrDefault(type, 0);
    }

    /**
     * Finds the walkable tile without a resource that is nearest to the centre of the map, which is where a player
     * could start today. Ties go to the first found in row order, so the answer is stable.
     *
     * @param map the map
     * @return the tile, or empty when the map has none
     */
    public static Optional<Spot> nearestFreeTileToCentre(TileMap map) {
        final var centreX = map.getWidth() / 2;
        final var centreY = map.getHeight() / 2;
        Spot best = null;
        var bestDistance = Long.MAX_VALUE;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final long dx = x - centreX;
                final long dy = y - centreY;
                final var distance = dx * dx + dy * dy;
                if (distance < bestDistance && isFree(map.getTile(x, y))) {
                    best = new Spot(x, y);
                    bestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean isFree(Tile tile) {
        return tile.isWalkable() && !tile.hasResource();
    }
}
