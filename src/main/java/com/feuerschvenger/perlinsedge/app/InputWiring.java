package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.PlayerManager;
import com.feuerschvenger.perlinsedge.app.managers.ResourceManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.controller.KeyController;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemDroppedOutsideCallback;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import lombok.extern.slf4j.Slf4j;

/**
 * Registers the mouse, drag-and-drop and keyboard handlers of the game and translates
 * canvas events into game actions.
 * Handler references are kept in fields so they can be removed with {@link #unregisterMouseHandlers()}.
 */
@Slf4j
public class InputWiring {
    private static final int CLICK_TILE_ADJUSTMENT = 5;

    private final GameStateManager gameState;
    private final BuildingManager buildingManager;
    private final ContainerManager containerManager;
    private final PlayerManager playerManager;
    private final ResourceManager resourceManager;
    private final InventoryPanel inventoryPanel;
    private final FxCamera camera;
    private final FxInputHandler inputHandler;
    private final FxMinimapDrawer minimapDrawer;
    private final Canvas mainCanvas;

    private final EventHandler<MouseEvent> canvasClickHandler = this::handleCanvasClick;
    private final EventHandler<MouseEvent> canvasMoveHandler = this::handleMouseMovement;
    private final EventHandler<MouseEvent> canvasExitHandler = this::handleMouseExit;
    private final EventHandler<MouseEvent> minimapClickHandler = this::handleMinimapClick;
    private final EventHandler<DragEvent> sceneDragDroppedHandler = this::handleSceneDragDropped;

    /**
     * Creates the wiring.
     *
     * @param gameState        shared game state
     * @param buildingManager  building placement and interaction
     * @param containerManager open container tracking
     * @param playerManager    player movement
     * @param resourceManager  resource harvesting
     * @param inventoryPanel   inventory view
     * @param camera           camera used to convert screen to tile coordinates
     * @param inputHandler     keyboard and scroll input source
     * @param minimapDrawer    minimap view
     * @param mainCanvas       canvas that receives game clicks
     */
    public InputWiring(GameStateManager gameState, BuildingManager buildingManager,
                       ContainerManager containerManager, PlayerManager playerManager,
                       ResourceManager resourceManager, InventoryPanel inventoryPanel, FxCamera camera,
                       FxInputHandler inputHandler, FxMinimapDrawer minimapDrawer, Canvas mainCanvas) {
        this.gameState = gameState;
        this.buildingManager = buildingManager;
        this.containerManager = containerManager;
        this.playerManager = playerManager;
        this.resourceManager = resourceManager;
        this.inventoryPanel = inventoryPanel;
        this.camera = camera;
        this.inputHandler = inputHandler;
        this.minimapDrawer = minimapDrawer;
        this.mainCanvas = mainCanvas;
    }

    /**
     * Sets the key controller that receives keyboard actions.
     *
     * @param keyController controller built by the orchestrator
     */
    public void registerKeyboard(KeyController keyController) {
        inputHandler.setGameKeyListener(keyController);
    }

