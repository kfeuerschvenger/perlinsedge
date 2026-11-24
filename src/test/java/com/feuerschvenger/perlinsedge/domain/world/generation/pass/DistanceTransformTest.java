package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DistanceTransformTest {

    private static final int SIDE = 7;
    private static final int CENTRE = 3;
    private static final int WIDTH = 30;
    private static final int HEIGHT = 20;
    private static final int SOURCE_PERCENT = 4;
    private static final int PERCENT = 100;
    private static final long SEED = 99L;

    @Test
    void aSingleSourceGivesTheChebyshevDistanceToIt() {
        final var source = CENTRE * SIDE + CENTRE;

        final var distance = DistanceTransform.chebyshev(SIDE, SIDE, index -> index == source);

        for (var y = 0; y < SIDE; y++) {
            for (var x = 0; x < SIDE; x++) {
                assertEquals(Math.max(Math.abs(x - CENTRE), Math.abs(y - CENTRE)), distance[y * SIDE + x]);
            }
        }
    }

    @Test
    void theTransformMatchesAScanOfEverySourceOnARandomMap() {
        final var random = new SplittableRandom(SEED);
        final var sources = new boolean[WIDTH * HEIGHT];
        for (var index = 0; index < sources.length; index++) {
            sources[index] = random.nextInt(PERCENT) < SOURCE_PERCENT;
        }
        sources[0] = true;

        final var distance = DistanceTransform.chebyshev(WIDTH, HEIGHT, index -> sources[index]);

        for (var index = 0; index < sources.length; index++) {
            assertEquals(nearestBySearch(sources, index), distance[index], "tile " + index);
        }
    }

    private static int nearestBySearch(boolean[] sources, int index) {
        var best = Integer.MAX_VALUE;
        for (var other = 0; other < sources.length; other++) {
            if (sources[other]) {
                final var dx = Math.abs(index % WIDTH - other % WIDTH);
                final var dy = Math.abs(index / WIDTH - other / WIDTH);
                best = Math.min(best, Math.max(dx, dy));
            }
        }
        return best;
    }

    @Test
    void withNoSourceEveryTileIsUnreachable() {
        final var distance = DistanceTransform.chebyshev(SIDE, SIDE, index -> false);

        for (final var value : distance) {
            assertEquals(DistanceTransform.UNREACHABLE, value);
        }
    }

    @Test
    void whenEveryTileIsASourceEveryDistanceIsZero() {
        final var distance = DistanceTransform.chebyshev(SIDE, SIDE, index -> true);

        assertArrayEquals(new int[SIDE * SIDE], distance);
    }

    @Test
    void aSingleTileMapWorks() {
        assertArrayEquals(new int[] {0}, DistanceTransform.chebyshev(1, 1, index -> true));
        assertArrayEquals(new int[] {DistanceTransform.UNREACHABLE},
                DistanceTransform.chebyshev(1, 1, index -> false));
    }

    @Test
    void invalidArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> DistanceTransform.chebyshev(1, 1, null));
        assertThrows(IllegalArgumentException.class, () -> DistanceTransform.chebyshev(0, 1, index -> true));
        assertThrows(IllegalArgumentException.class, () -> DistanceTransform.chebyshev(1, 0, index -> true));
    }
}
