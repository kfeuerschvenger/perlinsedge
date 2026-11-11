package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.*;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxRenderer;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.controller.KeyController;
import com.feuerschvenger.perlinsedge.ui.view.*;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * Thin coordinator of the game: builds the managers, starts new games and runs the per-frame
 * update and render. The loop, the input handlers and the mode transitions live in
 * {@link GameLoop}, {@link InputWiring} and {@link GameStateTransitions}.
 */
@Slf4j
public class GameOrchestrator {
    private final GameStateManager gameState;
    private final BuildingManager buildingManager;
    private final ContainerManager containerManager;
    private final PlayerManager playerManager;
    private final CombatManager combatManager;
    private final InventoryManager inventoryManager;
    private final MapManager mapManager;
    private final ItemDropManager itemDropManager;
    private final HotbarManager hotbarManager;
    private final DropKeyHandler dropKeys;

    private final FxRenderer renderer;
    private final FxCamera camera;
    private final FxInputHandler inputHandler;
    private final FxMinimapDrawer minimapDrawer;
    private final Canvas mainCanvas;
    private final InventoryPanel inventoryPanel;
    private final MapConfigPanel mapConfigPanel;
    private final DeathOverlay deathOverlay;

    private final GameLoop gameLoop;
    private boolean gameActive;
    /** Interpolation factor of the last frame, reused to draw the paused frame. */
    private double lastAlpha = 1.0;
    private final InputWiring inputWiring;
    private final GameStateTransitions transitions;

    /**
     * Builds the orchestrator and every manager it coordinates.
     *
     * @param renderer            canvas renderer
     * @param camera              camera
     * @param inputHandler        keyboard and scroll input
     * @param minimapDrawer       minimap view
     * @param mapConfigPanel      map tuning panel
     * @param mainCanvas          main game canvas
     * @param healthBar           player health view
     * @param inventoryPanel      inventory view
     * @param buildingHotbarPanel building hotbar view
     * @param hintLabel           short on-screen hints for actions that did nothing
     * @param deathOverlay        death screen shown while the player is dead
     * @param gameState           shared state, a new one is created when null
     * @param recipeManager       recipes, a new one is created when null
     */
    public GameOrchestrator(FxRenderer renderer, FxCamera camera, FxInputHandler inputHandler,
                            FxMinimapDrawer minimapDrawer, MapConfigPanel mapConfigPanel,
                            Canvas mainCanvas, HealthBar healthBar, InventoryPanel inventoryPanel,
                            BuildingHotbarPanel buildingHotbarPanel, HintLabel hintLabel,
                            DeathOverlay deathOverlay, GameStateManager gameState, RecipeManager recipeManager) {
        this.renderer = renderer;
        this.camera = camera;
        this.inputHandler = inputHandler;
        this.minimapDrawer = minimapDrawer;
        this.mapConfigPanel = mapConfigPanel;
        this.mainCanvas = mainCanvas;
        this.inventoryPanel = inventoryPanel;
        this.deathOverlay = deathOverlay;

        this.gameState = gameState != null ? gameState : new GameStateManager();
        RecipeManager recipes = recipeManager != null ? recipeManager : new RecipeManager();
        playerManager = new PlayerManager(this.gameState, healthBar, inventoryPanel);
        combatManager = new CombatManager(this.gameState);
        inventoryManager = new InventoryManager(this.gameState, inventoryPanel::updateInventoryDisplay, hintLabel::show);
        mapManager = new MapManager(this.gameState, mapConfigPanel);
        ResourceManager resourceManager = new ResourceManager(this.gameState, hintLabel::show);
        itemDropManager = new ItemDropManager(this.gameState, inventoryPanel::updateInventoryDisplay);
        dropKeys = new DropKeyHandler(this.gameState, inventoryPanel::getHoveredOrSelectedSlot, itemDropManager);
        containerManager = new ContainerManager();
        buildingManager = new BuildingManager(this.gameState, recipes, containerManager);
        hotbarManager = new HotbarManager(buildingManager, this.gameState, buildingHotbarPanel);

        gameLoop = new GameLoop(this::update, this::render);
        transitions = new GameStateTransitions(this.gameState, buildingManager, containerManager, hotbarManager,
                inventoryPanel, mapConfigPanel, recipes, gameLoop);
        transitions.setOnPaused(() -> render(lastAlpha, 0));
        inputWiring = new InputWiring(this.gameState, buildingManager, containerManager, playerManager,
                resourceManager, camera, inputHandler, minimapDrawer, mainCanvas,
                transitions::showOpenContainer);
    }

