package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxRenderer;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.view.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Dimension;

/**
 * Main JavaFX application class for Perlin's Edge game.
 * Manages application lifecycle, UI layers, and game initialization.
 */
public class GameApplication extends Application {
    // Core UI components
    private Stage primaryStage;
    private StackPane rootPane;
    private Pane gameLayer;
    private Pane uiLayer;

    // Rendering components
    private Canvas mainCanvas;
    private FxCamera camera;
    private FxRenderer renderer;
    private FxMinimapDrawer minimapDrawer;

    // Game systems
    private GameOrchestrator gameOrchestrator;
    private GameStateManager gameStateManager;
    private final RecipeManager recipeManager = new RecipeManager();

    // UI panels
    private MainMenuPanel mainMenuPanel;
    private MapSelectionPanel mapSelectionPanel;
    private HealthBar healthBar;
    private InventoryPanel inventoryPanel;
    private MapConfigPanel mapConfigPanel;
    private BuildingHotbarPanel hotbarPanel;

    @Override
    public void start(Stage primaryStage) {
        configureDebugProperties();
        initializeRootPane();
        initializePrimaryStage(primaryStage);
        showMainMenu();
    }

    /**
     * Configures debug properties based on application configuration.
     */
    private void configureDebugProperties() {
        System.setProperty("javafx.verbose", "true");
        System.setProperty("prism.verbose", "true");
        System.setProperty("prism.dirtyopts", "true");
    }

    /**
     * Initializes and configures the primary application window.
     *
     * @param stage Primary stage to configure
     */
    private void initializePrimaryStage(Stage stage) {
        this.primaryStage = stage;

        // Create scene with root pane
        Scene scene = new Scene(rootPane);
        primaryStage.setTitle("Perlin's Edge");
        primaryStage.setScene(scene);

        // Configure fullscreen settings
        AppConfig config = AppConfig.getInstance();
        primaryStage.setFullScreen(config.core().isFullscreen());
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.show();
    }

    /**
     * Initializes the root application pane with proper dimensions.
     */
    private void initializeRootPane() {
        AppConfig config = AppConfig.getInstance();
        Dimension screenSize = getScreenDimensions(config);

        rootPane = new StackPane();
        rootPane.setPrefSize(screenSize.getWidth(), screenSize.getHeight());
        rootPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
    }

    /**
     * Gets screen dimensions based on fullscreen configuration.
     */
    private Dimension getScreenDimensions(AppConfig config) {
        if (config.core().isFullscreen()) {
            return java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        }
        return new Dimension(config.core().getWindowWidth(), config.core().getWindowHeight());
    }

    /**
     * Displays the main menu screen.
     */
    private void showMainMenu() {
        rootPane.getChildren().clear();

        if (mainMenuPanel == null) {
            createMainMenuPanel();
        }

        rootPane.getChildren().add(mainMenuPanel);
        mainMenuPanel.toFront();
    }

