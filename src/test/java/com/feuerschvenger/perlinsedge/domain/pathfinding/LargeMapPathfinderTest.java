package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins that a search over a map of the default size stays cheap, which a weak node hash once made impossible. */
class LargeMapPathfinderTest {

    private static final int SIZE = 300;
    private static final int POCKET = 200;
    private static final int TIME_LIMIT_SECONDS = 20;

    private static TileMap walledPocketMap() {
        final var map = new TileMap(SIZE, SIZE);
        for (var x = 0; x < SIZE; x++) {
            for (var y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        for (var dx = -1; dx <= 1; dx++) {
            for (var dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    map.getTile(POCKET + dx, POCKET + dy).setBuilding(new Fence(POCKET + dx, POCKET + dy));
                }
            }
        }
        return map;
    }

    @Test
    void everyTileOfAMapOfTheDefaultSizeHasItsOwnHash() {
        final var hashes = new HashSet<Integer>();
        for (var x = 0; x < SIZE; x++) {
            for (var y = 0; y < SIZE; y++) {
                hashes.add(new PathNode(x, y).hashCode());
            }
        }

        assertEquals(SIZE * SIZE, hashes.size());
    }

    @Test
    void searchingForAWalledOffTileExpandsTheWholeMapQuickly() {
        final var map = walledPocketMap();
        final var pathfinder = new Pathfinder(map);

        final var result = assertTimeoutPreemptively(Duration.ofSeconds(TIME_LIMIT_SECONDS),
                () -> pathfinder.findPath(0, 0, POCKET, POCKET));

        assertTrue(result.nodes().isEmpty());
        assertTrue(pathfinder.getLastExpandedNodes() > SIZE * SIZE / 2);
    }
}
