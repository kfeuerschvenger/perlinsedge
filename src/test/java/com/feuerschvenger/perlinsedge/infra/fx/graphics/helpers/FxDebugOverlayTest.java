package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FrameStats;
import com.feuerschvenger.perlinsedge.ui.view.CanvasProbe;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.canvas.Canvas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("AppConfig.debug")
class FxDebugOverlayTest {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 500;

    private boolean debugBefore;

    @BeforeEach
    void rememberDebug() {
        debugBefore = AppConfig.getInstance().debug().isEnabled();
    }

    @AfterEach
    void restoreDebug() {
        AppConfig.getInstance().debug().setEnabled(debugBefore);
    }

    private static GameStateManager state(boolean withPlayer, boolean withMouseTile) {
        final GameStateManager state = new GameStateManager();
        final TileMap map = new TileMap(4, 4);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        map.getTile(1, 1).setWorldResource(new Resource(ResourceType.TREE, 1L));
        state.setCurrentMap(map);
        if (withPlayer) {
            state.setPlayer(new Player(2, 2));
        }
        if (withMouseTile) {
            state.setMouseOverTile(1, 1);
        }
        return state;
    }

    private static CanvasProbe overlay(GameStateManager state) {
        final FrameStats stats = new FrameStats();
        stats.recordFrame(0.016);
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        return CanvasProbe.render(WIDTH, HEIGHT, gc -> FxDebugDrawingHelper.drawDebugInfo(gc, state, canvas, stats, 12));
    }

    @Test
    void nothingIsDrawnWhileTheDebugModeIsOff() {
        AppConfig.getInstance().debug().setEnabled(false);

        assertEquals(0, overlay(state(true, true)).painted());
    }

    @Test
    void theOverlayIsWhiteTextAnchoredToTheRightEdge() {
        AppConfig.getInstance().debug().setEnabled(true);

        final CanvasProbe shot = overlay(state(false, false));

        assertTrue(shot.painted(WIDTH / 2, 0, WIDTH, HEIGHT) > 0, "text on the right half");
        assertEquals(0, shot.painted(0, 0, WIDTH / 4, HEIGHT), "nothing on the far left");
        assertTrue(shot.near(javafx.scene.paint.Color.WHITE, 0) > 0, "white text");
    }

    @Test
    void thePlayerLinesAreAddedWhenThereIsAPlayer() {
        AppConfig.getInstance().debug().setEnabled(true);

        final int without = overlay(state(false, false)).painted();
        final int with = overlay(state(true, false)).painted();

        assertTrue(with > without);
    }

    @Test
    void theTileUnderTheMouseAddsTheTileTypeAndTheResource() {
        AppConfig.getInstance().debug().setEnabled(true);

        final int without = overlay(state(true, false)).painted();
        final int with = overlay(state(true, true)).painted();

        assertTrue(with > without);
    }

    @Test
    void theEntityCountsFollowTheListsOfTheGame() {
        AppConfig.getInstance().debug().setEnabled(true);
        final GameStateManager empty = state(true, false);
        final GameStateManager busy = state(true, false);
        busy.setDroppedItems(List.of(new DroppedItem(1, 1, new Item(ItemType.STONE, 1), 0,
                DroppedItem.DropSource.RESOURCE)));

        assertNotEquals(overlay(empty).painted(), overlay(busy).painted());
    }
}
