package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxRenderer;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.view.BuildingCostPanel;
import com.feuerschvenger.perlinsedge.ui.view.BuildingPalettePanel;
import com.feuerschvenger.perlinsedge.ui.view.DeathOverlay;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.HintLabel;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.ItemHotbarPanel;
import com.feuerschvenger.perlinsedge.ui.view.MainMenuPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapSelectionPanel;
import com.feuerschvenger.perlinsedge.ui.view.MinimapFrame;
import com.feuerschvenger.perlinsedge.ui.view.PauseMenuPanel;
import com.feuerschvenger.perlinsedge.ui.view.UiTheme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.awt.Dimension;

/**
 * Main JavaFX application class for Perlin's Edge game.
 * Manages application lifecycle, UI layers, and game initialization.
 * Start the JVM with {@code -Dperlinsedge.debug=true} to enable verbose JavaFX and Prism logging.
 */
@Slf4j
public class GameApplication extends Application {
    /** Margin between the HUD and the window edge, in whole pixels. */
    private static final double HUD_MARGIN = 12;
    /** Gap between the hearts and the hotbar below them. */
    private static final double HUD_GAP = 6;
    /** Margin between the minimap and the window corner, in whole pixels. */
    private static final double MINIMAP_MARGIN = 12;
    /** Divisor that turns a free span into the offset that centres a node. */
    private static final double CENTER_DIVISOR = 2;
    /** Main menu size as a fraction of the root pane. */
    private static final double MAIN_MENU_WIDTH_RATIO = 0.35;
    private static final double MAIN_MENU_HEIGHT_RATIO = 0.45;
    /** Map selection panel size as a fraction of the root pane. */
    private static final double MAP_SELECTION_WIDTH_RATIO = 0.6;
    private static final double MAP_SELECTION_HEIGHT_RATIO = 0.7;
    /** Vertical position of the hint line as a fraction of the root pane height. */
    private static final double HINT_Y_RATIO = 0.7;

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
    private MinimapFrame minimapFrame;

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
    private BuildingPalettePanel palettePanel;
    private BuildingCostPanel buildingCostPanel;
    private ItemHotbarPanel itemHotbarPanel;
    private HintLabel hintLabel;
    private DeathOverlay deathOverlay;

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
        if (Boolean.getBoolean("perlinsedge.debug")) {
            System.setProperty("javafx.verbose", "true");
            System.setProperty("prism.verbose", "true");
            log.info("Verbose JavaFX and Prism logging enabled");
        }
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
        var scene = new Scene(rootPane);
        UiTheme.install(scene);
        primaryStage.setTitle("Perlin's Edge");
        primaryStage.setScene(scene);

