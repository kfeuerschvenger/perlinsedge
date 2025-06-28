package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerManager {
    private final GameStateManager gameState;
    private final HealthBar healthBar;
    private final InventoryPanel inventoryPanel;

    public PlayerManager(GameStateManager gameState, HealthBar healthBar, InventoryPanel inventoryPanel) {
        this.gameState = gameState;
        this.healthBar = healthBar;
        this.inventoryPanel = inventoryPanel;
    }

    /**
     * Initializes or re-initializes the player at a safe starting position on the current map.
     * This should be called when starting a new game.
     */
    public void initializePlayer() {
        TileMap map = gameState.getCurrentMap();
        if (map == null) {
            System.err.println("PlayerManager: Error: No map available to initialize player.");
            return;
        }

        int[] startPosition = findSafePosition(map); // Finds a safe random position
        if (startPosition == null) {
            // If no safe position is found, use the center of the map as fallback
            startPosition = new int[]{map.getWidth() / 2, map.getHeight() / 2};
            System.out.println("PlayerManager: No safe position found, using map center: " + startPosition[0] + "," + startPosition[1]);
        }

        // If no player exists, create a new one
        if (gameState.getPlayer() == null) {
            gameState.setPlayer(new Player(startPosition[0], startPosition[1]));
            System.out.println("PlayerManager: New player created in: " + startPosition[0] + "," + startPosition[1]);
        } else {
            // If player already exists (e.g., restarting), revive and move to new position
            gameState.getPlayer().revive(startPosition[0], startPosition[1]);
            System.out.println("PlayerManager: Existing player revived and moved to: " + startPosition[0] + "," + startPosition[1]);
        }
        // Ensure UI elements are updated
        inventoryPanel.updateInventoryDisplay();
        healthBar.updateHealth(gameState.getPlayer().getHealth(), gameState.getPlayer().getMaxHealth());
        gameState.setShouldCenterCamera(true); // To center the camera on the player at start
    }


    /**
     * Updates the player's state, including health, death, and respawn timer.
     * @param deltaTime The time elapsed since the last update.
     */
    public void updatePlayer(double deltaTime) {
        Player player = gameState.getPlayer();
        if (player == null) return;

        player.update(deltaTime);
        healthBar.updateHealth(player.getHealth(), player.getMaxHealth());
        inventoryPanel.updateInventoryDisplay();

        if (player.isDead() && gameState.getPlayerRespawnTimer() <= 0) {
            System.out.println("PlayerManager: Initiating death sequence.");
            healthBar.updateHealth(0, player.getMaxHealth()); // Ensure health bar is visually zero
            dropPlayerItems(); // Drop player's items
            player.clearInventory(); // Clear inventory
            inventoryPanel.updateInventoryDisplay(); // Update inventory UI
            gameState.setPlayerRespawnTimer(5.0); // Start the respawn timer
            System.out.println("PlayerManager: Respawn timer set to 5.0 seconds from updatePlayer.");
        }
    }

    public void movePlayer(TileMap map, int targetX, int targetY) {
        Player player = gameState.getPlayer();
        if (player == null) return;
        player.moveTo(map, targetX, targetY);
    }

    /**
     * Respawns the player at a safe location on the map.
     */
    public void respawnPlayer() {
        Player player = gameState.getPlayer();
        TileMap map = gameState.getCurrentMap();
        if (player == null || map == null) {
            System.err.println("PlayerManager: Cannot respawn player - player or map is null.");
            return;
        }

        System.out.println("PlayerManager: Attempting to respawn player...");
        // Find safe position
        int[] respawnPosition = findSafePosition(map);
        if (respawnPosition == null) {
            respawnPosition = findFallbackPosition(map);
            System.out.println("PlayerManager: No ideal safe position found, using fallback: " + respawnPosition[0] + "," + respawnPosition[1]);
        } else {
            System.out.println("PlayerManager: Found safe respawn position: " + respawnPosition[0] + "," + respawnPosition[1]);
        }

        // Revive player (this should reset isDead flag and health)
        player.revive(respawnPosition[0], respawnPosition[1]);
        gameState.setPlayerRespawnTimer(-1); // Reset timer to indicate not in respawn state
        gameState.setShouldCenterCamera(true); // Request camera to center on new position
        healthBar.updateHealth(player.getHealth(), player.getMaxHealth()); // Update health bar on respawn
        inventoryPanel.updateInventoryDisplay();
        System.out.println("PlayerManager: Player respawned successfully at " + player.getCurrentTileX() + "," + player.getCurrentTileY());
    }

    private void dropPlayerItems() {
        Player player = gameState.getPlayer();
        if (player == null) return;

        List<Item> itemsToDrop = new ArrayList<>(player.getInventory());
        int playerX = player.getCurrentTileX();
        int playerY = player.getCurrentTileY();

        System.out.println("PlayerManager: Dropping " + itemsToDrop.size() + " items from player inventory.");
        for (Item item : itemsToDrop) {
            if (item != null && item.getQuantity() > 0) {
                // Simplified call to findAdjacentDropPosition as it now uses gameState internally
                int[] dropPos = findAdjacentDropPosition(playerX, playerY);
                DroppedItem droppedItem = new DroppedItem(dropPos[0], dropPos[1], item, gameState.getGameTime(), DroppedItem.DropSource.PLAYER);
                gameState.getDroppedItems().add(droppedItem);
                System.out.println("PlayerManager: Dropped item " + item.getType().name() + " at " + dropPos[0] + "," + dropPos[1]);
            }
        }
    }

    /**
     * Finds an adjacent walkable tile for dropping items.
     * This method assumes it can access the current map via GameStateManager.
     * @param playerX Player's X coordinate.
     * @param playerY Player's Y coordinate.
     * @return An array containing [x, y] of a suitable drop position.
     */
    public int[] findAdjacentDropPosition(int playerX, int playerY) { // Removed TileMap parameter
        TileMap map = gameState.getCurrentMap(); // Get map from GameStateManager
        if (map == null) {
            System.err.println("PlayerManager: Cannot find drop position, map is null. Dropping at player's location.");
            return new int[]{playerX, playerY}; // Fallback if no map
        }

        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] dir : directions) {
            int x = playerX + dir[0];
            int y = playerY + dir[1];
            if (isWithinMapBounds(x, y, map)) {
                Tile tile = map.getTile(x, y);
                if (tile != null && tile.getType().isWalkable() && !tile.hasResource() && !tile.hasBuilding()) {
                    return new int[]{x, y};
                }
            }
        }
        System.out.println("PlayerManager: No adjacent safe drop position found, dropping at player's location.");
        return new int[]{playerX, playerY}; // Fallback to player's position if no adjacent found
    }

    private int[] findSafePosition(TileMap map) {
        Random random = new Random();
        int attempts = 0;
        int maxAttempts = AppConfig.getInstance().world().getMaxPositioningAttempts();
        System.out.println("PlayerManager: Searching for safe respawn position (max attempts: " + maxAttempts + ")");

        while (attempts < maxAttempts) {
            int x = random.nextInt(map.getWidth());
            int y = random.nextInt(map.getHeight());
            Tile tile = map.getTile(x, y);

            if (tile == null || !tile.getType().isWalkable() || tile.hasResource() || tile.hasBuilding()) { // Check for building
                attempts++;
                continue;
            }
            if (gameState.getEnemies() != null) {
                boolean enemyTooClose = gameState.getEnemies().stream().anyMatch(enemy ->
                        Math.abs(enemy.getCurrentTileX() - x) < 5 && Math.abs(enemy.getCurrentTileY() - y) < 5
                );
                if (enemyTooClose) {
                    attempts++;
                    continue;
                }
            }
            if (isPositionSurrounded(x, y, map)) {
                attempts++;
                continue;
            }
            return new int[]{x, y};
        }
        System.out.println("PlayerManager: Failed to find safe position after " + maxAttempts + " attempts.");
        return null;
    }

    private int[] findFallbackPosition(TileMap map) {
        int centerX = map.getWidth() / 2;
        int centerY = map.getHeight() / 2;
        System.out.println("PlayerManager: Searching for fallback respawn position around map center.");

        for (int radius = 0; radius < 30; radius++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int y = centerY - radius; y <= centerY + radius; y++) {
                    // Check only the perimeter of the current square/diamond
                    if (x == centerX - radius || x == centerX + radius ||
                            y == centerY - radius || y == centerY + radius) {

                        if (isValidRespawnPosition(map, x, y)) {
                            System.out.println("PlayerManager: Found fallback position at " + x + "," + y);
                            return new int[]{x, y};
                        }
                    }
                }
            }
        }
        System.out.println("PlayerManager: Fallback search failed, returning map center as last resort.");
        return new int[]{centerX, centerY};
    }

    private boolean isValidRespawnPosition(TileMap map, int x, int y) {
        if (!isWithinMapBounds(x, y, map)) return false;

        Tile tile = map.getTile(x, y);
        // Ensure no resource or building on respawn tile
        return tile != null && tile.getType().isWalkable() && !tile.hasResource() && !tile.hasBuilding();
    }

    private boolean isWithinMapBounds(int x, int y, TileMap map) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }

    private boolean isPositionSurrounded(int x, int y, TileMap map) {
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        int walkableNeighbors = 0;

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (isWithinMapBounds(nx, ny, map)) {
                Tile neighbor = map.getTile(nx, ny);
                // Consider tiles with resources or buildings as non-walkable for surrounding check
                if (neighbor != null && neighbor.getType().isWalkable() && !neighbor.hasResource() && !neighbor.hasBuilding()) {
                    walkableNeighbors++;
                }
            }
        }
        return walkableNeighbors < 2; // Needs at least 2 walkable, non-resource, non-building neighbors
    }

}