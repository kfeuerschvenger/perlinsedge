package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the renderer draws while the hammer is in hand: the grid, the tinted ghost and the health bar of a hurt building.
 * Every check compares a frame against a baseline frame of the same renderer, never against a fixed colour, so the
 * result does not depend on how the platform shades the canvas.
 */
class FxRendererBuildModeTest {

    private static final int SIZE = 30;
    private static final int HOVER = SIZE / 2;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private GameStateManager state;
    private TileMap map;
    private Canvas canvas;
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
        state.setPlayer(new Player(1, 1));
        final double zoom = AppConfig.getInstance().core().getInitialZoom();
        final FxRenderer[] built = new FxRenderer[1];
        FxToolkitSupport.onFx(() -> {
            canvas = new Canvas(WIDTH, HEIGHT);
            final FxCamera camera = new FxCamera(0, 0, zoom);
            camera.centerOnTile(HOVER, HOVER, canvas);
            built[0] = new FxRenderer(canvas.getGraphicsContext2D(), camera, state);
            return null;
        });
        renderer = built[0];
        state.setMouseOverTile(HOVER, HOVER);
    }

    /** Renders a frame and returns every pixel of the canvas. */
    private int[] frame() {
        return FxToolkitSupport.onFx(() -> {
            renderer.render(1.0, 0.016);
            final WritableImage image = canvas.snapshot(null, null);
            final PixelReader reader = image.getPixelReader();
            final int[] pixels = new int[WIDTH * HEIGHT];
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    pixels[y * WIDTH + x] = reader.getArgb(x, y);
                }
            }
            return pixels;
        });
    }

    private static int differing(int[] a, int[] b) {
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) count++;
        }
        return count;
    }

    /** The average colour of the pixels that differ between two frames, as red, green and blue. */
    private static double[] averageChange(int[] base, int[] changed) {
        double r = 0, g = 0, b = 0;
        int count = 0;
        for (int i = 0; i < base.length; i++) {
            if (base[i] != changed[i]) {
                r += (changed[i] >> 16) & 0xFF;
                g += (changed[i] >> 8) & 0xFF;
                b += changed[i] & 0xFF;
                count++;
            }
        }
        return count == 0 ? new double[]{0, 0, 0} : new double[]{r / count, g / count, b / count};
    }

    private void buildMode(BuildingType piece, Rgba tint) {
        renderer.setBuildingPreview(HOVER, HOVER, piece, tint);
    }

    @Test
    void theGridIsDrawnInBuildModeAndGoneWhenItEnds() {
        final int[] plain = frame();

        buildMode(null, null);
        final int[] building = frame();
        renderer.clearBuildingPreview();
        final int[] after = frame();

        assertTrue(differing(plain, building) > 200, "the grid outlines the tiles around the hovered one");
        assertArrayEquals(plain, after, "nothing of the build mode is left once it ends");
    }

    @Test
    void theGridStaysWithinAFewTilesOfTheHoveredOne() {
        final int[] plain = frame();
        buildMode(null, null);
        final int[] building = frame();

        // The corner of the canvas is far more than three tiles from the middle tile
        int cornerChanges = 0;
        for (int y = 0; y < 60; y++) {
            for (int x = 0; x < 60; x++) {
                if (plain[y * WIDTH + x] != building[y * WIDTH + x]) cornerChanges++;
            }
        }
        assertEquals(0, cornerChanges, "the grid does not reach the corner of the canvas");
    }

    @Test
    void theGhostIsTintedGreenWhenPlaceableRedWhenRefusedAndAmberWhenResourcesAreMissing() {
        buildMode(null, null);
        final int[] noGhost = frame();

        buildMode(BuildingType.WORKBENCH, Rgba.LIME_GREEN);
        final double[] green = averageChange(noGhost, frame());
        buildMode(BuildingType.WORKBENCH, Rgba.RED);
        final double[] red = averageChange(noGhost, frame());
        buildMode(BuildingType.WORKBENCH, Rgba.ORANGE);
        final double[] amber = averageChange(noGhost, frame());

        assertTrue(green[1] - green[0] > red[1] - red[0], "green leans greener than red does");
        assertTrue(red[0] - red[1] > green[0] - green[1], "red leans redder than green does");
        assertTrue(amber[0] > amber[2] && amber[1] > amber[2], "amber has the least blue");
        assertTrue(amber[0] - amber[2] > green[0] - green[2], "amber is warmer than green");
    }

    @Test
    void noGhostIsDrawnWithoutASelectedPiece() {
        buildMode(null, Rgba.YELLOW);
        final int[] none = frame();
        buildMode(null, null);
        final int[] bare = frame();

        assertArrayEquals(bare, none);
    }

    @Test
    void aDamagedBuildingShowsItsHealthBarOnlyWhileTheHammerHoversIt() {
        final Fence wall = new Fence(HOVER, HOVER);
        map.getTile(HOVER, HOVER).setBuilding(wall);

        buildMode(null, null);
        final int[] intact = frame();
        wall.damage(wall.getMaxHealth() / 2);
        final int[] hurt = frame();
        renderer.clearBuildingPreview();
        final int[] hurtWithoutHammer = frame();
        final Fence fresh = new Fence(HOVER, HOVER);
        map.getTile(HOVER, HOVER).setBuilding(fresh);
        final int[] freshWithoutHammer = frame();

        assertTrue(differing(intact, hurt) > 20, "the bar appears over the hurt building");
        assertArrayEquals(freshWithoutHammer, hurtWithoutHammer, "no bar without the hammer in hand");
    }

    @Test
    void theHealthBarRuleNeedsBuildModeAndADamagedBuildingUnderTheCursor() {
        final Building wall = new Fence(0, 0);
        assertFalse(com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingHealthBar.isShown(true, wall));
        wall.damage(1);
        assertTrue(com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingHealthBar.isShown(true, wall));
        assertFalse(com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingHealthBar.isShown(false, wall));
        assertFalse(com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxBuildingHealthBar.isShown(true, null));
    }
}
