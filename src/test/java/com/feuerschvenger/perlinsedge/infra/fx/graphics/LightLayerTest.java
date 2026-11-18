package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.Flicker;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;
import com.feuerschvenger.perlinsedge.domain.world.light.PointLight;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightLayerTest {

    private static final Rgba NIGHT = new Rgba(0.03, 0.05, 0.16, 1.0);
    private static final double NIGHT_STRENGTH = 0.72;
    private static final Rgba DUSK = new Rgba(1.0, 0.48, 0.14, 1.0);
    private static final double DUSK_STRENGTH = 0.25;
    private static final Rgba WARM = new Rgba(1.0, 0.72, 0.38, 1.0);
    private static final double WARM_STRENGTH = 0.16;
    private static final LightStyle STYLE = new LightStyle(WARM_STRENGTH, 0.72);
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

    private static boolean compose(LightLayer layer, LightRegistry lights, double time, Rgba tint, double strength) {
        return layer.compose(lights, STYLE, time, tint, strength,
                new LightLayer.GridView(SCALE, ORIGIN_X, ORIGIN_Y, cellsWide(), cellsHigh()));
    }

    private static boolean compose(LightLayer layer, LightRegistry lights, double time, double strength) {
        return compose(layer, lights, time, NIGHT, strength);
    }

    private static LightRegistry oneTorch(Flicker flicker) {
        final LightRegistry registry = new LightRegistry();
        registry.add(torch(5, 5, flicker));
        return registry;
    }

    private static LightRegistry oneTorch() {
        return oneTorch(Flicker.STEADY);
    }

    private static int alpha(int pixel) {
        return pixel >>> 24;
    }

    private static int red(int pixel) {
        return (pixel >> 16) & 0xFF;
    }

    private static int green(int pixel) {
        return (pixel >> 8) & 0xFF;
    }

    private static int blue(int pixel) {
        return pixel & 0xFF;
    }

    /** The column and the row of the cell the torch at tile (5, 5) stands on. */
    private static int[] centreCell(LightLayer layer) {
        return new int[]{(int) Math.round((ORIGIN_X - layer.phaseX()) / SCALE + 1),
                (int) Math.round((((5 + 5) * 8 + 8) * SCALE + ORIGIN_Y - layer.phaseY()) / SCALE + 1)};
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
    void manyLightsOutsideTheCanvasAreDroppedAndChangeNothingForTheOnesInView() {
        final Flicker flame = Flicker.crackling(0.10, 0.05, 3.2, 5).withShape(0.1, 0.25);
        final LightRegistry crowd = oneTorch(flame);
        for (int i = 0; i < 200; i++) {
            crowd.add(torch(-80 - i, 5 + (i % 7), flame));
            crowd.add(torch(90 + i, 5 + (i % 7), flame));
        }
        final LightLayer alone = new LightLayer();
        final LightLayer among = new LightLayer();

        compose(alone, oneTorch(flame), 2.5, NIGHT_STRENGTH);
        compose(among, crowd, 2.5, NIGHT_STRENGTH);

        assertEquals(1, among.lightCount(), "only the light in view is kept");
        assertEquals(1, among.regionCount());
        assertTrue(Arrays.equals(alone.pixelsCopy(), among.pixelsCopy()), "the far lights change no cell");
    }

    @Test
    void aLightJustOutsideTheCanvasThatStillReachesInIsKept() {
        // The torch reaches about 100 cells: a light a few cells left of the grid still lights its edge
        final LightRegistry registry = new LightRegistry();
        registry.add(torch(-3.0, 5, Flicker.STEADY));
        final LightLayer layer = new LightLayer();

        assertTrue(compose(layer, registry, 0.0, NIGHT_STRENGTH));

        assertEquals(1, layer.lightCount());
        assertEquals(0, layer.regionLeft(0), "the box is held to the grid");
    }

    @Test
    void theDarkIsUntouchedFarFromTheLightAndTurnsWarmUnderIt() {
        final LightLayer layer = new LightLayer();

        assertTrue(compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH));

        // The corner of the box lies outside the circle of the light: the plain tint, which is blue
        final int corner = layer.pixelAt(layer.regionLeft(0), layer.regionTop(0));
        assertEquals((float) Math.round(NIGHT_STRENGTH * 255), (float) alpha(corner), 1.0f);
        assertTrue(blue(corner) > red(corner));
        final int[] at = centreCell(layer);
        final int centre = layer.pixelAt(at[0], at[1]);
        assertTrue(red(centre) > blue(centre), "the light is warm: red over blue");
        assertEquals((float) Math.round(WARM_STRENGTH * 255), (float) alpha(centre), 2.0f,
                "a modest warm wash at the centre");
    }

    @Test
    void aLightAtDuskIsAWarmWashAndNotAHoleInTheOrangeTint() {
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(), 0.0, DUSK, DUSK_STRENGTH);
        final double warm = WARM_STRENGTH * DUSK_STRENGTH / 0.72;
        final int floor = (int) Math.round(Math.min(warm, DUSK_STRENGTH) * 255) - 1;
        final int ceiling = (int) Math.round(Math.max(warm, DUSK_STRENGTH) * 255) + 1;

        final int[] at = centreCell(layer);
        final int centre = layer.pixelAt(at[0], at[1]);
        assertTrue(red(centre) > blue(centre), "warm, red over blue");
        assertTrue(alpha(centre) >= Math.round(warm * 255) - 1, "never closer to the raw ground than the warm strength");
        // The colour of the wash is the colour of the light, not the orange of the tint: more green and blue in it
        assertTrue((double) green(centre) / alpha(centre) > DUSK.g() + 0.05, "a lit cell takes the colour of the light");
        for (int y = layer.regionTop(0); y < layer.regionBottom(0); y++) {
            for (int x = layer.regionLeft(0); x < layer.regionRight(0); x++) {
                final int a = alpha(layer.pixelAt(x, y));
                assertTrue(a >= floor && a <= ceiling, "alpha " + a + " outside " + floor + " to " + ceiling);
            }
        }
    }

    @Test
    void theBoxIsAsBigAsTheEllipseOfTheLightAndNoBigger() {
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH);

        // 4.5 tiles on the ground reach 4.5 * sqrt(2) * 16 = 102 art pixels across and half of that down
        assertEquals(2 * 4.5 * Math.sqrt(2) * 16, layer.regionRight(0) - layer.regionLeft(0), 3.0);
        assertEquals(2 * 4.5 * Math.sqrt(2) * 8, layer.regionBottom(0) - layer.regionTop(0), 3.0);
    }

    @Test
    void theLightFadesOutwardWithoutAStepAndNeverRises() {
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH);
        final int[] at = centreCell(layer);

        // Along the row of the light, from its centre to the edge of the box, the overlay only thickens, and by a
        // few levels at a time
        int previous = alpha(layer.pixelAt(at[0], at[1]));
        int biggest = 0;
        for (int x = at[0] + 1; x < layer.regionRight(0); x++) {
            final int now = alpha(layer.pixelAt(x, at[1]));
            assertTrue(now >= previous, "the overlay thinned again at " + x);
            biggest = Math.max(biggest, now - previous);
            previous = now;
        }
        assertEquals((float) Math.round(NIGHT_STRENGTH * 255), (float) previous, 1.0f,
                "it reaches the plain tint at the edge");
        assertTrue(biggest <= 4, "the biggest step between neighbouring cells is " + biggest);
        // Down the column the same, and many distinct levels, not a few flat rings
        previous = alpha(layer.pixelAt(at[0], at[1]));
        for (int y = at[1] + 1; y < layer.regionBottom(0); y++) {
            final int now = alpha(layer.pixelAt(at[0], y));
            // A tile is half as tall as it is wide on the screen, so the gradient is twice as steep down as across
            assertTrue(now >= previous && now - previous <= 8, "step at " + y);
            previous = now;
        }
        final java.util.Set<Integer> levels = new java.util.HashSet<>();
        for (int x = at[0]; x < layer.regionRight(0); x++) {
            levels.add(alpha(layer.pixelAt(x, at[1])));
        }
        assertTrue(levels.size() > 40, "a gradient of " + levels.size() + " levels");
    }

    @Test
    void aShapedAndFlickeringFlameStaysContinuousBetweenNeighbouringCells() {
        final LightLayer layer = new LightLayer();
        final Flicker flame = Flicker.crackling(0.10, 0.05, 3.2, 5)
                .withColourDrift(new Rgba(1, 0.58, 0.25, 1), new Rgba(1, 0.82, 0.45, 1), 0.7).withShape(0.10, 0.25);
        compose(layer, oneTorch(flame), 3.7, NIGHT_STRENGTH);

        int biggest = 0;
        for (int y = layer.regionTop(0); y < layer.regionBottom(0); y++) {
            for (int x = layer.regionLeft(0); x + 1 < layer.regionRight(0); x++) {
                final int a = layer.pixelAt(x, y);
                final int b = layer.pixelAt(x + 1, y);
                biggest = Math.max(biggest, Math.max(Math.abs(alpha(a) - alpha(b)), Math.abs(red(a) - red(b))));
            }
        }
        assertTrue(biggest <= 7, "the biggest step between neighbouring cells is " + biggest);
    }

    /** How far from the centre, in art pixels along a direction, the overlay is still clearly pushed back. */
    private static double reach(LightLayer layer, double turns) {
        final int[] at = centreCell(layer);
        final double dx = Math.cos(turns * 2 * Math.PI);
        final double dy = Math.sin(turns * 2 * Math.PI) / 2;
        final int plain = (int) Math.round(NIGHT_STRENGTH * 255) - 10;
        double last = 0.0;
        for (double step = 1.0; step < 160.0; step += 0.5) {
            final int x = at[0] + (int) Math.round(dx * step);
            final int y = at[1] + (int) Math.round(dy * step);
            if (x < layer.regionLeft(0) || x >= layer.regionRight(0) || y < layer.regionTop(0)
                    || y >= layer.regionBottom(0)) {
                break;
            }
            if (alpha(layer.pixelAt(x, y)) < plain) {
                last = step;
            }
        }
        return last;
    }

    @Test
    void theRadiusVariesWithTheAngleWithinTheAmplitudeAndChangesSmoothlyOverTime() {
        final Flicker round = Flicker.crackling(0.0 + 1e-9, 0.0 + 1e-9, 1.0, 9);
        final Flicker shaped = round.withShape(0.10, 0.25);
        final LightLayer reference = new LightLayer();
        compose(reference, oneTorch(round), 0.0, NIGHT_STRENGTH);
        double lowest = Double.MAX_VALUE;
        double highest = 0.0;

        for (double time = 0.0; time < 30.0; time += 3.0) {
            final LightLayer layer = new LightLayer();
            compose(layer, oneTorch(shaped), time, NIGHT_STRENGTH);
            for (double turns = 0.0; turns < 1.0; turns += 0.0625) {
                final double ratio = reach(layer, turns) / reach(reference, turns);
                assertTrue(ratio > 0.86 && ratio < 1.14, "ratio " + ratio + " at " + turns + " turns, time " + time);
                lowest = Math.min(lowest, ratio);
                highest = Math.max(highest, ratio);
            }
        }
        assertTrue(highest - lowest > 0.10, "the edge is not a perfect ellipse: " + (highest - lowest));

        // Frame to frame the edge moves by a cell or two at most, and over seconds it changes
        final LightLayer now = new LightLayer();
        final LightLayer next = new LightLayer();
        final LightLayer later = new LightLayer();
        compose(now, oneTorch(shaped), 4.0, NIGHT_STRENGTH);
        compose(next, oneTorch(shaped), 4.0 + 1.0 / 60.0, NIGHT_STRENGTH);
        compose(later, oneTorch(shaped), 9.0, NIGHT_STRENGTH);
        double frameStep = 0.0;
        double overSeconds = 0.0;
        for (double turns = 0.0; turns < 1.0; turns += 0.0625) {
            frameStep = Math.max(frameStep, Math.abs(reach(now, turns) - reach(next, turns)));
            overSeconds = Math.max(overSeconds, Math.abs(reach(now, turns) - reach(later, turns)));
        }
        assertTrue(frameStep <= 2.0, "a frame moved the edge by " + frameStep);
        assertTrue(overSeconds > 3.0, "seconds later the edge is elsewhere: " + overSeconds);
    }

    @Test
    void theColourOfALightStaysInTheOrangeToYellowRangeAndDriftsSmoothly() {
        final Flicker flame = Flicker.crackling(0.10, 0.05, 3.2, 6)
                .withColourDrift(new Rgba(1, 0.58, 0.25, 1), new Rgba(1, 0.82, 0.45, 1), 0.7);
        double lowest = 1.0;
        double highest = 0.0;
        double previous = Double.NaN;
        double biggest = 0.0;
        for (double time = 0.0; time < 20.0; time += 1.0 / 60.0) {
            final LightLayer layer = new LightLayer();
            compose(layer, oneTorch(flame), time, NIGHT_STRENGTH);
            final int[] at = centreCell(layer);
            final int centre = layer.pixelAt(at[0], at[1]);
            final double a = alpha(centre);
            final double g = green(centre) / a;
            // The light at its dimmest moment is nine tenths of the way from the tint to its colour
            assertTrue(red(centre) / a > 0.88, "red stays nearly full");
            assertTrue(g > 0.50 && g < 0.88, "green " + g + " is in the orange to yellow range");
            assertTrue(blue(centre) / a > 0.17 && blue(centre) / a < 0.51, "blue " + blue(centre) / a);
            lowest = Math.min(lowest, g);
            highest = Math.max(highest, g);
            if (!Double.isNaN(previous)) {
                biggest = Math.max(biggest, Math.abs(g - previous));
            }
            previous = g;
        }
        assertTrue(highest - lowest > 0.12, "the colour drifts: " + (highest - lowest));
        assertTrue(biggest < 0.08, "smooth from frame to frame: " + biggest);
    }

    @Test
    void theEffectOfALightScalesWithTheDarknessAndIsNothingAtNoon() {
        final LightLayer deep = new LightLayer();
        final LightLayer dusk = new LightLayer();
        final LightLayer noon = new LightLayer();
        compose(deep, oneTorch(), 0.0, NIGHT, NIGHT_STRENGTH);
        compose(dusk, oneTorch(), 0.0, NIGHT, 0.25);
        compose(noon, oneTorch(), 0.0, NIGHT, 0.0);
        final int[] at = centreCell(deep);

        // The wash at the centre is the warm strength scaled by how dark it is
        assertEquals(WARM_STRENGTH * 255, alpha(deep.pixelAt(at[0], at[1])), 2.0);
        assertEquals(WARM_STRENGTH * 255 * 0.25 / 0.72, alpha(dusk.pixelAt(at[0], at[1])), 2.0);
        for (int y = noon.regionTop(0); y < noon.regionBottom(0); y++) {
            for (int x = noon.regionLeft(0); x < noon.regionRight(0); x++) {
                assertEquals(0, noon.pixelAt(x, y), "no tint, no darkness, no wash at " + x + "," + y);
            }
        }
    }

    @Test
    void twoLightsCombineTheirLightAndWeighTheirColoursByContribution() {
        final LightLayer alone = new LightLayer();
        final LightLayer both = new LightLayer();
        final LightRegistry pair = new LightRegistry();
        pair.add(new PointLight(5, 5, 4.5, new Rgba(1, 0.1, 0.1, 1), 1.0, Flicker.STEADY));
        pair.add(new PointLight(8, 5, 4.5, new Rgba(0.1, 0.1, 1, 1), 1.0, Flicker.STEADY));
        final LightRegistry first = new LightRegistry();
        first.add(pair.get(0));
        compose(alone, first, 0.0, NIGHT_STRENGTH);
        compose(both, pair, 0.0, NIGHT_STRENGTH);

        int thicker = 0;
        for (int y = alone.regionTop(0); y < alone.regionBottom(0); y++) {
            for (int x = alone.regionLeft(0); x < alone.regionRight(0); x++) {
                final int one = alpha(alone.pixelAt(x, y));
                final int two = alpha(both.pixelAt(x, y));
                assertTrue(two <= one + 1, "a second light never darkens at " + x + "," + y);
                thicker += two > one + 1 ? 1 : 0;
            }
        }
        assertEquals(0, thicker);
        assertEquals(2, both.lightCount());
        final int[] at = centreCell(alone);
        final int nearFirst = both.pixelAt(at[0], at[1]);
        assertTrue(red(nearFirst) > blue(nearFirst), "the first light colours the cells it lights most");
        // The second light is three tiles along the ground, which is 48 cells across and 24 down on the screen
        final int nearSecond = both.pixelAt(at[0] + 48, at[1] + 24);
        assertTrue(blue(nearSecond) > red(nearSecond), "the second light colours its own cells");
        final int between = both.pixelAt(at[0] + 24, at[1] + 12);
        assertTrue(red(between) > 0 && blue(between) > 0 && Math.abs(red(between) - blue(between)) < alpha(between),
                "in between the two colours mix");
    }

    @Test
    void aCracklingLightMovesItsEdgeAndASteadyOneDoesNot() {
        final LightRegistry steady = oneTorch();
        final LightRegistry crackling = oneTorch(Flicker.crackling(0.10, 0.05, 3.2, 3));
        final LightLayer steadyLayer = new LightLayer();
        final LightLayer cracklingLayer = new LightLayer();

        compose(steadyLayer, steady, 0.0, NIGHT_STRENGTH);
        compose(cracklingLayer, crackling, 0.0, NIGHT_STRENGTH);
        final int steadyFirst = steadyLayer.composes();
        final int cracklingFirst = cracklingLayer.composes();
        final int[] first = cracklingLayer.pixelsCopy();
        for (double time = 0.1; time < 1.0; time += 0.1) {
            compose(steadyLayer, steady, time, NIGHT_STRENGTH);
            compose(cracklingLayer, crackling, time, NIGHT_STRENGTH);
        }

        assertEquals(steadyFirst, steadyLayer.composes(), "a steady light is not worked out again");
        assertTrue(cracklingLayer.composes() > cracklingFirst + 3, "the light moved: " + cracklingLayer.composes());
        assertNotEquals(Arrays.hashCode(first), Arrays.hashCode(cracklingLayer.pixelsCopy()));
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
    void theGradientFollowsTheLightByWholeCellsWhenTheCameraMovesByWholeArtPixels() {
        final LightLayer layer = new LightLayer();
        final LightLayer moved = new LightLayer();
        layer.compose(oneTorch(), STYLE, 0.0, NIGHT, NIGHT_STRENGTH,
                new LightLayer.GridView(SCALE, ORIGIN_X, ORIGIN_Y, cellsWide(), cellsHigh()));
        moved.compose(oneTorch(), STYLE, 0.0, NIGHT, NIGHT_STRENGTH,
                new LightLayer.GridView(SCALE, ORIGIN_X + 10, ORIGIN_Y, cellsWide(), cellsHigh()));

        assertEquals(layer.regionRight(0) - layer.regionLeft(0), moved.regionRight(0) - moved.regionLeft(0));
        for (int y = layer.regionTop(0); y < layer.regionBottom(0); y++) {
            for (int x = layer.regionLeft(0); x < layer.regionRight(0); x++) {
                assertEquals(layer.pixelAt(x, y), moved.pixelAt(x + 5, y), "a camera step of five art pixels at " + x);
            }
        }
    }

    private static PointLight small(double x, double y, Flicker flicker) {
        return new PointLight(x, y, 2.0, WARM, 1.0, flicker);
    }

    @Test
    void lightsThatOverlapAreOneRegionAndAChainOfThemIsOneToo() {
        final LightLayer pair = new LightLayer();
        final LightRegistry close = new LightRegistry();
        close.add(torch(5, 5, Flicker.STEADY));
        close.add(torch(8, 5, Flicker.STEADY));
        compose(pair, close, 0.0, NIGHT_STRENGTH);
        assertEquals(1, pair.regionCount());

        // The ends of the chain are far apart, and the one in the middle overlaps both
        final LightLayer chain = new LightLayer();
        final LightRegistry three = new LightRegistry();
        three.add(small(3, 8, Flicker.STEADY));
        three.add(small(8, 3, Flicker.STEADY));
        three.add(small(5.5, 5.5, Flicker.STEADY));
        compose(chain, three, 0.0, NIGHT_STRENGTH);
        assertEquals(1, chain.regionCount());
        assertEquals(3, chain.lightCount());
    }

    @Test
    void lightsFarApartAreWorkedOutInTheirOwnBoxesAndNotInTheStretchBetweenThem() {
        final Flicker shaped = Flicker.crackling(0.0 + 1e-9, 0.0 + 1e-9, 1.0, 9).withShape(0.2, 0.25);
        final LightRegistry pair = new LightRegistry();
        pair.add(small(1.5, 5.5, shaped));
        pair.add(small(9.5, 5.5, Flicker.STEADY));
        final LightLayer both = new LightLayer();
        compose(both, pair, 1.5, NIGHT_STRENGTH);

        assertEquals(2, both.regionCount());
        assertEquals(2, both.lightCount());
        final int[] widths = {both.regionRight(0) - both.regionLeft(0), both.regionRight(1) - both.regionLeft(1)};
        final int unionWide = Math.max(both.regionRight(0), both.regionRight(1))
                - Math.min(both.regionLeft(0), both.regionLeft(1));
        final int unionHigh = Math.max(both.regionBottom(0), both.regionBottom(1))
                - Math.min(both.regionTop(0), both.regionTop(1));
        final int union = unionWide * unionHigh;
        final int sum = widths[0] * (both.regionBottom(0) - both.regionTop(0))
                + widths[1] * (both.regionBottom(1) - both.regionTop(1));
        assertTrue(union > 2 * sum, "the union " + union + " is much more than the regions " + sum);
        assertEquals(0, both.pixelAt(121, 97), "nothing is worked out between the regions");

        // Every region is what its light alone gives, whatever the shape of the other light was
        final LightLayer round = new LightLayer();
        final LightRegistry second = new LightRegistry();
        second.add(small(9.5, 5.5, Flicker.STEADY));
        compose(round, second, 1.5, NIGHT_STRENGTH);
        for (int y = round.regionTop(0); y < round.regionBottom(0); y++) {
            for (int x = round.regionLeft(0); x < round.regionRight(0); x++) {
                assertEquals(round.pixelAt(x, y), both.pixelAt(x, y), "the round light at " + x + "," + y);
            }
        }
    }

    @Test
    void moreLightsThanTheArraysStartWithAreWorkedOutOnceAndKeptWhileNothingChanges() {
        final LightRegistry many = new LightRegistry();
        final Flicker flame = Flicker.crackling(0.1, 0.05, 3.0, 5).withShape(0.1, 0.25);
        for (int i = 0; i < 7; i++) {
            many.add(small(2 + i * 1.5, 2 + (i % 2) * 6, i % 2 == 0 ? flame : Flicker.STEADY));
        }
        final LightLayer layer = new LightLayer();

        assertTrue(compose(layer, many, 2.0, NIGHT_STRENGTH));
        assertEquals(7, layer.lightCount());
        assertTrue(layer.changed());
        final int[] first = layer.pixelsCopy();
        assertTrue(compose(layer, many, 2.0, NIGHT_STRENGTH));

        assertFalse(layer.changed(), "the same lights at the same moment are kept");
        assertEquals(1, layer.composes());
        assertTrue(Arrays.equals(first, layer.pixelsCopy()));
        // The cells do not depend on the order the regions were found in: a layer of its own gives the same cells
        final LightLayer fresh = new LightLayer();
        compose(fresh, many, 2.0, NIGHT_STRENGTH);
        assertTrue(Arrays.equals(first, fresh.pixelsCopy()));
    }

    @Test
    void aLightWhoseShapeChangedIsWorkedOutAgainAtTheSameMoment() {
        final Flicker round = Flicker.crackling(0.0 + 1e-9, 0.0 + 1e-9, 1.0, 9);
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(round.withShape(0.2, 0.25)), 4.0, NIGHT_STRENGTH);
        assertEquals(1, layer.composes());

        // The same amplitude and the same moment, with another seed and another drift: the numbers per light are equal
        compose(layer, oneTorch(Flicker.crackling(0.0 + 1e-9, 0.0 + 1e-9, 1.0, 10).withShape(0.2, 0.25)), 4.0,
                NIGHT_STRENGTH);
        assertTrue(layer.changed(), "another seed");
        compose(layer, oneTorch(Flicker.crackling(0.0 + 1e-9, 0.0 + 1e-9, 1.0, 10).withShape(0.2, 0.5)), 4.0,
                NIGHT_STRENGTH);
        assertTrue(layer.changed(), "another drift");
        assertEquals(3, layer.composes());
    }

    @Test
    void twoStylesThatAddUpToTheSameNumberAreNotTheSameStyle() {
        final LightLayer layer = new LightLayer();
        final LightRegistry registry = oneTorch();
        // All the numbers are exact in binary: 0.125 * 1000 + 1.0 equals 0.1259765625 * 1000 + 0.0234375
        final LightLayer.GridView grid = new LightLayer.GridView(SCALE, ORIGIN_X, ORIGIN_Y, cellsWide(), cellsHigh());
        layer.compose(registry, new LightStyle(0.125, 1.0), 0.0, NIGHT, NIGHT_STRENGTH, grid);
        layer.compose(registry, new LightStyle(0.1259765625, 0.0234375), 0.0, NIGHT, NIGHT_STRENGTH, grid);

        assertTrue(layer.changed());
        assertEquals(2, layer.composes());
    }

    @Test
    void theLightGridIsTheGridOfTheTilesAtEveryZoomStep() {
        final var graphics = com.feuerschvenger.perlinsedge.config.AppConfig.getInstance().graphics();
        for (double zoom = 0.5; zoom <= 5.0; zoom += 0.5) {
            final double artScale = ZoomSteps.artScale(zoom);
            assertEquals(graphics.getTileHalfWidth() * zoom, LightLayer.TILE_ART_HALF_WIDTH * artScale, 1e-9, "" + zoom);
            assertEquals(graphics.getTileHalfHeight() * zoom, LightLayer.TILE_ART_HALF_HEIGHT * artScale, 1e-9, "" + zoom);
        }
        assertEquals(16.0, LightLayer.TILE_ART_HALF_WIDTH);
        assertEquals(8.0, LightLayer.TILE_ART_HALF_HEIGHT);
    }

    @Test
    void aLightOnAHalfPixelMovesByExactlyTheWholePixelPanWhateverTheParityOfTheCamera() {
        // The light stands half a pixel off the grid at scale one, a tie that must round the same way at every pan
        final LightRegistry registry = new LightRegistry();
        registry.add(torch(6.03125, 6, Flicker.STEADY));
        int offset = Integer.MIN_VALUE;

        for (int pan = 0; pan < 4; pan++) {
            final LightLayer layer = new LightLayer();
            layer.compose(registry, STYLE, 0.0, NIGHT, NIGHT_STRENGTH,
                    new LightLayer.GridView(1.0, 240 + pan, ORIGIN_Y, cellsWide(), cellsHigh()));
            final int left = layer.regionLeft(0) - pan;
            offset = pan == 0 ? left : offset;
            assertEquals(offset, left, "a pan of " + pan + " moved the light by another distance");
        }
    }

    @Test
    void aCanvasOfAnotherSizeIsWorkedOutAgainFromScratch() {
        final LightLayer layer = new LightLayer();
        compose(layer, oneTorch(), 0.0, NIGHT_STRENGTH);

        assertTrue(layer.compose(oneTorch(), STYLE, 0.0, NIGHT, NIGHT_STRENGTH,
                new LightLayer.GridView(SCALE, ORIGIN_X, ORIGIN_Y, cellsWide() + 10, cellsHigh())));

        assertTrue(layer.changed());
        assertEquals(cellsWide() + 10, layer.width());
        assertEquals(2, layer.composes());
    }

    @Test
    void theLightsAreMergedIntoRegionsOnlyWhenWhatTheyDependOnChanged() {
        final LightRegistry lights = oneTorch();
        lights.add(torch(6, 5, Flicker.STEADY));
        final LightLayer layer = new LightLayer();

        compose(layer, lights, 0.0, NIGHT_STRENGTH);
        final int regions = layer.regionCount();
        compose(layer, lights, 0.1, NIGHT_STRENGTH);
        compose(layer, lights, 0.2, NIGHT_STRENGTH);

        assertEquals(1, layer.groupRuns(), "two calls that found no change merged nothing");
        assertEquals(regions, layer.regionCount(), "and kept the regions of the last merge");
        assertEquals(1, regions, "the two torches overlap into one region");

        lights.add(torch(4, 6, Flicker.STEADY));
        compose(layer, lights, 0.3, NIGHT_STRENGTH);

        assertEquals(2, layer.groupRuns());
    }
}
