package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerManagerTest {

    private static final int SIZE = 21;
    private static final double RESPAWN_SECONDS = 5.0;

    private GameStateManager state;
    private PlayerManager manager;

    private static TileMap map(TileType fill) {
        final TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, fill));
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

    @Test
    void withoutAMapNoPlayerIsCreated() {
        onFx(manager::initializePlayer);

        assertNull(state.getPlayer());
    }

    @Test
    void aNewPlayerStandsOnAWalkableTileAndTheCameraIsToCentreOnIt() {
        state.setCurrentMap(map(TileType.GRASS));
        state.setShouldCenterCamera(false);

        onFx(manager::initializePlayer);

        final Player player = state.getPlayer();
        assertNotNull(player);
        assertTrue(state.getCurrentMap().getTile(player.getCurrentTileX(), player.getCurrentTileY()).isWalkable());
        assertTrue(state.shouldCenterCamera());
        assertTrue(state.getPlayerRespawnTimer() <= 0);
    }

    @Test
    void anExistingDeadPlayerIsRevivedInsteadOfReplaced() {
        state.setCurrentMap(map(TileType.GRASS));
        final Player player = new Player(0, 0);
        player.takeDamage(player.getMaxHealth() * 2);
        state.setPlayer(player);
        assertTrue(player.isDead());

        onFx(manager::initializePlayer);

        assertEquals(player, state.getPlayer());
        assertFalse(player.isDead());
    }

    @Test
    void whenNoTileIsSafeThePlayerStartsAtTheCentreOfTheMap() {
        state.setCurrentMap(map(TileType.DEEP_WATER));

        onFx(manager::initializePlayer);

        assertEquals(SIZE / 2, state.getPlayer().getCurrentTileX());
        assertEquals(SIZE / 2, state.getPlayer().getCurrentTileY());
    }

    @Test
    void theRespawnTimerCountsDownAndRespawnsThePlayerWhenItRunsOut() {
        state.setCurrentMap(map(TileType.GRASS));
        final Player player = new Player(0, 0);
        player.takeDamage(player.getMaxHealth() * 2);
        state.setPlayer(player);

        assertFalse(manager.handleRespawn(1.0), "no timer is running");

        state.setPlayerRespawnTimer(RESPAWN_SECONDS);
        final boolean[] respawned = new boolean[2];
        onFx(() -> respawned[0] = manager.handleRespawn(RESPAWN_SECONDS - 1.0));
        assertFalse(respawned[0]);
        assertEquals(1.0, state.getPlayerRespawnTimer(), 1e-9);
        assertTrue(player.isDead());

        onFx(() -> respawned[1] = manager.handleRespawn(1.0));
        assertTrue(respawned[1]);
        assertFalse(player.isDead());
        assertTrue(state.getPlayerRespawnTimer() <= 0);
    }

    @Test
    void respawningNeedsBothAPlayerAndAMap() {
        onFx(manager::respawnPlayer);
        state.setCurrentMap(map(TileType.GRASS));
        onFx(manager::respawnPlayer);

        assertNull(state.getPlayer());
    }

    @Test
    void respawningPutsTheDeadPlayerBackOnTheMapAndClearsTheTimer() {
        state.setCurrentMap(map(TileType.DEEP_WATER));
        final Player player = new Player(0, 0);
        player.takeDamage(player.getMaxHealth() * 2);
        state.setPlayer(player);
        state.setPlayerRespawnTimer(RESPAWN_SECONDS);

        onFx(manager::respawnPlayer);

        assertFalse(player.isDead());
        assertEquals(SIZE / 2, player.getCurrentTileX());
        assertTrue(state.getPlayerRespawnTimer() <= 0);
        assertTrue(state.shouldCenterCamera());
    }

    @Test
    void aDeathStartsTheRespawnTimerAndOnlyOnce() {
        state.setCurrentMap(map(TileType.GRASS));
        final Player player = new Player(3, 3);
        state.setPlayer(player);
        player.takeDamage(player.getMaxHealth() * 2);

        onFx(() -> manager.updatePlayer(0.016));
        assertEquals(RESPAWN_SECONDS, state.getPlayerRespawnTimer(), 1e-9);

        state.setPlayerRespawnTimer(2.0);
        onFx(() -> manager.updatePlayer(0.016));
        assertEquals(2.0, state.getPlayerRespawnTimer(), 1e-9, "the death sequence does not restart the timer");
    }

    @Test
    void updatingWithoutAPlayerDoesNothing() {
        onFx(() -> manager.updatePlayer(0.016));
        onFx(() -> manager.advancePlayer(0.016));
        manager.keepClearOfEnemies();

        assertNull(state.getPlayer());
        assertTrue(state.getPlayerRespawnTimer() <= 0);
    }

    @Test
    void thePlayerWalksToAReachableTileAndNotToAnUnreachableOne() {
        final TileMap grass = map(TileType.GRASS);
        grass.setTile(10, 4, new Tile(10, 4, TileType.DEEP_WATER));
        state.setCurrentMap(grass);
        state.setPlayer(new Player(4, 4));

        assertTrue(manager.movePlayer(grass, 6, 4));
        assertTrue(state.getPlayer().isMoving());
        assertFalse(manager.movePlayer(grass, 10, 4), "water cannot be walked to");
    }

    @Test
    void movingWithoutAPlayerIsRefused() {
        assertFalse(manager.movePlayer(map(TileType.GRASS), 1, 1));
    }

    @Test
    void aTileNextToThePlayerIsOfferedForADrop() {
        state.setCurrentMap(map(TileType.GRASS));

        final int[] spot = manager.findAdjacentDropPosition(10, 10);

        assertEquals(2, spot.length);
        assertNotEquals(0, Math.abs(spot[0] - 10) + Math.abs(spot[1] - 10), "not the player's own tile");
        assertTrue(Math.abs(spot[0] - 10) <= 1 && Math.abs(spot[1] - 10) <= 1);
    }

    @Test
    void withoutAMapTheDropFallsBackToThePlayersOwnTile() {
        assertArrayEquals(new int[]{7, 8}, manager.findAdjacentDropPosition(7, 8));
    }
}
