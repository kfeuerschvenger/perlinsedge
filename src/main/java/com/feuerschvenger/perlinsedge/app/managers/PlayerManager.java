package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Manages player lifecycle including initialization, movement, health, death, and respawn.
 * Handles inventory management and item dropping on death.
 */
@Slf4j
public class PlayerManager {
    // Configuration constants
    private static final int MIN_ENEMY_DISTANCE = 5; // Minimum tiles from enemies for safe position
    private static final int MIN_WALKABLE_NEIGHBORS = 2; // Required adjacent walkable tiles
    private static final int FALLBACK_SEARCH_RADIUS = 30; // Max radius for fallback position search
    private static final double RESPAWN_TIMER = 5.0; // Player respawn delay in seconds

    // Tile validator predicates
    private static final Predicate<Tile> WALKABLE_CLEAR_VALIDATOR =
            tile -> tile != null && tile.isWalkable();

    // Dependencies
    private final GameStateManager gameState;
    private final HealthBar healthBar;
    private final InventoryPanel inventoryPanel;

    public PlayerManager(GameStateManager gameState, HealthBar healthBar, InventoryPanel inventoryPanel) {
        this.gameState = gameState;
        this.healthBar = healthBar;
        this.inventoryPanel = inventoryPanel;
    }

    /**
     * Handles player respawn logic including timer countdown and actual respawn.
     *
     * @param deltaTime Time since last update in seconds
     * @return true if respawn occurred, false otherwise
     */
    public boolean handleRespawn(double deltaTime) {
        double currentTimer = gameState.getPlayerRespawnTimer();
        if (currentTimer <= 0) return false;

        double newTimer = currentTimer - deltaTime;
        gameState.setPlayerRespawnTimer(newTimer);

        if (newTimer <= 0) {
            respawnPlayer();
            return true;
        }
        return false;
    }

    /**
     * Initializes or reinitializes player at a safe starting position.
     * Creates new player or revives existing one based on game state.
     */
    public void initializePlayer() {
        TileMap map = gameState.getCurrentMap();
        if (map == null) {
            log.warn("Cannot initialize player: Map unavailable");
            return;
        }

        int[] startPosition = findSafePosition(map);
        if (startPosition == null) {
            startPosition = getFallbackPosition(map);
            log.debug("Using fallback position: {},{}", startPosition[0], startPosition[1]);
        }

        Player player = gameState.getPlayer();
        if (player == null) {
            player = new Player(startPosition[0], startPosition[1]);
            gameState.setPlayer(player);
            log.info("New player created at {},{}", startPosition[0], startPosition[1]);
        } else {
            player.revive(startPosition[0], startPosition[1]);
            log.info("Player revived at {},{}", startPosition[0], startPosition[1]);
        }

        // A pending respawn would otherwise teleport the freshly placed, living player when it expires
        gameState.setPlayerRespawnTimer(-1);
        updatePlayerUI();
        gameState.setShouldCenterCamera(true);
    }

    /**
     * Updates player state including health and death/respawn handling.
     * @param deltaTime Time elapsed since last update (in seconds)
     */
    public void updatePlayer(double deltaTime) {
        Player player = getPlayer();
        if (player == null) return;

        player.update(deltaTime);
        updatePlayerUI();

        // Handle player death only if not already processing respawn
        if (player.isDead() && gameState.getPlayerRespawnTimer() <= 0) {
            handlePlayerDeath();
        }
    }

    /**
     * Respawns player at a safe location and resets state.
     */
    public void respawnPlayer() {
        Player player = getPlayer();
        TileMap map = gameState.getCurrentMap();
        if (player == null || map == null) {
            log.warn("Respawn failed: Player or map missing");
            return;
        }

        int[] respawnPosition = findSafePosition(map);
        if (respawnPosition == null) {
            respawnPosition = getFallbackPosition(map);
            log.debug("Using fallback respawn position");
        }

        player.revive(respawnPosition[0], respawnPosition[1]);
        gameState.setPlayerRespawnTimer(-1); // Reset timer
        gameState.setShouldCenterCamera(true);
        updatePlayerUI();
        log.info("Player respawned at {},{}", respawnPosition[0], respawnPosition[1]);
    }

    /**
     * Moves player to target tile coordinates.
     * @param map Current game map
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     */
    public void movePlayer(TileMap map, int targetX, int targetY) {
        Player player = getPlayer();
        if (player != null) {
            player.moveTo(map, targetX, targetY);
        }
    }

    /**
     * Handles player death sequence:
     * 1. Updates health bar
     * 2. Drops inventory items
     * 3. Clears inventory
     * 4. Starts respawn timer
     */
    private void handlePlayerDeath() {
        healthBar.updateHealth(0, getPlayer().getMaxHealth());
        dropPlayerItems();
        getPlayer().clearInventory();
        updatePlayerUI();
        gameState.setPlayerRespawnTimer(RESPAWN_TIMER);
        log.info("Death sequence initiated");
    }