        // Configure fullscreen settings
        var config = AppConfig.getInstance();
        primaryStage.setFullScreen(config.core().isFullscreen());
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.show();
    }

    /**
     * Initializes the root application pane with proper dimensions.
     */
    private void initializeRootPane() {
        var config = AppConfig.getInstance();
        var screenSize = getScreenDimensions(config);

        rootPane = new StackPane();
        rootPane.setPrefSize(screenSize.getWidth(), screenSize.getHeight());
        rootPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
    }

    /**
     * Gets screen dimensions based on fullscreen configuration.
     */
    private static Dimension getScreenDimensions(AppConfig config) {
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
        mainMenuPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(MAIN_MENU_WIDTH_RATIO));
        mainMenuPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(MAIN_MENU_HEIGHT_RATIO));

        // Action bindings
        mainMenuPanel.setOnNewGameAction(this::showMapSelectionMenu);
        mainMenuPanel.setOnLoadGameAction(() -> log.warn("Load game not implemented"));
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
        mapSelectionPanel.prefWidthProperty().bind(rootPane.widthProperty().multiply(MAP_SELECTION_WIDTH_RATIO));
        mapSelectionPanel.prefHeightProperty().bind(rootPane.heightProperty().multiply(MAP_SELECTION_HEIGHT_RATIO));
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
        var config = AppConfig.getInstance();
        double width = rootPane.getPrefWidth();
        double height = rootPane.getPrefHeight();

        // Create main rendering canvas
        mainCanvas = new Canvas(width, height);

        // Create camera system
        camera = new FxCamera(width / CENTER_DIVISOR, height / CENTER_DIVISOR, config.core().getInitialZoom());

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
        var minimapCanvas = new Canvas(
                config.graphics().getMinimapWidth(),
                config.graphics().getMinimapHeight()
        );
        minimapFrame = new MinimapFrame(minimapCanvas);
        minimapFrame.setTranslateX(rootPane.getPrefWidth() - MinimapFrame.outerWidth(minimapCanvas) - MINIMAP_MARGIN);
        minimapFrame.setTranslateY(rootPane.getPrefHeight() - MinimapFrame.outerHeight(minimapCanvas) - MINIMAP_MARGIN);
        minimapDrawer = new FxMinimapDrawer(minimapCanvas, camera, mainCanvas);
    }

    /**
     * Creates the game layer containing rendering canvases.
     */
    private Pane createGameLayer() {
        var layer = new Pane(mainCanvas, minimapFrame);
        layer.setPrefSize(rootPane.getPrefWidth(), rootPane.getPrefHeight());
        return layer;
    }

    /**
     * Creates the UI layer with game interface components.
     */
    private Pane createUILayer(AppConfig config) {
        var layer = new Pane();
        layer.setPickOnBounds(false);

        // Create UI components
        createUIConfigPanels(config);
        createPlayerUIComponents();

        // Add components to layer
        // The inventory comes after the hotbars so the stack dragged out of it is drawn over their slots
        layer.getChildren().addAll(mapConfigPanel, healthBar, itemHotbarPanel, palettePanel,
                buildingCostPanel, inventoryPanel, hintLabel, deathOverlay);
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
        // Item hotbar (bottom-center, a fixed margin above the bottom edge; the UI layer ignores the world zoom)
        itemHotbarPanel = new ItemHotbarPanel();
        itemHotbarPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(itemHotbarPanel.widthProperty()).divide(CENTER_DIVISOR)
        );
        itemHotbarPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(itemHotbarPanel.heightProperty()).subtract(HUD_MARGIN)
        );

        // Hearts: above the hotbar, aligned to its left edge
        healthBar = new HealthBar();
        healthBar.layoutXProperty().bind(itemHotbarPanel.layoutXProperty());
        healthBar.layoutYProperty().bind(
                itemHotbarPanel.layoutYProperty().subtract(healthBar.heightProperty()).subtract(HUD_GAP)
        );

        // Building palette: centred above the hearts, shown only while the hammer is in hand
        palettePanel = new BuildingPalettePanel(recipeManager);
        palettePanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(palettePanel.widthProperty()).divide(CENTER_DIVISOR)
        );
        palettePanel.layoutYProperty().bind(
                healthBar.layoutYProperty().subtract(palettePanel.heightProperty()).subtract(HUD_GAP)
        );

        // What the selected piece will consume: the bottom left corner, clear of the centred hotbar and of the minimap
        // in the bottom right corner. The palette shows, hides and refreshes it.
        buildingCostPanel = new BuildingCostPanel(recipeManager);
        buildingCostPanel.setLayoutX(HUD_MARGIN);
        buildingCostPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(buildingCostPanel.heightProperty()).subtract(HUD_MARGIN)
        );
        palettePanel.setCostPanel(buildingCostPanel);

        // Inventory panel (centered)
        inventoryPanel = new InventoryPanel(gameStateManager);
        inventoryPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(inventoryPanel.widthProperty()).divide(CENTER_DIVISOR)
        );
        inventoryPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(inventoryPanel.heightProperty()).divide(CENTER_DIVISOR)
        );
        inventoryPanel.setVisible(false);
        inventoryPanel.setManaged(false);
        // The hotbar stays on screen and takes part in the dragging while the inventory is open
        inventoryPanel.attachHotbar(itemHotbarPanel);

        // Hint line (centered, above the hotbar)
        hintLabel = new HintLabel();
        hintLabel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(hintLabel.widthProperty()).divide(CENTER_DIVISOR)
        );
        hintLabel.layoutYProperty().bind(rootPane.heightProperty().multiply(HINT_Y_RATIO));

        // Death screen (full screen, above the other panels)
        deathOverlay = new DeathOverlay();
        deathOverlay.prefWidthProperty().bind(rootPane.widthProperty());
        deathOverlay.prefHeightProperty().bind(rootPane.heightProperty());
    }

    /**
     * Initializes the game orchestrator and pause menu.
     */
    private void initializeGameOrchestrator() {
        if (gameOrchestrator == null) {
            // Create input handler
            var inputHandler = new FxInputHandler(
                    primaryStage.getScene(),
                    mainCanvas,
                    camera
            );

            // Create orchestrator
            gameOrchestrator = new GameOrchestrator(
                    new GameView(renderer, camera, inputHandler, minimapDrawer, mainCanvas),
                    new GamePanels(mapConfigPanel, healthBar, inventoryPanel, palettePanel, itemHotbarPanel,
                            hintLabel, deathOverlay),
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
        var pauseMenuPanel = new PauseMenuPanel();

        // Center positioning
        pauseMenuPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(pauseMenuPanel.widthProperty()).divide(CENTER_DIVISOR)
        );
        pauseMenuPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(pauseMenuPanel.heightProperty()).divide(CENTER_DIVISOR)
        );

        // Initial state
        pauseMenuPanel.setVisible(false);
        pauseMenuPanel.setManaged(false);

        // Action bindings
        pauseMenuPanel.setOnExitAction(primaryStage::close);
        pauseMenuPanel.setOnBackToMainMenuAction(() -> {
            gameOrchestrator.leaveGame();
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
