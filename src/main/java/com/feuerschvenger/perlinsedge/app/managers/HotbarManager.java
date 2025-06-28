package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.ui.view.BuildingHotbarPanel;

/**
 * Manages the building hotbar, handling its visibility, selection, and interaction
 * with the BuildingManager based on user input.
 */
public class HotbarManager {

    private final BuildingHotbarPanel buildingHotbarPanel;
    private final BuildingManager buildingManager;
    private final GameStateManager gameState;

    /**
     * Constructs a HotbarManager.
     *
     * @param recipeManager   The RecipeManager to populate the hotbar with building recipes.
     * @param buildingManager The BuildingManager to notify about selected building types.
     * @param gameState       The GameStateManager to check current game mode.
     */
    public HotbarManager(RecipeManager recipeManager, BuildingManager buildingManager, GameStateManager gameState, BuildingHotbarPanel buildingHotbarPanel) {
        this.buildingManager = buildingManager;
        this.gameState = gameState;
        this.buildingHotbarPanel = buildingHotbarPanel;

        this.buildingHotbarPanel.setOnBuildingSelected(this.buildingManager::setSelectedBuildingType);
    }

    /**
     * Handles a number key press to select a building type from the hotbar.
     * This method is intended to be called by the KeyController.
     *
     * @param number The number corresponding to the hotbar slot (1-9).
     */
    public void handleHotbarNumberSelection(int number) {
        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            buildingHotbarPanel.selectBuildingBySlotNumber(number);
        }
    }

    /**
     * Shows the building hotbar and refreshes its display.
     */
    public void showHotbar() {
        buildingHotbarPanel.refreshHotbarDisplay();
        buildingHotbarPanel.setVisible(true);
        buildingHotbarPanel.setManaged(true);
        System.out.println("HotbarManager: Hotbar shown.");
    }

    /**
     * Hides the building hotbar.
     */
    public void hideHotbar() {
        buildingHotbarPanel.setVisible(false);
        buildingHotbarPanel.setManaged(false);
        buildingManager.setSelectedBuildingType(null);
        System.out.println("HotbarManager: Hotbar hidden.");
    }

    /**
     * Checks if the hotbar is currently visible.
     *
     * @return true if the hotbar is visible, false otherwise.
     */
    public boolean isHotbarVisible() {
        return buildingHotbarPanel.isVisible();
    }

    public BuildingHotbarPanel getHotbarPanel() {
        return buildingHotbarPanel;
    }

}
