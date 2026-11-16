package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.infra.fx.graphics.ShadowList;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GroundShadow;
import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxShadowPassTest {

    private static final int WIDTH = 240;
    private static final int HEIGHT = 160;
    private static final double NOON = Math.PI / 2;

    private static Silhouette block(int width, int height) {
        final int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xFFFFFFFF);
        return new Silhouette(width, height, pixels);
    }

    /** The grey level of a white canvas that one frame of shadows was laid on, read back pixel by pixel. */
    private static int[][] render(FxShadowPass pass, Canvas canvas, ShadowList shadows, double scale, double originX,
                                  double originY, double strength) {
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        pass.draw(gc, shadows, scale, originX, originY, WIDTH, HEIGHT, NOON, 1.0, strength);
        final PixelReader reader = canvas.snapshot(null, null).getPixelReader();
        final int[][] grey = new int[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                grey[y][x] = reader.getArgb(x, y) & 0xFF;
            }
        }
        return grey;
    }

    private static ShadowList oneBlock(double x, double y) {
        final ShadowList list = new ShadowList();
        list.addSilhouette(x, y, block(10, 12), 12, 1.0, 1.0);
        return list;
    }

    @Test
    void aShadowDarkensTheWorldByTheShadowOpacity() {
        final int[][] grey = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 60), 2.0, 0, 0, 1.0));

        final int expected = (int) Math.round(255 * (1.0 - GroundShadow.OPACITY));
        // Straight under the anchor of a noon shadow
        assertEquals(expected, grey[63][120], 2);
        assertEquals(255, grey[10][10], "far from every shadow the world is untouched");
    }

    @Test
    void theStrengthOfTheDayScalesTheDarkness() {
        final int[][] half = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 60), 2.0, 0, 0, 0.5));
        final int[][] none = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 60), 2.0, 0, 0, 0.0));

        assertEquals(Math.round(255 * (1.0 - GroundShadow.OPACITY * 0.5)), half[63][120], 2);
        for (int[] row : none) {
            for (int value : row) {
                assertEquals(255, value, "no shadow at all at night");
            }
        }
    }

    @Test
    void overlappingShadowsAreAsDarkAsOneAndTheyAreOnlyCompositedOnce() {
        final int[][] single = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 60), 2.0, 0, 0, 1.0));

        final ShadowList crowd = new ShadowList();
        crowd.addSilhouette(120, 60, block(10, 12), 12, 1.0, 1.0);
        crowd.addSilhouette(120, 60, block(10, 12), 12, 1.0, 1.0);
        crowd.addSilhouette(124, 62, block(10, 12), 12, 1.0, 1.0);
        crowd.add(120, 60, 14, 1.0);
        final int[][] overlapped = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), crowd, 2.0, 0, 0, 1.0));

        int darkest = 255;
        int lightestInsideShadow = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                darkest = Math.min(darkest, overlapped[y][x]);
                if (single[y][x] < 255) {
                    lightestInsideShadow = Math.max(lightestInsideShadow, overlapped[y][x]);
                    assertEquals(single[y][x], overlapped[y][x], 1, "same darkness as one shadow at " + x + "," + y);
                }
            }
        }
        final int expected = (int) Math.round(255 * (1.0 - GroundShadow.OPACITY));
        assertEquals(expected, darkest, 2, "no cell is darker than a single shadow");
        assertTrue(lightestInsideShadow <= expected + 2);
    }

    @Test
    void aFadedShadowIsLighterThanAFullOne() {
        final ShadowList faded = new ShadowList();
        faded.addSilhouette(120, 60, block(10, 12), 12, 1.0, 0.5);
        final int[][] grey = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), faded, 2.0, 0, 0, 1.0));

        assertEquals(Math.round(255 * (1.0 - GroundShadow.OPACITY * 0.5)), grey[63][120], 3);
    }

    @Test
    void everyShadowPixelIsACrispBlockOfTheArtScaleAtEveryScale() {
        for (double scale : new double[]{2.0, 3.0, 4.0}) {
            final int[][] grey = FxToolkitSupport.onFx(() ->
                    render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 80), scale, 0, 0, 1.0));

            final int block = (int) scale;
            int shaded = 0;
            for (int by = 0; by + block <= HEIGHT; by += block) {
                for (int bx = 0; bx + block <= WIDTH; bx += block) {
                    final int value = grey[by][bx];
                    shaded += value < 255 ? 1 : 0;
                    for (int dy = 0; dy < block; dy++) {
                        for (int dx = 0; dx < block; dx++) {
                            assertEquals(value, grey[by + dy][bx + dx],
                                    "scale " + scale + " block " + bx + "," + by + " must be one flat colour");
                        }
                    }
                }
            }
            assertTrue(shaded > 0, "scale " + scale);
        }
    }

    @Test
    void theGridFollowsTheCameraSoShadowsKeepTheirPlaceOnTheArt() {
        // Camera origin 1 pixel off the screen grid: the blocks shift with it
        final int[][] grey = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(121, 60), 2.0, 1, 0, 1.0));

        // Blocks are aligned to odd columns, so the pair (1,2) is a block and (0,1) is not
        int sameInsideBlock = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 1; x + 1 < WIDTH; x += 2) {
                if (grey[y][x] < 255) {
                    assertEquals(grey[y][x], grey[y][x + 1], "block aligned to the camera at " + x + "," + y);
                    sameInsideBlock++;
                }
            }
        }
        assertTrue(sameInsideBlock > 0);
    }

    @Test
    void aFrameWithoutShadowsWipesTheShadowsOfThePreviousFrame() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        final int[][] first = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 1.0));
        final int[][] second = FxToolkitSupport.onFx(() -> render(pass, canvas, new ShadowList(), 2.0, 0, 0, 1.0));
        final int[][] third = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(60, 100), 2.0, 0, 0, 1.0));

        assertTrue(first[63][120] < 255);
        assertEquals(255, second[63][120]);
        assertEquals(255, third[63][120], "the old place is clean once the shadow has moved");
        assertTrue(third[103][60] < 255);
    }

    @Test
    void aShadowThatBecomesInvisibleLeavesNothingBehindWhenItReturnsElsewhere() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 1.0));
        final int[][] night = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 0.0));
        final int[][] morning = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(40, 120), 2.0, 0, 0, 1.0));

        assertEquals(255, night[63][120]);
        assertEquals(255, morning[63][120], "the shadow of the old frame did not come back");
        assertTrue(morning[123][40] < 255);
    }
}
