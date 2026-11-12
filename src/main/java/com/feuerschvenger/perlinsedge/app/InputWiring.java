package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.BuildingManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.InteractionManager;
import com.feuerschvenger.perlinsedge.app.managers.PlayerManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.TilePicker;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.infra.fx.input.ScrollInterceptor;
import com.feuerschvenger.perlinsedge.ui.controller.KeyController;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Registers the mouse and keyboard handlers of the game and translates canvas events into game actions.
 * Dragging items out of the inventory is handled by the inventory panel itself.
 * Handler references are kept in fields so they can be removed with {@link #unregisterMouseHandlers()}.
 */
@Slf4j
public class InputWiring {
    private final GameStateManager gameState;
    private final BuildingManager buildingManager;
    private final PlayerManager playerManager;
    private final InteractionManager interactionManager;
    private final FxCamera camera;
    private final FxInputHandler inputHandler;
    private final FxMinimapDrawer minimapDrawer;
    private final Canvas mainCanvas;

    /** Receives the tile found by {@link #pickTile}, reused so a pick allocates nothing. */
    private final int[] pickedTile = new int[2];
    private double lastMouseX;
    private double lastMouseY;
    private boolean mouseOverCanvas;

    private final EventHandler<MouseEvent> canvasClickHandler = this::handleCanvasPress;
    private final EventHandler<MouseEvent> canvasMoveHandler = this::handleMouseMovement;
    private final EventHandler<MouseEvent> canvasDragHandler = this::handleMouseDragOrRelease;
    private final EventHandler<MouseEvent> canvasExitHandler = this::handleMouseExit;
    private final EventHandler<MouseEvent> minimapClickHandler = this::handleMinimapClick;

    /**
     * Creates the wiring.
     *
     * @param gameState        shared game state
     * @param buildingManager  building placement and removal
     * @param playerManager    player movement
     * @param interactionManager walks to and interacts with resources and buildings
     * @param camera           camera used to convert screen to tile coordinates
     * @param inputHandler     keyboard and scroll input source
     * @param minimapDrawer    minimap view
     * @param mainCanvas       canvas that receives game clicks
     */
    public InputWiring(GameStateManager gameState, BuildingManager buildingManager,
                       PlayerManager playerManager, InteractionManager interactionManager, FxCamera camera,
                       FxInputHandler inputHandler, FxMinimapDrawer minimapDrawer, Canvas mainCanvas) {
        this.gameState = gameState;
        this.buildingManager = buildingManager;
        this.playerManager = playerManager;
        this.interactionManager = interactionManager;
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

    /**
     * Lets a handler use the mouse wheel before the camera does.
     *
     * @param interceptor receives the wheel delta and the Ctrl state, and returns true when it used the event
     */
    public void registerWheelInterceptor(ScrollInterceptor interceptor) {
        inputHandler.setScrollInterceptor(interceptor);
    }

    /**
     * Adds the mouse handlers to the game canvas and the minimap.
     * Idempotent: any previously registered handlers are removed first, so repeated calls never stack them.
     */
    public void registerMouseHandlers() {
        unregisterMouseHandlers();
        mainCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, canvasClickHandler);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, canvasMoveHandler);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, canvasDragHandler);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, canvasDragHandler);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, canvasExitHandler);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().addEventHandler(MouseEvent.MOUSE_CLICKED, minimapClickHandler);
        }
    }

    /** Removes the handlers added by {@link #registerMouseHandlers()}. */
    public void unregisterMouseHandlers() {
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, canvasClickHandler);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_MOVED, canvasMoveHandler);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, canvasDragHandler);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_RELEASED, canvasDragHandler);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_EXITED, canvasExitHandler);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().removeEventHandler(MouseEvent.MOUSE_CLICKED, minimapClickHandler);
        }
    }

    // ==================================================================
    //  Event handlers
    // ==================================================================

    /**
     * Acts on the press of a button over the canvas, not on the click that follows its release.
     * JavaFX delivers a click only to the node that is the deepest one common to the press and the release, so a click
     * is lost when the pointer ends over another node, such as a panel that appeared under it. The press
     * always reaches the canvas it happened on, and the tile is the one the cursor showed at that moment.
     */
    private void handleCanvasPress(MouseEvent event) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        mouseOverCanvas = true;
        if (gameState.isGamePaused() || gameState.isPlayerDead()) return;

        if (!pickTile(event.getX(), event.getY())) return;
        int tileX = pickedTile[0];
        int tileY = pickedTile[1];

        switch (gameState.getCurrentGameMode()) {
            case BUILDING_MODE:
                handleBuildingModeClick(event, tileX, tileY);
                break;
            case EXPLORATION:
                handleExplorationModeClick(event, tileX, tileY);
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

    /**
     * Left click interacts with the tile, walking up to it first when it is out of reach; right click walks to it.
     * Either order flashes the clicked tile green when it was accepted and red when it was refused; a click with no
     * effect, because the resource was hit a moment ago, does not flash.
     */
    private void handleExplorationModeClick(MouseEvent event, int tileX, int tileY) {
        final boolean accepted;
        if (event.getButton() == MouseButton.PRIMARY) {
            final InteractionManager.Outcome outcome = interactionManager.interact(tileX, tileY);
            // A click that changed nothing, such as one during the hit cooldown, earns no flash: green would
            // promise an effect and red a refusal
            if (outcome == InteractionManager.Outcome.IGNORED) return;
            accepted = outcome.isAccepted();
        } else if (event.getButton() == MouseButton.SECONDARY) {
            interactionManager.cancel();
            accepted = playerManager.movePlayer(gameState.getCurrentMap(), tileX, tileY);
        } else {
            return;
        }
        gameState.getClickFeedback().flash(tileX, tileY, accepted, gameState.getGameTime());
    }

    /**
     * Finds the tile under a canvas point into {@link #pickedTile}, with the one rule hover and clicks share.
     *
     * @return true when the point is over a tile of the current map
     */
    private boolean pickTile(double screenX, double screenY) {
        TileMap map = gameState.getCurrentMap();
        if (map == null) return false;
        return TilePicker.pick(map, screenX, screenY, camera.getX(), camera.getY(), camera.getZoom(),
                AppConfig.getInstance().graphics().getTileHalfWidth(),
                AppConfig.getInstance().graphics().getTileHalfHeight(), pickedTile);
    }

    /**
     * Recomputes the tile under the mouse from the last known mouse position.
     * Called once per frame, so the cursor follows the ground when the camera pans, zooms or recentres and when the
     * mode changes, not only when the mouse itself moves.
     */
    public void refreshHover() {
        if (!mouseOverCanvas || shouldIgnoreMouseInput() || !pickTile(lastMouseX, lastMouseY)) {
            gameState.setMouseOverTile(-1, -1);
            return;
        }
        gameState.setMouseOverTile(pickedTile[0], pickedTile[1]);
    }

    private void handleMouseMovement(MouseEvent event) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        mouseOverCanvas = true;
    }

    /**
     * Follows the pointer while a button is held. JavaFX sends drag and release events instead of move events then,
     * and keeps sending them after the pointer left the canvas, so whether the pointer is still over the canvas is read
     * from its position.
     */
    private void handleMouseDragOrRelease(MouseEvent event) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        mouseOverCanvas = event.getX() >= 0 && event.getY() >= 0
                && event.getX() <= mainCanvas.getWidth() && event.getY() <= mainCanvas.getHeight();
    }

    private boolean shouldIgnoreMouseInput() {
        return gameState.getCurrentGameMode() == GameMode.INVENTORY_OPEN ||
                gameState.getCurrentGameMode() == GameMode.CRAFTING_OPEN ||
                gameState.getCurrentGameMode() == GameMode.PAUSED;
    }

    private void handleMouseExit(MouseEvent event) {
        mouseOverCanvas = false;
        gameState.setMouseOverTile(-1, -1);
    }

    private void handleMinimapClick(MouseEvent event) {
        if (gameState.isGamePaused()) return;
        double[] tileCoords = minimapDrawer.miniMapToTile(event.getX(), event.getY());
        camera.centerOnTile(tileCoords[0], tileCoords[1], mainCanvas);
    }
}
