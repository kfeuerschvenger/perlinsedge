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
    /** The enemies of a default size map, which keep the density the game had on the smaller map it used to have. */
    private static final int SLIMES_AT_DEFAULT_SIZE = 120;
    private static final int MIMICS_AT_DEFAULT_SIZE = 12;
    private static final int SKELETONS_AT_DEFAULT_SIZE = 24;

    private final GameStateManager state = new GameStateManager();

    private MapManager managerOnFlatMap() {
        final TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        state.setCurrentMap(map);
        state.setPlayer(new Player(PLAYER_TILE, PLAYER_TILE));
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
        final int safeRadius = AppConfig.getInstance().world().getStartSafeRadius();

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
        assertTrue(world.getSlimesCount(SIZE, SIZE) < world.getSlimesCount(SIZE * 2, SIZE * 2));
    }

    @Test
    void theDefaultMapStartsWithTheDensityOfTheSmallerMapTheGameHadBefore() {
        final var world = AppConfig.getInstance().world();

        assertEquals(SLIMES_AT_DEFAULT_SIZE, world.getSlimesCount(world.getWidth(), world.getHeight()));
        assertEquals(MIMICS_AT_DEFAULT_SIZE, world.getMimicsCount(world.getWidth(), world.getHeight()));
        assertEquals(SKELETONS_AT_DEFAULT_SIZE, world.getSkeletonsCount(world.getWidth(), world.getHeight()));
    }
}
