package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.Flicker;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.PointLight;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycleSettings;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.LightLayer;
import com.feuerschvenger.perlinsedge.ui.view.FxToolkitSupport;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxSkyTintTest {

    private static final double DAWN = 90;
    private static final double DAY = 600;
    private static final double DUSK = 90;
    private static final double NIGHT = 420;

    private static DayCycle at(double seconds) {
        final DayCycle cycle = new DayCycle(new DayCycleSettings(DAWN, DAY, DUSK, NIGHT, 0, 17, 0.6, 2.0));
        cycle.advance(seconds);
        return cycle;
    }

    /** Lays the tint of a moment on a mid grey canvas and returns the colour of its centre as {r, g, b} in 0 to 255. */
    private static int[] centreOver(FxSkyTint tint, DayCycle sky) {
        return FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(8, 8);
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.rgb(128, 128, 128));
            gc.fillRect(0, 0, 8, 8);
            tint.draw(gc, sky, new LightRegistry(), 0.0, 2.0, 0, 0, 8, 8);
            final int argb = canvas.snapshot(null, null).getPixelReader().getArgb(4, 4);
            return new int[]{(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
        });
    }

    @Test
    void midDayLeavesTheWorldUntouched() {
        final int[] noon = centreOver(new FxSkyTint(), at(390));

        assertEquals(128, noon[0], 1);
        assertEquals(128, noon[1], 1);
        assertEquals(128, noon[2], 1);
    }

    @Test
    void nightDarkensTheWorldTowardBlueWithoutBlackingItOut() {
        final int[] night = centreOver(new FxSkyTint(), at(DAWN + DAY + DUSK + NIGHT / 2));

        assertTrue(night[2] > night[0] && night[2] > night[1], "blue wins");
        assertTrue(night[0] < 70 && night[1] < 70 && night[2] < 90, "clearly darker than the mid grey below");
        assertTrue(night[0] > 30 && night[1] > 30 && night[2] > 55, "not black, so the night stays barely playable");
    }

    @Test
    void dawnAndDuskShiftTheWorldTowardWarm() {
        final int[] dawn = centreOver(new FxSkyTint(), at(DAWN / 2));
        final int[] dusk = centreOver(new FxSkyTint(), at(DAWN + DAY + DUSK / 2));

        assertTrue(dawn[0] > dawn[2], "red over blue at dawn");
        assertTrue(dusk[0] > dusk[2], "red over blue at dusk");
    }

    @Test
    void aSecondFrameOfTheSameSkyGivesTheSameColour() {
        final FxSkyTint tint = new FxSkyTint();
        final DayCycle night = at(DAWN + DAY + DUSK + 100);

        assertEquals(centreOver(tint, night)[2], centreOver(tint, night)[2]);
    }

    @Test
    void aSecondFrameOfTheSameSkyReusesTheColourInsteadOfBuildingANewOne() {
        final FxSkyTint tint = new FxSkyTint();
        final DayCycle night = at(DAWN + DAY + DUSK + 100);

        centreOver(tint, night);
        final Color first = tint.colour();
        centreOver(tint, night);

        assertSame(first, tint.colour());
        assertEquals(0.72, first.getOpacity(), 0.01);
    }

    private static final int W = 240;
    private static final int H = 160;
    private static final Rgba WARM = new Rgba(1.0, 0.72, 0.38, 1.0);

    private static LightRegistry torchAt(double x, double y, double radius) {
        final LightRegistry registry = new LightRegistry();
        registry.add(new PointLight(x, y, radius, WARM, 1.0, Flicker.STEADY));
        return registry;
    }

    /** Lays the tint of a moment and its lights on a mid grey canvas and reads every pixel back as a grey level. */
    private static int[][] greysWithLights(FxSkyTint tint, DayCycle sky, LightRegistry lights, double scale) {
        return FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(W, H);
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setImageSmoothing(false);
            gc.setFill(Color.rgb(128, 128, 128));
            gc.fillRect(0, 0, W, H);
            // The origin is a multiple of every art scale used here, so the cells start at the canvas edge
            tint.draw(gc, sky, lights, 0.0, scale, 120, -24, W, H);
            final PixelReader reader = canvas.snapshot(null, null).getPixelReader();
            final int[][] grey = new int[H][W];
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    final int argb = reader.getArgb(x, y);
                    grey[y][x] = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
                }
            }
            return grey;
        });
    }

    private static DayCycle night() {
        return at(DAWN + DAY + DUSK + NIGHT / 2);
    }

    @Test
    void aLightPushesBackTheNightAroundItAndLeavesTheFarWorldAsDark() {
        final int[][] unlit = greysWithLights(new FxSkyTint(), night(), new LightRegistry(), 2.0);
        // Tile (3, 3) lies at x 120 on the canvas and at y 88 for this camera
        final int[][] lit = greysWithLights(new FxSkyTint(), night(), torchAt(3, 3, 2.5), 2.0);

        assertEquals(unlit[5][5], lit[5][5], 2, "the far corner is as dark as without the light");
        assertEquals(unlit[150][230], lit[150][230], 2);
        assertTrue(lit[88][120] > unlit[88][120] + 40, "the light is clearly brighter: " + lit[88][120]);
        assertTrue(lit[88][120] > lit[88][180], "brighter at the light than further out");
    }

    @Test
    void theTorchWarmsTheWorldItLights() {
        final int[] centre = FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(W, H);
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setImageSmoothing(false);
            gc.setFill(Color.rgb(128, 128, 128));
            gc.fillRect(0, 0, W, H);
            new FxSkyTint().draw(gc, night(), torchAt(3, 3, 2.5), 0.0, 2.0, 120, -24, W, H);
            final int argb = canvas.snapshot(null, null).getPixelReader().getArgb(120, 88);
            return new int[]{(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
        });

        assertTrue(centre[0] > centre[2] + 20, "red over blue under a warm light: " + centre[0] + " " + centre[2]);
    }

    @Test
    void aLightChangesNothingAtMiddayAndDoesNoWork() {
        final FxSkyTint tint = new FxSkyTint();
        final int[][] noon = greysWithLights(tint, at(390), torchAt(3, 3, 2.5), 2.0);

        for (int[] row : noon) {
            for (int value : row) {
                assertEquals(128, value, 1, "no tint to push back at midday");
            }
        }
        assertEquals(0, tint.layer().composes());
    }

    @Test
    void withoutLightsTheNightIsOnePlainFillAndNoLightWorkIsDone() {
        final FxSkyTint tint = new FxSkyTint();
        final int[][] plain = greysWithLights(tint, night(), new LightRegistry(), 2.0);
        final int[] flat = centreOver(new FxSkyTint(), night());

        assertEquals(0, tint.layer().composes());
        assertEquals((flat[0] + flat[1] + flat[2]) / 3, plain[80][100], 2);
        assertEquals(plain[0][0], plain[159][239]);
    }

    @Test
    void aLightOutsideTheCanvasIsAPlainFillToo() {
        final FxSkyTint tint = new FxSkyTint();
        final int[][] far = greysWithLights(tint, night(), torchAt(90, 3, 2.5), 2.0);

        assertEquals(0, tint.layer().composes());
        assertEquals(far[0][0], far[159][239]);
    }

    @Test
    void everyRingOfALightIsACrispBlockOfTheArtScaleAtEveryScale() {
        for (double scale : new double[]{2.0, 3.0, 4.0}) {
            final int[][] unlit = greysWithLights(new FxSkyTint(), night(), new LightRegistry(), scale);
            final int[][] lit = greysWithLights(new FxSkyTint(), night(), torchAt(3, 3, 2.5), scale);

            final int block = (int) scale;
            final Set<Integer> levels = new HashSet<>();
            int differing = 0;
            for (int by = 0; by + block <= H; by += block) {
                for (int bx = 0; bx + block <= W; bx += block) {
                    final int value = lit[by][bx];
                    levels.add(value);
                    differing += Math.abs(value - unlit[by][bx]) > 2 ? 1 : 0;
                    for (int dy = 0; dy < block; dy++) {
                        for (int dx = 0; dx < block; dx++) {
                            // Some pipelines (Direct3D on a runner without a GPU) shade a canvas corner pixel of a
                            // plain fill one level apart; the light must add no unevenness of its own on top of that
                            final int backgroundStep = unlit[by + dy][bx + dx] - unlit[by][bx];
                            assertEquals(value + backgroundStep, lit[by + dy][bx + dx],
                                    "scale " + scale + " block " + bx + "," + by + " must be one flat colour");
                        }
                    }
                }
            }
            assertTrue(differing > 50, "scale " + scale + " lit " + differing + " blocks");
            assertTrue(levels.size() >= 4, "scale " + scale + " shows the rings: " + levels.size());
            assertEquals(unlit[5][5], lit[5][5], 2, "scale " + scale + ": the far world keeps the plain tint");
        }
    }

    @Test
    void aSecondFrameOfTheSameLightsIsNotWorkedOutAgain() {
        final FxSkyTint tint = new FxSkyTint();
        final LightRegistry lights = torchAt(3, 3, 2.5);
        final int[][] first = greysWithLights(tint, night(), lights, 2.0);
        final int[][] second = greysWithLights(tint, night(), lights, 2.0);

        assertEquals(1, tint.layer().composes());
        assertTrue(java.util.Arrays.deepEquals(first, second), "the kept cells draw exactly as before");
    }

    /** Lays the tint and its lights on a mid grey canvas and reads every pixel back as ARGB. */
    private static int[][] argbWithLights(FxSkyTint tint, LightRegistry lights, double scale, double originX,
                                          double originY) {
        return FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(W, H);
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setImageSmoothing(false);
            gc.setFill(Color.rgb(128, 128, 128));
            gc.fillRect(0, 0, W, H);
            tint.draw(gc, night(), lights, 0.0, scale, originX, originY, W, H);
            final PixelReader reader = canvas.snapshot(null, null).getPixelReader();
            final int[][] argb = new int[H][W];
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    argb[y][x] = reader.getArgb(x, y);
                }
            }
            return argb;
        });
    }

    /** The biggest difference of any channel between two pixels. */
    private static int apart(int a, int b) {
        int biggest = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            biggest = Math.max(biggest, Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF)));
        }
        return biggest;
    }

    @Test
    void thePlainTintAroundTheLitBoxMeetsItsCellsWithoutASeamAtEveryScaleAndOddCamera() {
        for (double scale : new double[]{2.0, 3.0, 4.0}) {
            for (double originX : new double[]{119.0, 121.0, 123.0}) {
                // The torch is at the middle of the canvas, with an odd offset along Y as well
                final double originY = 80 - 56 * scale + 1;
                final FxSkyTint tint = new FxSkyTint();
                final int[][] argb = argbWithLights(tint, torchAt(3, 3, 1.2), scale, originX, originY);
                final LightLayer layer = tint.layer();
                final String at = "scale " + scale + " origin " + originX + "," + originY;

                final double left = layer.phaseX() + (layer.regionLeft(0) - 1) * scale;
                final double top = layer.phaseY() + (layer.regionTop(0) - 1) * scale;
                final double right = layer.phaseX() + (layer.regionRight(0) - 1) * scale;
                final double bottom = layer.phaseY() + (layer.regionBottom(0) - 1) * scale;
                assertTrue(left > 0 && top > 0 && right < W && bottom < H, "the box is inside the canvas: " + at);
                // The corners of the box are no part of the light, so the cells there are the plain tint
                for (int row : new int[]{(int) top, (int) bottom - 1}) {
                    assertTrue(apart(argb[row][(int) left - 1], argb[row][(int) left]) <= 1, "left edge, row " + row + " " + at);
                    assertTrue(apart(argb[row][(int) right - 1], argb[row][(int) right]) <= 1, "right edge, row " + row + " " + at);
                }
                for (int column : new int[]{(int) left, (int) right - 1}) {
                    assertTrue(apart(argb[(int) top - 1][column], argb[(int) top][column]) <= 1, "top edge, column " + column + " " + at);
                    assertTrue(apart(argb[(int) bottom - 1][column], argb[(int) bottom][column]) <= 1, "bottom edge, column " + column + " " + at);
                }
            }
        }
    }

    @Test
    void aBoxThatTouchesTheRightAndBottomEdgeOfTheCanvasDrawsItsLastColumnAndRow() {
        final double scale = 2.0;
        // A light near the bottom right corner: its box is held to the grid, which reaches past the canvas
        final LightRegistry corner = torchAt(6.9, 3.3, 2.5);
        final FxSkyTint tint = new FxSkyTint();
        final int[][] lit = argbWithLights(tint, corner, scale, 120, -24);
        final int[][] unlit = argbWithLights(new FxSkyTint(), new LightRegistry(), scale, 120, -24);
        final LightLayer layer = tint.layer();

        assertEquals(layer.width(), layer.regionRight(0), "the box touches the right edge of the cells");
        assertEquals(layer.height(), layer.regionBottom(0), "and the bottom edge");
        // The top right and the bottom left corners of the box lie outside the light, whatever the edge does
        final int topRow = (int) (layer.phaseY() + (layer.regionTop(0) - 1) * scale);
        final int leftColumn = (int) (layer.phaseX() + (layer.regionLeft(0) - 1) * scale);
        assertTrue(apart(lit[topRow][W - 1], unlit[topRow][W - 1]) <= 1, "last column " + Integer.toHexString(lit[topRow][W - 1]));
        assertTrue(apart(lit[H - 1][leftColumn], unlit[H - 1][leftColumn]) <= 1, "last row " + Integer.toHexString(lit[H - 1][leftColumn]));
    }

    @Test
    void lightsFarApartAreTwoRegionsWithThePlainTintBetweenThem() {
        final LightRegistry pair = new LightRegistry();
        pair.add(new PointLight(3, 6, 2.0, WARM, 1.0, Flicker.STEADY));
        pair.add(new PointLight(6, 3, 2.0, WARM, 1.0, Flicker.STEADY));
        final FxSkyTint tint = new FxSkyTint();
        final int[][] lit = argbWithLights(tint, pair, 2.0, 120, -24);
        final int[][] unlit = argbWithLights(new FxSkyTint(), new LightRegistry(), 2.0, 120, -24);

        assertEquals(2, tint.layer().regionCount());
        // Tile (3, 6) is at x 24, y 136 on this camera and tile (6, 3) at x 216, y 136
        assertTrue(apart(lit[136][24], unlit[136][24]) > 20, "the first light");
        assertTrue(apart(lit[136][216], unlit[136][216]) > 20, "the second light");
        assertTrue(apart(unlit[136][120], lit[136][120]) <= 1, "between the two it is the plain tint");
        assertTrue(apart(unlit[2][120], lit[2][120]) <= 1);
        assertTrue(apart(unlit[157][120], lit[157][120]) <= 1);
    }

    /** Lays the tint of a moment and its lights on a mid grey canvas and returns the pixel of a corner as ARGB. */
    private static int cornerOver(DayCycle sky, LightRegistry lights) {
        return FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(W, H);
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setImageSmoothing(false);
            gc.setFill(Color.rgb(128, 128, 128));
            gc.fillRect(0, 0, W, H);
            new FxSkyTint().draw(gc, sky, lights, 0.0, 2.0, 120, -24, W, H);
            return canvas.snapshot(null, null).getPixelReader().getArgb(2, 2);
        });
    }

    @Test
    void theFillWithLightsAndTheFillWithoutThemAreTheSameColourAtEveryMomentOfTheDay() {
        for (double seconds = 3; seconds < DAWN + DAY + DUSK + NIGHT; seconds += 13.7) {
            final DayCycle sky = at(seconds);
            if (sky.skyTintStrength() < DayCycle.MIN_VISIBLE_TINT_STRENGTH) {
                continue;
            }
            assertEquals(Integer.toHexString(cornerOver(sky, new LightRegistry())),
                    Integer.toHexString(cornerOver(sky, torchAt(3, 3, 2.5))),
                    "the plain tint stepped when a light came into view at " + seconds + " s");
        }
    }

    @Test
    void aSkyThatMovedGetsANewColour() {
        final FxSkyTint tint = new FxSkyTint();
        final DayCycle sky = at(10);

        centreOver(tint, sky);
        final Color first = tint.colour();
        sky.advance(5);
        centreOver(tint, sky);

        assertNotSame(first, tint.colour());
    }
}
