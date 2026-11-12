package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.HotbarManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;
import com.feuerschvenger.perlinsedge.ui.view.PauseMenuPanel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Owns the game mode transitions: pause and resume, the inventory and building modes,
 * and the visibility of the panels that go with them.
 * Building mode, the inventory and the open container are only ever entered or left through
 * {@link #setGameMode(GameMode)}, so their exit and enter actions always run together.
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
    /** Runs once the game is paused, so the frame left under the pause menu is drawn in the paused state. */
    @Setter
    private Runnable onPaused;

    /** Pauses when running and resumes when paused. Open panels and building mode are left before pausing. */
    public void togglePauseMenu() {
        if (gameState.isGamePaused()) {
            resumeGame();
        } else {
            setGameMode(GameMode.EXPLORATION);
            pauseGame();
        }
    }

    /** Resumes the game from the paused state and restarts the loop. */
    public void resumeGame() {
        gameState.setGamePaused(false);
        gameState.setCurrentGameMode(GameMode.EXPLORATION);
        hidePauseMenu();
        gameLoop.start();
    }

    /**
     * Brings every panel and mode back to the state of a fresh game: exploration, nothing open, nothing paused.
     * Used before a new game starts and when the player leaves for the main menu.
     */
    public void resetUi() {
        setGameMode(GameMode.EXPLORATION);
        containerManager.closeContainer();
        hotbarManager.hideHotbar();
        inventoryPanel.closeInventory();
        hidePauseMenu();
        gameState.setInventoryOpen(false);
        gameState.setGamePaused(false);
        gameState.setCurrentGameMode(GameMode.EXPLORATION);
    }

    /** Shows or hides the map configuration panel. */
    public void toggleMapConfigPanel() {
        boolean isVisible = !mapConfigPanel.isVisible();
        mapConfigPanel.setVisible(isVisible);
        mapConfigPanel.setManaged(isVisible);
        log.debug("Map Config Panel: {}", isVisible ? "VISIBLE" : "HIDDEN");
    }

    /** Toggles building mode on and off. Entering it closes the inventory. */
    public void toggleBuildingMode() {
        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            setGameMode(GameMode.EXPLORATION);
        } else if (!gameState.isGamePaused()) {
            setGameMode(GameMode.BUILDING_MODE);
        }
    }

    /** Toggles the player inventory. Opening it leaves building mode. */
    public void toggleInventory() {
        if (gameState.isGamePaused() || gameState.getPlayer() == null) return;

        GameMode mode = gameState.getCurrentGameMode();
        if (mode == GameMode.INVENTORY_OPEN || mode == GameMode.CRAFTING_OPEN) {
            setGameMode(GameMode.EXPLORATION);
        } else {
            inventoryPanel.setCraftingRecipes(recipeManager.getAllItemRecipes());
            setGameMode(GameMode.INVENTORY_OPEN);
        }
    }

    /**
     * Shows the container that {@link ContainerManager} has just opened next to the player inventory.
     * The mode becomes the inventory mode with the inventory flag set, so the inventory key closes it again.
     */
    public void showOpenContainer() {
        Container container = containerManager.getOpenContainer();
        if (container == null) return;

        hotbarManager.hideHotbar();
        inventoryPanel.setCraftingRecipes(recipeManager.getAllItemRecipes());
        inventoryPanel.openDualInventory(container);
        gameState.setInventoryOpen(true);
        gameState.setCurrentGameMode(GameMode.INVENTORY_OPEN);
    }

    /**
     * Sets the current game mode and runs the exit and enter actions of the modes involved.
     *
     * Every mode but exploration needs a player, which a rebuild that failed halfway leaves without, so those switches
     * are ignored then.
     *
     * @param newMode target game mode
     */
    public void setGameMode(GameMode newMode) {
        if (gameState.getCurrentGameMode() == newMode) return;
        if (newMode != GameMode.EXPLORATION && gameState.getPlayer() == null) {
            log.debug("Ignoring the switch to {}: there is no player", newMode);
            return;
        }

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

    private void pauseGame() {
        gameState.setGamePaused(true);
        gameLoop.stop();
        gameState.setCurrentGameMode(GameMode.PAUSED);
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(true);
            pauseMenuPanel.setManaged(true);
        }
        if (onPaused != null) {
            onPaused.run();
        }
    }

    private void hidePauseMenu() {
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(false);
            pauseMenuPanel.setManaged(false);
        }
    }
}
