package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarveTest {

    private static final int WIDTH = 5;
    private static final int HEIGHT = 4;
    private static final int TOO_THIN = 0;
    private static final int TOO_WIDE = 3;

    private static Set<Integer> carve(int[] path, int length, int stroke) {
        final var carved = new TreeSet<Integer>();
        Carve.path(WIDTH, HEIGHT, path, length, stroke, carved::add);
        return carved;
    }

    private static int at(int x, int y) {
        return y * WIDTH + x;
    }

    @Test
    void aStraightPathWithAStrokeOfOneCarvesExactlyItsTiles() {
        final var path = new int[] {at(0, 1), at(1, 1), at(2, 1)};

        assertEquals(Set.of(at(0, 1), at(1, 1), at(2, 1)), carve(path, path.length, Carve.MIN_STROKE));
    }

    @Test
    void aDiagonalStepAlsoCarvesTheCornerSoTheStrokeIsFourConnected() {
        final var path = new int[] {at(0, 0), at(1, 1), at(2, 2)};

        final var carved = carve(path, path.length, Carve.MIN_STROKE);

        assertEquals(Set.of(at(0, 0), at(1, 0), at(1, 1), at(2, 1), at(2, 2)), carved);
        final var regions = Regions.label(WIDTH, HEIGHT, carved::contains);
        assertEquals(1, regions.count());
    }

    @Test
    void aStrokeOfTwoStampsABlockToTheRightAndBelow() {
        final var path = new int[] {at(1, 1)};

        assertEquals(Set.of(at(1, 1), at(2, 1), at(1, 2), at(2, 2)), carve(path, 1, Carve.MAX_STROKE));
    }

    @Test
    void aStrokeOfTwoIsClippedAtTheEdgeOfTheMap() {
        final var path = new int[] {at(WIDTH - 1, HEIGHT - 1)};

        assertEquals(Set.of(at(WIDTH - 1, HEIGHT - 1)), carve(path, 1, Carve.MAX_STROKE));
    }

    @Test
    void onlyTheLeadingTilesOfTheArrayAreCarved() {
        final var path = new int[] {at(0, 0), at(1, 0), at(2, 0), at(3, 0)};

        assertEquals(Set.of(at(0, 0), at(1, 0)), carve(path, 2, Carve.MIN_STROKE));
    }

    @Test
    void anEmptyPathCarvesNothing() {
        assertTrue(carve(new int[0], 0, Carve.MIN_STROKE).isEmpty());
    }

    @Test
    void aTileThatSeveralStepsReachIsReportedMoreThanOnce() {
        final var path = new int[] {at(0, 0), at(1, 0)};
        final var calls = new int[1];

        Carve.path(WIDTH, HEIGHT, path, path.length, Carve.MAX_STROKE, index -> {
            calls[0]++;
        });

        assertTrue(calls[0] > carve(path, path.length, Carve.MAX_STROKE).size());
    }

    @Test
    void aPathFoundBySearchingCarvesOneConnectedStroke() {
        final var side = 40;
        final var cost = new float[side * side];
        Arrays.fill(cost, 1F);
        final var path = new int[cost.length];
        final var length = new LeastCostPath(side, side, cost, 1F).find(0, cost.length - 1, path);
        final var carved = new TreeSet<Integer>();

        Carve.path(side, side, path, length, Carve.MIN_STROKE, carved::add);

        assertEquals(1, Regions.label(side, side, carved::contains).count());
        assertTrue(carved.size() > length);
    }

    @Test
    void anInvalidStrokeOrLengthIsRefused() {
        final var path = new int[] {0, 1};
        final IntConsumer nothing = index -> { };

        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(WIDTH, HEIGHT, path, path.length, TOO_THIN, nothing));
        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(WIDTH, HEIGHT, path, path.length, TOO_WIDE, nothing));
        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(WIDTH, HEIGHT, path, path.length + 1, Carve.MIN_STROKE, nothing));
        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(WIDTH, HEIGHT, path, -1, Carve.MIN_STROKE, nothing));
    }

    @Test
    void anEmptyGridIsRefused() {
        final var path = new int[] {0};
        final IntConsumer nothing = index -> { };

        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(0, HEIGHT, path, 1, Carve.MIN_STROKE, nothing));
        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(WIDTH, 0, path, 1, Carve.MIN_STROKE, nothing));
        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(-WIDTH, HEIGHT, path, 1, Carve.MIN_STROKE, nothing));
    }

    @Test
    void aPathTileOutsideTheGridIsRefusedBeforeAnythingIsCarved() {
        final var calls = new AtomicInteger();
        final var below = new int[] {0, -1};
        final var beyond = new int[] {0, WIDTH * HEIGHT};
        final IntConsumer count = index -> calls.incrementAndGet();

        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(WIDTH, HEIGHT, below, below.length, Carve.MIN_STROKE, count));
        assertThrows(IllegalArgumentException.class,
                () -> Carve.path(WIDTH, HEIGHT, beyond, beyond.length, Carve.MIN_STROKE, count));
        assertEquals(0, calls.get());
    }

    @Test
    void tilesPastTheLengthAreNotChecked() {
        final var path = new int[] {0, WIDTH * HEIGHT};

        assertEquals(Set.of(0), carve(path, 1, Carve.MIN_STROKE));
    }

    @Test
    void missingArgumentsAreRefused() {
        final var path = new int[] {0};
        final IntConsumer nothing = index -> { };

        assertThrows(NullPointerException.class,
                () -> Carve.path(WIDTH, HEIGHT, null, 0, Carve.MIN_STROKE, nothing));
        assertThrows(NullPointerException.class,
                () -> Carve.path(WIDTH, HEIGHT, path, 1, Carve.MIN_STROKE, null));
    }
}
