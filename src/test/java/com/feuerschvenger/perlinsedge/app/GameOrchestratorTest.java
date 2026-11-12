package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxRenderer;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.view.BuildingHotbarPanel;
import com.feuerschvenger.perlinsedge.ui.view.DeathOverlay;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import com.feuerschvenger.perlinsedge.ui.view.HealthBar;
import com.feuerschvenger.perlinsedge.ui.view.HintLabel;
import com.feuerschvenger.perlinsedge.ui.view.InventoryPanel;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the orchestrator on the JavaFX thread with the real views, the way the application builds it.
 */
class GameOrchestratorTest {

    private GameStateManager state;
    private GameOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        AppConfig config = AppConfig.getInstance();
        RecipeManager recipes = new RecipeManager();
        state = new GameStateManager();
        orchestrator = FxToolkitSupport.onFx(() -> {
            Canvas canvas = new Canvas(800, 600);
            new Scene(new Pane(canvas), 800, 600);
            FxCamera camera = new FxCamera(400, 300, config.core().getInitialZoom());
            Canvas minimapCanvas = new Canvas(config.graphics().getMinimapWidth(), config.graphics().getMinimapHeight());
            return new GameOrchestrator(
                    new FxRenderer(canvas.getGraphicsContext2D(), camera, state),
                    camera,
                    new FxInputHandler(canvas.getScene(), canvas, camera),
                    new FxMinimapDrawer(minimapCanvas, camera, canvas),
                    new MapConfigPanel(MapType.DESERT, 1L),
                    canvas,
                    new HealthBar(),
                    new InventoryPanel(state),
                    new BuildingHotbarPanel(recipes),
                    new HintLabel(),
                    new DeathOverlay(),
                    state,
                    recipes);
        });
    }

    @AfterEach
    void tearDown() {
        FxToolkitSupport.onFx(() -> {
            orchestrator.stopGameLoop();
            return null;
        });
    }

    private void onFx(Runnable action) {
        FxToolkitSupport.onFx(() -> {
            action.run();
            return null;
        });
    }

    @Test
    void noGameIsActiveBeforeOneIsStarted() {
        assertFalse(orchestrator.isGameActive());
    }

    @Test
    void startingAGameMakesItActiveAndStoppingTheLoopMakesItInactive() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        assertTrue(orchestrator.isGameActive());

        onFx(orchestrator::stopGameLoop);
        assertFalse(orchestrator.isGameActive());

        onFx(orchestrator::startGameLoop);
        assertTrue(orchestrator.isGameActive());
    }

    @Test
    void leavingTheGameMakesItInactive() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));

        onFx(orchestrator::leaveGame);

        assertFalse(orchestrator.isGameActive());
    }

    @Test
    void aNewGameCancelsAPendingRespawnAndResetsTheWorldState() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.setPlayerRespawnTimer(3.5);
        state.incrementGameTime(42);
        state.getDroppedItems().add(new DroppedItem(0, 0, new Item(ItemType.STONE, 1), 0, DroppedItem.DropSource.OTHER));

        onFx(() -> orchestrator.startNewGame(MapType.DESERT));

        assertEquals(-1, state.getPlayerRespawnTimer(), "a stale timer would teleport the new player when it expires");
        assertEquals(0, state.getGameTime(), 1e-9);
        assertTrue(state.getDroppedItems().isEmpty());
        assertNotNull(state.getPlayer());
        assertFalse(state.getPlayer().isDead());
    }
}
