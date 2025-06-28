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
 * Main application class for the Perlin's Edge game.
 * Initializes the game window, UI layers, and manages game state transitions.
 */
public class GameApplication extends Application {

    private double screenWidth, screenHeight;

    private Stage primaryStage;
    private StackPane rootPane;
    private Pane gameLayer;
    private Pane uiLayer;
    private Canvas mainCanvas;
    private FxCamera camera;
    private FxRenderer renderer;
    private FxMinimapDrawer minimapDrawer;
    private GameOrchestrator gameOrchestrator;

    private MainMenuPanel mainMenuPanel;
    private MapSelectionPanel mapSelectionPanel;
    private HealthBar healthBar;
    private InventoryPanel inventoryPanel;
    private MapConfigPanel mapConfigPanel;
    private BuildingHotbarPanel hotbarPanel;

    private GameStateManager gameStateManager;
    private RecipeManager recipeManager = new RecipeManager();

    @Override
    public void start(Stage primaryStage) {

        System.setProperty("javafx.verbose", "true");
        System.setProperty("prism.verbose", "true");
        System.setProperty("prism.dirtyopts", "true");

        this.primaryStage = primaryStage;
        AppConfig config = AppConfig.getInstance();


        if (config.core().isFullscreen()) {
            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            screenWidth = screenSize.getWidth();
            screenHeight = screenSize.getHeight();
        } else {
            screenWidth = config.core().getWindowWidth();
            screenHeight = config.core().getWindowHeight();
        }

        // Initialize main root pane
        rootPane = new StackPane();
        rootPane.setPrefSize(screenWidth, screenHeight);
        rootPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));

        // Configure primary stage
        Scene scene = new Scene(rootPane);
        primaryStage.setTitle("Perlin's Edge");
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(config.core().isFullscreen());
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.show();

        // Start with main menu
        showMainMenu();
    }

    /**
     * Displays the main menu screen and hides other layers.
     */
    private void showMainMenu() {
        rootPane.getChildren().clear();

        // Initialize main menu if needed
        if (mainMenuPanel == null) {
            mainMenuPanel = new MainMenuPanel();
            // Bind the preferred size of the main menu panel to a percentage of the root's size
            mainMenuPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.35)); // e.g., 35% of root width
            mainMenuPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(0.45)); // e.g., 45% of root height
            mainMenuPanel.setOnNewGameAction(this::showMapSelectionMenu);
            mainMenuPanel.setOnLoadGameAction(() -> System.out.println("Load game not implemented yet."));
            mainMenuPanel.setOnExitAction(primaryStage::close);
        }

        rootPane.getChildren().add(mainMenuPanel);
        mainMenuPanel.toFront();
    }

    /**
     * Displays the map selection menu screen.
     */
    private void showMapSelectionMenu() {
        rootPane.getChildren().clear();

        // Initialize map selection menu if needed
        if (mapSelectionPanel == null) {
            mapSelectionPanel = new MapSelectionPanel();
            mapSelectionPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.6));
            mapSelectionPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(0.7));
            mapSelectionPanel.minWidthProperty().bind(mapSelectionPanel.prefWidthProperty());
            mapSelectionPanel.minHeightProperty().bind(mapSelectionPanel.prefHeightProperty());
            mapSelectionPanel.setOnMapSelected(this::startGame);
            mapSelectionPanel.setOnBackAction(this::showMainMenu);
        }

        rootPane.getChildren().add(mapSelectionPanel);
        mapSelectionPanel.toFront();
    }

    /**
     * Starts a new game with the selected map type.
     *
     * @param selectedMapType The map type selected by the player
     */
    private void startGame(MapType selectedMapType) {
        rootPane.getChildren().clear();
        AppConfig config = AppConfig.getInstance();
        double width = rootPane.getPrefWidth();
        double height = rootPane.getPrefHeight();

        if (gameStateManager == null) { // Only create once for the application lifecycle
            gameStateManager = new GameStateManager(); // Will be properly initialized in GameOrchestrator
            gameStateManager.setScreenSize(screenWidth, screenHeight);
        }

        // Initialize game components if not already created
        if (mainCanvas == null) {
            initializeGameComponents(width, height, config);
        } else {
            // Re-add existing components
            rootPane.getChildren().addAll(gameLayer, uiLayer);
        }

        // Initialize game orchestrator if needed
        if (gameOrchestrator == null) {
            initializeGameOrchestrator();
        }

        gameOrchestrator.startNewGame(selectedMapType);
    }

    /**
     * Initializes core game components (canvas, camera, renderer, etc.)
     *
     * @param width Preferred width of the game area
     * @param height Preferred height of the game area
     * @param config Application configuration instance
     */
    private void initializeGameComponents(double width, double height, AppConfig config) {
        mainCanvas = new Canvas(width, height);

        // Initialize minimap canvas
        Canvas minimapCanvas = new Canvas(
                config.graphics().getMinimapWidth(),
                config.graphics().getMinimapHeight()
        );
        minimapCanvas.setTranslateX(width - minimapCanvas.getWidth() - 12);
        minimapCanvas.setTranslateY(height - minimapCanvas.getHeight() - 12);

        // Initialize camera centered on screen with initial zoom
        camera = new FxCamera(width / 2, height / 2, config.core().getInitialZoom());

        // Initialize renderer and minimap
        renderer = new FxRenderer(mainCanvas.getGraphicsContext2D(), camera, gameStateManager);
        minimapDrawer = new FxMinimapDrawer(minimapCanvas, camera, mainCanvas);

        // Create game layer containing canvases
        gameLayer = new Pane(mainCanvas, minimapCanvas);
        gameLayer.setPrefSize(width, height);

        // Create UI layer
        uiLayer = createUILayer(config);
        uiLayer.setPrefSize(width, height);

        // Add layers to root pane
        rootPane.getChildren().addAll(gameLayer, uiLayer);
    }

    /**
     * Creates the UI layer containing all interface elements.
     *
     * @param config Application configuration instance
     * @return Configured UI layer pane
     */
    private Pane createUILayer(AppConfig config) {
        Pane uiLayer = new Pane();
        uiLayer.setPickOnBounds(false);

        mapConfigPanel = new MapConfigPanel(
                config.world().getCurrentMapType(),
                config.world().getDefaultSeed()
        );
        mapConfigPanel.setVisible(false);
        mapConfigPanel.setManaged(false);

        healthBar = new HealthBar();
        healthBar.setLayoutX(12);
        healthBar.layoutYProperty().bind(
                mainCanvas.heightProperty().subtract(healthBar.heightProperty()).subtract(12)
        );

        inventoryPanel = new InventoryPanel(gameStateManager);
        inventoryPanel.layoutXProperty().bind(
                mainCanvas.widthProperty().subtract(inventoryPanel.widthProperty()).divide(2)
        );
        inventoryPanel.layoutYProperty().bind(
                mainCanvas.heightProperty().subtract(inventoryPanel.heightProperty()).divide(2)
        );
        inventoryPanel.setVisible(false);
        inventoryPanel.setManaged(false);

        hotbarPanel = new BuildingHotbarPanel(recipeManager);

        hotbarPanel.layoutXProperty().bind(
                mainCanvas.widthProperty().subtract(hotbarPanel.widthProperty()).divide(2)
        );
        hotbarPanel.setLayoutY(mainCanvas.getHeight() - hotbarPanel.getHeight() - 82);

        uiLayer.getChildren().addAll(mapConfigPanel, healthBar, inventoryPanel, hotbarPanel);
        return uiLayer;
    }

    /**
     * Initializes the game orchestrator and pause menu.
     */
    private void initializeGameOrchestrator() {
        FxInputHandler inputHandler = new FxInputHandler(
                primaryStage.getScene(),
                mainCanvas,
                camera
        );

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

        setupPauseMenu();
    }

    /**
     * Configures the pause menu panel and links it to the orchestrator.
     */
    private void setupPauseMenu() {
        PauseMenuPanel pauseMenuPanel = new PauseMenuPanel();

        // Center pause menu on screen
        pauseMenuPanel.layoutXProperty().bind(
                mainCanvas.widthProperty()
                        .subtract(pauseMenuPanel.widthProperty())
                        .divide(2)
        );
        pauseMenuPanel.layoutYProperty().bind(
                mainCanvas.heightProperty()
                        .subtract(pauseMenuPanel.heightProperty())
                        .divide(2)
        );

        pauseMenuPanel.setVisible(false);
        pauseMenuPanel.setManaged(false);

        // Configure menu actions
        pauseMenuPanel.setOnExitAction(primaryStage::close);
        pauseMenuPanel.setOnBackToMainMenuAction(() -> {
            gameOrchestrator.stopGameLoop();
            showMainMenu();
        });

        // Add to UI layer and link to orchestrator
        uiLayer.getChildren().add(pauseMenuPanel);
        pauseMenuPanel.toFront();
        gameOrchestrator.setPauseMenuPanel(pauseMenuPanel);
    }

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}