package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.canvas.Canvas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * On a wide canvas the static shadow window reaches much farther sideways than down, and a list rebuilt while the camera
 * stands at the far end of the window must still hold the casters in the far right and bottom strip.
 */
class FxRendererWideWindowTest {

    private static final int SIZE = 400;
    private static final int MIDDLE = SIZE / 2;

    private GameStateManager state;
    private TileMap map;
    private Canvas canvas;
    private FxCamera camera;
    private FxRenderer renderer;


    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        state.setCurrentMap(map);
        final DayCycle sky = state.getDayCycle();
        sky.advance(sky.settings().dawnSeconds() + sky.settings().daySeconds() / 2.0 - sky.secondsIntoCycle());
        assumeTrue(sky.shadowStrength() > 0.5, "the sky must cast shadows for the test");
    }

    /** Builds the renderer and the camera at a zoom, centred on the middle of the map. */
    private void start(double zoom) {
        final FxRenderer[] built = new FxRenderer[1];
        FxToolkitSupport.onFx(() -> {
            canvas = new Canvas(1920, 1080);
            camera = new FxCamera(0, 0, zoom);
            camera.centerOnTile(MIDDLE, MIDDLE, canvas);
            built[0] = new FxRenderer(canvas.getGraphicsContext2D(), camera, state);
            return null;
        });
        renderer = built[0];
    }

    private void frame() {
        FxToolkitSupport.onFx(() -> {
            renderer.render(1.0, 0.016);
            return null;
        });
    }

    /**
     * Pans the camera most of the way to the edge of the window it was placed with, then plants trees along the far right
     * and bottom edges of that window and rebuilds the list there.
     */
    private void rebuildAtThePanOffset(double direction, double zoom) {
        start(zoom);
        frame();
        final double[] window = renderer.staticWindow();
        // Far less than the margin of the window, which is a quarter of the canvas, so the window is kept
        final double pan = direction * (window[2] - canvas.getWidth()) / 2.0 * 0.9;
        FxToolkitSupport.onFx(() -> {
            camera.pan(pan, pan * canvas.getHeight() / canvas.getWidth(), canvas);
            return null;
        });
        frame();

        final double[] placed = renderer.staticWindow();
        // Trees along the right edge and the bottom edge of the window, one pixel inside it, which is where the casters
        // of the far strip stand
        final Set<Long> planted = new HashSet<>();
        for (int k = 0; k <= 8; k++) {
            plantAtGround(placed[0] + placed[2] - 1, placed[1] + placed[3] * k / 8.0 - (k == 8 ? 1 : 0), planted);
            plantAtGround(placed[0] + placed[2] * k / 8.0 - (k == 8 ? 1 : 0), placed[1] + placed[3] - 1, planted);
        }
        assumeTrue(!planted.isEmpty(), "the edges of the window are on the map");
        plantedCount = planted.size();

        frame();
    }

    private int plantedCount;

    /** Plants a tree on the tile whose ground point is the given screen point of the last frame. */
    private void plantAtGround(double groundX, double groundY, Set<Long> planted) {
        final AppConfig.GraphicsConfig graphics = AppConfig.getInstance().graphics();
        final double zoom = camera.getZoom();
        final double halfWidth = graphics.getTileHalfWidth() * zoom;
        final double halfHeight = graphics.getTileHalfHeight() * zoom;
        final double sum = (groundY - halfHeight - camera.getY()) / halfHeight;
        final double diff = (groundX - camera.getX()) / halfWidth;
        final int x = (int) Math.floor((sum + diff) / 2.0);
        final int y = (int) Math.floor((sum - diff) / 2.0);
        final Tile tile = map.getTile(x, y);
        if (tile != null && planted.add((long) x * SIZE + y)) {
            tile.setWorldResource(new Resource(ResourceType.TREE, 1));
        }
    }

    @Test
    void aListRebuiltWhileThePanLeansOneWayKeepsTheCastersOfTheFarStrip() {
        rebuildAtThePanOffset(1.0, AppConfig.getInstance().core().getInitialZoom());

        assertEquals(plantedCount, renderer.staticShadowCount());
    }

    @Test
    void aListRebuiltWhileThePanLeansTheOtherWayKeepsTheCastersOfTheFarStrip() {
        rebuildAtThePanOffset(-1.0, AppConfig.getInstance().core().getInitialZoom());

        assertEquals(plantedCount, renderer.staticShadowCount());
    }

    @Test
    void theFarStripIsKeptWhenZoomedOutToo() {
        rebuildAtThePanOffset(1.0, 0.5);

        assertEquals(plantedCount, renderer.staticShadowCount());
    }
}
