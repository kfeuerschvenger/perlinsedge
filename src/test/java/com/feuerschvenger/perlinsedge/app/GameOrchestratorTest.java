package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.ClickFeedback;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.light.Flicker;
import com.feuerschvenger.perlinsedge.domain.world.light.PointLight;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
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
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    private static final int WORLD_SIDE = 150;

    /** Draws the minimap until told to fail once. */
    private static final class FailingOnceMinimap extends FxMinimapDrawer {
        boolean failNext;
        /** The map the minimap was last given successfully. */
        TileMap shown;

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
            shown = newMap;
        }
    }

    private GameStateManager state;
    private GameOrchestrator orchestrator;
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
            minimap = new FailingOnceMinimap(minimapCanvas, camera, canvas);
            return new GameOrchestrator(
                    new GameView(new FxRenderer(canvas.getGraphicsContext2D(), camera, state), camera,
                            new FxInputHandler(canvas.getScene(), canvas, camera), minimap, canvas),
                    new GamePanels(new HealthBar(), new InventoryPanel(state),
                            new BuildingPalettePanel(recipes), new ItemHotbarPanel(), new HintLabel(),
                            new DeathOverlay()),
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

    /** Rebuilds the world with a map of the given type. */
    private void applyConfig(MapType type) {
        orchestrator.rebuildWorld(new MapRequest(type, 7L, WORLD_SIDE, WORLD_SIDE));
    }

    @Test
    void theMapKeyDoesNothingBeforeAGameIsStarted() {
        MapType before = AppConfig.getInstance().world().getCurrentMapType();

        onFx(orchestrator::regenerateMap);

        assertNull(state.getCurrentMap(), "no world was built on the main menu");
        assertEquals(before, AppConfig.getInstance().world().getCurrentMapType(),
                "the map type is not changed by a regeneration that is ignored");
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
    void aRebuildKeepsTheTimeOfDayAndTheFastTimeBecauseItIsTheSameSession() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.getDayCycle().advance(1234.5);
        final double minute = state.getDayCycle().clockMinuteOfDay();
        final int day = state.getDayCycle().dayNumber();
        final AppConfig.DayCycleConfig config = AppConfig.getInstance().dayCycle();
        final boolean before = config.isFastTime();
        config.setFastTime(true);
        try {
            onFx(orchestrator::regenerateMap);
            onFx(() -> applyConfig(MapType.TUNDRA));

            assertEquals(minute, state.getDayCycle().clockMinuteOfDay(), "the clock is the clock of the session");
            assertEquals(day, state.getDayCycle().dayNumber());
            assertTrue(config.isFastTime(), "so is the fast time");
        } finally {
            config.setFastTime(before);
        }
    }

    @Test
    void aRebuildEmptiesTheLightRegistryAndTheHeldTorchLightsAgainOnTheNewMap() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.getPlayer().addItem(new Item(ItemType.TORCH, 1));
        state.getPlayer().selectHotbarSlot(0);
        state.syncLights(1.0);
        state.getLights().add(new PointLight(3, 3, 2.0, new Rgba(1, 1, 1, 1), 1.0, Flicker.STEADY));
        assertEquals(2, state.getLights().size());

        onFx(orchestrator::regenerateMap);

        assertEquals(0, state.getLights().size(), "the lights of the old map are gone");
        state.syncLights(1.0);
        assertEquals(1, state.getLights().size(), "the torch in hand lights the new map");
        assertEquals(state.getPlayer().getInterpolatedX(1.0), state.getLights().get(0).x(), 1e-12);
    }

    @Test
    void anUpdateRegistersTheHeldTorchWhereThePlayerStands() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.getPlayer().addItem(new Item(ItemType.TORCH, 1));
        state.getPlayer().selectHotbarSlot(0);

        onFx(() -> orchestrator.update(1.0 / 60.0));

        assertEquals(1, state.getLights().size());
        state.getPlayer().selectHotbarSlot(3);
        onFx(() -> orchestrator.update(1.0 / 60.0));
        assertEquals(0, state.getLights().size());
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
        assertSame(state.getCurrentMap(), minimap.shown, "the minimap shows the map that replaced the old one");

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
    void theSkyAdvancesWithTheGameAndStandsStillWhilePaused() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        final double start = state.getDayCycle().secondsIntoCycle();

        onFx(() -> orchestrator.update(1.0));
        assertEquals(start + 1.0, state.getDayCycle().secondsIntoCycle(), 1e-9);

        state.setGamePaused(true);
        onFx(() -> orchestrator.update(1.0));
        assertEquals(start + 1.0, state.getDayCycle().secondsIntoCycle(), 1e-9, "a paused game must not move the sun");
    }

    @Test
    void theFastTimeSpeedsUpTheSkyButNotTheGameClock() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        final double start = state.getDayCycle().secondsIntoCycle();
        final AppConfig.DayCycleConfig config = AppConfig.getInstance().dayCycle();
        final boolean before = config.isFastTime();
        config.setFastTime(true);
        try {
            onFx(() -> orchestrator.update(1.0));

            assertEquals(start + config.getFastTimeMultiplier(), state.getDayCycle().secondsIntoCycle(), 1e-6);
            assertEquals(1.0, state.getGameTime(), 1e-9);
        } finally {
            config.setFastTime(before);
        }
    }

    @Test
    void aNewGameSwitchesTheFastTimeOff() {
        final AppConfig.DayCycleConfig config = AppConfig.getInstance().dayCycle();
        final boolean before = config.isFastTime();
        try {
            config.setFastTime(true);

            onFx(() -> orchestrator.startNewGame(MapType.DESERT));

            assertFalse(config.isFastTime(), "the fast time of the old world is not carried into the new one");
        } finally {
            config.setFastTime(before);
        }
    }

    @Test
    void aNewGameStartsTheSkyInTheEarlyMorningAgain() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.getDayCycle().advance(5000);

        onFx(() -> orchestrator.startNewGame(MapType.DESERT));

        assertEquals(AppConfig.getInstance().dayCycle().settings().startSeconds(),
                state.getDayCycle().secondsIntoCycle(), 1e-9);
        assertEquals(1, state.getDayCycle().dayNumber());
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
        assertDoesNotThrow(() -> onFx(() -> {
            orchestrator.update(1.0 / 60.0);
            orchestrator.render(0.5, 1.0 / 60.0);
        }));

        state.setCurrentMap(null);
        assertDoesNotThrow(() -> onFx(() -> {
            orchestrator.update(1.0 / 60.0);
            orchestrator.render(0.5, 1.0 / 60.0);
        }));
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

    @Test
    void anUpdateWithTheHammerInHandEntersBuildModeAndAnotherSlotLeavesIt() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.getPlayer().setItemAt(1, new Item(ItemType.HAMMER, 1));
        state.getPlayer().selectHotbarSlot(1);

        onFx(() -> orchestrator.update(1.0 / 60.0));
        assertEquals(GameMode.BUILDING_MODE, state.getCurrentGameMode());

        state.getPlayer().selectHotbarSlot(2);
        onFx(() -> orchestrator.update(1.0 / 60.0));
        assertEquals(GameMode.EXPLORATION, state.getCurrentGameMode());
    }

    @Test
    void escapeDropsTheSelectedPieceFirstAndPausesOnlyWhenNothingIsSelected() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));
        state.getPlayer().setItemAt(1, new Item(ItemType.HAMMER, 1));
        state.getPlayer().selectHotbarSlot(1);
        onFx(() -> orchestrator.update(1.0 / 60.0));
        orchestrator.buildingManager().setSelectedBuildingType(com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType.FENCE);

        onFx(orchestrator::handleEscape);

        assertNull(orchestrator.buildingManager().getSelectedBuildingType());
        assertEquals(GameMode.BUILDING_MODE, state.getCurrentGameMode(), "the hammer stays in hand");
        assertFalse(state.isGamePaused());

        onFx(orchestrator::handleEscape);

        assertTrue(state.isGamePaused());
    }

    @Test
    void escapeWhileExploringPauses() {
        onFx(() -> orchestrator.startNewGame(MapType.DESERT));

        onFx(orchestrator::handleEscape);

        assertTrue(state.isGamePaused());
    }
}
