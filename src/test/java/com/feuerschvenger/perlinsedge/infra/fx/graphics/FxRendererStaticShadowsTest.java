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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The static shadow list of the renderer is built again only when what it was built from changed.
 */
class FxRendererStaticShadowsTest {

    private static final int SIZE = 60;
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
        // The middle of the day, when shadows are at full strength
        final DayCycle sky = state.getDayCycle();
        sky.advance(sky.settings().dawnSeconds() + sky.settings().daySeconds() / 2.0 - sky.secondsIntoCycle());
        assumeTrue(sky.shadowStrength() > 0.5, "the sky must cast shadows for the test");
        final double zoom = AppConfig.getInstance().core().getInitialZoom();
        final FxRenderer[] built = new FxRenderer[1];
        FxToolkitSupport.onFx(() -> {
            canvas = new Canvas(800, 600);
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

    private void plantTree(int x, int y) {
        map.getTile(x, y).setWorldResource(new Resource(ResourceType.TREE, 1));
    }

    @Test
    void frames_with_the_same_window_sun_and_map_build_the_static_list_once() {
        plantTree(MIDDLE, MIDDLE);

        frame();
        frame();
        frame();

        assertEquals(1, renderer.staticListRebuilds());
        assertEquals(1, renderer.staticShadowCount(), "the kept list still holds the shadow of the tree");
    }

    @Test
    void a_pan_inside_the_window_keeps_the_static_list() {
        plantTree(MIDDLE, MIDDLE);
        frame();

        FxToolkitSupport.onFx(() -> {
            camera.pan(3, 2, canvas);
            return null;
        });
        frame();

        assertEquals(1, renderer.staticListRebuilds());
        assertEquals(1, renderer.staticShadowCount());
    }

    @Test
    void chopping_a_tree_inside_the_window_removes_its_shadow_on_the_next_frame() {
        plantTree(MIDDLE, MIDDLE);
        plantTree(MIDDLE + 1, MIDDLE);
        frame();
        assertEquals(2, renderer.staticShadowCount());

        map.getTile(MIDDLE, MIDDLE).setWorldResource(null);
        frame();

        assertEquals(2, renderer.staticListRebuilds());
        assertEquals(1, renderer.staticShadowCount());
    }

    @Test
    void planting_a_tree_adds_its_shadow_on_the_next_frame() {
        frame();
        assertEquals(0, renderer.staticShadowCount());

        plantTree(MIDDLE, MIDDLE);
        frame();

        assertEquals(1, renderer.staticShadowCount());
        assertEquals(2, renderer.staticListRebuilds());
    }

    @Test
    void a_step_of_the_sun_builds_the_list_again() {
        plantTree(MIDDLE, MIDDLE);
        frame();
        final DayCycle sky = state.getDayCycle();
        final double lengthBefore = sky.shadowLengthFactor();
        sky.advance(sky.settings().daySeconds() / 4.0);
        assumeTrue(sky.shadowLengthFactor() != lengthBefore && sky.shadowStrength() > 0.5,
                "the step must change how long shadows are");

        frame();

        assertEquals(2, renderer.staticListRebuilds());
        assertEquals(1, renderer.staticShadowCount());
    }

    @Test
    void a_pan_past_the_window_builds_the_list_again() {
        plantTree(MIDDLE, MIDDLE);
        frame();

        // Far more than the margin of the window, which is a quarter of the canvas or forty cells
        FxToolkitSupport.onFx(() -> {
            camera.pan(-2000, -1000, canvas);
            return null;
        });
        frame();

        assertTrue(renderer.staticListRebuilds() >= 2, "the window was placed again");
    }
}
