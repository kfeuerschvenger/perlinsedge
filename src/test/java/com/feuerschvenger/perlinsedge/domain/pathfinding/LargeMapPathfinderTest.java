package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins that a search over a map of the default size stays cheap: the node hash fills the buckets of a HashMap instead of piling the nodes into a few. */
class LargeMapPathfinderTest {

    private static final int SIZE = 300;
    private static final int POCKET = 200;
    private static final int TIME_LIMIT_SECONDS = 20;
    private static final float LOAD_FACTOR = 0.75f;
    /** A uniformly random hash fills about 73 % of that many buckets, a weak one fills a few percent. */
    private static final double MIN_BUCKET_SHARE = 0.6;

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
    void theNodesOfAMapOfTheDefaultSizeSpreadOverTheBucketsOfAHashMap() {
        final var tiles = SIZE * SIZE;
        final var tableSize = Integer.highestOneBit((int) (tiles / LOAD_FACTOR)) << 1;
        final var buckets = new HashSet<Integer>();
        for (var x = 0; x < SIZE; x++) {
            for (var y = 0; y < SIZE; y++) {
                final var hash = new PathNode(x, y).hashCode();
                buckets.add((hash ^ (hash >>> Short.SIZE)) & (tableSize - 1));
            }
        }

        assertTrue(buckets.size() >= MIN_BUCKET_SHARE * Math.min(tiles, tableSize),
                () -> "only " + buckets.size() + " buckets of " + tableSize + " are used by " + tiles + " nodes");
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
