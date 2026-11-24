package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IslandShapePassTest {

    private static final int SIDE = 60;
    private static final int RADIUS = 6;
    private static final float GAIN = 1.5F;
    private static final float LIFT = 0.7F;
    private static final float SEA = 0.5F;
    private static final float FALLOFF = 2F;
    private static final float TOLERANCE = 1e-5F;
    private static final int SMALL = 150;
    private static final int LARGE = 300;
    private static final int DENSITY = 13;
    private static final int MINIMUM = 1;
    private static final int PER_100K = 100_000;

    private static IslandParams params(int per100k, int minimum) {
        return new IslandParams(per100k, minimum, RADIUS, GAIN, LIFT, SEA, FALLOFF);
    }

    private static GenerationContext flatHeightContext(long seed, int side, float height) {
        final var context = TestContexts.of(MapType.ISLANDS, seed, side, side, TestContexts.FLAT);
        Arrays.fill(context.defineField(NoiseField.HEIGHT), height);
        return context;
    }

    @Test
    void theIslandCountIsADensityPerAreaWithAFloor() {
        final var pass = new IslandShapePass(params(DENSITY, MINIMUM));

        assertEquals(3, pass.islandCount(SMALL, SMALL));
        assertEquals(12, pass.islandCount(LARGE, LARGE));
        assertEquals(MINIMUM, pass.islandCount(2, 2));
        assertEquals(5, new IslandShapePass(params(DENSITY, 5)).islandCount(2, 2));
        assertEquals(1, new IslandShapePass(params(PER_100K, MINIMUM)).islandCount(1, 1));
    }

    @Test
    void awayFromEveryIslandTheHeightIsTheSeaFloor() {
        final var context = flatHeightContext(1L, SIDE, 0.9F);

        new IslandShapePass(params(DENSITY, 1)).apply(context);

        final var heights = context.field(NoiseField.HEIGHT);
        var seaFloor = 0;
        for (var value : heights) {
            if (Math.abs(value + SEA) < TOLERANCE) {
                seaFloor++;
            }
        }
        assertTrue(seaFloor > heights.length / 2, "sea floor tiles " + seaFloor);
    }

    @Test
    void theLiftIsHighestAtAnIslandCentreAndFadesToTheSeaWithinTheRadius() {
        final var context = flatHeightContext(2L, SIDE, 0F);

        new IslandShapePass(params(DENSITY, 1)).apply(context);

        final var heights = context.field(NoiseField.HEIGHT);
        final var highest = highest(context);
        var centre = -1;
        for (var i = 0; i < heights.length; i++) {
            if (centre < 0 && heights[i] == highest) {
                centre = i;
            }
        }
        assertEquals(LIFT - SEA, highest, TOLERANCE);
        final var cx = context.xOf(centre);
        final var cy = context.yOf(centre);
        final var edgeX = cx + RADIUS < SIDE ? cx + RADIUS : cx - RADIUS;
        assertEquals(-SEA, heights[context.index(edgeX, cy)], TOLERANCE);
        final var halfX = cx + RADIUS / 2 < SIDE ? cx + RADIUS / 2 : cx - RADIUS / 2;
        final var half = heights[context.index(halfX, cy)];
        assertTrue(half < highest && half > -SEA, "half way " + half);
    }

    @Test
    void theNoiseHeightScalesTheLiftAtTheCentre() {
        final var low = flatHeightContext(3L, SIDE, 0F);
        final var high = flatHeightContext(3L, SIDE, 0.5F);
        final var pass = new IslandShapePass(params(DENSITY, 1));

        pass.apply(low);
        pass.apply(high);

        assertEquals(0.5F * GAIN, highest(high) - highest(low), TOLERANCE);
    }

    @Test
    void theSameSeedGivesTheSameIslandsAndAnotherSeedMovesThem() {
        final var first = flatHeightContext(4L, SIDE, 0F);
        final var same = flatHeightContext(4L, SIDE, 0F);
        final var other = flatHeightContext(5L, SIDE, 0F);
        final var pass = new IslandShapePass(params(DENSITY, 3));

        pass.apply(first);
        pass.apply(same);
        pass.apply(other);

        assertTrue(Arrays.equals(first.field(NoiseField.HEIGHT), same.field(NoiseField.HEIGHT)));
        assertFalse(Arrays.equals(first.field(NoiseField.HEIGHT), other.field(NoiseField.HEIGHT)));
    }

    @Test
    void aMapSmallerThanTheMarginsStillGetsItsCentresInside() {
        final var context = flatHeightContext(6L, 3, 0F);

        new IslandShapePass(params(DENSITY, 4)).apply(context);

        assertTrue(highest(context) > -SEA);
    }

    @Test
    void needsTheHeightField() {
        final var context = TestContexts.of(MapType.ISLANDS, 1L, SIDE, SIDE, TestContexts.FLAT);
        final var pass = new IslandShapePass(params(DENSITY, 1));

        assertThrows(IllegalStateException.class, () -> pass.apply(context));
    }

    @Test
    void paramsRefuseUnusableValues() {
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(-1, 1, RADIUS, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(1, 0, RADIUS, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(1, 1, 0, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(1, 1, RADIUS, GAIN, LIFT, SEA, 0F));
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(1, 1, RADIUS, GAIN, LIFT, SEA, Float.NaN));
        assertThrows(NullPointerException.class, () -> new IslandShapePass(null));
    }

    @Test
    void namesItselfForTheTimings() {
        assertEquals("island-shape", new IslandShapePass(params(DENSITY, 1)).name());
    }

    private static float highest(GenerationContext context) {
        var highest = -Float.MAX_VALUE;
        for (var value : context.field(NoiseField.HEIGHT)) {
            highest = Math.max(highest, value);
        }
        return highest;
    }
}