    /**
     * Drops all player inventory items into the game world.
     */
    private void dropPlayerItems() {
        Player player = getPlayer();
        if (player == null) return;

        List<Item> inventory = player.getInventory();
        log.debug("Dropping {} items", inventory.size());

        for (Item item : inventory) {
            if (item != null && item.getQuantity() > 0) {
                int[] dropPos = findAdjacentDropPosition(player.getCurrentTileX(), player.getCurrentTileY());
                gameState.getDroppedItems().add(
                        new DroppedItem(
                                dropPos[0], dropPos[1], item,
                                gameState.getGameTime(),
                                DroppedItem.DropSource.PLAYER
                        )
                );
                log.debug("Dropped {} at {},{}", item.getType().name(), dropPos[0], dropPos[1]);
            }
        }
    }

    /**
     * Finds a safe position for player spawn/respawn.
     * Ensures walkable tile with no resources/buildings and safe distance from enemies.
     */
    private int[] findSafePosition(TileMap map) {
        Random random = new Random();
        int maxAttempts = AppConfig.getInstance().world().getMaxPositioningAttempts();
        log.debug("Searching safe position (max attempts: {})", maxAttempts);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = random.nextInt(map.getWidth());
            int y = random.nextInt(map.getHeight());

            if (isValidRespawnPosition(map, x, y)) {
                return new int[]{x, y};
            }
        }
        log.debug("Safe position not found after {} attempts", maxAttempts);
        return null;
    }

    /**
     * Finds fallback position around map center when no safe position is found.
     */
    private int[] getFallbackPosition(TileMap map) {
        int centerX = map.getWidth() / 2;
        int centerY = map.getHeight() / 2;
        log.debug("Searching fallback position around center");

        for (int radius = 0; radius < FALLBACK_SEARCH_RADIUS; radius++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int y = centerY - radius; y <= centerY + radius; y++) {
                    // Check perimeter only for efficiency
                    if (isPerimeterPosition(centerX, centerY, radius, x, y) &&
                            isValidRespawnPosition(map, x, y)) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        log.debug("Fallback search failed, using map center");
        return new int[]{centerX, centerY};
    }

    /**
     * Finds adjacent position for item dropping.
     * @return Valid drop position or player position if none found
     */
    public int[] findAdjacentDropPosition(int playerX, int playerY) {
        TileMap map = gameState.getCurrentMap();
        if (map == null) {
            log.warn("Drop position fallback: Map unavailable");
            return new int[]{playerX, playerY};
        }

        return IsometricUtils.findAdjacentPosition(
                playerX, playerY,
                map,
                WALKABLE_CLEAR_VALIDATOR,
                IsometricUtils.ALL_DIRECTIONS
        );
    }

    /**
     * Validates position for player respawn:
     * - Within map bounds
     * - Walkable tile
     * - No resources/buildings
     * - Safe distance from enemies
     * - Sufficient walkable neighbors
     */
    private boolean isValidRespawnPosition(TileMap map, int x, int y) {
        if (!isWithinMapBounds(x, y, map)) return false;

        Tile tile = map.getTile(x, y);
        if (!WALKABLE_CLEAR_VALIDATOR.test(tile)) {
            return false;
        }

        if (isEnemyTooClose(x, y)) return false;
        return !isPositionSurrounded(x, y, map);
    }

    /**
     * Checks if position has insufficient walkable neighbors.
     */
    private boolean isPositionSurrounded(int x, int y, TileMap map) {
        return IsometricUtils.isPositionSurrounded(
                x, y, map,
                WALKABLE_CLEAR_VALIDATOR,
                MIN_WALKABLE_NEIGHBORS,
                IsometricUtils.CARDINAL_DIRECTIONS
        );
    }

    /**
     * Checks if enemies are within minimum safe distance.
     */
    private boolean isEnemyTooClose(int x, int y) {
        if (gameState.getEnemies() == null) return false;

        return gameState.getEnemies().stream().anyMatch(enemy ->
                Math.abs(enemy.getCurrentTileX() - x) < MIN_ENEMY_DISTANCE &&
                        Math.abs(enemy.getCurrentTileY() - y) < MIN_ENEMY_DISTANCE
        );
    }

    /**
     * Checks if coordinates are within map boundaries.
     */
    private boolean isWithinMapBounds(int x, int y, TileMap map) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }

    /**
     * Checks if position is on the perimeter of a search radius.
     */
    private boolean isPerimeterPosition(int centerX, int centerY, int radius, int x, int y) {
        return x == centerX - radius || x == centerX + radius ||
                y == centerY - radius || y == centerY + radius;
    }

    /**
     * Retrieves player instance, null before a game has started.
     * A missing player is a normal state for the per-frame update, so it is not logged.
     */
    private Player getPlayer() {
        return gameState.getPlayer();
    }

    /**
     * Updates all player-related UI components.
     */
    private void updatePlayerUI() {
        Player player = getPlayer();
        if (player != null) {
            healthBar.updateHealth(player.getHealth(), player.getMaxHealth());
            inventoryPanel.updateInventoryDisplay();
        }
    }


}