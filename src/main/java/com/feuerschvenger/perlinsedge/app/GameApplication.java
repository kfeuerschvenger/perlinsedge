package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.ui.view.UiTheme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.awt.Dimension;

/**
 * Main JavaFX application class for Perlin's Edge game.
 * Manages application lifecycle, the window and the move from the menus to a running game.
 * Start the JVM with {@code -Dperlinsedge.debug=true} to enable verbose JavaFX and Prism logging.
 */
@Slf4j
public class GameApplication extends Application {

    private Stage primaryStage;
    private StackPane rootPane;
    private MenuNavigator menus;

    // Game systems, built the first time a game starts
    private GameScreen gameScreen;
    private GameOrchestrator gameOrchestrator;
    private GameStateManager gameStateManager;
    private final RecipeManager recipeManager = new RecipeManager();

    @Override
    public void start(Stage primaryStage) {
        configureDebugProperties();
        initializeRootPane();
        initializePrimaryStage(primaryStage);
        menus = new MenuNavigator(rootPane, this::startGame, primaryStage::close);
        menus.showMainMenu();
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
     * Starts a new game with the selected map type. The screen and the orchestrator are built the first time and
     * reused by every game after it.
     *
     * @param selectedMapType Map type to generate
     */
    private void startGame(MapType selectedMapType) {
        rootPane.getChildren().clear();

        if (gameStateManager == null) {
            gameStateManager = new GameStateManager();
        }
        if (gameScreen == null) {
            gameScreen = new GameScreen(rootPane, gameStateManager, recipeManager);
        }
        gameScreen.show();
        if (gameOrchestrator == null) {
            gameOrchestrator = gameScreen.createOrchestrator(primaryStage.getScene(), primaryStage::close,
                    menus::showMainMenu);
        }

        gameOrchestrator.startNewGame(selectedMapType);
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }

}
