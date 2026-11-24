package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.Optional;

/** Questions the harness asks of a generated map. */
public final class MapStats {

    private MapStats() {
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
