package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.ui.view.BuildingHotbarPanel;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the building hotbar UI component and its interaction with game systems.
 * Handles visibility, selection, and synchronization with building mode state.
 */
@Slf4j
public class HotbarManager {
    // Dependencies
    private final BuildingHotbarPanel buildingHotbarPanel;
    private final BuildingManager buildingManager;
    private final GameStateManager gameState;

    /**
     * Initializes the hotbar manager with required dependencies.
     *
     * @param buildingManager Controls building selection and placement
     * @param gameState Provides current game state information
     * @param buildingHotbarPanel The UI hotbar component to manage
     */
    public HotbarManager(BuildingManager buildingManager,
                         GameStateManager gameState,
                         BuildingHotbarPanel buildingHotbarPanel) {
        this.buildingManager = buildingManager;
        this.gameState = gameState;
        this.buildingHotbarPanel = buildingHotbarPanel;

        // Link UI selection to building manager
        buildingHotbarPanel.setOnBuildingSelected(buildingManager::setSelectedBuildingType);
    }

    /**
     * Displays and activates the building hotbar UI.
     * Refreshes content before showing to ensure current state.
     */
    public void showHotbar() {
        buildingHotbarPanel.refreshHotbarDisplay();
        buildingHotbarPanel.setVisible(true);
        buildingHotbarPanel.setManaged(true);
        log.debug("Hotbar shown");
    }

    /**
     * Hides and deactivates the building hotbar UI.
     * Clears any active building selection.
     */
    public void hideHotbar() {
        buildingHotbarPanel.setVisible(false);
        buildingHotbarPanel.setManaged(false);
        buildingManager.setSelectedBuildingType(null); // Clear selection
        buildingHotbarPanel.clearSelection();
        log.debug("Hotbar hidden");
    }

    /**
     * Shows the next or previous group of buildings when there are more buildings than number keys.
     * Only responds when in building mode.
     *
     * @param delta 1 for the next page, -1 for the previous page
     */
    public void changePage(int delta) {
        if (isInBuildingMode()) {
            buildingHotbarPanel.changePage(delta);
        }
    }

    /**
     * Uses a mouse wheel notch to flip the hotbar page while in building mode.
     * The notch is left to the camera when the game is not in building mode or the hotbar has a single page.
     *
     * @param delta -1 for the previous page, 1 for the next one
     * @return true when the notch changed the page and must not zoom the camera
     */
    public boolean handleWheel(int delta) {
        if (!isInBuildingMode() || !buildingHotbarPanel.hasMultiplePages()) {
            return false;
        }
        buildingHotbarPanel.changePage(delta);
        return true;
    }

    /**
     * Checks if the hotbar is currently visible.
     * @return true if hotbar is visible, false otherwise
     */
    public boolean isHotbarVisible() {
        return buildingHotbarPanel.isVisible();
    }

    /**
     * Processes number key input for hotbar selection.
     * Only responds when in building mode.
     *
     * @param slotNumber Hotbar slot number (1-9) within the page that is showing
     */
    public void handleHotbarNumberSelection(int slotNumber) {
        if (isInBuildingMode()) {
            buildingHotbarPanel.selectBuildingBySlotNumber(slotNumber);
            log.debug("Selected slot: {}", slotNumber);
        }
    }

    /**
     * Checks if game is currently in building mode.
     * @return true if in BUILDING_MODE, false otherwise
     */
    private boolean isInBuildingMode() {
        return gameState.getCurrentGameMode() == GameMode.BUILDING_MODE;
    }

}