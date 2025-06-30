package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.*;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxRenderer;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.controller.KeyController;
import com.feuerschvenger.perlinsedge.ui.view.*;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemDroppedOutsideCallback;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;

/**
 * Main game orchestrator managing the game loop, state transitions, and interactions between subsystems.
 * Handles initialization, input processing, rendering, and coordination of game managers.
 */
public class GameOrchestrator {
    // Game system managers
    private final RecipeManager recipeManager;
    private final BuildingManager buildingManager;
    private final ContainerManager containerManager;
    private final GameStateManager gameState;
    private final PlayerManager playerManager;
    private final CombatManager combatManager;
    private final InventoryManager inventoryManager;
    private final MapManager mapManager;
    private final ResourceManager resourceManager;
    private final ItemDropManager itemDropManager;
    private final HotbarManager hotbarManager;

    // UI and rendering components
    private final FxRenderer renderer;
    private final FxCamera camera;
    private final FxInputHandler inputHandler;
    private final FxMinimapDrawer minimapDrawer;
    private final Canvas mainCanvas;
    private final InventoryPanel inventoryPanel;
    private final MapConfigPanel mapConfigPanel;

    // Game loop and UI state
    private AnimationTimer gameLoop;
    private PauseMenuPanel pauseMenuPanel;

    public GameOrchestrator(FxRenderer renderer, FxCamera camera, FxInputHandler inputHandler,
                            FxMinimapDrawer minimapDrawer, MapConfigPanel mapConfigPanel,
                            Canvas mainCanvas, HealthBar healthBar, InventoryPanel inventoryPanel,
                            BuildingHotbarPanel buildingHotbarPanel, GameStateManager gameState, RecipeManager recipeManager) {
        this.renderer = renderer;
        this.camera = camera;
        this.inputHandler = inputHandler;
        this.minimapDrawer = minimapDrawer;
        this.mapConfigPanel = mapConfigPanel;
        this.mainCanvas = mainCanvas;
        this.inventoryPanel = inventoryPanel;

        // Initialize game managers with dependency injection
        this.gameState = gameState != null ? gameState : new GameStateManager();
        this.recipeManager = recipeManager != null ? recipeManager : new RecipeManager();
        playerManager = new PlayerManager(this.gameState, healthBar, inventoryPanel);
        combatManager = new CombatManager(this.gameState);
        inventoryManager = new InventoryManager(this.gameState, inventoryPanel);
        mapManager = new MapManager(this.gameState, mapConfigPanel);
        resourceManager = new ResourceManager(this.gameState);
        itemDropManager = new ItemDropManager(this.gameState, inventoryPanel);
        containerManager = new ContainerManager();
        buildingManager = new BuildingManager(this.gameState, recipeManager, containerManager);
        hotbarManager = new HotbarManager(buildingManager, this.gameState, buildingHotbarPanel);
    }

    // Game lifecycle methods
    // =======================================================================

    /**
     * Starts a new game session with the specified map type.
     *
     * @param selectedMapType The map type to generate for the new game
     */
    public void startNewGame(MapType selectedMapType) {
        System.out.println("GameOrchestrator: Starting new game with map type: " + selectedMapType.name());
        stopGameLoop(); // Ensure no existing game loop is running

        // Reset game state and configuration
        gameState.resetGameState();
        AppConfig.getInstance().world().setCurrentMapType(selectedMapType);

        // Generate new map
        long newSeed = new Random().nextLong();
        MapGenerationContext newMapContext = mapManager.createMapContext(selectedMapType, newSeed);
        mapManager.generateNewMap(newMapContext);

        // Initialize player and enemies
        playerManager.initializePlayer();
        mapManager.spawnEnemies();

        // Set up game systems
        initializeGameComponents();
        minimapDrawer.updateMap(gameState.getCurrentMap());
        centerOnPlayer();
        startGameLoop();

        System.out.println("GameOrchestrator: New game started successfully!");
    }

    /**
     * Initializes game components including input handlers and UI callbacks.
     */
    private void initializeGameComponents() {
        System.out.println("GameOrchestrator: Initializing game components");
        mapConfigPanel.setMapConfigListener(mapManager);
        setupInventoryCallbacks();
        setupSceneDragHandlers();
        setupKeyController();
        setupMouseEventHandlers();
        setupGameLoop();
    }

    /**
     * Starts the main game loop.
     */
    public void startGameLoop() {
        if (gameLoop != null) gameLoop.start();
    }

