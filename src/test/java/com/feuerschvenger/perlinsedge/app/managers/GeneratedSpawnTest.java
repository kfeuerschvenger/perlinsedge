package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.world.model.SpawnPoint;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The player starts on the spawn point the generator chose, and comes back to it after dying unless an enemy stands
 * close to it.
 */
class GeneratedSpawnTest {

    private static final int SIZE = 41;
    private static final int SPAWN_X = 7;
    private static final int SPAWN_Y = 9;
    private static final int NEAR_ENEMY_OFFSET = 4;
    private static final int FAR_ENEMY_OFFSET = 5;
    private static final int SAFE_DISTANCE = 5;
    private static final int RESPAWNS = 25;

    private GameStateManager state;
    private PlayerManager manager;

    private static TileMap grassMap(SpawnPoint spawn) {
        final TileMap map = new TileMap(SIZE, SIZE, spawn);
        for (var x = 0; x < SIZE; x++) {
            for (var y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        manager = FxToolkitSupport.onFx(() -> new PlayerManager(state, new HealthBar(), new InventoryPanel(state)));
    }

    private void onFx(Runnable action) {
        FxToolkitSupport.onFx(() -> {
            action.run();
            return null;
        });
    }

    private void placeEnemyAt(int x, int y) {
        final List<Enemy> enemies = new ArrayList<>();
        enemies.add(new Slime(x, y));
        state.setCreatures(enemies);
    }

    @Test
    void theNewGamePlayerStandsOnTheGeneratedSpawn() {
        state.setCurrentMap(grassMap(new SpawnPoint(SPAWN_X, SPAWN_Y)));

        onFx(manager::initializePlayer);

        assertEquals(SPAWN_X, state.getPlayer().getCurrentTileX());
        assertEquals(SPAWN_Y, state.getPlayer().getCurrentTileY());
    }

    @Test
    void withoutAnEnemyNearbyTheRespawnReturnsToTheGeneratedSpawn() {
        state.setCurrentMap(grassMap(new SpawnPoint(SPAWN_X, SPAWN_Y)));
        onFx(manager::initializePlayer);
        placeEnemyAt(SPAWN_X + FAR_ENEMY_OFFSET + SAFE_DISTANCE, SPAWN_Y);

        for (var i = 0; i < RESPAWNS; i++) {
            onFx(manager::respawnPlayer);

            assertEquals(SPAWN_X, state.getPlayer().getCurrentTileX());
            assertEquals(SPAWN_Y, state.getPlayer().getCurrentTileY());
        }
    }

    @Test
    void anEnemyWithinFiveTilesSendsTheRespawnToARandomSafeTile() {
        state.setCurrentMap(grassMap(new SpawnPoint(SPAWN_X, SPAWN_Y)));
        onFx(manager::initializePlayer);
        placeEnemyAt(SPAWN_X + NEAR_ENEMY_OFFSET, SPAWN_Y);

        onFx(manager::respawnPlayer);

        final var player = state.getPlayer();
        final var tooClose = Math.abs(player.getCurrentTileX() - (SPAWN_X + NEAR_ENEMY_OFFSET)) < SAFE_DISTANCE
                && Math.abs(player.getCurrentTileY() - SPAWN_Y) < SAFE_DISTANCE;
        assertFalse(tooClose, "the player did not land next to the enemy");
        assertNotEquals(List.of(SPAWN_X, SPAWN_Y), List.of(player.getCurrentTileX(), player.getCurrentTileY()));
    }

    @Test
    void aMapWithoutASpawnStillRespawnsOnAWalkableTile() {
        state.setCurrentMap(grassMap(null));
        onFx(manager::initializePlayer);

        onFx(manager::respawnPlayer);

        final var player = state.getPlayer();
        assertTrue(state.getCurrentMap().getTile(player.getCurrentTileX(), player.getCurrentTileY()).isWalkable());
    }
}
