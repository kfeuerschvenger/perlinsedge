package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Enemies spawn on tiles of their own and never on the player, however small the map. */
class EnemySpawnTest {

    /** A flat map small enough for the enemies to crowd it. */
    private static final int SIZE = 16;
    private static final int PLAYER_TILE = SIZE / 2;

    @Test
    void noEnemySpawnsOnATileAnotherEnemyHolds() {
        final GameStateManager state = new GameStateManager();
        final TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        state.setCurrentMap(map);
        state.setPlayer(new Player(PLAYER_TILE, PLAYER_TILE));
        final MapManager manager = new MapManager(state);

        for (int round = 0; round < 20; round++) {
            manager.spawnEnemies();

            final Set<String> tiles = new HashSet<>();
            for (Enemy enemy : state.getEnemies()) {
                assertTrue(tiles.add(enemy.getCurrentTileX() + "," + enemy.getCurrentTileY()),
                        "two enemies spawned on the tile (" + enemy.getCurrentTileX() + ", " + enemy.getCurrentTileY() + ")");
                assertFalse(enemy.getCurrentTileX() == PLAYER_TILE && enemy.getCurrentTileY() == PLAYER_TILE,
                        "an enemy spawned on the player");
            }
            assertFalse(state.getEnemies().isEmpty(), "some enemies spawn on the small map");
            assertTrue(state.getEnemies().size() < SIZE * SIZE, "never more enemies than tiles");
        }
    }
}
