package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import javafx.event.Event;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Handlers are counted through a game state that records how often the click handler asks whether the game is
 * paused, which is the first thing every canvas click does. No toolkit is needed: events are fired on the
 * canvas node directly.
 */
class InputWiringTest {

    /** Counts the calls to {@link #isGamePaused()}, and reports the game as paused so a click does nothing else. */
    private static final class CountingGameState extends GameStateManager {
        final AtomicInteger pausedChecks = new AtomicInteger();

        @Override
        public boolean isGamePaused() {
            pausedChecks.incrementAndGet();
            return true;
        }
    }

    private CountingGameState state;
    private Canvas mainCanvas;
    private Canvas minimapCanvas;
    private InputWiring wiring;

    @BeforeEach
    void setUp() {
        state = new CountingGameState();
        mainCanvas = new Canvas(200, 200);
        minimapCanvas = new Canvas(100, 60);
        FxCamera camera = new FxCamera(100, 100, 1.0);
        FxMinimapDrawer minimap = new FxMinimapDrawer(minimapCanvas, camera, mainCanvas);
        wiring = new InputWiring(state, null, null, null, camera, null, minimap, mainCanvas);
    }

    /** A game click is handled on the press, a minimap click on the click that follows the release. */
    private void click(Canvas target) {
        Event.fireEvent(target, new MouseEvent(target == mainCanvas ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_CLICKED,
                10, 10, 10, 10, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null));
    }

    @Test
    void registeringTwiceHandlesEachClickOnce() {
        wiring.registerMouseHandlers();
        wiring.registerMouseHandlers();

        click(mainCanvas);

        assertEquals(1, state.pausedChecks.get());
    }

    @Test
    void registeringManyTimesNeverStacksTheCanvasOrMinimapHandlers() {
        for (int newGame = 0; newGame < 5; newGame++) {
            wiring.registerMouseHandlers();
        }

        click(mainCanvas);
        click(minimapCanvas);

        assertEquals(2, state.pausedChecks.get(), "one canvas click and one minimap click, each handled once");
    }

    @Test
    void unregisteringRemovesEveryHandler() {
        wiring.registerMouseHandlers();
        wiring.unregisterMouseHandlers();

        click(mainCanvas);
        click(minimapCanvas);

        assertEquals(0, state.pausedChecks.get());
    }

    /** A running game with a dead player that counts how often a click looks at the map. */
    private static final class DeadPlayerGameState extends GameStateManager {
        final AtomicInteger mapReads = new AtomicInteger();

        @Override
        public boolean isPlayerDead() {
            return true;
        }

        @Override
        public TileMap getCurrentMap() {
            mapReads.incrementAndGet();
            return null;
        }
    }

    @Test
    void clicksAreIgnoredWhileThePlayerIsDead() {
        DeadPlayerGameState dead = new DeadPlayerGameState();
        FxCamera camera = new FxCamera(100, 100, 1.0);
        InputWiring deadWiring = new InputWiring(dead, null, null, null, camera, null,
                new FxMinimapDrawer(minimapCanvas, camera, mainCanvas), mainCanvas);
        deadWiring.registerMouseHandlers();

        click(mainCanvas);

        assertEquals(0, dead.mapReads.get());
    }

    private static void move(Canvas target, double x, double y) {
        Event.fireEvent(target, new MouseEvent(MouseEvent.MOUSE_MOVED, x, y, x, y, MouseButton.NONE, 0,
                false, false, false, false, false, false, false, true, false, false, null));
    }

    @Test
    void theHoveredTileFollowsTheCameraWithoutAMouseMove() {
        GameStateManager running = new GameStateManager();
        TileMap map = new TileMap(8, 8);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        running.setCurrentMap(map);
        FxCamera camera = new FxCamera(0, 0, 1.0);
        InputWiring hover = new InputWiring(running, null, null, null, camera, null,
                new FxMinimapDrawer(minimapCanvas, camera, mainCanvas), mainCanvas);
        hover.registerMouseHandlers();

        move(mainCanvas, 0, 112);
        hover.refreshHover();
        assertEquals(3, running.getMouseOverTileX());
        assertEquals(3, running.getMouseOverTileY());

        camera.setPosition(64, 0);
        hover.refreshHover();
        assertEquals(2, running.getMouseOverTileX(), "the mouse did not move, the ground under it did");
        assertEquals(4, running.getMouseOverTileY());
    }

    @Test
    void leavingTheCanvasClearsTheHoveredTile() {
        GameStateManager running = new GameStateManager();
        TileMap map = new TileMap(8, 8);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        running.setCurrentMap(map);
        FxCamera camera = new FxCamera(0, 0, 1.0);
        InputWiring hover = new InputWiring(running, null, null, null, camera, null,
                new FxMinimapDrawer(minimapCanvas, camera, mainCanvas), mainCanvas);
        hover.registerMouseHandlers();
        move(mainCanvas, 0, 112);
        hover.refreshHover();

        Event.fireEvent(mainCanvas, new MouseEvent(MouseEvent.MOUSE_EXITED, 0, 112, 0, 112, MouseButton.NONE, 0,
                false, false, false, false, false, false, false, true, false, false, null));
        hover.refreshHover();

        assertEquals(-1, running.getMouseOverTileX());
    }

    @Test
    void handlersCanBeRegisteredAgainAfterUnregistering() {
        wiring.registerMouseHandlers();
        wiring.unregisterMouseHandlers();
        wiring.registerMouseHandlers();

        click(mainCanvas);

        assertEquals(1, state.pausedChecks.get());
    }
}
