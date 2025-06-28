package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.*;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
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
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;

/**
 * Orchestrates the main game loop, manages game state, and handles interactions between different game systems.
 */
public class GameOrchestrator {
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

    private final FxRenderer renderer;
    private final FxCamera camera;
    private final FxInputHandler inputHandler;
    private final FxMinimapDrawer minimapDrawer;
    private final Canvas mainCanvas;
    private final InventoryPanel inventoryPanel;
    private final MapConfigPanel mapConfigPanel;

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

        // Initialize game managers
        this.gameState = gameState != null ? gameState : new GameStateManager();
        this.recipeManager = recipeManager != null ? recipeManager : new RecipeManager();
        playerManager = new PlayerManager(this.gameState, healthBar, this.inventoryPanel);
        combatManager = new CombatManager(this.gameState);
        inventoryManager = new InventoryManager(this.gameState, this.inventoryPanel);
        mapManager = new MapManager(this.gameState, this.mapConfigPanel);
        resourceManager = new ResourceManager(this.gameState);
        itemDropManager = new ItemDropManager(this.gameState, this.inventoryPanel);
        containerManager = new ContainerManager();
        buildingManager = new BuildingManager(this.gameState, this.recipeManager, containerManager);
        hotbarManager = new HotbarManager(this.recipeManager, buildingManager, this.gameState, buildingHotbarPanel);
    }

    /**
     * Starts a new game with the specified map type.
     * This method orchestrates the setup of a new game session.
     * @param selectedMapType The MapType chosen by the player.
     */
    public void startNewGame(MapType selectedMapType) {
        System.out.println("GameOrchestrator: Starting new game with map type: " + selectedMapType.name());
        stopGameLoop(); // Stop the game loop if already running (e.g., restarting a game)

        // 1. Reset global game state
        gameState.resetGameState();
        AppConfig.getInstance().world().setCurrentMapType(selectedMapType); // Set the selected map type

        // 2. Create a map context with a new random seed for the new game
        Random random = new Random();
        long newSeed = random.nextLong();
        MapGenerationContext newMapContext = mapManager.createMapContext(selectedMapType, newSeed);

        // 3. Generate the new map
        mapManager.generateNewMap(newMapContext); // This will update currentMap in gameState.

        // 4. Initialize the player on the new map. PlayerManager handles safe positioning.
        // This MUST happen before initializing managers that depend on Player/Map state.
        playerManager.initializePlayer();

        // 5. Spawn enemies after player is initialized, using player's position to avoid nearby spawns.
        mapManager.spawnEnemies(gameState.getCurrentMap()); // Pass player position for enemy spawning

        // 6. Initialize other game components (handlers, loop, etc.)
        initializeGameComponents();

        // 7. Update minimap with the newly generated map
        minimapDrawer.updateMap(gameState.getCurrentMap());

        // 8. Center the camera on the player
        centerOnPlayer();

        // 9. Start the main game loop
        startGameLoop();
        System.out.println("GameOrchestrator: New game started successfully!");
    }

    /**
     * Initializes various game components that need to be set up once
     * the main game objects (map, player, etc.) are ready.
     * This method can be called safely multiple times as it attempts to remove
     * old handlers before adding new ones.
     */
    private void initializeGameComponents() {
        System.out.println("GameOrchestrator: Initializing game components (input handlers, etc.).");
        mapConfigPanel.setMapConfigListener(mapManager);
        setupInventoryCallbacks();
        setupSceneDragHandlers();
        setupKeyController();
        setupMouseEventHandlers();
        setupGameLoop();
    }

    private void setupSceneDragHandlers() {
        Scene scene = mainCanvas.getScene();

        if (scene == null) {
            System.err.println("Warning: Scene is not available when trying to set up drag handlers.");
            return;
        }

        scene.setOnDragOver(null);
        //scene.setOnDragDropped(null);

        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                Node sourceNode = (Node) event.getGestureSource();
                if (sourceNode != null) {
                    Item draggedItem = (Item) sourceNode.getProperties().get("draggedItem");
                    if (draggedItem != null && inventoryPanel.getOnItemDroppedOutsideCallback() != null) {
                        inventoryPanel.getOnItemDroppedOutsideCallback().drop(draggedItem);
                        event.setDropCompleted(true);
                        return;
                    }
                }
            }
            event.setDropCompleted(false);
            event.consume();
        });
    }

    private void setupInventoryCallbacks() {
        inventoryPanel.setOnItemDroppedInside(
                (srcSlot, srcContainer, tgtSlot, tgtContainer, item) -> {
                    System.out.println("TRANSFER START: " + item.getDisplayName() +
                            " from " + srcContainer.getClass().getSimpleName() + ":" + srcSlot +
                            " to " + tgtContainer.getClass().getSimpleName() + ":" + tgtSlot);

                    containerManager.transferBetweenSlots(
                            srcContainer, srcSlot,
                            tgtContainer, tgtSlot,
                            item
                    );

                    // FIX: Refresh UI immediately
                    Platform.runLater(() -> {
                        System.out.println("REFRESHING UI AFTER TRANSFER");
                        inventoryPanel.updateInventoryDisplay();
                    });
                }
        );

        inventoryPanel.setOnItemDroppedOutside(item -> {
            System.out.println("ITEM DROPPED OUTSIDE: " + item.getDisplayName());
            Player player = gameState.getPlayer();
            if (player != null) {
                // Find and remove the actual item instance
                for (int i = 0; i < player.getCapacity(); i++) {
                    Item invItem = player.getItemAt(i);
                    if (invItem == item) {
                        player.setItemAt(i, null);
                        System.out.println("Removed from slot: " + i);
                        break;
                    }
                }

                int[] dropPos = playerManager.findAdjacentDropPosition(
                        player.getCurrentTileX(),
                        player.getCurrentTileY()
                );

                // Create new instance for dropped item
                DroppedItem dropped = new DroppedItem(
                        dropPos[0], dropPos[1],
                        new Item(item.getType(), item.getQuantity()),
                        gameState.getGameTime(),
                        DroppedItem.DropSource.PLAYER
                );

                gameState.getDroppedItems().add(dropped);
                gameState.setLastDropTime(gameState.getGameTime());

                // Force UI refresh
                Platform.runLater(() -> {
                    inventoryPanel.updateInventoryDisplay();
                });
            }
        });

        // FIX: Ensure recipes are set when opening inventory
        inventoryPanel.setOnCraftRecipe(recipe -> {
            System.out.println("CRAFT ATTEMPT: " + recipe.getName());
            inventoryManager.handleCraftRecipe(recipe);

            // Refresh UI after crafting
            Platform.runLater(() -> {
                inventoryPanel.updateInventoryDisplay();
            });
        });
    }

    private void generateNewMap() {
        System.out.println("GameOrchestrator: Regenerating current map.");
        Random random = new Random();
        long newSeed = random.nextLong();
        MapGenerationContext currentContext = mapManager.getCurrentMapContext();
        MapGenerationContext newContext = mapManager.createMapContext(
                currentContext.getMapType(), newSeed
        );
        mapManager.generateNewMap(newContext);
        minimapDrawer.updateMap(gameState.getCurrentMap());
        playerManager.initializePlayer();

        mapManager.spawnEnemies(gameState.getCurrentMap());

        centerOnPlayer();
        System.out.println("GameOrchestrator: Map regenerated.");
    }

    private void setupKeyController() {
        KeyController keyController = new KeyController(
                this::togglePauseMenu,
                this::centerOnPlayer,
                this::generateNewMap,
                () -> togglePanelVisibility(mapConfigPanel, "Map Config Panel"),
                this::toggleInventory, // Changed to use GameOrchestrator's toggleInventory
                combatManager::handlePlayerAttack,
                this::toggleBuildingMode,
                hotbarManager::handleHotbarNumberSelection
        );

        inputHandler.setGameKeyListener(keyController);
    }

    private void setupMouseEventHandlers() {
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMovement);
        mainCanvas.removeEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseExit);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().removeEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMinimapClick);
        }

        mainCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMovement);
        mainCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseExit);
        if (minimapDrawer.getCanvas() != null) {
            minimapDrawer.getCanvas().addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMinimapClick);
        }
    }

    private void handleCanvasClick(MouseEvent event) {
        if (gameState.isGamePaused()) return;

        int[] tileCords = getClickedTile(event.getX(), event.getY());
        int tileX = tileCords[0];
        int tileY = tileCords[1];

        if (gameState.getCurrentMap() == null || !isWithinMapBounds(tileX, tileY)) return;

        Tile tile = gameState.getCurrentMap().getTile(tileX, tileY);
        if (tile == null) return;

        switch (gameState.getCurrentGameMode()) {
            case BUILDING_MODE:
                if (buildingManager.getSelectedBuildingType() != null) {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        buildingManager.placeBuilding(tileX, tileY);
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        buildingManager.removeBuilding(tileX, tileY);
                    }
                } else {
                    if (event.getButton() == MouseButton.SECONDARY) {
                        buildingManager.removeBuilding(tileX, tileY);
                    } else {
                        System.out.println("GameOrchestrator: No building type selected in building mode. Use 1-9 to select.");
                    }
                }
                break;
            case EXPLORATION:
                if (event.getButton() == MouseButton.PRIMARY) {
                    // Always try to interact with a building first if one exists
                    if (tile.hasBuilding()) { // Check if the tile has a building first
                        if (buildingManager.interactWithBuilding(tileX, tileY)) {
                            if (containerManager.isContainerOpen()) {
                                inventoryPanel.openDualInventory(gameState.getPlayer(), containerManager.getOpenContainer());
                                gameState.setCurrentGameMode(GameMode.INVENTORY_OPEN);
                                System.out.println("GameOrchestrator: Building with container opened, entering INVENTORY_OPEN mode.");
                            } else {
                                System.out.println("GameOrchestrator: Building interacted with, but no container opened.");
                            }
                            return; // Interaction with building was handled, prevent resource interaction
                        }
                    }
                    // If no building interaction or building interaction didn't consume the click, try resource interaction
                    if (tile.hasResource()) { // Only try to interact with resource if it exists
                        resourceManager.handleTileInteraction(gameState.getPlayer(), tile);
                        return; // Resource interaction handled
                    }
                } else if (event.getButton() == MouseButton.SECONDARY) {
                    playerManager.movePlayer(gameState.getCurrentMap(), tileX, tileY);
                }
                break;
            case INVENTORY_OPEN:
            case CRAFTING_OPEN:
                System.out.println("GameOrchestrator: Click ignored while in UI mode.");
                break;
            case PAUSED:
                System.out.println("GameOrchestrator: Click ignored while paused.");
                break;
        }
    }

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

    private boolean isWithinMapBounds(int x, int y) {
        return x >= 0 && x < gameState.getCurrentMap().getWidth() &&
                y >= 0 && y < gameState.getCurrentMap().getHeight();
    }

    private void handleMouseMovement(MouseEvent event) {
        // Adjusted logic: only update mouse-over tile if not in UI-blocking mode OR paused
        if (gameState.getCurrentGameMode() == GameMode.INVENTORY_OPEN ||
                gameState.getCurrentGameMode() == GameMode.CRAFTING_OPEN ||
                gameState.getCurrentGameMode() == GameMode.PAUSED) {
            gameState.setMouseOverTileX(-1);
            gameState.setMouseOverTileY(-1);
            return;
        }

        int[] tileCords = getHoveredTile(event.getX(), event.getY());
        gameState.setMouseOverTileX(tileCords[0]);
        gameState.setMouseOverTileY(tileCords[1]);
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
        gameState.setMouseOverTileX(-1);
        gameState.setMouseOverTileY(-1);
    }

    private void handleMinimapClick(MouseEvent event) {
        if (gameState.isGamePaused()) return;
        double[] tileCords = minimapDrawer.miniMapToTile(event.getX(), event.getY());
        camera.centerOnTile(tileCords[0], tileCords[1], mainCanvas);
    }

    private void setupGameLoop() {
        if (gameLoop == null) {
            gameLoop = new AnimationTimer() {
                private long lastUpdate = 0;
                private double accumulator = 0;
                private static final double FIXED_TIME_STEP = 1.0 / 60.0;

                @Override
                public void handle(long now) {
                    if (lastUpdate == 0) {
                        lastUpdate = now;
                        return;
                    }

                    double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // Tiempo real en segundos
                    lastUpdate = now;

                    if (deltaTime > 0.25) deltaTime = 0.25;

                    accumulator += deltaTime;
                    while (accumulator >= FIXED_TIME_STEP) {
                        update(FIXED_TIME_STEP);
                        accumulator -= FIXED_TIME_STEP;
                    }

                    double alpha = accumulator / FIXED_TIME_STEP;
                    render(alpha, deltaTime);
                }
            };
        }
    }

    private void update(double deltaTime) {
        if (gameState.isGamePaused()) return;

        if (gameState.getPlayerRespawnTimer() > 0) {
            gameState.setPlayerRespawnTimer(gameState.getPlayerRespawnTimer() - deltaTime); // Decrement timer
            if (gameState.getPlayerRespawnTimer() <= 0) {
                System.out.println("GameOrchestrator: Respawn timer reached 0. Calling respawnPlayer.");
                playerManager.respawnPlayer(); // This will revive the player
                centerOnPlayer();
            }
        }

        gameState.incrementGameTime(deltaTime);
        gameState.setAttackCooldownTimer(Math.max(0, gameState.getAttackCooldownTimer() - deltaTime));
        handleEdgePanning(deltaTime);
        playerManager.updatePlayer(deltaTime);
        itemDropManager.updateDroppedItems(deltaTime);
        itemDropManager.checkItemPickup();
        updateEnemies(deltaTime);

        if (gameState.shouldCenterCamera()) {
            centerOnPlayer();
            gameState.setShouldCenterCamera(false);
        }
    }

    private void updateEnemies(double deltaTime) {
        Player player = gameState.getPlayer();
        TileMap map = gameState.getCurrentMap();

        if (player == null || map == null) return;

        for (Enemy enemy : gameState.getEnemies()) {
            enemy.update(deltaTime, map, player);
        }
    }

    private void handleEdgePanning(double deltaTime) {
        boolean[] edgeActive = inputHandler.getEdgeActive();
        double panSpeed = AppConfig.getInstance().core().getPanSpeed() * deltaTime * 60;

        if (edgeActive[0]) camera.pan(0, panSpeed, mainCanvas);
        if (edgeActive[1]) camera.pan(-panSpeed, 0, mainCanvas);
        if (edgeActive[2]) camera.pan(0, -panSpeed, mainCanvas);
        if (edgeActive[3]) camera.pan(panSpeed, 0, mainCanvas);
    }

    private void render(double alpha, double deltaTime) {
        if (gameState.getCurrentMap() == null || gameState.getPlayer() == null) {
            return;
        }

        Color previewColor = null;
        BuildingType previewBuilding = null;

        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            previewColor = buildingManager.getPlacementPreviewColor(
                    gameState.getMouseOverTileX(),
                    gameState.getMouseOverTileY()
            );
            previewBuilding = buildingManager.getSelectedBuildingType();
        }

        renderer.setBuildingPreview(
                gameState.getMouseOverTileX(),
                gameState.getMouseOverTileY(),
                previewBuilding,
                previewColor
        );

        renderer.render(alpha, deltaTime); // Pasar deltaTime
        minimapDrawer.updateMap(gameState.getCurrentMap());
        minimapDrawer.draw();

        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            renderer.drawBuildingModeBorder(mainCanvas.getWidth(), mainCanvas.getHeight());
        }
    }

    private void togglePauseMenu() {
        if (gameState.isGamePaused()) {
            resumeGame();
        } else {
            // Close other UI panels before pausing
            if (inventoryPanel.isVisible()) {
                inventoryPanel.closeInventory(); // Use closeInventory which handles UI and container state
            }
            if (hotbarManager.isHotbarVisible()) {
                hotbarManager.hideHotbar();
            }
            if (buildingManager.isBuildingModeActive()) {
                buildingManager.toggleBuildingMode(); // Deactivate building mode
            }
            pauseGame();
        }
    }

    private void pauseGame() {
        gameState.setGamePaused(true);
        stopGameLoop();
        // Set game mode to PAUSED
        gameState.setCurrentGameMode(GameMode.PAUSED);
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(true);
            pauseMenuPanel.setManaged(true);
        }
        System.out.println("GameOrchestrator: Game paused. Mode: " + gameState.getCurrentGameMode());
    }

    public void resumeGame() {
        gameState.setGamePaused(false);
        // Return to the mode before pausing, or to EXPLORATION if unknown
        if (gameState.getCurrentGameMode() == GameMode.PAUSED) {
            gameState.setCurrentGameMode(GameMode.EXPLORATION);
        }
        if (pauseMenuPanel != null) {
            pauseMenuPanel.setVisible(false);
            pauseMenuPanel.setManaged(false);
        }
        startGameLoop();
        System.out.println("GameOrchestrator: Game resumed. Mode: " + gameState.getCurrentGameMode());
    }

    public void saveGame() {
        System.out.println("Saving game...");
        // TODO: Implement save logic
    }

    public void loadGame() {
        System.out.println("Loading game...");
        // TODO: Implement load logic
    }

    private void togglePanelVisibility(Node panel, String panelName) {
        boolean isVisible = !panel.isVisible();
        panel.setVisible(isVisible);
        panel.setManaged(isVisible);

        if (panel == inventoryPanel) {
            if (isVisible) {
                if (hotbarManager.isHotbarVisible()) {
                    hotbarManager.hideHotbar();
                }
                List<Recipe> recipes = recipeManager.getAllRecipes();
                System.out.println("LOADING RECIPES: " + recipes.size() + " available");
                inventoryPanel.setCraftingRecipes(recipes);
                containerManager.openContainer(gameState.getPlayer());
                inventoryPanel.openPlayerInventory();
                gameState.setCurrentGameMode(GameMode.INVENTORY_OPEN);
                System.out.println("GameOrchestrator: Inventory panel opened. Mode: " + gameState.getCurrentGameMode());
            } else {
                containerManager.closeContainer();
                inventoryPanel.closeInventory();
                if (gameState.getCurrentGameMode() == GameMode.INVENTORY_OPEN || gameState.getCurrentGameMode() == GameMode.CRAFTING_OPEN) {
                    gameState.setCurrentGameMode(GameMode.EXPLORATION);
                }
                System.out.println("GameOrchestrator: Inventory panel closed. Mode: " + gameState.getCurrentGameMode());
            }
            inventoryPanel.setCraftingRecipes(recipeManager.getAllRecipes());
        } else if (panel == mapConfigPanel) {
            if (isVisible) {
                System.out.println(panelName + ": VISIBLE");
            } else {
                System.out.println(panelName + ": HIDDEN");
            }
        }
    }

    public void startGameLoop() {
        if (gameLoop != null) gameLoop.start();
    }

    public void stopGameLoop() {
        if (gameLoop != null) gameLoop.stop();
    }

    public void setPauseMenuPanel(PauseMenuPanel pauseMenuPanel) {
        this.pauseMenuPanel = pauseMenuPanel;
        pauseMenuPanel.setOnResumeAction(this::resumeGame);
        pauseMenuPanel.setOnSaveAction(this::saveGame);
        pauseMenuPanel.setOnLoadAction(this::loadGame);
    }

    public void centerOnPlayer() {
        Player player = gameState.getPlayer();
        if (player != null) {
            camera.centerOnTile(player.getCurrentTileX(), player.getCurrentTileY(), mainCanvas);
        }
    }

    /**
     * Sets the current game mode, handling transitions.
     * @param newMode The mode to switch to.
     */
    public void setGameMode(GameMode newMode) {
        if (gameState.getCurrentGameMode() == newMode) {
            return; // No change needed
        }

        // Logic to exit current mode gracefully
        switch (gameState.getCurrentGameMode()) {
            case BUILDING_MODE:
                if (buildingManager.isBuildingModeActive()) {
                    buildingManager.toggleBuildingMode(); // Esto desactivará el modo de construcción internamente
                }
                buildingManager.setSelectedBuildingType(null);
                // Ocultar la hotbar de construcción al salir del modo construcción usando HotbarManager
                hotbarManager.hideHotbar();
                break;
            case INVENTORY_OPEN:
            case CRAFTING_OPEN:
                containerManager.closeContainer(); // Cerrar cualquier contenedor/UI abierto
                inventoryPanel.closeInventory(); // Asegurarse de que la UI también se cierre
                break;
            case PAUSED:
                // Si se sale del modo de pausa, no se necesita un cierre especial de la UI, resumeGame lo maneja.
                break;
            case EXPLORATION:
                // No se necesita limpieza específica al salir de la exploración a menos que se entre en un modo de UI
                break;
        }

        // Logic to enter new mode
        switch (newMode) {
            case BUILDING_MODE:
                if (!buildingManager.isBuildingModeActive()) {
                    buildingManager.toggleBuildingMode(); // Esto activará el modo de construcción internamente
                }
                hotbarManager.showHotbar(); // Mostrar la hotbar de construcción usando HotbarManager
                // No abrir inventoryPanel.openBuildingPanel(); aquí, ya que el usuario no quiere el inventario.
                break;
            case INVENTORY_OPEN:
                containerManager.openContainer(gameState.getPlayer());
                inventoryPanel.openPlayerInventory(); // Abre la UI, por defecto al inventario del jugador
                break;
            case CRAFTING_OPEN:
                // Si alguna vez queremos ir directamente al crafteo, añadiríamos lógica específica aquí.
                // Por ahora, generalmente se accede a través de las pestañas de InventoryPanel.
                break;
            case PAUSED:
                // La pausa se maneja con togglePauseMenu, no directamente con setGameMode
                break;
            case EXPLORATION:
                // No se necesita configuración específica al entrar en exploración, solo asegura que otras UI estén cerradas.
                break;
        }

        gameState.setCurrentGameMode(newMode);
        System.out.println("Game mode changed to: " + newMode);
    }

    /**
     * Toggles the building mode.
     */
    public void toggleBuildingMode() {
        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            setGameMode(GameMode.EXPLORATION);
            hotbarManager.hideHotbar();
            System.out.println("Building mode: OFF");
        } else {
            if (inventoryPanel.isVisible()) {
                togglePanelVisibility(inventoryPanel, "Inventory Panel");
            }
            if (gameState.getCurrentGameMode() != GameMode.PAUSED) {
                setGameMode(GameMode.BUILDING_MODE);
                hotbarManager.showHotbar();
                System.out.println("Building mode: ON");
            }
        }
    }

    /**
     * Toggles the inventory mode.
     */
    public void toggleInventory() {
        // Alternar el inventario significa abrir/cerrar el panel de inventario (que también maneja la pestaña de crafteo)
        // Asegurarse de que la hotbar de construcción se oculte si el inventario se abre.
        if (hotbarManager.isHotbarVisible()) { // Usa HotbarManager
            hotbarManager.hideHotbar(); // Usa HotbarManager
        }

        if (inventoryPanel.isVisible() && (gameState.getCurrentGameMode() == GameMode.INVENTORY_OPEN || gameState.getCurrentGameMode() == GameMode.CRAFTING_OPEN)) {
            setGameMode(GameMode.EXPLORATION); // Cerrar inventario/crafteo
        } else {
            inventoryPanel.setCraftingRecipes(recipeManager.getAllRecipes());
            inventoryPanel.openPlayerInventory();
            setGameMode(GameMode.INVENTORY_OPEN); // Abrir inventario
        }
    }

}