package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Enemies spawn on tiles of their own, in numbers that follow the area, and never near the player. */
class EnemySpawnTest {

    /** A flat map that is wide enough to leave room outside the start safe radius. */
    private static final int SIZE = 100;
    private static final int PLAYER_TILE = SIZE / 2;
    private static final int ROUNDS = 20;
    private static final int SMALL_SIZE = 40;
    /** The side of the walkable patch of the crowded map, in a world of deep water. */
    private static final int PATCH_SIDE = 5;
    private static final int CROWDED_SIZE = 100;
    private static final int PATCH_START = 5;
    /** The enemies of a map of the default size, from the densities per area of the world configuration. */
    private static final int SLIMES_AT_DEFAULT_SIZE = 120;
    private static final int MIMICS_AT_DEFAULT_SIZE = 12;
    private static final int SKELETONS_AT_DEFAULT_SIZE = 24;

    private final GameStateManager state = new GameStateManager();

    private MapManager managerOnFlatMap() {
        return managerOnFlatMap(SIZE);
    }

    private MapManager managerOnFlatMap(int size) {
        final TileMap map = new TileMap(size, size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        state.setCurrentMap(map);
        state.setPlayer(new Player(size / 2, size / 2));
        return new MapManager(state);
    }

    /** A map of deep water with one small walkable patch in a corner, far from the player in the middle. */
    private MapManager managerOnCrowdedMap() {
        final TileMap map = new TileMap(CROWDED_SIZE, CROWDED_SIZE);
        for (int x = 0; x < CROWDED_SIZE; x++) {
            for (int y = 0; y < CROWDED_SIZE; y++) {
                final var inPatch = x >= PATCH_START && x < PATCH_START + PATCH_SIDE
                        && y >= PATCH_START && y < PATCH_START + PATCH_SIDE;
                map.setTile(x, y, new Tile(x, y, inPatch ? TileType.GRASS : TileType.DEEP_WATER));
            }
        }
        state.setCurrentMap(map);
        state.setPlayer(new Player(CROWDED_SIZE / 2, CROWDED_SIZE / 2));
        return new MapManager(state);
    }

    @Test
    void noEnemySpawnsOnATileAnotherEnemyHolds() {
        final MapManager manager = managerOnFlatMap();

        for (int round = 0; round < ROUNDS; round++) {
            manager.spawnEnemies();

            final Set<String> tiles = new HashSet<>();
            for (Enemy enemy : state.getEnemies()) {
                assertTrue(tiles.add(enemy.getCurrentTileX() + "," + enemy.getCurrentTileY()),
                        "two enemies spawned on the tile (" + enemy.getCurrentTileX() + ", " + enemy.getCurrentTileY() + ")");
                assertFalse(enemy.getCurrentTileX() == PLAYER_TILE && enemy.getCurrentTileY() == PLAYER_TILE,
                        "an enemy spawned on the player");
            }
            assertFalse(state.getEnemies().isEmpty(), "some enemies spawn on the map");
        }
    }

    @Test
    void noEnemySpawnsWithinTheStartSafeRadiusOfThePlayer() {
        final MapManager manager = managerOnFlatMap();
        final int safeRadius = AppConfig.getInstance().world().getStartSafeRadius(SIZE, SIZE);

        for (int round = 0; round < ROUNDS; round++) {
            manager.spawnEnemies();

            for (Enemy enemy : state.getEnemies()) {
                final double distance = Math.hypot(enemy.getCurrentTileX() - PLAYER_TILE,
                        enemy.getCurrentTileY() - PLAYER_TILE);
                assertTrue(distance >= safeRadius, "an enemy spawned " + distance + " tiles from the player");
            }
        }
    }

    @Test
    void theNumberOfEnemiesFollowsTheAreaOfTheMap() {
        final MapManager manager = managerOnFlatMap();
        final var world = AppConfig.getInstance().world();

        manager.spawnEnemies();

        final int expected = world.getSlimesCount(SIZE, SIZE) + world.getMimicsCount(SIZE, SIZE)
                + world.getSkeletonsCount(SIZE, SIZE);
        assertEquals(expected, state.getEnemies().size());
        assertTrue(world.getSlimesCount(SIZE, SIZE) <world.getSlimesCount(SIZE * 2, SIZE * 2));
    }

    @Test
    void theDefaultMapStartsWithTheEnemiesOfItsAreaAtTheConfiguredDensity() {
        final var world = AppConfig.getInstance().world();

        assertEquals(SLIMES_AT_DEFAULT_SIZE, world.getSlimesCount(world.getWidth(), world.getHeight()));
        assertEquals(MIMICS_AT_DEFAULT_SIZE, world.getMimicsCount(world.getWidth(), world.getHeight()));
        assertEquals(SKELETONS_AT_DEFAULT_SIZE, world.getSkeletonsCount(world.getWidth(), world.getHeight()));
    }

    @Test
    void aSmallMapStillSpawnsEnemiesOutsideAStartSafeRadiusThatFitsIt() {
        final MapManager manager = managerOnFlatMap(SMALL_SIZE);
        final var world = AppConfig.getInstance().world();
        final int safeRadius = world.getStartSafeRadius(SMALL_SIZE, SMALL_SIZE);

        manager.spawnEnemies();

        final int expected = world.getSlimesCount(SMALL_SIZE, SMALL_SIZE) + world.getMimicsCount(SMALL_SIZE, SMALL_SIZE)
                + world.getSkeletonsCount(SMALL_SIZE, SMALL_SIZE);
        assertTrue(safeRadius < AppConfig.getInstance().world().getStartSafeRadius(SIZE, SIZE));
        assertFalse(state.getEnemies().isEmpty(), "a small map has enemies");
        assertTrue(state.getEnemies().size() <= expected);
        for (Enemy enemy : state.getEnemies()) {
            final double distance = Math.hypot(enemy.getCurrentTileX() - SMALL_SIZE / 2.0,
                    enemy.getCurrentTileY() - SMALL_SIZE / 2.0);
            assertTrue(distance >= safeRadius, "an enemy spawned " + distance + " tiles from the player");
        }
    }

    @Test
    void aCrowdedMapNeverHoldsMoreEnemiesThanFreeTilesAndStillSpawnsSome() {
        final MapManager manager = managerOnCrowdedMap();

        manager.spawnEnemies();

        final var tiles = new HashSet<String>();
        for (Enemy enemy : state.getEnemies()) {
            assertTrue(tiles.add(enemy.getCurrentTileX() + "," + enemy.getCurrentTileY()));
        }
        assertFalse(state.getEnemies().isEmpty(), "some enemies spawn on the patch");
        assertTrue(state.getEnemies().size() <= PATCH_SIDE * PATCH_SIDE);
    }
}
