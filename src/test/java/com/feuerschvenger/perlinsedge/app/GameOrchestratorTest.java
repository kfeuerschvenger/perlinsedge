package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.ClickFeedback;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the orchestrator on the JavaFX thread with the real views, the way the application builds it.
 */
class GameOrchestratorTest {

    /** Mirrors the generator until told to fail. */
    private static final class SwitchablePanel extends MapConfigPanel {
        boolean failing;

        SwitchablePanel() {
            super(MapType.DESERT, 1L);
        }

        @Override
        public void updateValuesFromGenerator(IMapGenerator generator) {
            if (failing) {
                throw new IllegalStateException("panel failed");
            }
            super.updateValuesFromGenerator(generator);
        }
    }

    /** Draws the minimap until told to fail once. */
    private static final class FailingOnceMinimap extends FxMinimapDrawer {
        boolean failNext;

        FailingOnceMinimap(Canvas minimapCanvas, FxCamera camera, Canvas canvas) {
            super(minimapCanvas, camera, canvas);
        }

        @Override
        public void updateMap(TileMap newMap) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("minimap failed");
            }
            super.updateMap(newMap);
        }
    }

    private GameStateManager state;
    private GameOrchestrator orchestrator;
    private SwitchablePanel panel;
    private FailingOnceMinimap minimap;

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
            panel = new SwitchablePanel();
            minimap = new FailingOnceMinimap(minimapCanvas, camera, canvas);
            return new GameOrchestrator(
                    new FxRenderer(canvas.getGraphicsContext2D(), camera, state),
                    camera,
                    new FxInputHandler(canvas.getScene(), canvas, camera),
                    minimap,
                    panel,
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

    /** The map tuning panel's configuration with the values of a default map, for the given type. */
    private void applyConfig(MapType type) {
        orchestrator.applyMapConfig(type, 7L, 0.01f, 3, 0.01f, 3, 0.01f, 3, 0.05f, 0.5f, 0.05f, 0.5f, 0.3f, 0.3f);
    }

    @Test
    void theMapKeysDoNothingBeforeAGameIsStarted() {
        MapType before = AppConfig.getInstance().world().getCurrentMapType();

        onFx(orchestrator::regenerateMap);
        onFx(() -> applyConfig(MapType.TUNDRA));

        assertNull(state.getCurrentMap(), "no world was built on the main menu");
        assertEquals(before, AppConfig.getInstance().world().getCurrentMapType(),
                "the map type is not changed by a configuration that is ignored");
    }

    @Test
    void regeneratingWhileAGameRunsBuildsANewWorld() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        TileMap first = state.getCurrentMap();

        onFx(orchestrator::regenerateMap);

        assertNotNull(state.getCurrentMap());
        assertNotSame(first, state.getCurrentMap());
        assertNotNull(state.getPlayer());
    }

    @Test
    void regeneratingAfterLeavingTheGameDoesNothing() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        onFx(orchestrator::leaveGame);
        TileMap left = state.getCurrentMap();

        onFx(orchestrator::regenerateMap);

        assertSame(left, state.getCurrentMap());
    }

    @Test
    void regeneratingWhileTheLoopIsStoppedDoesNothingAndResumingAllowsIt() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        TileMap first = state.getCurrentMap();

        onFx(orchestrator::stopGameLoop);
        onFx(orchestrator::regenerateMap);
        assertSame(first, state.getCurrentMap());

        onFx(orchestrator::startGameLoop);
        onFx(orchestrator::regenerateMap);
        assertNotSame(first, state.getCurrentMap());
    }

    @Test
    void aFailedRebuildLeavesTheGameRunningSoTheMapKeysCanTryAgain() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        MapType before = AppConfig.getInstance().world().getCurrentMapType();
        TileMap first = state.getCurrentMap();

        // A null map type cannot be generated, so the rebuild throws after the world was left
        assertThrows(RuntimeException.class, () -> onFx(() -> applyConfig(null)));
        assertEquals(before, AppConfig.getInstance().world().getCurrentMapType(),
                "the map type is still the one that matches the map on screen");
        assertSame(first, state.getCurrentMap(), "the failed generation left the old map in place");

        onFx(orchestrator::regenerateMap);
        assertNotSame(first, state.getCurrentMap(), "the retry rebuilt the world");
    }

    @Test
    void aMapPanelThatFailsDoesNotCostTheRebuild() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        TileMap first = state.getCurrentMap();
        panel.failing = true;

        onFx(() -> applyConfig(MapType.TUNDRA));
        panel.failing = false;

        assertNotSame(first, state.getCurrentMap());
        assertNotNull(state.getPlayer(), "the player lives on the new map");
        assertEquals(MapType.TUNDRA, AppConfig.getInstance().world().getCurrentMapType(),
                "the map type matches the map on screen");
    }

    @Test
    void aFailureAfterTheMapWasReplacedRemovesThePlayerKeepsTheTypeInStepAndTheMapKeysTryAgain() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        TileMap first = state.getCurrentMap();

        assertThrows(IllegalStateException.class, () -> onFx(() -> {
            minimap.failNext = true;
            applyConfig(MapType.TUNDRA);
        }));

        assertNotSame(first, state.getCurrentMap(), "the new map is in place");
        assertNull(state.getPlayer(), "the player of the old map does not roam the new one");
        assertEquals(MapType.TUNDRA, AppConfig.getInstance().world().getCurrentMapType());

        onFx(orchestrator::regenerateMap);
        assertNotNull(state.getPlayer(), "the retry placed a player again");
    }

    @Test
    void aFrameAfterAFailedRebuildIsSkippedAndTheModeStaysExploration() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        assertThrows(IllegalStateException.class, () -> onFx(() -> {
            minimap.failNext = true;
            applyConfig(MapType.TUNDRA);
        }));

        onFx(() -> orchestrator.update(1.0 / 60.0));

        assertNull(state.getPlayer());
        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
    }

    @Test
    void aRebuildClearsTheFlashOfTheOldWorld() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.getClickFeedback().flash(3, 4, true, state.getGameTime());
        assertEquals(ClickFeedback.Kind.ACCEPTED, state.getClickFeedback().kind(state.getGameTime()));

        onFx(orchestrator::regenerateMap);

        assertEquals(ClickFeedback.Kind.NONE, state.getClickFeedback().kind(state.getGameTime()));
    }

    @Test
    void aFrameWithNoPlayerOrNoMapIsSkippedInsteadOfCrashing() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));

        state.setPlayer(null);
        onFx(() -> {
            orchestrator.update(1.0 / 60.0);
            orchestrator.render(0.5, 1.0 / 60.0);
        });

        state.setCurrentMap(null);
        onFx(() -> {
            orchestrator.update(1.0 / 60.0);
            orchestrator.render(0.5, 1.0 / 60.0);
        });
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
