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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        pass.draw(gc, new ShadowList(), shadows, new CanvasView(scale, originX, originY, WIDTH, HEIGHT),
                new SunShadow(NOON, 1.0, strength));
        final PixelReader reader = canvas.snapshot(null, null).getPixelReader();
        final int[][] grey = new int[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                grey[y][x] = reader.getArgb(x, y) & 0xFF;
            }
        }
        return grey;
    }

    /** The silhouette every test block shares, as the sprite cache hands out one silhouette per sprite. */
    private static final Silhouette BLOCK = block(10, 12);

    private static ShadowList oneBlock(double x, double y) {
        final ShadowList list = new ShadowList();
        list.addSilhouette(x, y, BLOCK, 12, 1.0, 1.0);
        return list;
    }

    @Test
    void aShadowDarkensTheWorldByTheShadowOpacity() {
        final int[][] grey = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 60), 2.0, 0, 0, 1.0));

        final int expected = (int) Math.round(255 * (1.0 - GroundShadow.OPACITY));
        // Straight under the anchor of a noon shadow
        assertEquals((float) expected, (float) grey[63][120], 2.0f);
        assertEquals(255, grey[10][10], "far from every shadow the world is untouched");
    }

    @Test
    void theStrengthOfTheDayScalesTheDarkness() {
        final int[][] half = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 60), 2.0, 0, 0, 0.5));
        final int[][] none = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), oneBlock(120, 60), 2.0, 0, 0, 0.0));

        assertEquals((float) Math.round(255 * (1.0 - GroundShadow.OPACITY * 0.5)), (float) half[63][120], 2.0f);
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
                    assertEquals((float) single[y][x], (float) overlapped[y][x], 1.0f,
                            "same darkness as one shadow at " + x + "," + y);
                }
            }
        }
        final int expected = (int) Math.round(255 * (1.0 - GroundShadow.OPACITY));
        assertEquals((float) expected, (float) darkest, 2.0f, "no cell is darker than a single shadow");
        assertTrue(lightestInsideShadow <= expected + 2);
    }

    @Test
    void aFadedShadowIsLighterThanAFullOne() {
        final ShadowList faded = new ShadowList();
        faded.addSilhouette(120, 60, block(10, 12), 12, 1.0, 0.5);
        final int[][] grey = FxToolkitSupport.onFx(() ->
                render(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), faded, 2.0, 0, 0, 1.0));

        assertEquals((float) Math.round(255 * (1.0 - GroundShadow.OPACITY * 0.5)), (float) grey[63][120], 3.0f);
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

    /** Draws one frame with an explicit sun, which the render helper above keeps at noon. */
    private static int[][] renderWithSun(FxShadowPass pass, Canvas canvas, ShadowList shadows, double originX,
                                         double strength, double angle, double length) {
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        pass.draw(gc, new ShadowList(), shadows, new CanvasView(2.0, originX, 0, WIDTH, HEIGHT),
                new SunShadow(angle, length, strength));
        final PixelReader reader = canvas.snapshot(null, null).getPixelReader();
        final int[][] grey = new int[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                grey[y][x] = reader.getArgb(x, y) & 0xFF;
            }
        }
        return grey;
    }

    @Test
    void aFrameWithTheSameInputsKeepsTheLayerAndDoesNotCastOrUploadAgain() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        final int[][] first = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 1.0));
        assertEquals(1, pass.recasts());
        assertEquals(1, pass.uploads());

        // A new list with the same content is the same frame as far as the shadows go
        final int[][] second = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 1.0));
        final int[][] third = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 1.0));

        assertEquals(1, pass.recasts(), "nothing changed, so nothing was cast again");
        assertEquals(1, pass.uploads(), "and nothing was uploaded again");
        assertTrue(Arrays.deepEquals(first, second), "the kept layer is drawn exactly as before");
        assertTrue(Arrays.deepEquals(first, third));
    }

    @Test
    void theStrengthOfTheDayChangesTheDarknessWithoutCastingAgain() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 1.0));
        final int[][] half = FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 0.5));

        assertEquals(1, pass.recasts());
        assertEquals((float) Math.round(255 * (1.0 - GroundShadow.OPACITY * 0.5)), (float) half[63][120], 2.0f);
    }

    @Test
    void anythingTheLayerDependsOnCastsAgain() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(120, 60), 2.0, 0, 0, 1.0));

        FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(122, 60), 2.0, 0, 0, 1.0));
        assertEquals(2, pass.recasts(), "a shadow that moved");
        FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(122, 60), 2.0, 1, 0, 1.0));
        assertEquals(3, pass.recasts(), "a camera that moved");
        FxToolkitSupport.onFx(() -> render(pass, canvas, oneBlock(122, 60), 4.0, 1, 0, 1.0));
        assertEquals(4, pass.recasts(), "another art scale");
        FxToolkitSupport.onFx(() -> renderWithSun(pass, canvas, oneBlock(122, 60), 1, 1.0, NOON + 0.1, 1.0));
        assertEquals(5, pass.recasts(), "another sun step");
        FxToolkitSupport.onFx(() -> renderWithSun(pass, canvas, oneBlock(122, 60), 1, 1.0, NOON + 0.1, 1.5));
        assertEquals(6, pass.recasts(), "another shadow length");
        final ShadowList fainter = new ShadowList();
        fainter.addSilhouette(122, 60, block(10, 12), 12, 1.0, 0.5);
        FxToolkitSupport.onFx(() -> renderWithSun(pass, canvas, fainter, 1, 1.0, NOON + 0.1, 1.5));
        assertEquals(7, pass.recasts(), "a shadow that faded");
        FxToolkitSupport.onFx(() -> renderWithSun(pass, canvas, fainter, 1, 0.0, NOON + 0.1, 1.5));
        assertEquals(8, pass.recasts(), "shadows that became too faint to see are wiped");
        FxToolkitSupport.onFx(() -> renderWithSun(pass, canvas, new ShadowList(), 1, 0.0, NOON + 0.1, 1.5));
        assertEquals(8, pass.recasts(), "while they stay invisible the list does not matter");
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

    /** Draws one frame with a static and a dynamic list at art scale two and reads the grey levels back. */
    private static int[][] renderBoth(FxShadowPass pass, Canvas canvas, ShadowList fixed, ShadowList moving,
                                      double originX) {
        return renderBoth(pass, canvas, fixed, moving, 2.0, originX);
    }

    /** Draws one frame with a static and a dynamic list at an art scale and reads the grey levels back. */
    private static int[][] renderBoth(FxShadowPass pass, Canvas canvas, ShadowList fixed, ShadowList moving,
                                      double scale, double originX) {
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        pass.draw(gc, fixed, moving, new CanvasView(scale, originX, 0, WIDTH, HEIGHT), new SunShadow(NOON, 1.0, 1.0));
        final PixelReader reader = canvas.snapshot(null, null).getPixelReader();
        final int[][] grey = new int[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                grey[y][x] = reader.getArgb(x, y) & 0xFF;
            }
        }
        return grey;
    }

    private static int[][] freshBoth(ShadowList fixed, ShadowList moving, double originX) {
        return FxToolkitSupport.onFx(() -> renderBoth(new FxShadowPass(), new Canvas(WIDTH, HEIGHT), fixed, moving,
                originX));
    }

    @Test
    void movingOnlyADynamicShadowDoesNotCastTheStaticOnesAgainAndUploadsOnlyTheOldAndNewBox() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        final ShadowList tree = oneBlock(60, 120);
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, tree, oneBlock(180, 60), 0));
        assertEquals(1, pass.staticRecasts());
        assertEquals(1, pass.recasts());
        assertEquals(1, pass.uploads());

        final int[][] moved = FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, tree, oneBlock(184, 60), 0));

        assertEquals(1, pass.staticRecasts(), "the static shadows were not stamped again");
        assertEquals(2, pass.recasts());
        assertEquals(2, pass.uploads());
        final int[] box = pass.lastUpload();
        // The static block stands on the left at about cell 30, the creature on the right at about cell 90
        assertTrue(box[0] > 60, "the upload does not reach back to the static shadow: " + Arrays.toString(box));
        assertTrue(box[2] <= 110 && box[3] <= 82, Arrays.toString(box));
        assertTrue(Arrays.deepEquals(freshBoth(tree, oneBlock(184, 60), 0), moved),
                "the combined image is what a full recast gives");
    }

    @Test
    void aStaticAndADynamicShadowOnTheSameSpotAreAsDarkAsOne() {
        final int[][] single = freshBoth(new ShadowList(), oneBlock(120, 60), 0);
        final ShadowList faded = new ShadowList();
        faded.addSilhouette(120, 60, BLOCK, 12, 1.0, 0.5);

        final int[][] fadedStatic = freshBoth(faded, oneBlock(120, 60), 0);
        final int[][] fadedDynamic = freshBoth(oneBlock(120, 60), faded, 0);
        final int[][] both = freshBoth(oneBlock(120, 60), oneBlock(122, 60), 0);

        // Two draws that blend to the stronger shadow differ from one draw by the rounding of two 8 bit blends
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                assertEquals((float) single[y][x], (float) fadedStatic[y][x], 2.0f,
                        "the stronger dynamic shadow wins over a fainter static one at " + x + "," + y);
            }
        }
        assertTrue(Arrays.deepEquals(single, fadedDynamic), "the stronger static shadow wins over a fainter dynamic one");
        final int expected = (int) Math.round(255 * (1.0 - GroundShadow.OPACITY));
        int darkest = 255;
        for (int[] row : both) {
            for (int value : row) {
                darkest = Math.min(darkest, value);
            }
        }
        assertEquals((float) expected, (float) darkest, 2.0f, "no cell is darker than a single shadow");
    }

    @Test
    void aDynamicShadowThatLeavesAStaticOneGivesTheStaticCellsBack() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        final ShadowList tree = oneBlock(120, 60);
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, tree, oneBlock(124, 64), 0));

        final int[][] left = FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, tree, new ShadowList(), 0));

        assertTrue(Arrays.deepEquals(freshBoth(tree, new ShadowList(), 0), left));
        assertEquals(1, pass.staticRecasts());
    }

    @Test
    void aChangeOfTheStaticListCastsTheStaticShadowsAgainButACreatureDoesNot() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, oneBlock(60, 120), oneBlock(180, 60), 0));

        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, oneBlock(62, 120), oneBlock(180, 60), 0));
        assertEquals(2, pass.staticRecasts(), "a static shadow that moved");
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, oneBlock(62, 120), oneBlock(184, 60), 0));
        assertEquals(2, pass.staticRecasts(), "a creature that moved");
    }

    @Test
    void aCameraPanOfWholeArtPixelsDoesNotCastTheStaticShadowsAgainAndLooksLikeAFreshCast() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        // Static shadows are kept in world coordinates, so the same list is the same world at every camera offset
        final ShadowList world = new ShadowList();
        world.addSilhouette(60, 120, BLOCK, 12, 1.0, 1.0);
        world.addSilhouette(140, 90, BLOCK, 12, 1.0, 1.0);
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, world, oneBlock(150, 80), 0));
        assertEquals(1, pass.staticRecasts());

        for (int pan = 1; pan <= 12; pan++) {
            // The creature stays on the screen where it is, as one that walks with the camera does
            final double origin = -2.0 * pan;
            final int[][] panned = FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, world, oneBlock(150, 80), origin));

            assertEquals(1, pass.staticRecasts(), "a pan of " + pan + " art pixels cast the static shadows again");
            assertTrue(Arrays.deepEquals(freshBoth(world, oneBlock(150, 80), origin), panned),
                    "the panned frame is what a fresh cast gives at " + origin);
        }
    }

    @Test
    void aCameraPanOfSeveralPixelsInsideTheArtPixelAndAcrossItNeverCastsTheStaticShadowsAgain() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        final ShadowList world = new ShadowList();
        world.addSilhouette(60, 120, BLOCK, 12, 1.0, 1.0);
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, world, new ShadowList(), 0));

        for (int pan = 1; pan <= 9; pan++) {
            final double origin = 3.0 * pan;
            final int[][] panned = FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, world, new ShadowList(), origin));

            assertEquals(1, pass.staticRecasts(), "a pan of " + origin + " pixels cast the static shadows again");
            assertTrue(Arrays.deepEquals(freshBoth(world, new ShadowList(), origin), panned), "at " + origin);
        }
    }

    @Test
    void aCameraThatLeavesTheStaticWindowMovesItAndStillLooksLikeAFreshCast() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        final ShadowList world = new ShadowList();
        world.addSilhouette(60, 120, BLOCK, 12, 1.0, 1.0);
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, world, new ShadowList(), 0));

        // Far past the margin of cells, in both directions
        final double far = -2.0 * (FxShadowPass.WINDOW_MARGIN_CELLS + 20);
        final int[][] moved = FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, world, new ShadowList(), far));

        assertEquals(2, pass.staticRecasts(), "the window had to move");
        assertTrue(Arrays.deepEquals(freshBoth(world, new ShadowList(), far), moved));
    }

    @Test
    void aStaticShadowPannedUnderACreatureStaysAsDarkAsOneAtEveryOffset() {
        final ShadowList faded = new ShadowList();
        faded.addSilhouette(120, 60, BLOCK, 12, 1.0, 0.5);
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, faded, oneBlock(120, 60), 0));

        for (int pan = 1; pan <= 6; pan++) {
            final double origin = 2.0 * pan;
            // The creature walks with the camera, so its screen position moves while the world shadow stays
            final ShadowList creature = oneBlock(120 + origin, 60);
            final int[][] panned = FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, faded, creature, origin));

            assertTrue(Arrays.deepEquals(freshBoth(faded, creature, origin), panned), "at " + origin);
        }
        assertEquals(1, pass.staticRecasts());
    }

    @Test
    void theStaticWindowCoversTheCanvasWithAMarginOnEverySide() {
        final FxShadowPass pass = new FxShadowPass();
        pass.beginFrame(new CanvasView(2.0, 10, 20, WIDTH, HEIGHT));

        final double margin = FxShadowPass.WINDOW_MARGIN_CELLS * 2.0;
        assertTrue(pass.windowLeft() <= -margin + 1e-9, "left " + pass.windowLeft());
        assertTrue(pass.windowTop() <= -margin + 1e-9, "top " + pass.windowTop());
        assertTrue(pass.windowLeft() + pass.windowWidth() >= WIDTH + margin - 1e-9);
        assertTrue(pass.windowTop() + pass.windowHeight() >= HEIGHT + margin - 1e-9);
        final double before = pass.windowLeft();
        pass.beginFrame(new CanvasView(2.0, 10, 20, WIDTH, HEIGHT));
        assertEquals(before, pass.windowLeft(), "asking twice in a frame changes nothing");
        assertTrue(pass.windowOverhangXPixels() >= 2 * margin);
        assertTrue(pass.windowOverhangYPixels() >= 2 * margin);
    }

    /** Draws one frame of empty shadow lists at the given scale and camera, which casts the static layer when it moved. */
    private static int drawEmpty(FxShadowPass pass, Canvas canvas, double scale, double originX) {
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        pass.draw(gc, new ShadowList(), new ShadowList(), new CanvasView(scale, originX, 0, WIDTH, HEIGHT),
                new SunShadow(NOON, 1.0, 1.0));
        return pass.staticRecasts();
    }

    @Test
    void aStaticShadowCastOnceStaysOnItsOddWidthSpriteAfterAnyWholePixelPanWithoutCastingAgain() {
        final Silhouette odd = block(5, 6);
        final ShadowList fixed = new ShadowList();
        fixed.addSilhouette(120, 60, odd, 6, 1.0, 1.0);
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, fixed, new ShadowList(), 3.0, 0));
        assertEquals(1, pass.staticRecasts());

        for (double pan : new double[]{1, 2, 3, 101}) {
            // The sprite of the same world point is drawn at its world position plus the camera
            final ShadowList sprite = new ShadowList();
            sprite.addSilhouette(120 + pan, 60, odd, 6, 1.0, 1.0);
            final int[][] panned = FxToolkitSupport.onFx(() -> renderBoth(pass, canvas, fixed, new ShadowList(), 3.0, pan));
            final int[][] expected = FxToolkitSupport.onFx(() -> renderBoth(new FxShadowPass(), new Canvas(WIDTH, HEIGHT),
                    new ShadowList(), sprite, 3.0, pan));

            assertEquals(1, pass.staticRecasts(), "a pan of " + pan + " cast again");
            assertTrue(Arrays.deepEquals(expected, panned), "the shadow slipped off its sprite after a pan of " + pan);
        }
    }

    @Test
    void theImageOfALayerHasAPadThatRepeatsItsLastColumnAndRowSoTheNudgedSourceStaysInside() {
        final int wide = 5;
        final int high = 4;
        final int[] cells = new int[wide * high];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = 0xFF000000 | (i + 1);
        }
        final int[][] read = FxToolkitSupport.onFx(() -> {
            final javafx.scene.image.WritableImage image = FxShadowPass.newPaddedImage(wide, high);
            FxShadowPass.uploadPadded(image, wide, high, new int[]{0, 0, wide, high}, cells);
            final int[][] argb = new int[high + 1][wide + 1];
            for (int y = 0; y <= high; y++) {
                for (int x = 0; x <= wide; x++) {
                    argb[y][x] = image.getPixelReader().getArgb(x, y);
                }
            }
            return argb;
        });

        assertEquals(high + 1, read.length);
        assertEquals(wide + 1, read[0].length);
        for (int y = 0; y < high; y++) {
            assertEquals(cells[y * wide + wide - 1], read[y][wide], "the pad column repeats the last column, row " + y);
        }
        for (int x = 0; x < wide; x++) {
            assertEquals(cells[(high - 1) * wide + x], read[high][x], "the pad row repeats the last row, column " + x);
        }
        assertEquals(cells[cells.length - 1], read[high][wide], "the corner repeats the last cell");
        assertEquals(cells[0], read[0][0], "the cells themselves are untouched");
    }

    @Test
    void theMarginOfTheStaticWindowGrowsWithTheCanvasAndNeverFallsBelowTheMinimum() {
        // 240 by 160 at art scale 1 is 242 by 162 cells: a quarter is 61 across and 41 down
        assertEquals(61, FxShadowPass.marginFor(242));
        assertEquals(41, FxShadowPass.marginFor(162));
        assertEquals(FxShadowPass.WINDOW_MARGIN_CELLS, FxShadowPass.marginFor(122), "a small canvas keeps the minimum");
        assertEquals(481, FxShadowPass.marginFor(1922));

        final FxShadowPass pass = new FxShadowPass();
        pass.beginFrame(new CanvasView(1.0, 0, 0, WIDTH, HEIGHT));
        assertEquals(-62.0, pass.windowLeft(), 1e-9, "61 cells and the border cell");
        assertEquals(-42.0, pass.windowTop(), 1e-9, "41 cells and the border cell");
        assertEquals(242 + 2 * 61, pass.windowWidth(), 1e-9);
        assertEquals(162 + 2 * 41, pass.windowHeight(), 1e-9);
        assertTrue(pass.windowOverhangXPixels() >= 2 * 61, "the walk ring covers the window after a pan of the margin");
        assertTrue(pass.windowOverhangYPixels() >= 2 * 41, "the vertical ring follows the vertical margin");
        assertTrue(pass.windowOverhangYPixels() < pass.windowOverhangXPixels(), "each axis has its own overhang");
    }

    @Test
    void aPanJustUnderTheMarginDoesNotCastTheStaticLayerAndALargerOneCastsOnceAndRecentres() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        FxToolkitSupport.onFx(() -> drawEmpty(pass, canvas, 1.0, 0));
        assertEquals(1, pass.staticRecasts());
        // The window of the world, which a pan inside the margin leaves where it is, and its place on the screen when
        // it was cast around the camera
        final double worldLeft = pass.windowLeft() - pass.snappedOffsetX();
        final double centredLeft = pass.windowLeft();

        // The window reaches 61 cells past the canvas along X, so a camera 60 cells away is still inside it, both ways
        for (double origin : new double[]{-60, 60, -30, 0}) {
            FxToolkitSupport.onFx(() -> drawEmpty(pass, canvas, 1.0, origin));
            assertEquals(1, pass.staticRecasts(), "a pan of " + origin + " cells inside the margin cast again");
            assertEquals(worldLeft, pass.windowLeft() - pass.snappedOffsetX(), 1e-9, "the window did not move");
        }
        FxToolkitSupport.onFx(() -> drawEmpty(pass, canvas, 1.0, -62));

        assertEquals(2, pass.staticRecasts(), "a pan past the margin casts once");
        assertEquals(centredLeft, pass.windowLeft(), 1e-9, "the window is centred on the camera again");
        assertNotEquals(worldLeft, pass.windowLeft() - pass.snappedOffsetX(), "and it moved in the world");
        FxToolkitSupport.onFx(() -> drawEmpty(pass, canvas, 1.0, -62 - 60));
        assertEquals(2, pass.staticRecasts(), "and the new window holds again for a pan under the margin");
    }

    @Test
    void aStaticShadowOfAnOddDrawnWidthLandsWhereTheSpriteDoesAtEveryCameraOffset() {
        // 5 art pixels at scale 3 is 15 screen pixels: the middle of the sprite is half a pixel off the grid, so its left
        // edge is a tie that the rounding of the draw call settles by the parity of its position on the screen
        final Silhouette odd = block(5, 6);
        for (double origin : new double[]{0, 1, 2, 3, -1, -2, 7, 100, 101}) {
            final ShadowList moving = new ShadowList();
            moving.addSilhouette(120, 60, odd, 6, 1.0, 1.0);
            final ShadowList fixed = new ShadowList();
            // The renderer gives a static shadow relative to the camera, so the same screen position is this world one
            fixed.addSilhouette(120 - origin, 60, odd, 6, 1.0, 1.0);

            final int[][] drawnAsSprite = FxToolkitSupport.onFx(() -> renderBoth(new FxShadowPass(),
                    new Canvas(WIDTH, HEIGHT), new ShadowList(), moving, 3.0, origin));
            final int[][] drawnAsStatic = FxToolkitSupport.onFx(() -> renderBoth(new FxShadowPass(),
                    new Canvas(WIDTH, HEIGHT), fixed, new ShadowList(), 3.0, origin));

            assertTrue(Arrays.deepEquals(drawnAsSprite, drawnAsStatic), "the shadow slips a pixel at camera " + origin);
        }
    }

    /** Draws one frame with a static list that carries a version. */
    private static void drawVersioned(FxShadowPass pass, Canvas canvas, ShadowList fixed, long version) {
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        pass.draw(gc, fixed, version, new ShadowList(), new CanvasView(2.0, 0, 0, WIDTH, HEIGHT),
                new SunShadow(NOON, 1.0, 1.0));
    }

    @Test
    void aStaticListWithAVersionIsCastAgainOnlyWhenTheVersionChanges() {
        final FxShadowPass pass = new FxShadowPass();
        final Canvas canvas = FxToolkitSupport.onFx(() -> new Canvas(WIDTH, HEIGHT));
        final ShadowList list = oneBlock(60, 120);

        FxToolkitSupport.onFx(() -> {
            drawVersioned(pass, canvas, list, 7);
            drawVersioned(pass, canvas, list, 7);
            return null;
        });
        assertEquals(1, pass.staticRecasts(), "the same version is not compared shadow by shadow");

        // The content is not read while the version stays: the caller promises it did not change
        list.add(10, 20, 8, 1.0);
        FxToolkitSupport.onFx(() -> {
            drawVersioned(pass, canvas, list, 7);
            return null;
        });
        assertEquals(1, pass.staticRecasts());

        FxToolkitSupport.onFx(() -> {
            drawVersioned(pass, canvas, list, 8);
            return null;
        });
        assertEquals(2, pass.staticRecasts(), "a new version casts the list again");
    }
}
