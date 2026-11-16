package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.Flicker;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;
import com.feuerschvenger.perlinsedge.domain.world.light.PointLight;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightLayerTest {

    private static final Rgba NIGHT = new Rgba(0.03, 0.05, 0.16, 1.0);
    private static final double NIGHT_STRENGTH = 0.72;
    private static final Rgba WARM = new Rgba(1.0, 0.72, 0.38, 1.0);
    private static final LightStyle STYLE = new LightStyle(5, 0.20, 0.72);
    private static final int CANVAS_W = 480;
    private static final int CANVAS_H = 320;
    private static final double SCALE = 2.0;
    /** The camera puts tile (5, 5) at the middle of the canvas. */
    private static final double ORIGIN_X = 240;
    private static final double ORIGIN_Y = 20;

    private static int cellsWide() {
        return (int) Math.ceil(CANVAS_W / SCALE) + 2;
    }

    private static int cellsHigh() {
        return (int) Math.ceil(CANVAS_H / SCALE) + 2;
    }

    private static PointLight torch(double x, double y, Flicker flicker) {
        return new PointLight(x, y, 4.5, WARM, 1.0, flicker);
    }

    private static boolean compose(LightLayer layer, LightRegistry lights, double time, double strength) {
        return layer.compose(lights, STYLE, time, NIGHT, strength, SCALE, ORIGIN_X, ORIGIN_Y, cellsWide(), cellsHigh());
    }

    private static LightRegistry oneTorch() {
        final LightRegistry registry = new LightRegistry();
        registry.add(torch(5, 5, Flicker.STEADY));
        return registry;
    }

    private static int alpha(int pixel) {
        return pixel >>> 24;
    }

    @Test
    void noLightMeansNothingToDraw() {
        final LightLayer layer = new LightLayer();

        assertFalse(compose(layer, new LightRegistry(), 0.0, NIGHT_STRENGTH));
        assertFalse(layer.hasLights());
        assertEquals(0, layer.composes(), "no work was done");
    }

    @Test
    void aLightFarOutsideTheCanvasMeansNothingToDraw() {
        final LightLayer layer = new LightLayer();
        final LightRegistry registry = new LightRegistry();
        registry.add(torch(80, 5, Flicker.STEADY));

        assertFalse(compose(layer, registry, 0.0, NIGHT_STRENGTH));
        assertEquals(0, layer.composes());
    }

    @Test
    void theDarkIsUntouchedFarFromTheLightAndLightenedUnderIt() {
        final LightLayer layer = new LightLayer();

        assertTrue(compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH));

        // The corner of the box lies outside the circle of the light: the plain tint
        final int corner = layer.pixelAt(layer.boxLeft(), layer.boxTop());
        assertEquals(Math.round(NIGHT_STRENGTH * 255), alpha(corner), 1);
        // The cell on the light is nearly clear of the dark, and what is left of it is the warm glow
        final int centreColumn = (int) Math.round((ORIGIN_X - layer.phaseX()) / SCALE + 1);
        final int centreRow = (int) Math.round((((5 + 5) * 8 + 8) * SCALE + ORIGIN_Y - layer.phaseY()) / SCALE + 1);
        final int centre = layer.pixelAt(centreColumn, centreRow);
        assertTrue(alpha(centre) < alpha(corner) / 2, "alpha " + alpha(centre) + " against " + alpha(corner));
        assertTrue(((centre >> 16) & 0xFF) > (centre & 0xFF), "the light is warm: red over blue");
    }

    @Test
    void theBoxIsAsBigAsTheEllipseOfTheLightAndNoBigger() {
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH);

        // 4.5 tiles on the ground reach 4.5 * sqrt(2) * 16 = 102 art pixels across and half of that down
        assertEquals(2 * 4.5 * Math.sqrt(2) * 16, layer.boxRight() - layer.boxLeft(), 3.0);
        assertEquals(2 * 4.5 * Math.sqrt(2) * 8, layer.boxBottom() - layer.boxTop(), 3.0);
    }

    @Test
    void theLightIsCutIntoTheConfiguredNumberOfFlatRings() {
        for (int rings : new int[]{3, 5}) {
            final LightLayer layer = new LightLayer();
            final LightStyle style = new LightStyle(rings, 0.20, 0.72);
            layer.compose(oneTorch(), style, 0.0, NIGHT, NIGHT_STRENGTH, SCALE, ORIGIN_X, ORIGIN_Y, cellsWide(),
                    cellsHigh());

            final Set<Integer> levels = new HashSet<>();
            for (int y = layer.boxTop(); y < layer.boxBottom(); y++) {
                for (int x = layer.boxLeft(); x < layer.boxRight(); x++) {
                    levels.add(layer.pixelAt(x, y));
                }
            }
            assertTrue(levels.size() <= rings + 1, rings + " rings gave " + levels.size() + " distinct cells");
            assertTrue(levels.size() >= rings, "the rings are all there: " + levels.size());
        }
    }

    @Test
    void theLightNeverBrightensBeyondFullAndNeverDarkensTheTint() {
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH);

        for (int y = layer.boxTop(); y < layer.boxBottom(); y++) {
            for (int x = layer.boxLeft(); x < layer.boxRight(); x++) {
                final int pixel = layer.pixelAt(x, y);
                assertTrue(alpha(pixel) <= Math.round(NIGHT_STRENGTH * 255) + 1, "alpha " + alpha(pixel));
                assertTrue(((pixel >> 16) & 0xFF) <= alpha(pixel) && ((pixel >> 8) & 0xFF) <= alpha(pixel)
                        && (pixel & 0xFF) <= alpha(pixel), "premultiplied colour never above the alpha");
            }
        }
    }

    @Test
    void theEffectOfALightScalesWithTheStrengthOfTheTint() {
        final LightLayer deep = new LightLayer();
        final LightLayer dusk = new LightLayer();
        final LightLayer noon = new LightLayer();
        compose(deep, oneTorch(), 0.0, NIGHT_STRENGTH);
        compose(dusk, oneTorch(), 0.0, 0.25);
        compose(noon, oneTorch(), 0.0, 0.0);
        final int column = (int) Math.round((ORIGIN_X - deep.phaseX()) / SCALE + 1);
        final int row = (int) Math.round((((5 + 5) * 8 + 8) * SCALE + ORIGIN_Y - deep.phaseY()) / SCALE + 1);
        final int ring = column + 40;

        final int deepLift = alpha(deep.pixelAt(deep.boxLeft(), deep.boxTop())) - alpha(deep.pixelAt(ring, row));
        final int duskLift = alpha(dusk.pixelAt(dusk.boxLeft(), dusk.boxTop())) - alpha(dusk.pixelAt(ring, row));

        assertTrue(deepLift > 0 && duskLift > 0, "both are lit: " + deepLift + " and " + duskLift);
        assertTrue(duskLift < deepLift / 2.0, "a weak tint has little to push back: " + duskLift + " against " + deepLift);
        for (int y = noon.boxTop(); y < noon.boxBottom(); y++) {
            for (int x = noon.boxLeft(); x < noon.boxRight(); x++) {
                assertEquals(0, noon.pixelAt(x, y), "no tint, no darkness, no glow at " + x + "," + y);
            }
        }
    }

    @Test
    void twoLightsCombineWithoutGoingPastFullAndLightMoreThanOne() {
        final LightLayer alone = new LightLayer();
        final LightLayer both = new LightLayer();
        final LightRegistry pair = new LightRegistry();
        pair.add(torch(5, 5, Flicker.STEADY));
        pair.add(torch(6, 5, Flicker.STEADY));
        compose(alone, oneTorch(), 0.0, NIGHT_STRENGTH);
        compose(both, pair, 0.0, NIGHT_STRENGTH);

        int lighter = 0;
        for (int y = alone.boxTop(); y < alone.boxBottom(); y++) {
            for (int x = alone.boxLeft(); x < alone.boxRight(); x++) {
                final int one = alpha(alone.pixelAt(x, y));
                final int two = alpha(both.pixelAt(x, y));
                // The warm glow of a bright ring adds alpha of its own, so only the dim outer rings, which have none,
                // are compared
                if (one >= 100) {
                    assertTrue(two <= one + 1, "a second light never darkens at " + x + "," + y);
                }
                lighter += two < one ? 1 : 0;
            }
        }
        assertTrue(lighter > 100, "the second light lit " + lighter + " more cells");
        assertEquals(2, both.lightCount());
    }

    @Test
    void aCracklingLightMovesItsRingsAndASteadyOneDoesNot() {
        final LightRegistry steady = oneTorch();
        final LightRegistry crackling = new LightRegistry();
        crackling.add(torch(5, 5, Flicker.crackling(0.10, 0.05, 3.2, 3)));
        final LightLayer steadyLayer = new LightLayer();
        final LightLayer cracklingLayer = new LightLayer();

        compose(steadyLayer, steady, 0.0, NIGHT_STRENGTH);
        compose(cracklingLayer, crackling, 0.0, NIGHT_STRENGTH);
        final int steadyFirst = steadyLayer.composes();
        final int cracklingFirst = cracklingLayer.composes();
        final int[] first = cracklingLayer.pixels().clone();
        for (double time = 0.1; time < 1.0; time += 0.1) {
            compose(steadyLayer, steady, time, NIGHT_STRENGTH);
            compose(cracklingLayer, crackling, time, NIGHT_STRENGTH);
        }

        assertEquals(steadyFirst, steadyLayer.composes(), "a steady light is not worked out again");
        assertTrue(cracklingLayer.composes() > cracklingFirst + 3, "the bands moved: " + cracklingLayer.composes());
        assertNotEquals(java.util.Arrays.hashCode(first), java.util.Arrays.hashCode(cracklingLayer.pixels()));
    }

    @Test
    void theSameFrameTwiceIsNotWorkedOutAgainAndAMovedLightIs() {
        final LightLayer layer = new LightLayer();
        final LightRegistry registry = oneTorch();

        compose(layer, registry, 0.0, NIGHT_STRENGTH);
        assertTrue(layer.changed());
        compose(layer, registry, 0.0, NIGHT_STRENGTH);
        assertFalse(layer.changed());
        assertEquals(1, layer.composes());

        registry.clear();
        registry.add(torch(5.5, 5, Flicker.STEADY));
        compose(layer, registry, 0.0, NIGHT_STRENGTH);
        assertTrue(layer.changed());
        assertEquals(2, layer.composes());
        compose(layer, registry, 0.0, 0.7);
        assertTrue(layer.changed(), "a tint that changed");
    }

    @Test
    void theRingsFollowTheLightByWholeCellsWhenTheCameraMovesByWholeArtPixels() {
        final LightLayer layer = new LightLayer();
        final LightLayer moved = new LightLayer();
        layer.compose(oneTorch(), STYLE, 0.0, NIGHT, NIGHT_STRENGTH, SCALE, ORIGIN_X, ORIGIN_Y, cellsWide(), cellsHigh());
        moved.compose(oneTorch(), STYLE, 0.0, NIGHT, NIGHT_STRENGTH, SCALE, ORIGIN_X + 10, ORIGIN_Y, cellsWide(),
                cellsHigh());

        assertEquals(layer.boxRight() - layer.boxLeft(), moved.boxRight() - moved.boxLeft());
        for (int y = layer.boxTop(); y < layer.boxBottom(); y++) {
            for (int x = layer.boxLeft(); x < layer.boxRight(); x++) {
                assertEquals(layer.pixelAt(x, y), moved.pixelAt(x + 5, y), "a camera step of five art pixels at " + x);
            }
        }
    }

    @Test
    void aCanvasOfAnotherSizeIsWorkedOutAgainFromScratch() {
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH);

        assertTrue(layer.compose(oneTorch(), STYLE, 0.0, NIGHT, NIGHT_STRENGTH, SCALE, ORIGIN_X, ORIGIN_Y,
                cellsWide() + 10, cellsHigh()));

        assertTrue(layer.changed());
        assertEquals(cellsWide() + 10, layer.width());
        assertEquals(2, layer.composes());
    }
}