    /**
     * Creates and configures the main menu panel.
     */
    private void createMainMenuPanel() {
        mainMenuPanel = new MainMenuPanel();

        // Size binding (35% width, 45% height of root pane)
        mainMenuPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.35));
        mainMenuPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(0.45));

        // Action bindings
        mainMenuPanel.setOnNewGameAction(this::showMapSelectionMenu);
        mainMenuPanel.setOnLoadGameAction(() -> System.out.println("Load game not implemented"));
        mainMenuPanel.setOnExitAction(primaryStage::close);
    }

    /**
     * Displays the map selection menu.
     */
    private void showMapSelectionMenu() {
        rootPane.getChildren().clear();

        if (mapSelectionPanel == null) {
            createMapSelectionPanel();
        }

        rootPane.getChildren().add(mapSelectionPanel);
        mapSelectionPanel.toFront();
    }

    /**
     * Creates and configures the map selection panel.
     */
    private void createMapSelectionPanel() {
        mapSelectionPanel = new MapSelectionPanel();

        // Size binding (60% width, 70% height of root pane)
        mapSelectionPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.6));
        mapSelectionPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(0.7));
        mapSelectionPanel.minWidthProperty().bind(mapSelectionPanel.prefWidthProperty());
        mapSelectionPanel.minHeightProperty().bind(mapSelectionPanel.prefHeightProperty());

        // Action bindings
        mapSelectionPanel.setOnMapSelected(this::startGame);
        mapSelectionPanel.setOnBackAction(this::showMainMenu);
    }

    /**
     * Starts a new game with the selected map type.
     *
     * @param selectedMapType Map type to generate
     */
    private void startGame(MapType selectedMapType) {
        rootPane.getChildren().clear();

        // Initialize game systems
        initializeGameStateManager();
        initializeGameComponents();
        initializeGameOrchestrator();

        // Start game
        gameOrchestrator.startNewGame(selectedMapType);
    }

    /**
     * Initializes game state manager if needed.
     */
    private void initializeGameStateManager() {
        if (gameStateManager == null) {
            gameStateManager = new GameStateManager();
        }
    }

    /**
     * Initializes core game components (canvas, layers, UI).
     */
    private void initializeGameComponents() {
        if (mainCanvas == null) {
            createCoreRenderingComponents();
        }

        rootPane.getChildren().addAll(gameLayer, uiLayer);
    }

    /**
     * Creates core rendering components and UI layers.
     */
    private void createCoreRenderingComponents() {
        AppConfig config = AppConfig.getInstance();
        double width = rootPane.getPrefWidth();
        double height = rootPane.getPrefHeight();

        // Create main rendering canvas
        mainCanvas = new Canvas(width, height);

        // Create camera system
        camera = new FxCamera(width / 2, height / 2, config.core().getInitialZoom());

        // Create renderers
        renderer = new FxRenderer(mainCanvas.getGraphicsContext2D(), camera, gameStateManager);
        createMinimap(config);

        // Build UI layers
        gameLayer = createGameLayer();
        uiLayer = createUILayer(config);
    }

    /**
     * Creates and configures the minimap component.
     */
    private void createMinimap(AppConfig config) {
        Canvas minimapCanvas = new Canvas(
                config.graphics().getMinimapWidth(),
                config.graphics().getMinimapHeight()
        );
        minimapCanvas.setTranslateX(rootPane.getPrefWidth() - minimapCanvas.getWidth() - 12);
        minimapCanvas.setTranslateY(rootPane.getPrefHeight() - minimapCanvas.getHeight() - 12);
        minimapDrawer = new FxMinimapDrawer(minimapCanvas, camera, mainCanvas);
    }

    /**
     * Creates the game layer containing rendering canvases.
     */
    private Pane createGameLayer() {
        Pane layer = new Pane(mainCanvas, minimapDrawer.getCanvas());
        layer.setPrefSize(rootPane.getPrefWidth(), rootPane.getPrefHeight());
        return layer;
    }

    /**
     * Creates the UI layer with game interface components.
     */
    private Pane createUILayer(AppConfig config) {
        Pane layer = new Pane();
        layer.setPickOnBounds(false);

        // Create UI components
        createUIConfigPanels(config);
        createPlayerUIComponents();

        // Add components to layer
        layer.getChildren().addAll(mapConfigPanel, healthBar, inventoryPanel, hotbarPanel);
        return layer;
    }

    /**
     * Creates configuration panels.
     */
    private void createUIConfigPanels(AppConfig config) {
        mapConfigPanel = new MapConfigPanel(
                config.world().getCurrentMapType(),
                config.world().getDefaultSeed()
        );
        mapConfigPanel.setVisible(false);
        mapConfigPanel.setManaged(false);
    }

    /**
     * Creates player-related UI components.
     */
    private void createPlayerUIComponents() {
        // Health bar (bottom-left)
        healthBar = new HealthBar();
        healthBar.setLayoutX(12);
        healthBar.layoutYProperty().bind(
                rootPane.heightProperty().subtract(healthBar.heightProperty()).subtract(12)
        );

        // Inventory panel (centered)
        inventoryPanel = new InventoryPanel(gameStateManager);
        inventoryPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(inventoryPanel.widthProperty()).divide(2)
        );
        inventoryPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(inventoryPanel.heightProperty()).divide(2)
        );
        inventoryPanel.setVisible(false);
        inventoryPanel.setManaged(false);

        // Hotbar panel (bottom-center)
        hotbarPanel = new BuildingHotbarPanel(recipeManager);
        hotbarPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(hotbarPanel.widthProperty()).divide(2)
        );
        hotbarPanel.setLayoutY(rootPane.getHeight() - hotbarPanel.getHeight() - 82);
    }

    /**
     * Initializes the game orchestrator and pause menu.
     */
    private void initializeGameOrchestrator() {
        if (gameOrchestrator == null) {
            // Create input handler
            FxInputHandler inputHandler = new FxInputHandler(
                    primaryStage.getScene(),
                    mainCanvas,
                    camera
            );

            // Create orchestrator
            gameOrchestrator = new GameOrchestrator(
                    renderer,
                    camera,
                    inputHandler,
                    minimapDrawer,
                    mapConfigPanel,
                    mainCanvas,
                    healthBar,
                    inventoryPanel,
                    hotbarPanel,
                    gameStateManager,
                    recipeManager
            );

            // Configure pause menu
            configurePauseMenu();
        }
    }

    /**
     * Configures and adds the pause menu to the UI.
     */
    private void configurePauseMenu() {
        PauseMenuPanel pauseMenuPanel = new PauseMenuPanel();

        // Center positioning
        pauseMenuPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(pauseMenuPanel.widthProperty()).divide(2)
        );
        pauseMenuPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(pauseMenuPanel.heightProperty()).divide(2)
        );

        // Initial state
        pauseMenuPanel.setVisible(false);
        pauseMenuPanel.setManaged(false);

        // Action bindings
        pauseMenuPanel.setOnExitAction(primaryStage::close);
        pauseMenuPanel.setOnBackToMainMenuAction(() -> {
            gameOrchestrator.stopGameLoop();
            showMainMenu();
        });

        // Add to UI and link to orchestrator
        uiLayer.getChildren().add(pauseMenuPanel);
        pauseMenuPanel.toFront();
        gameOrchestrator.setPauseMenuPanel(pauseMenuPanel);
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}