package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.HotbarManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;
import com.feuerschvenger.perlinsedge.ui.view.PauseMenuPanel;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Owns the game mode transitions: pause and resume, the inventory and building modes,
 * and the visibility of the panels that go with them.
 */
@Slf4j
@RequiredArgsConstructor
public class GameStateTransitions {
    private final GameStateManager gameState;
    private final BuildingManager buildingManager;
    private final ContainerManager containerManager;
    private final HotbarManager hotbarManager;
    private final InventoryPanel inventoryPanel;
    private final MapConfigPanel mapConfigPanel;
    private final RecipeManager recipeManager;
    private final GameLoop gameLoop;

    @Setter
    private PauseMenuPanel pauseMenuPanel;

    /** Pauses when running and resumes when paused. Open panels are closed before pausing. */
    public void togglePauseMenu() {
        if (gameState.isGamePaused()) {
            resumeGame();
        } else {
            closeOpenUiPanels();
            pauseGame();
        }
    }

    /** Resumes the game from the paused state and restarts the loop. */
    public void resumeGame() {
        gameState.setGamePaused(false);
        gameState.setCurrentGameMode(GameMode.EXPLORATION);
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(false);
            pauseMenuPanel.setManaged(false);
        }
        gameLoop.start();
    }

    /** Shows or hides the map configuration panel. */
    public void toggleMapConfigPanel() {
        togglePanelVisibility(mapConfigPanel, "Map Config Panel");
    }

    /** Toggles building mode on and off, closing the inventory when entering it. */
    public void toggleBuildingMode() {
        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            setGameMode(GameMode.EXPLORATION);
            hotbarManager.hideHotbar();
        } else {
            if (inventoryPanel.isVisible()) {
                togglePanelVisibility(inventoryPanel, "Inventory Panel");
            }
            if (!gameState.isGamePaused()) {
                setGameMode(GameMode.BUILDING_MODE);
                hotbarManager.showHotbar();
            }
        }
    }

    /** Toggles the player inventory. */
    public void toggleInventory() {
        if (hotbarManager.isHotbarVisible()) {
            hotbarManager.hideHotbar();
        }

        if (inventoryPanel.isVisible() && gameState.isInventoryOpen()) {
            setGameMode(GameMode.EXPLORATION);
        } else {
            inventoryPanel.setCraftingRecipes(recipeManager.getAllItemRecipes());
            inventoryPanel.openPlayerInventory();
            setGameMode(GameMode.INVENTORY_OPEN);
        }
    }

    /**
     * Sets the current game mode and runs the exit and enter actions of the modes involved.
     *
     * @param newMode target game mode
     */
    public void setGameMode(GameMode newMode) {
        if (gameState.getCurrentGameMode() == newMode) return;

        switch (gameState.getCurrentGameMode()) {
            case BUILDING_MODE:
                buildingManager.deactivateBuildingMode();
                hotbarManager.hideHotbar();
                break;
            case INVENTORY_OPEN:
            case CRAFTING_OPEN:
                containerManager.closeContainer();
                inventoryPanel.closeInventory();
                gameState.setInventoryOpen(false);
                break;
            default:
                break;
        }

        switch (newMode) {
            case BUILDING_MODE:
                buildingManager.activateBuildingMode();
                hotbarManager.showHotbar();
                break;
            case INVENTORY_OPEN:
                containerManager.openContainer(gameState.getPlayer());
                gameState.setInventoryOpen(true);
                inventoryPanel.openPlayerInventory();
                break;
            default:
                break;
        }

        gameState.setCurrentGameMode(newMode);
    }

    private void closeOpenUiPanels() {
        if (inventoryPanel.isVisible()) {
            inventoryPanel.closeInventory();
        }
        if (hotbarManager.isHotbarVisible()) {
            hotbarManager.hideHotbar();
        }
        if (buildingManager.isBuildingModeActive()) {
            buildingManager.toggleBuildingMode();
        }
    }

    private void pauseGame() {
        gameState.setGamePaused(true);
        gameLoop.stop();
        gameState.setCurrentGameMode(GameMode.PAUSED);
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(true);
            pauseMenuPanel.setManaged(true);
        }
    }

    private void togglePanelVisibility(Node panel, String panelName) {
        boolean isVisible = !panel.isVisible();
        panel.setVisible(isVisible);
        panel.setManaged(isVisible);

        if (panel == inventoryPanel) {
            handleInventoryPanelToggle(isVisible);
        } else if (panel == mapConfigPanel) {
            log.debug("{}: {}", panelName, isVisible ? "VISIBLE" : "HIDDEN");
        }
    }

    private void handleInventoryPanelToggle(boolean isVisible) {
        if (isVisible) {
            hotbarManager.hideHotbar();
            inventoryPanel.setCraftingRecipes(recipeManager.getAllItemRecipes());
            containerManager.openContainer(gameState.getPlayer());
            inventoryPanel.openPlayerInventory();
            gameState.setCurrentGameMode(GameMode.INVENTORY_OPEN);
        } else {
            containerManager.closeContainer();
            inventoryPanel.closeInventory();
            if (gameState.isInventoryOpen()) {
                gameState.setCurrentGameMode(GameMode.EXPLORATION);
            }
        }
        inventoryPanel.setCraftingRecipes(recipeManager.getAllItemRecipes());
    }
}
