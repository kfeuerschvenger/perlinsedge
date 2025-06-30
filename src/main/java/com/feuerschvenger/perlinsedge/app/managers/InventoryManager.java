package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;

/**
 * Manages player inventory operations including crafting and UI synchronization.
 * Handles recipe validation, resource consumption, and inventory updates.
 */
public class InventoryManager {
    // Dependencies
    private final GameStateManager gameState;
    private final InventoryPanel inventoryPanel;

    /**
     * Initializes inventory manager with required dependencies.
     *
     * @param gameState Provides access to player state
     * @param inventoryPanel UI component to update after operations
     */
    public InventoryManager(GameStateManager gameState, InventoryPanel inventoryPanel) {
        this.gameState = gameState;
        this.inventoryPanel = inventoryPanel;
    }

    /**
     * Attempts to craft an item based on the specified recipe.
     * Validates resources, consumes materials, and updates UI on success.
     *
     * @param recipe Recipe to craft (must not be null)
     */
    public void handleCraftRecipe(Recipe recipe) {
        if (recipe == null) {
            logError("Cannot craft: Recipe is null");
            return;
        }

        Player player = getPlayer();
        if (player == null) return;

        if (!canCraft(player, recipe)) {
            logCraftFailure(recipe);
            return;
        }

        executeCrafting(player, recipe);
        updateInventoryDisplay();
    }

    /**
     * Removes a specific item instance from the player's inventory.
     *
     * @param item The exact item instance to remove (by reference equality)
     */
    public void removeItemFromPlayerInventory(Item item) {
        Player player = getPlayer();
        if (player == null) return;

        for (int i = 0; i < player.getCapacity(); i++) {
            if (player.getItemAt(i) == item) { // Reference comparison
                player.setItemAt(i, null);
                updateInventoryDisplay();
                return;
            }
        }
    }

    /**
     * Checks if player can craft the recipe.
     * @return true if player has sufficient resources, false otherwise
     */
    private boolean canCraft(Player player, Recipe recipe) {
        return player.canCraft(recipe);
    }

    /**
     * Executes the crafting process:
     * 1. Consumes required resources
     * 2. Adds crafted item to inventory
     */
    private void executeCrafting(Player player, Recipe recipe) {
        player.craftItem(recipe);
        logCraftSuccess(recipe);
    }

    /**
     * Refreshes the inventory UI display.
     */
    private void updateInventoryDisplay() {
        inventoryPanel.updateInventoryDisplay();
    }

    /**
     * Retrieves the current player instance.
     * @return Player instance or null if unavailable
     */
    private Player getPlayer() {
        Player player = gameState.getPlayer();
        if (player == null) {
            logError("Crafting failed: Player is null");
        }
        return player;
    }

    /**
     * Logs a successful crafting operation.
     */
    private void logCraftSuccess(Recipe recipe) {
        System.out.println("Crafted: " + recipe.getName());
    }

    /**
     * Logs a failed crafting attempt.
     */
    private void logCraftFailure(Recipe recipe) {
        System.out.println("Craft failed: Insufficient materials for " + recipe.getName());
    }

    /**
     * Logs an error message.
     */
    private void logError(String message) {
        System.err.println("InventoryManager: " + message);
    }

}