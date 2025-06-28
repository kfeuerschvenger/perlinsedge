package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;

import java.util.Iterator;
import java.util.List;

public class ItemDropManager {
    private final GameStateManager gameState;
    private final InventoryPanel inventoryPanel;

    public ItemDropManager(GameStateManager gameState, InventoryPanel inventoryPanel) {
        this.gameState = gameState;
        this.inventoryPanel = inventoryPanel;
    }

    public void checkItemPickup() {
        Player player = gameState.getPlayer();
        if (player == null || player.isDead()) return;

        int playerX = player.getCurrentTileX();
        int playerY = player.getCurrentTileY();
        List<DroppedItem> droppedItems = gameState.getDroppedItems();
        Iterator<DroppedItem> iterator = droppedItems.iterator();

        while (iterator.hasNext()) {
            DroppedItem droppedItem = iterator.next();

            if (!droppedItem.canBePickedUp(playerX, playerY)) {
                continue;
            }

            int dx = Math.abs(playerX - droppedItem.getTileX());
            int dy = Math.abs(playerY - droppedItem.getTileY());

            // Check if player is on the same tile or an adjacent tile
            if (dx <= 1 && dy <= 1) {
                // Add a small delay after dropping to prevent instant re-pickup
                if (gameState.getGameTime() - gameState.getLastDropTime() < 0.5) { // 0.5 seconds cooldown
                    continue;
                }

                // Attempt to pick up the item
                int pickedUp = player.pickUpItem(droppedItem.getItem());
                if (pickedUp > 0) { // If any quantity was picked up
                    if (pickedUp == droppedItem.getItem().getQuantity()) {
                        // If all quantity was picked up, remove the dropped item entirely
                        iterator.remove();
                    } else {
                        // Otherwise, reduce the quantity of the dropped item
                        droppedItem.getItem().setQuantity(droppedItem.getItem().getQuantity() - pickedUp);
                    }
                    // Update the inventory UI to reflect the change
                    inventoryPanel.updateInventoryDisplay(); // Corrected method call
                }
            }
        }
    }

    public void updateDroppedItems(double deltaTime) {
        // TODO: Implement item despawn logic if needed
        // Example: Remove items after a certain amount of time
        // long despawnTime = AppConfig.getInstance().gameMechanics().getItemDespawnTimeSeconds();
        // Iterator<DroppedItem> iterator = gameState.getDroppedItems().iterator();
        // while (iterator.hasNext()) {
        //     DroppedItem droppedItem = iterator.next();
        //     if (gameState.getGameTime() - droppedItem.getDropTime() > despawnTime) {
        //         iterator.remove();
        //         System.out.println("Item " + droppedItem.getItem().getType().getDisplayName() + " despawned.");
        //     }
        // }
    }
}