    /**
     * Stops the main game loop.
     */
    public void stopGameLoop() {
        if (gameLoop != null) gameLoop.stop();
    }

    // Input handling setup
    // =======================================================================

    /**
     * Configures drag-and-drop handlers for the game scene.
     */
    private void setupSceneDragHandlers() {
        Scene scene = mainCanvas.getScene();
        if (scene == null) {
            System.err.println("Warning: Scene not available for drag handlers");
            return;
        }

        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });

        scene.setOnDragDropped(this::handleSceneDragDropped);
    }

    /**
     * Sets up inventory UI callbacks for item interactions.
     */
    private void setupInventoryCallbacks() {
        // Item transfer between containers
        inventoryPanel.setOnItemDroppedInside((srcSlot, srcContainer, tgtSlot, tgtContainer, item) -> {
            containerManager.transferBetweenSlots(srcContainer, srcSlot, tgtContainer, tgtSlot, item);
            Platform.runLater(inventoryPanel::updateInventoryDisplay);
        });

        // Item dropped outside inventory
        inventoryPanel.setOnItemDroppedOutside(item -> {
            Player player = gameState.getPlayer();
            if (player != null) {
                inventoryManager.removeItemFromPlayerInventory(item);
                itemDropManager.createDroppedItem(item, player);
                Platform.runLater(inventoryPanel::updateInventoryDisplay);
            }
        });

        // Crafting attempt
        inventoryPanel.setOnCraftRecipe(recipe -> {
            inventoryManager.handleCraftRecipe(recipe);
            Platform.runLater(inventoryPanel::updateInventoryDisplay);
        });
    }

    /**
     * Configures keyboard input controller.
     */
    private void setupKeyController() {
        KeyController keyController = new KeyController(
                this::togglePauseMenu,
                this::centerOnPlayer,
                this::regenerateMap,
                () -> togglePanelVisibility(mapConfigPanel, "Map Config Panel"),
                this::toggleInventory,
                combatManager::handlePlayerAttack,
                this::toggleBuildingMode,
                hotbarManager::handleHotbarNumberSelection
        );
        inputHandler.setGameKeyListener(keyController);
    }

    /**
     * Regenerates the current map with a new seed.
     */
    private void regenerateMap() {
        System.out.println("GameOrchestrator: Regenerating current map");
        MapGenerationContext currentContext = mapManager.getCurrentMapContext();
        MapGenerationContext newContext = mapManager.createMapContext(
                currentContext.getMapType(), new Random().nextLong()
        );
        mapManager.generateNewMap(newContext);
        minimapDrawer.updateMap(gameState.getCurrentMap());
        playerManager.initializePlayer();
        mapManager.spawnEnemies();
        centerOnPlayer();
    }

    /**
     * Sets up mouse event handlers for game interaction.
     */
    private void setupMouseEventHandlers() {
        // Clear existing handlers to avoid duplicates
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMovement);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseExit);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().removeEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMinimapClick);
        }

        // Register new handlers
        mainCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMovement);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseExit);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMinimapClick);
        }
    }

    // Mouse event handlers
    // =======================================================================

    /**
     * Handles drag-and-drop events on the game scene.
     */
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

    /**
     * Handles canvas click events based on current game mode.
     */
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
                System.out.println("GameOrchestrator: Click ignored in UI mode");
                break;
        }
    }

    /**
     * Processes clicks in building mode.
     */
    private void handleBuildingModeClick(MouseEvent event, int tileX, int tileY) {
        if (event.getButton() == MouseButton.PRIMARY) {
            buildingManager.placeBuilding(tileX, tileY);
        } else if (event.getButton() == MouseButton.SECONDARY) {
            buildingManager.removeBuilding(tileX, tileY);
        }
    }

    /**
     * Processes clicks in exploration mode.
     */
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

    /**
     * Converts screen coordinates to tile coordinates.
     */
    private int[] getClickedTile(double screenX, double screenY) {
        return IsometricUtils.screenToTileAdjusted(
                screenX, screenY,
                camera.getX(), camera.getY(),
                camera.getZoom(),
                AppConfig.getInstance().graphics().getTileHalfWidth(),
                AppConfig.getInstance().graphics().getTileHalfHeight(),
                5
        );
    }

    /**
     * Checks if coordinates are within map boundaries.
     */
    private boolean isWithinMapBounds(int x, int y) {
        TileMap map = gameState.getCurrentMap();
        return map != null && x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }

    /**
     * Handles mouse movement for tile highlighting.
     */
    private void handleMouseMovement(MouseEvent event) {
        if (shouldIgnoreMouseInput()) {
            gameState.setMouseOverTile(-1, -1);
            return;
        }

        int[] tileCoords = getHoveredTile(event.getX(), event.getY());
        gameState.setMouseOverTile(tileCoords[0], tileCoords[1]);
    }

    /**
     * Determines if mouse input should be ignored based on game state.
     */
    private boolean shouldIgnoreMouseInput() {
        return gameState.getCurrentGameMode() == GameMode.INVENTORY_OPEN ||
                gameState.getCurrentGameMode() == GameMode.CRAFTING_OPEN ||
                gameState.getCurrentGameMode() == GameMode.PAUSED;
    }

    /**
     * Converts screen coordinates to tile coordinates for hovering.
     */
    private int[] getHoveredTile(double screenX, double screenY) {
        return IsometricUtils.screenToTile(
                screenX, screenY,
                camera.getX(), camera.getY(),
                camera.getZoom(),
                AppConfig.getInstance().graphics().getTileHalfWidth(),
                AppConfig.getInstance().graphics().getTileHalfHeight()
        );
    }

    /**
     * Handles mouse exit from game canvas.
     */
    private void handleMouseExit(MouseEvent event) {
        gameState.setMouseOverTile(-1, -1);
    }

    /**
     * Handles minimap click events for camera movement.
     */
    private void handleMinimapClick(MouseEvent event) {
        if (gameState.isGamePaused()) return;
        double[] tileCoords = minimapDrawer.miniMapToTile(event.getX(), event.getY());
        camera.centerOnTile(tileCoords[0], tileCoords[1], mainCanvas);
    }

    // Game loop implementation
    // =======================================================================

    /**
     * Initializes the game loop with fixed timestep physics.
     */
    private void setupGameLoop() {
        if (gameLoop == null) {
            gameLoop = new AnimationTimer() {
                private long lastUpdate = 0;
                private double accumulator = 0;
                private static final double FIXED_TIME_STEP = 1.0 / 60.0; // 60 FPS

                @Override
                public void handle(long now) {
                    if (lastUpdate == 0) {
                        lastUpdate = now;
                        return;
                    }

                    // Calculate delta time in seconds
                    double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                    lastUpdate = now;
                    deltaTime = Math.min(deltaTime, 0.25); // Clamp to prevent spiral of death

                    // Fixed timestep updates
                    accumulator += deltaTime;
                    while (accumulator >= FIXED_TIME_STEP) {
                        update(FIXED_TIME_STEP);
                        accumulator -= FIXED_TIME_STEP;
                    }

                    // Render with interpolation
                    double alpha = accumulator / FIXED_TIME_STEP;
                    render(alpha, deltaTime);
                }
            };
        }
    }

    /**
     * Updates game state with fixed timestep.
     *
     * @param deltaTime Fixed timestep value (1/60 second)
     */
    private void update(double deltaTime) {
        if (gameState.isGamePaused()) return;

        // Handle player respawn logic
        if (playerManager.handleRespawn(deltaTime)) {
            centerOnPlayer();
        }

        // Update game systems
        gameState.incrementGameTime(deltaTime);
        gameState.setAttackCooldownTimer(Math.max(0, gameState.getAttackCooldownTimer() - deltaTime));
        handleEdgePanning(deltaTime);
        playerManager.updatePlayer(deltaTime);
        itemDropManager.update();
        combatManager.updateEnemies(deltaTime);

        // Handle camera centering request
        if (gameState.shouldCenterCamera()) {
            centerOnPlayer();
            gameState.setShouldCenterCamera(false);
        }
    }

    /**
     * Handles camera panning based on input.
     */
    private void handleEdgePanning(double deltaTime) {
        boolean[] edgeActive = inputHandler.getEdgeActive();
        double panSpeed = AppConfig.getInstance().core().getPanSpeed() * deltaTime * 60;

        if (edgeActive[0]) camera.pan(0, panSpeed, mainCanvas);  // Up
        if (edgeActive[1]) camera.pan(-panSpeed, 0, mainCanvas); // Left
        if (edgeActive[2]) camera.pan(0, -panSpeed, mainCanvas); // Down
        if (edgeActive[3]) camera.pan(panSpeed, 0, mainCanvas);  // Right
    }

    /**
     * Renders the game state.
     *
     * @param alpha Interpolation factor for smooth rendering
     * @param deltaTime Time since last render (seconds)
     */
    private void render(double alpha, double deltaTime) {
        if (gameState.getCurrentMap() == null || gameState.getPlayer() == null) return;

        // Set building preview if in building mode
        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            int[] mouseTile = gameState.getMouseOverTile();
            Color previewColor = buildingManager.getPlacementPreviewColor(mouseTile[0], mouseTile[1]);
            renderer.setBuildingPreview(
                    mouseTile[0],
                    mouseTile[1],
                    buildingManager.getSelectedBuildingType(),
                    previewColor
            );
        }

        // Perform rendering
        renderer.render(alpha, deltaTime);
        minimapDrawer.updateMap(gameState.getCurrentMap());
        minimapDrawer.draw();

        // Draw special overlays
        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            renderer.drawBuildingModeBorder(mainCanvas.getWidth(), mainCanvas.getHeight());
        }
    }

    // Game state management
    // =======================================================================

    /**
     * Toggles pause menu state.
     */
    private void togglePauseMenu() {
        if (gameState.isGamePaused()) {
            resumeGame();
        } else {
            closeOpenUIPanels();
            pauseGame();
        }
    }

    /**
     * Closes all open UI panels before pausing.
     */
    private void closeOpenUIPanels() {
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

    /**
     * Pauses the game and shows pause menu.
     */
    private void pauseGame() {
        gameState.setGamePaused(true);
        stopGameLoop();
        gameState.setCurrentGameMode(GameMode.PAUSED);
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(true);
            pauseMenuPanel.setManaged(true);
        }
    }

    /**
     * Resumes the game from paused state.
     */
    public void resumeGame() {
        gameState.setGamePaused(false);
        gameState.setCurrentGameMode(GameMode.EXPLORATION);
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(false);
            pauseMenuPanel.setManaged(false);
        }
        startGameLoop();
    }

    /**
     * Centers camera on player position.
     */
    public void centerOnPlayer() {
        Player player = gameState.getPlayer();
        if (player != null) {
            camera.centerOnTile(player.getCurrentTileX(), player.getCurrentTileY(), mainCanvas);
        }
    }

    /**
     * Toggles visibility of UI panels.
     *
     * @param panel The panel to toggle
     * @param panelName Name for logging purposes
     */
    private void togglePanelVisibility(Node panel, String panelName) {
        boolean isVisible = !panel.isVisible();
        panel.setVisible(isVisible);
        panel.setManaged(isVisible);

        if (panel == inventoryPanel) {
            handleInventoryPanelToggle(isVisible);
        } else if (panel == mapConfigPanel) {
            System.out.println(panelName + ": " + (isVisible ? "VISIBLE" : "HIDDEN"));
        }
    }

    /**
     * Handles inventory panel visibility changes.
     */
    private void handleInventoryPanelToggle(boolean isVisible) {
        if (isVisible) {
            // Show inventory
            hotbarManager.hideHotbar();
            List<Recipe> recipes = recipeManager.getAllItemRecipes();
            inventoryPanel.setCraftingRecipes(recipes);
            containerManager.openContainer(gameState.getPlayer());
            inventoryPanel.openPlayerInventory();
            gameState.setCurrentGameMode(GameMode.INVENTORY_OPEN);
        } else {
            // Hide inventory
            containerManager.closeContainer();
            inventoryPanel.closeInventory();
            if (gameState.isInventoryOpen()) {
                gameState.setCurrentGameMode(GameMode.EXPLORATION);
            }
        }
        inventoryPanel.setCraftingRecipes(recipeManager.getAllItemRecipes());
    }

    /**
     * Toggles building mode on/off.
     */
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

    /**
     * Toggles inventory visibility.
     */
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
     * Sets the current game mode and handles transitions.
     *
     * @param newMode Target game mode
     */
    public void setGameMode(GameMode newMode) {
        if (gameState.getCurrentGameMode() == newMode) return;

        // Exit current mode
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
        }

        // Enter new mode
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
        }

        gameState.setCurrentGameMode(newMode);
    }

    // Save/Load functionality (stubs)
    // =======================================================================

    public void saveGame() {
        System.out.println("Saving game...");
        // TODO: Implement save logic
    }

    public void loadGame() {
        System.out.println("Loading game...");
        // TODO: Implement load logic
    }

    public void setPauseMenuPanel(PauseMenuPanel pauseMenuPanel) {
        this.pauseMenuPanel = pauseMenuPanel;
        pauseMenuPanel.setOnResumeAction(this::resumeGame);
        pauseMenuPanel.setOnSaveAction(this::saveGame);
        pauseMenuPanel.setOnLoadAction(this::loadGame);
    }

}