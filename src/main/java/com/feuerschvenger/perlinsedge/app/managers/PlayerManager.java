package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.pathfinding.OccupiedTiles;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.utils.DamageNumbers;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.Set;
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
    private static final int CENTER_DIVISOR = 2; // Divides a map span to find its centre
    private static final double RESPAWN_TIMER = 5.0; // Player respawn delay in seconds

    /** Stands for "no position found", so a search never returns null. */
    private static final int[] NO_POSITION = new int[0];

    // Tile validator predicates
    private static final Predicate<Tile> WALKABLE_CLEAR_VALIDATOR =
            tile -> tile != null && tile.isWalkable();

    // Dependencies
    private final GameStateManager gameState;
    private final HealthBar healthBar;
    private final InventoryPanel inventoryPanel;
    /** Draws the candidate tiles of the spawn and respawn search. */
    private final Random random = new Random();

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
        if (currentTimer <= 0) {
            return false;
        }

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
        if (startPosition.length == 0) {
            startPosition = getFallbackPosition(map);
            log.debug("Using fallback position: {},{}", startPosition[0], startPosition[1]);
        }

        var player = gameState.getPlayer();
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
        var player = getPlayer();
        if (player == null) {
            return;
        }

        advancePlayer(deltaTime);
        // Every hit the player took since the last tick becomes one floating number over the player
        gameState.getDamageNumbers().spawn(DamageNumbers.Kind.PLAYER, player.drainDamageTaken(),
                player.getPosX(), player.getPosY(), gameState.getGameTime());
        updatePlayerUI();

        // Handle player death only if not already processing respawn
        if (player.isDead() && gameState.getPlayerRespawnTimer() <= 0) {
            handlePlayerDeath();
        }
    }

    /**
     * Moves the player one tick while keeping it clear of the enemies. The walk is checked before the update, for an
     * enemy that moved onto it, and again right after, because arriving on a node makes the next node the target at
     * once: without the second check that node could be a tile an enemy holds for the rest of the tick.
     *
     * @param deltaTime Time since last update in seconds
     */
    void advancePlayer(double deltaTime) {
        final var player = getPlayer();
        if (player == null) {
            return;
        }
        keepClearOfEnemies();
        player.update(deltaTime);
        keepClearOfEnemies();
    }

    /**
     * Ends the player's walk before it steps onto a tile an enemy has moved onto or toward since the walk was
     * planned, or onto a tile that is no longer walkable, such as one where a solid piece was built after the walk was
     * planned. It does nothing while the player is not walking.
     */
    public void keepClearOfEnemies() {
        final var player = getPlayer();
        if (player == null || !player.isMoving()) {
            return;
        }
        // The check runs every tick, so the set of taken tiles is only built on the rare tick that halts the walk
        final PathNode next = player.getNextTile();
        if (next == null) {
            return;
        }
        if (OccupiedTiles.contains(gameState.getEnemies(), next.x, next.y)) {
            player.haltBefore(OccupiedTiles.of(gameState.getEnemies()));
        } else if (isBlockedTerrain(next)) {
            player.haltBefore(Set.of(next));
        }
    }

    /** Tells whether a tile of the map has become impossible to walk on since the walk toward it was planned. */
    private boolean isBlockedTerrain(PathNode node) {
        final TileMap map = gameState.getCurrentMap();
        final Tile tile = map != null ? map.getTile(node.x, node.y) : null;
        return tile != null && !tile.isWalkable();
    }

    /**
     * Respawns player at a safe location and resets state.
     */
    public void respawnPlayer() {
        var player = getPlayer();
        var map = gameState.getCurrentMap();
        if (player == null || map == null) {
            log.warn("Respawn failed: Player or map missing");
            return;
        }

        int[] respawnPosition = findSafePosition(map);
        if (respawnPosition.length == 0) {
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
     * @return true when the player started walking, false when the order did nothing, which includes an order onto a
     *         tile an enemy occupies: the walk goes around enemies and never ends on one
     */
    public boolean movePlayer(TileMap map, int targetX, int targetY) {
        var player = getPlayer();
        return player != null
                && player.moveTo(map, targetX, targetY, OccupiedTiles.of(gameState.getEnemies()));
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
        var player = getPlayer();
        if (player == null) {
            return;
        }

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
     *
     * @return the position, or {@link #NO_POSITION} when none was found within the attempts
     */
    private int[] findSafePosition(TileMap map) {
        int maxAttempts = AppConfig.getInstance().world().getMaxPositioningAttempts();
        log.debug("Searching safe position (max attempts: {})", maxAttempts);

        for (var attempt = 0; attempt < maxAttempts; attempt++) {
            var x = random.nextInt(map.getWidth());
            var y = random.nextInt(map.getHeight());

            if (isValidRespawnPosition(map, x, y)) {
                return new int[]{x, y};
            }
        }
        log.debug("Safe position not found after {} attempts", maxAttempts);
        return NO_POSITION;
    }

    /**
     * Finds fallback position around map center when no safe position is found.
     */
    private int[] getFallbackPosition(TileMap map) {
        int centerX = map.getWidth() / CENTER_DIVISOR;
        int centerY = map.getHeight() / CENTER_DIVISOR;
        log.debug("Searching fallback position around center");

        for (var radius = 0; radius < FALLBACK_SEARCH_RADIUS; radius++) {
            var found = searchPerimeter(map, centerX, centerY, radius);
            if (found != NO_POSITION) {
                return found;
            }
        }
        log.debug("Fallback search failed, using map center");
        return new int[]{centerX, centerY};
    }

    /**
     * Searches the square ring at the given radius around the center for a valid respawn position.
     * Only the perimeter is checked, because the inner rings were covered by the smaller radii.
     *
     * @return the first valid position, or {@link #NO_POSITION} when the ring has none
     */
    private int[] searchPerimeter(TileMap map, int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isPerimeterPosition(centerX, centerY, radius, x, y) && isValidRespawnPosition(map, x, y)) {
                    return new int[]{x, y};
                }
            }
        }
        return NO_POSITION;
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
                IsometricUtils.allDirections()
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
        if (!isWithinMapBounds(x, y, map)) {
            return false;
        }

        var tile = map.getTile(x, y);
        if (!WALKABLE_CLEAR_VALIDATOR.test(tile)) {
            return false;
        }

        if (isEnemyTooClose(x, y)) {
            return false;
        }
        return !isPositionSurrounded(x, y, map);
    }

    /**
     * Checks if position has insufficient walkable neighbors.
     */
    private static boolean isPositionSurrounded(int x, int y, TileMap map) {
        return IsometricUtils.isPositionSurrounded(
                x, y, map,
                WALKABLE_CLEAR_VALIDATOR,
                MIN_WALKABLE_NEIGHBORS,
                IsometricUtils.cardinalDirections()
        );
    }

    /**
     * Checks if enemies are within minimum safe distance.
     */
    private boolean isEnemyTooClose(int x, int y) {
        if (gameState.getEnemies() == null) {
            return false;
        }

        return gameState.getEnemies().stream().anyMatch(enemy ->
                Math.abs(enemy.getCurrentTileX() - x) < MIN_ENEMY_DISTANCE &&
                        Math.abs(enemy.getCurrentTileY() - y) < MIN_ENEMY_DISTANCE
        );
    }

    /**
     * Checks if coordinates are within map boundaries.
     */
    private static boolean isWithinMapBounds(int x, int y, TileMap map) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }

    /**
     * Checks if position is on the perimeter of a search radius.
     */
    private static boolean isPerimeterPosition(int centerX, int centerY, int radius, int x, int y) {
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
        var player = getPlayer();
        if (player != null) {
            healthBar.updateHealth(player.getHealth(), player.getMaxHealth());
            inventoryPanel.updateInventoryDisplay();
        }
    }


}
