package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.pathfinding.InteractionPlanner;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.SpawnPoint;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An order to a tile that no walk can reach is refused without the search that would explore the whole region the
 * player stands in, on maps of the default size.
 */
class UnreachableOrderTest {

    private static final int SIZE = 300;
    private static final long SEED = 7L;
    private static final int MIN_DISTANCE = 30;
    /** Fewer tiles than any search that explores the region of the spawn of a map of {@link #SIZE} expands. */
    private static final int MIN_EXPANDED_NODES = 30;
    /**
     * Two map types are enough: the order is refused by the regions of the map whatever the terrain, and a map of
     * {@link #SIZE} is costly to generate, so each extra type adds time without adding a case.
     */
    private static final List<MapType> TYPES = List.of(MapType.DENSE_FOREST, MapType.SWAMP);

    private final MapGenerator generator = new MapGenerator(FastNoiseLiteAdapter.FACTORY);

    /** Finds a walkable tile far from the spawn in the region of the spawn. */
    private static int[] farTile(TileMap map, SpawnPoint spawn) {
        final var regions = map.getWalkableRegions();
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                if (Math.abs(x - spawn.x()) + Math.abs(y - spawn.y()) >= MIN_DISTANCE
                        && regions.regionAt(x, y) == regions.regionAt(spawn.x(), spawn.y())) {
                    return new int[]{x, y};
                }
            }
        }
        throw new IllegalStateException("no walkable tile far from the spawn");
    }

    /** Walls in a walkable tile far from the spawn, so that no walk can reach it. */
    private static int[] wallInATile(TileMap map, SpawnPoint spawn) {
        final var tile = farTile(map, spawn);
        wallAround(map, tile[0], tile[1]);
        return tile;
    }

    private static void wallAround(TileMap map, int x, int y) {
        for (var dx = -1; dx <= 1; dx++) {
            for (var dy = -1; dy <= 1; dy++) {
                final var tile = map.getTile(x + dx, y + dy);
                if ((dx != 0 || dy != 0) && tile != null) {
                    tile.setWorldResource(null);
                    tile.setBuilding(new Fence(x + dx, y + dy));
                }
            }
        }
    }

    @Test
    void aWalledInTileIsRefusedByTheRegionsWithoutASearchOrARelabel() {
        for (final var type : TYPES) {
            final var map = generator.generate(new MapRequest(type, SEED, SIZE, SIZE));
            final var spawn = map.getSpawn().orElseThrow();
            final var target = wallInATile(map, spawn);
            final var searches = new AtomicInteger();
            final var player = new Player(spawn.x(), spawn.y(), (searchedMap, occupied) -> {
                searches.incrementAndGet();
                return new Pathfinder(searchedMap, occupied);
            });
            final var regions = map.getWalkableRegions();

            // The search alone explores the region of the player before it finds nothing
            final var pathfinder = new Pathfinder(map);
            final var searched = pathfinder.findPath(spawn.x(), spawn.y(), target[0], target[1]);
            assertFalse(searched.reachable(), type.name());
            assertTrue(pathfinder.getLastExpandedNodes() > MIN_EXPANDED_NODES, type.name());

            // The region check rules the order out, which is the only way it is refused without that search
            assertFalse(regions.mayReach(spawn.x(), spawn.y(), target[0], target[1]), type.name());
            final var relabels = regions.getRelabelCount();
            assertFalse(player.moveTo(map, target[0], target[1]), type.name());
            assertFalse(player.moveTo(map, target[0], target[1]), type.name());

            assertFalse(player.isMoving(), type.name());
            assertEquals(0, searches.get(), type.name());
            assertEquals(relabels, regions.getRelabelCount(), type.name());
        }
    }

    @Test
    void aWalledInTargetOfAnInteractionHasNoWalkToIt() {
        final var map = generator.generate(new MapRequest(MapType.DESERT, SEED, SIZE, SIZE));
        final var spawn = map.getSpawn().orElseThrow();
        final var target = wallInATile(map, spawn);
        map.getTile(target[0], target[1]).setBuilding(new Fence(target[0], target[1]));

        assertTrue(InteractionPlanner.pathToReach(map, spawn.x(), spawn.y(), target[0], target[1]).isEmpty());
    }

    @Test
    void aReachableTileOfAGeneratedMapStillStartsAWalk() {
        for (final var type : TYPES) {
            final var map = generator.generate(new MapRequest(type, SEED, SIZE, SIZE));
            final var spawn = map.getSpawn().orElseThrow();
            final var target = farTile(map, spawn);

            assertTrue(new Player(spawn.x(), spawn.y()).moveTo(map, target[0], target[1]), type.name());
        }
    }
}
