package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
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
import com.feuerschvenger.perlinsedge.ui.view.MinimapFrame;
import com.feuerschvenger.perlinsedge.ui.view.PauseMenuPanel;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * The screen of a running game: the canvas the world is drawn on with the minimap over it, and the layer of panels
 * above it. The positions follow the root pane through bindings, so they track a window that is resized.
 */
final class GameScreen {
    /** Margin between the HUD and the window edge, in whole pixels. */
    private static final double HUD_MARGIN = 12;
    /** Gap between the hearts and the hotbar below them. */
    private static final double HUD_GAP = 6;
    /** Margin between the minimap and the window corner, in whole pixels. */
    private static final double MINIMAP_MARGIN = 12;
    /** Divisor that turns a free span into the offset that centres a node. */
    private static final double CENTER_DIVISOR = 2;
    /** Vertical position of the hint line as a fraction of the root pane height. */
    private static final double HINT_Y_RATIO = 0.7;

    private final StackPane rootPane;
    private final GameStateManager gameStateManager;
    private final RecipeManager recipeManager;

    private final Canvas mainCanvas;
    private final FxCamera camera;
    private final FxRenderer renderer;
    private final FxMinimapDrawer minimapDrawer;
    private final Pane gameLayer;
    private final Pane uiLayer;

    private HealthBar healthBar;
    private InventoryPanel inventoryPanel;
    private BuildingPalettePanel palettePanel;
    private BuildingCostPanel buildingCostPanel;
    private ItemHotbarPanel itemHotbarPanel;
    private HintLabel hintLabel;
    private DeathOverlay deathOverlay;

    /**
     * Builds the canvas, the minimap and every panel of the game.
     *
     * @param rootPane         the pane the screen is shown on, which sets the size
     * @param gameStateManager the state the panels and the renderer read
     * @param recipeManager    the recipes the building panels list
     */
    GameScreen(StackPane rootPane, GameStateManager gameStateManager, RecipeManager recipeManager) {
        this.rootPane = rootPane;
        this.gameStateManager = gameStateManager;
        this.recipeManager = recipeManager;

        final var config = AppConfig.getInstance();
        final double width = rootPane.getPrefWidth();
        final double height = rootPane.getPrefHeight();

        mainCanvas = new Canvas(width, height);
        camera = new FxCamera(width / CENTER_DIVISOR, height / CENTER_DIVISOR, config.core().getInitialZoom());
        renderer = new FxRenderer(mainCanvas.getGraphicsContext2D(), camera, gameStateManager);

        final var minimapCanvas = new Canvas(config.graphics().getMinimapWidth(), config.graphics().getMinimapHeight());
        final var minimapFrame = new MinimapFrame(minimapCanvas);
        minimapFrame.setTranslateX(width - MinimapFrame.outerWidth(minimapCanvas) - MINIMAP_MARGIN);
        minimapFrame.setTranslateY(height - MinimapFrame.outerHeight(minimapCanvas) - MINIMAP_MARGIN);
        minimapDrawer = new FxMinimapDrawer(minimapCanvas, camera, mainCanvas);

        gameLayer = new Pane(mainCanvas, minimapFrame);
        gameLayer.setPrefSize(width, height);
        uiLayer = createUiLayer();
    }

    /** Puts the world layer and the panel layer on the root pane. */
    void show() {
        rootPane.getChildren().addAll(gameLayer, uiLayer);
    }

    /**
     * Builds the orchestrator that runs the game on this screen, together with the pause menu.
     *
     * @param scene        the scene the input is read from
     * @param onExit       called when the player leaves from the pause menu to the desktop
     * @param onBackToMenu called after the game is left from the pause menu, to show the main menu
     * @param options      the options of the launch, which decide whether the debug keys work
     * @return the orchestrator
     */
    GameOrchestrator createOrchestrator(Scene scene, Runnable onExit, Runnable onBackToMenu, LaunchOptions options) {
        final var inputHandler = new FxInputHandler(scene, mainCanvas, camera);

        final var orchestrator = new GameOrchestrator(
                new GameView(renderer, camera, inputHandler, minimapDrawer, mainCanvas),
                new GamePanels(healthBar, inventoryPanel, palettePanel, itemHotbarPanel, hintLabel,
                        deathOverlay),
                gameStateManager,
                recipeManager,
                options
        );
        configurePauseMenu(orchestrator, onExit, onBackToMenu);
        return orchestrator;
    }