    /** Configures drag-and-drop handlers on the game scene. */
    public void registerSceneDragHandlers() {
        Scene scene = mainCanvas.getScene();
        if (scene == null) {
            log.warn("Scene not available for drag handlers");
            return;
        }

        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });

        scene.setOnDragDropped(sceneDragDroppedHandler);
    }

    /**
     * Adds the mouse handlers to the game canvas and the minimap.
     * Idempotent: any previously registered handlers are removed first, so repeated calls never stack them.
     */
    public void registerMouseHandlers() {
        unregisterMouseHandlers();
        mainCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED, canvasClickHandler);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, canvasMoveHandler);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, canvasExitHandler);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().addEventHandler(MouseEvent.MOUSE_CLICKED, minimapClickHandler);
        }
    }

    /** Removes the handlers added by {@link #registerMouseHandlers()}. */
    public void unregisterMouseHandlers() {
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_CLICKED, canvasClickHandler);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_MOVED, canvasMoveHandler);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_EXITED, canvasExitHandler);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().removeEventHandler(MouseEvent.MOUSE_CLICKED, minimapClickHandler);
        }
    }

    // ==================================================================
    //  Event handlers
    // ==================================================================

    private void handleSceneDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (!db.hasString()) {
            event.setDropCompleted(false);
            return;
        }

        Node sourceNode = (Node) event.getGestureSource();
        if (sourceNode == null) {
            event.setDropCompleted(false);
            return;
        }

        Item draggedItem = (Item) sourceNode.getProperties().get("draggedItem");
        ItemDroppedOutsideCallback callback = inventoryPanel.getOnItemDroppedOutsideCallback();

        if (draggedItem != null && callback != null) {
            callback.drop(draggedItem);
            event.setDropCompleted(true);
        } else {
            event.setDropCompleted(false);
        }
        event.consume();
    }

    private void handleCanvasClick(MouseEvent event) {
        if (gameState.isGamePaused()) return;

        int[] tileCoords = getClickedTile(event.getX(), event.getY());
        int tileX = tileCoords[0];
        int tileY = tileCoords[1];

        if (!isWithinMapBounds(tileX, tileY)) return;

        Tile tile = gameState.getCurrentMap().getTile(tileX, tileY);
        if (tile == null) return;

        switch (gameState.getCurrentGameMode()) {
            case BUILDING_MODE:
                handleBuildingModeClick(event, tileX, tileY);
                break;
            case EXPLORATION:
                handleExplorationModeClick(event, tileX, tileY, tile);
                break;
            case INVENTORY_OPEN:
            case CRAFTING_OPEN:
            case PAUSED:
                log.debug("Click ignored in UI mode");
                break;
        }
    }

    private void handleBuildingModeClick(MouseEvent event, int tileX, int tileY) {
        if (event.getButton() == MouseButton.PRIMARY) {
            buildingManager.placeBuilding(tileX, tileY);
        } else if (event.getButton() == MouseButton.SECONDARY) {
            buildingManager.removeBuilding(tileX, tileY);
        }
    }

    private void handleExplorationModeClick(MouseEvent event, int tileX, int tileY, Tile tile) {
        if (event.getButton() == MouseButton.PRIMARY) {
            // Prioritize building interaction
            if (tile.hasBuilding() && buildingManager.interactWithBuilding(tileX, tileY)) {
                if (containerManager.isContainerOpen()) {
                    inventoryPanel.openDualInventory(containerManager.getOpenContainer());
                    gameState.setCurrentGameMode(GameMode.INVENTORY_OPEN);
                }
            }
            // Then resource interaction
            else if (tile.hasResource()) {
                resourceManager.handleTileInteraction(gameState.getPlayer(), tile);
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            playerManager.movePlayer(gameState.getCurrentMap(), tileX, tileY);
        }
    }

    private int[] getClickedTile(double screenX, double screenY) {
        return IsometricUtils.screenToTileAdjusted(
                screenX, screenY,
                camera.getX(), camera.getY(),
                camera.getZoom(),
                AppConfig.getInstance().graphics().getTileHalfWidth(),
                AppConfig.getInstance().graphics().getTileHalfHeight(),
                CLICK_TILE_ADJUSTMENT
        );
    }

    private boolean isWithinMapBounds(int x, int y) {
        TileMap map = gameState.getCurrentMap();
        return map != null && x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }

    private void handleMouseMovement(MouseEvent event) {
        if (shouldIgnoreMouseInput()) {
            gameState.setMouseOverTile(-1, -1);
            return;
        }

        int[] tileCoords = getHoveredTile(event.getX(), event.getY());
        gameState.setMouseOverTile(tileCoords[0], tileCoords[1]);
    }

    private boolean shouldIgnoreMouseInput() {
        return gameState.getCurrentGameMode() == GameMode.INVENTORY_OPEN ||
                gameState.getCurrentGameMode() == GameMode.CRAFTING_OPEN ||
                gameState.getCurrentGameMode() == GameMode.PAUSED;
    }

    private int[] getHoveredTile(double screenX, double screenY) {
        return IsometricUtils.screenToTile(
                screenX, screenY,
                camera.getX(), camera.getY(),
                camera.getZoom(),
                AppConfig.getInstance().graphics().getTileHalfWidth(),
                AppConfig.getInstance().graphics().getTileHalfHeight()
        );
    }

    private void handleMouseExit(MouseEvent event) {
        gameState.setMouseOverTile(-1, -1);
    }

    private void handleMinimapClick(MouseEvent event) {
        if (gameState.isGamePaused()) return;
        double[] tileCoords = minimapDrawer.miniMapToTile(event.getX(), event.getY());
        camera.centerOnTile(tileCoords[0], tileCoords[1], mainCanvas);
    }
}
