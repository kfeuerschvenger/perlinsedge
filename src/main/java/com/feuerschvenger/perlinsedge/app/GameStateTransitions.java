package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingStation;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.ui.view.BuildingPalettePanel;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
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
    private final BuildingPalettePanel palette;
    private final InventoryPanel inventoryPanel;
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
        palette.hidePalette();
        inventoryPanel.closeInventory();
        hidePauseMenu();
        gameState.setInventoryOpen(false);
        gameState.setCraftingStation(CraftingStation.INVENTORY);
        gameState.setGamePaused(false);
        gameState.setCurrentGameMode(GameMode.EXPLORATION);
    }

    /**
     * Keeps the build mode in step with the hand: the game is in build mode while the hammer is the item held in hand,
     * and out of it as soon as any other slot is selected or the hammer leaves its slot. Build mode is never entered
     * while the player is dead or the game is paused, and a mode with a panel open, such as the inventory, is left
     * alone, so closing the panel with the hammer still in hand brings the build mode back. Called every update, which
     * covers every way the selection can change: the number keys, the wheel, a click on a slot and moving items.
     */
    public void syncBuildModeWithHand() {
        final var player = gameState.getPlayer();
        final boolean hammerInHand = player != null && player.getActiveItemType() == ItemType.HAMMER
                && !gameState.isPlayerDead() && !gameState.isGamePaused();
        final var mode = gameState.getCurrentGameMode();
        if (mode == GameMode.BUILDING_MODE && !hammerInHand) {
            setGameMode(GameMode.EXPLORATION);
        } else if (mode == GameMode.EXPLORATION && hammerInHand) {
            setGameMode(GameMode.BUILDING_MODE);
        }
    }

    /** Toggles the player inventory. Opening it leaves building mode. */
    public void toggleInventory() {
        if (gameState.isGamePaused() || gameState.getPlayer() == null) {
            return;
        }

        var mode = gameState.getCurrentGameMode();
        if (mode == GameMode.INVENTORY_OPEN || mode == GameMode.CRAFTING_OPEN) {
            setGameMode(GameMode.EXPLORATION);
        } else {
            gameState.setCraftingStation(CraftingStation.INVENTORY);
            inventoryPanel.setCraftingRecipes(recipeManager.getItemRecipesFor(CraftingStation.INVENTORY));
            setGameMode(GameMode.INVENTORY_OPEN);
        }
    }

    /**
     * Shows the container that {@link ContainerManager} has just opened next to the player inventory.
     * The mode becomes the inventory mode with the inventory flag set, so the inventory key closes it again.
     */
    public void showOpenContainer() {
        var container = containerManager.getOpenContainer();
        if (container == null) {
            return;
        }

        palette.hidePalette();
        gameState.setCraftingStation(CraftingStation.INVENTORY);
        inventoryPanel.setCraftingRecipes(recipeManager.getItemRecipesFor(CraftingStation.INVENTORY));
        inventoryPanel.openDualInventory(container, containerTitle(container));
        gameState.setInventoryOpen(true);
        gameState.setCurrentGameMode(GameMode.INVENTORY_OPEN);
    }

    /** The title of a container view: the name of the building that holds it, or a plain name for any other. */
    private static String containerTitle(Container container) {
        final var name = container instanceof Building building ? building.getType().getDisplayName() : "Container";
        return name + " Inventory";
    }

    /**
     * Opens the inventory as the crafting view of a station the player clicked: the title of the inventory becomes the
     * name of the station and the crafting list holds the recipes of that station only. The station stays the one
     * recipes are crafted at until the view closes.
     *
     * @param station the station that was clicked
     */
    public void showStation(CraftingStation station) {
        palette.hidePalette();
        gameState.setCraftingStation(station);
        inventoryPanel.setCraftingRecipes(recipeManager.getItemRecipesFor(station));
        inventoryPanel.openStation(station);
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
        if (gameState.getCurrentGameMode() == newMode) {
            return;
        }
        if (newMode != GameMode.EXPLORATION && gameState.getPlayer() == null) {
            log.debug("Ignoring the switch to {}: there is no player", newMode);
            return;
        }

        switch (gameState.getCurrentGameMode()) {
            case BUILDING_MODE:
                buildingManager.deactivateBuildingMode();
                palette.hidePalette();
                break;
            case INVENTORY_OPEN, CRAFTING_OPEN:
                containerManager.closeContainer();
                inventoryPanel.closeInventory();
                gameState.setInventoryOpen(false);
                gameState.setCraftingStation(CraftingStation.INVENTORY);
                break;
            default:
                break;
        }

        switch (newMode) {
            case BUILDING_MODE:
                buildingManager.activateBuildingMode();
                palette.showPalette();
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