    private Pane createUiLayer() {
        final var layer = new Pane();
        layer.setPickOnBounds(false);

        createPlayerUiComponents();

        layer.getChildren().addAll(healthBar, itemHotbarPanel, palettePanel,
                buildingCostPanel, inventoryPanel, hintLabel, deathOverlay);
        return layer;
    }

    private void createPlayerUiComponents() {
        itemHotbarPanel = new ItemHotbarPanel();
        itemHotbarPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(itemHotbarPanel.widthProperty()).divide(CENTER_DIVISOR));
        itemHotbarPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(itemHotbarPanel.heightProperty()).subtract(HUD_MARGIN));

        healthBar = new HealthBar();
        healthBar.layoutXProperty().bind(itemHotbarPanel.layoutXProperty());
        healthBar.layoutYProperty().bind(
                itemHotbarPanel.layoutYProperty().subtract(healthBar.heightProperty()).subtract(HUD_GAP));

        palettePanel = new BuildingPalettePanel(recipeManager);
        palettePanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(palettePanel.widthProperty()).divide(CENTER_DIVISOR));
        palettePanel.layoutYProperty().bind(
                healthBar.layoutYProperty().subtract(palettePanel.heightProperty()).subtract(HUD_GAP));

        buildingCostPanel = new BuildingCostPanel(recipeManager);
        buildingCostPanel.setLayoutX(HUD_MARGIN);
        buildingCostPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(buildingCostPanel.heightProperty()).subtract(HUD_MARGIN));
        palettePanel.setCostPanel(buildingCostPanel);

        inventoryPanel = new InventoryPanel(gameStateManager);
        inventoryPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(inventoryPanel.widthProperty()).divide(CENTER_DIVISOR));
        inventoryPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(inventoryPanel.heightProperty()).divide(CENTER_DIVISOR));
        inventoryPanel.setVisible(false);
        inventoryPanel.setManaged(false);
        inventoryPanel.attachHotbar(itemHotbarPanel);

        hintLabel = new HintLabel();
        hintLabel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(hintLabel.widthProperty()).divide(CENTER_DIVISOR));
        hintLabel.layoutYProperty().bind(rootPane.heightProperty().multiply(HINT_Y_RATIO));

        deathOverlay = new DeathOverlay();
        deathOverlay.prefWidthProperty().bind(rootPane.widthProperty());
        deathOverlay.prefHeightProperty().bind(rootPane.heightProperty());
    }

    private void configurePauseMenu(GameOrchestrator orchestrator, Runnable onExit, Runnable onBackToMenu) {
        final var pauseMenuPanel = new PauseMenuPanel();

        pauseMenuPanel.layoutXProperty().bind(
                rootPane.widthProperty().subtract(pauseMenuPanel.widthProperty()).divide(CENTER_DIVISOR));
        pauseMenuPanel.layoutYProperty().bind(
                rootPane.heightProperty().subtract(pauseMenuPanel.heightProperty()).divide(CENTER_DIVISOR));

        pauseMenuPanel.setVisible(false);
        pauseMenuPanel.setManaged(false);

        pauseMenuPanel.setOnExitAction(onExit);
        pauseMenuPanel.setOnBackToMainMenuAction(() -> {
            orchestrator.leaveGame();
            onBackToMenu.run();
        });

        uiLayer.getChildren().add(pauseMenuPanel);
        pauseMenuPanel.toFront();
        orchestrator.setPauseMenuPanel(pauseMenuPanel);
    }
}