    // ==================================================================
    //  Game lifecycle
    // ==================================================================

    /**
     * Starts a new game session with the specified map type.
     *
     * @param selectedMapType The map type to generate for the new game
     */
    public void startNewGame(MapType selectedMapType) {
        log.info("Starting new game with map type: {}", selectedMapType.name());
        leaveGame();

        gameState.resetGameState();
        AppConfig.getInstance().world().setCurrentMapType(selectedMapType);

        long newSeed = new Random().nextLong();
        MapGenerationContext newMapContext = mapManager.createMapContext(selectedMapType, newSeed);
        mapManager.generateNewMap(newMapContext);

        playerManager.initializePlayer();
        mapManager.spawnEnemies();

        initializeGameComponents();
        minimapDrawer.updateMap(gameState.getCurrentMap());
        centerOnPlayer();
        startGameLoop();

        log.info("New game started");
    }

    /**
     * Stops the loop and returns every panel and mode to the state of a fresh game.
     * Called before a new game and when the player goes back to the main menu.
     */
    public void leaveGame() {
        stopGameLoop();
        transitions.resetUi();
        deathOverlay.hide();
        renderer.clearBuildingPreview();
        renderer.invalidateWorldCaches();
    }

    /** Starts the main game loop. */
    public void startGameLoop() {
        gameLoop.start();
        gameActive = true;
    }

    /** Stops the main game loop. */
    public void stopGameLoop() {
        gameLoop.stop();
        gameActive = false;
    }

    /** @return true while a game is running, false on the main menu */
    public boolean isGameActive() {
        return gameActive;
    }

    /** Resumes the game from the paused state. */
    public void resumeGame() {
        transitions.resumeGame();
    }

    /** Centers the camera on the player position. */
    public void centerOnPlayer() {
        Player player = gameState.getPlayer();
        if (player != null) {
            camera.centerOnTile(player.getCurrentTileX(), player.getCurrentTileY(), mainCanvas);
        }
    }

    /** Placeholder for the save feature. */
    public void saveGame() {
        log.info("Saving game is not implemented yet");
    }

    /** Placeholder for the load feature. */
    public void loadGame() {
        log.info("Loading game is not implemented yet");
    }

    /**
     * Links the pause menu so the transitions can show and hide it.
     *
     * @param pauseMenuPanel pause menu view
     */
    public void setPauseMenuPanel(PauseMenuPanel pauseMenuPanel) {
        transitions.setPauseMenuPanel(pauseMenuPanel);
        pauseMenuPanel.setOnResumeAction(this::resumeGame);
        pauseMenuPanel.setOnSaveAction(this::saveGame);
        pauseMenuPanel.setOnLoadAction(this::loadGame);
    }

    // ==================================================================
    //  Wiring
    // ==================================================================

    private void initializeGameComponents() {
        log.debug("Initializing game components");
        mapConfigPanel.setMapConfigListener(this::applyMapConfig);
        setupInventoryCallbacks();
        inputWiring.registerKeyboard(createKeyController());
        inputWiring.registerWheelInterceptor(
                (deltaY, ctrlDown) -> !gameState.isPlayerDead() && hotbarManager.handleWheel(deltaY, ctrlDown));
        inputWiring.registerMouseHandlers();
    }

    private void setupInventoryCallbacks() {
        inventoryPanel.setOnItemDroppedInside((srcSlot, srcContainer, tgtSlot, tgtContainer, item) -> {
            containerManager.transferBetweenSlots(srcContainer, srcSlot, tgtContainer, tgtSlot, item);
            Platform.runLater(inventoryPanel::updateInventoryDisplay);
        });

        // The stack leaves the slot it was dragged from and is thrown in front of the player in one step, so a
        // second event for the same stack finds the slot empty and cannot duplicate it
        inventoryPanel.setOnItemDropped((slot, wholeStack) ->
                itemDropManager.dropFromSlot(slot.container(), slot.slot(), wholeStack));

        inventoryPanel.setOnCraftRecipe(inventoryManager::handleCraftRecipe);
    }

