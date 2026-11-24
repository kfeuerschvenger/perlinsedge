package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxRenderer;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.view.BuildingPalettePanel;
import com.feuerschvenger.perlinsedge.ui.view.DeathOverlay;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.HintLabel;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.ItemHotbarPanel;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives a real key press through the scene of an orchestrator built with each set of launch options, to prove the
 * launch flag reaches the key controller.
 */
class GameOrchestratorDebugKeysTest {

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;
    private static final double CAMERA_CENTRE_X = 400;
    private static final double CAMERA_CENTRE_Y = 300;

    private GameOrchestrator orchestrator;
    private Scene scene;

    @AfterEach
    void tearDown() {
        if (orchestrator != null) {
            FxToolkitSupport.onFx(() -> {
                orchestrator.stopGameLoop();
                return null;
            });
        }
        AppConfig.getInstance().debug().setEnabled(false);
    }

    private void build(LaunchOptions options) {
        final var config = AppConfig.getInstance();
        final var recipes = new RecipeManager();
        final var state = new GameStateManager();
        orchestrator = FxToolkitSupport.onFx(() -> {
            final var canvas = new Canvas(VIEW_WIDTH, VIEW_HEIGHT);
            scene = new Scene(new Pane(canvas), VIEW_WIDTH, VIEW_HEIGHT);
            final var camera = new FxCamera(CAMERA_CENTRE_X, CAMERA_CENTRE_Y, config.core().getInitialZoom());
            final var minimapCanvas = new Canvas(config.graphics().getMinimapWidth(),
                    config.graphics().getMinimapHeight());
            return new GameOrchestrator(
                    new GameView(new FxRenderer(canvas.getGraphicsContext2D(), camera, state), camera,
                            new FxInputHandler(scene, canvas, camera),
                            new FxMinimapDrawer(minimapCanvas, camera, canvas), canvas),
                    new GamePanels(new HealthBar(), new InventoryPanel(state), new BuildingPalettePanel(recipes),
                            new ItemHotbarPanel(), new HintLabel(), new DeathOverlay()),
                    state,
                    recipes,
                    options);
        });
        FxToolkitSupport.onFx(() -> {
            orchestrator.startNewGame(MapType.DESERT);
            return null;
        });
    }

    private void pressF1() {
        FxToolkitSupport.onFx(() -> {
            Event.fireEvent(scene.getRoot(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1, false, false, false, false));
            Event.fireEvent(scene.getRoot(), new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.F1, false, false, false, false));
            return null;
        });
    }

    @Test
    void f1TogglesTheDebugModeWhenTheLaunchHasTheDebugFlag() {
        build(new LaunchOptions(true));
        assertFalse(AppConfig.getInstance().debug().isEnabled());

        pressF1();
        assertTrue(AppConfig.getInstance().debug().isEnabled());

        pressF1();
        assertFalse(AppConfig.getInstance().debug().isEnabled());
    }

    @Test
    void f1DoesNothingWithTheDefaultLaunchOptions() {
        build(LaunchOptions.DEFAULT);

        pressF1();

        assertFalse(AppConfig.getInstance().debug().isEnabled());
    }

    @Test
    void anOrchestratorWithoutLaunchOptionsIsRefused() {
        final var thrown = assertThrows(IllegalStateException.class, () -> build(null));

        assertInstanceOf(NullPointerException.class, thrown.getCause());
        assertTrue(thrown.getCause().getMessage().contains("options"));
    }
}
