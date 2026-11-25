package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldQuantileTest {

    private static final int SIZE = 10_000;
    private static final int TOLERANCE = 30;
    private static final int SKEW_TOLERANCE = 60;
    private static final double TENTH = 0.1;
    private static final double QUARTER = 0.25;
    private static final double HALF = 0.5;
    private static final double NINE_TENTHS = 0.9;
    private static final double THIRD = 0.3;
    private static final double FIFTH = 0.2;
    private static final float CONSTANT = 0.4F;
    private static final double OUT_OF_RANGE = 1.1;
    private static final int EVEN = 2;
    private static final float SPAN = 2F;
    private static final int CALLS_PER_TILE = 2;

    private static float[] ramp() {
        final var values = new float[SIZE];
        for (var index = 0; index < SIZE; index++) {
            values[index] = index / (float) SIZE;
        }
        return values;
    }

    private static int countAtOrAbove(float[] values, IntPredicate mask, float threshold) {
        var count = 0;
        for (var index = 0; index < values.length; index++) {
            if (mask.test(index) && values[index] >= threshold) {
                count++;
            }
        }
        return count;
    }

    @Test
    void aRampSplitsAtTheRequestedShare() {
        final var values = ramp();
        final IntPredicate all = index -> true;

        for (final var share : new double[] {TENTH, HALF, NINE_TENTHS}) {
            final var threshold = FieldQuantile.above(values, all, share);

            assertEquals(share * SIZE, countAtOrAbove(values, all, threshold), TOLERANCE, "share " + share);
        }
    }

    @Test
    void onlyTheSelectedTilesCount() {
        final var values = ramp();
        final IntPredicate even = index -> index % EVEN == 0;

        final var threshold = FieldQuantile.above(values, even, QUARTER);

        assertEquals(QUARTER * SIZE / EVEN, countAtOrAbove(values, even, threshold), TOLERANCE);
    }

    @Test
    void aSkewedFieldStillSplitsByCountNotByValue() {
        final var values = ramp();
        for (var index = 0; index < SIZE; index++) {
            values[index] = values[index] * values[index] * values[index];
        }
        final IntPredicate all = index -> true;

        final var threshold = FieldQuantile.above(values, all, FIFTH);

        assertEquals(FIFTH * SIZE, countAtOrAbove(values, all, threshold), SKEW_TOLERANCE);
    }

    @Test
    void aFieldWithNegativeValuesWorksLikeAnyOther() {
        final var values = ramp();
        for (var index = 0; index < SIZE; index++) {
            values[index] = values[index] * SPAN - 1F;
        }
        final IntPredicate all = index -> true;

        final var threshold = FieldQuantile.above(values, all, THIRD);

        assertEquals(THIRD * SIZE, countAtOrAbove(values, all, threshold), TOLERANCE);
    }

    @Test
    void aShareOfZeroLeavesNoTileAboveTheThreshold() {
        final var threshold = FieldQuantile.above(ramp(), index -> true, 0.0);

        assertEquals(Float.POSITIVE_INFINITY, threshold);
    }

    @Test
    void aShareOfOneReturnsTheLowestSelectedValue() {
        final var values = ramp();

        final var threshold = FieldQuantile.above(values, index -> index >= SIZE / EVEN, 1.0);

        assertEquals(values[SIZE / EVEN], threshold);
    }

    @Test
    void noSelectedTileLeavesNoTileAboveTheThreshold() {
        final var threshold = FieldQuantile.above(ramp(), index -> false, HALF);

        assertEquals(Float.POSITIVE_INFINITY, threshold);
    }

    @Test
    void aConstantFieldKeepsNothingForAShareBelowOne() {
        final var values = new float[SIZE];
        Arrays.fill(values, CONSTANT);

        assertEquals(Float.POSITIVE_INFINITY, FieldQuantile.above(values, index -> true, HALF));
    }

    @Test
    void aConstantFieldKeepsEverythingForAShareOfOne() {
        final var values = new float[SIZE];
        Arrays.fill(values, CONSTANT);

        assertEquals(CONSTANT, FieldQuantile.above(values, index -> true, 1.0));
    }

    @Test
    void aSingleSelectedTileKeepsNothingForAShareBelowOne() {
        final var values = ramp();

        assertEquals(Float.POSITIVE_INFINITY, FieldQuantile.above(values, index -> index == SIZE / EVEN, HALF));
    }

    @Test
    void theMaskIsCalledTwicePerTile() {
        final var calls = new AtomicInteger();

        FieldQuantile.above(ramp(), index -> {
            calls.incrementAndGet();
            return true;
        }, HALF);

        assertEquals(SIZE * CALLS_PER_TILE, calls.get());
    }

    @Test
    void aShareOutsideZeroToOneIsRefused() {
        final var values = ramp();
        final IntPredicate all = index -> true;

        assertThrows(IllegalArgumentException.class, () -> FieldQuantile.above(values, all, -TENTH));
        assertThrows(IllegalArgumentException.class, () -> FieldQuantile.above(values, all, OUT_OF_RANGE));
        assertThrows(IllegalArgumentException.class, () -> FieldQuantile.above(values, all, Double.NaN));
    }

    @Test
    void missingArgumentsAreRefused() {
        final var values = ramp();
        final IntPredicate all = index -> true;

        assertThrows(NullPointerException.class, () -> FieldQuantile.above(null, all, HALF));
        assertThrows(NullPointerException.class, () -> FieldQuantile.above(values, null, HALF));
    }

    @Test
    void theMaskIsCalledOncePerTileWhenTheRangeAnswersAlone() {
        final var calls = new AtomicInteger();
        final IntPredicate counting = index -> {
            calls.incrementAndGet();
            return true;
        };

        FieldQuantile.above(ramp(), counting, 1.0);

        assertEquals(SIZE, calls.get());
    }

    @Test
    void aSelectedValueThatIsNotFiniteIsRefused() {
        final var values = ramp();
        values[SIZE / EVEN] = Float.NaN;
        final var infinite = ramp();
        infinite[SIZE / EVEN] = Float.POSITIVE_INFINITY;

        assertThrows(IllegalArgumentException.class, () -> FieldQuantile.above(values, index -> true, HALF));
        assertThrows(IllegalArgumentException.class, () -> FieldQuantile.above(infinite, index -> true, HALF));
    }

    @Test
    void aValueThatIsNotFiniteIsIgnoredWhenItsTileIsNotSelected() {
        final var values = ramp();
        values[0] = Float.NaN;

        final var threshold = FieldQuantile.above(values, index -> index > 0, HALF);

        assertEquals(HALF * SIZE, countAtOrAbove(values, index -> index > 0, threshold), TOLERANCE);
    }
}