    private KeyController createKeyController() {
        KeyController controller = new KeyController(
                transitions::togglePauseMenu,
                this::centerOnPlayer,
                this::regenerateMap,
                this::toggleMapConfigPanelInGame,
                transitions::toggleInventory,
                combatManager::handlePlayerAttack,
                transitions::toggleBuildingMode,
                hotbarManager::handleHotbarNumberSelection,
                hotbarManager::changePage
        );
        controller.setDropActions(dropKeys::dropOne, dropKeys::dropStack);
        controller.setInputBlocked(gameState::isPlayerDead);
        return controller;
    }

    private void toggleMapConfigPanelInGame() {
        if (gameActive) {
            transitions.toggleMapConfigPanel();
        }
    }

    private void regenerateMap() {
        if (!gameActive) return;
        log.debug("Regenerating current map");
        MapGenerationContext currentContext = mapManager.getCurrentMapContext();
        rebuildWorld(mapManager.createMapContext(currentContext.getMapType(), new Random().nextLong()));
    }

    /** Applies a map configuration chosen in the map tuning panel by rebuilding the world with it. */
    private void applyMapConfig(MapType mapType, long seed,
                                float heightFreq, int heightOctaves,
                                float tempFreq, int tempOctaves,
                                float moistureFreq, int moistureOctaves,
                                float stoneFreq, float stoneThreshold,
                                float crystalsFreq, float crystalsThreshold,
                                float treeDensity, float riverDensity) {
        AppConfig.getInstance().world().setCurrentMapType(mapType);
        rebuildWorld(new MapGenerationContext(
                mapType, seed,
                heightFreq, heightOctaves,
                tempFreq, tempOctaves,
                moistureFreq, moistureOctaves,
                stoneFreq, stoneThreshold,
                crystalsFreq, crystalsThreshold,
                treeDensity, riverDensity
        ));
    }

    /**
     * Replaces the current map while the game keeps running.
     * The panels, the open container and the modes go back to the fresh-game state, the player is placed on the
     * new map, and only then are the enemies spawned, so they keep their distance from the player's new position.
     */
    private void rebuildWorld(MapGenerationContext context) {
        if (!gameActive) {
            log.debug("Ignoring a map rebuild: no game is running");
            return;
        }
        leaveGame();
        mapManager.generateNewMap(context);
        playerManager.initializePlayer();
        mapManager.spawnEnemies();
        minimapDrawer.updateMap(gameState.getCurrentMap());
        centerOnPlayer();
        startGameLoop();
    }

    // ==================================================================
    //  Per-frame work
    // ==================================================================

    /**
     * Updates game state with the fixed timestep.
     *
     * @param deltaTime Fixed timestep value (1/60 second)
     */
    private void update(double deltaTime) {
        if (gameState.isGamePaused()) return;

        if (playerManager.handleRespawn(deltaTime)) {
            centerOnPlayer();
        }

        gameState.incrementGameTime(deltaTime);
        handleEdgePanning(deltaTime);
        playerManager.updatePlayer(deltaTime);
        if (gameState.isPlayerDead()) {
            // Building mode and the inventory cannot stay open under the death screen
            transitions.setGameMode(GameMode.EXPLORATION);
        }
        itemDropManager.update();
        combatManager.updateEnemies(deltaTime);

        if (gameState.shouldCenterCamera()) {
            centerOnPlayer();
            gameState.setShouldCenterCamera(false);
        }
    }

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
        lastAlpha = alpha;

        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            int[] mouseTile = gameState.getMouseOverTile();
            Rgba previewColor = buildingManager.getPlacementPreviewColor(mouseTile[0], mouseTile[1]);
            renderer.setBuildingPreview(
                    mouseTile[0],
                    mouseTile[1],
                    buildingManager.getSelectedBuildingType(),
                    previewColor
            );
        } else {
            renderer.clearBuildingPreview();
        }

        renderer.render(alpha, deltaTime);
        deathOverlay.update(gameState.isPlayerDead(), gameState.getPlayerRespawnTimer());
        // The tile layer is cached inside the drawer, only the player marker and the camera view move per frame
        minimapDrawer.updateMap(gameState.getCurrentMap());
        minimapDrawer.setPlayerTile(gameState.getPlayer().getCurrentTileX(), gameState.getPlayer().getCurrentTileY());
        minimapDrawer.draw();

        if (gameState.getCurrentGameMode() == GameMode.BUILDING_MODE) {
            renderer.drawBuildingModeBorder(mainCanvas.getWidth(), mainCanvas.getHeight());
        }
    }
}
