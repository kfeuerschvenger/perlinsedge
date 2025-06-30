package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Manages dropped items in the game world, handling pickup mechanics, despawning, and item creation.
 */
public class ItemDropManager {
    // Configuration constants
    private static final double PICKUP_COOLDOWN_SECONDS = 0.5; // Prevents instant re-pickup
    private static final double DESPAWN_TIME_SECONDS = 300; // 5 minutes

    // Tile validator for drop positions
    private static final Predicate<Tile> DROP_VALIDATOR =
            tile -> tile != null && tile.isWalkable();

    // Dependencies
    private final GameStateManager gameState;
    private final InventoryPanel inventoryPanel;

    public ItemDropManager(GameStateManager gameState, InventoryPanel inventoryPanel) {
        this.gameState = gameState;
        this.inventoryPanel = inventoryPanel;
    }

    /**
     * Creates a dropped item at a position adjacent to the player.
     *
     * @param item The item to drop
     * @param player The player dropping the item
     */
    public void createDroppedItem(Item item, Player player) {
        // Create a copy of the item to avoid reference issues
        Item itemCopy = new Item(item.getType(), item.getQuantity());

        int[] dropPos = IsometricUtils.findAdjacentPosition(
                player.getCurrentTileX(),
                player.getCurrentTileY(),
                gameState.getCurrentMap(),
                DROP_VALIDATOR,
                IsometricUtils.ALL_DIRECTIONS
        );

        DroppedItem dropped = new DroppedItem(
                dropPos[0], dropPos[1],
                itemCopy,
                gameState.getGameTime(),
                DroppedItem.DropSource.PLAYER
        );

        gameState.getDroppedItems().add(dropped);
        gameState.setLastDropTime(gameState.getGameTime());
        System.out.printf("Dropped item: %s at (%d, %d)%n",
                item.getType().getDisplayName(), dropPos[0], dropPos[1]);
    }

    /**
     * Updates all dropped item operations: despawning and pickup checks.
     */
    public void update() {
        updateDespawning();
        checkItemPickup();
    }

    /**
     * Updates despawning logic for stale items.
     */
    private void updateDespawning() {
        double currentTime = gameState.getGameTime();
        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        Iterator<DroppedItem> iterator = droppedItems.iterator();

        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();

            if (shouldDespawn(item, currentTime)) {
                iterator.remove();
                logDespawn(item);
            }
        }
    }

    /**
     * Checks for and processes item pickups near the player.
     */
    private void checkItemPickup() {
        Player player = getPlayer();
        if (player == null || player.isDead()) return;

        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        Iterator<DroppedItem> iterator = droppedItems.iterator();
        int playerX = player.getCurrentTileX();
        int playerY = player.getCurrentTileY();

        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();

            if (!isPickupValid(playerX, playerY, item)) continue;

            processItemPickup(player, iterator, item);
        }
    }

    /**
     * Validates if an item can be picked up:
     * 1. Within pickup range (adjacent tiles)
     * 2. Past the pickup cooldown period
     */
    private boolean isPickupValid(int playerX, int playerY, DroppedItem item) {
        if (!item.canBePickedUp(playerX, playerY)) return false;

        return (gameState.getGameTime() - gameState.getLastDropTime()) >= PICKUP_COOLDOWN_SECONDS;
    }

    /**
     * Attempts to pick up an item and updates game state accordingly.
     */
    private void processItemPickup(Player player, Iterator<DroppedItem> iterator, DroppedItem item) {
        int quantityPickedUp = player.pickUpItem(item.getItem());

        if (quantityPickedUp <= 0) return;

        if (item.getItem().getQuantity() <= 0) {
            iterator.remove(); // Removes item if fully picked up
        }

        inventoryPanel.updateInventoryDisplay();
        logPickup(item.getItem(), quantityPickedUp);
    }

    /**
     * Checks if an item should be despawned based on drop time.
     */
    private boolean shouldDespawn(DroppedItem item, double currentTime) {
        return (currentTime - item.getDropTime()) > DESPAWN_TIME_SECONDS;
    }

    /**
     * Retrieves the player instance if valid.
     */
    private Player getPlayer() {
        Player player = gameState.getPlayer();
        if (player == null) {
            System.err.println("ItemDropManager: Player is null");
        }
        return player;
    }

    /**
     * Logs successful item pickup.
     */
    private void logPickup(Item item, int quantity) {
        System.out.printf("Picked up %dx %s%n", quantity, item.getType().getDisplayName());
    }

    /**
     * Logs item despawn event.
     */
    private void logDespawn(DroppedItem item) {
        System.out.printf("Despawned %s (dropped %.1f minutes ago)%n",
                item.getItem().getType().getDisplayName(),
                (gameState.getGameTime() - item.getDropTime()) / 60);
    }

}