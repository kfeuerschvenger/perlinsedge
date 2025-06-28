package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;

public class InventoryManager {
    private final GameStateManager gameState;
    private final InventoryPanel inventoryPanel;

    public InventoryManager(GameStateManager gameState, InventoryPanel inventoryPanel) {
        this.gameState = gameState;
        this.inventoryPanel = inventoryPanel;
    }

    /**
     * Handles the crafting of a given recipe.
     * Checks if the player has the necessary materials and performs the crafting.
     *
     * @param recipe The recipe to craft.
     */
    public void handleCraftRecipe(Recipe recipe) {
        Player player = gameState.getPlayer();
        if (player == null) return;

        if (!player.canCraft(recipe)) {
            System.out.println("Not enough materials to craft: " + recipe.getName());
            return;
        }
        player.craftItem(recipe);
        inventoryPanel.updateInventoryDisplay();
    }

}