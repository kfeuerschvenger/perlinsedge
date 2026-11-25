package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the equalising of a field: the order of the values is kept, ties are fair and the output is even. */
class FieldEqualiserTest {

    /** The number of values of the larger fields. */
    private static final int SIZE = 20_000;

    /** The seed of the pseudo random fields, so every run sees the same values. */
    private static final long SEED = 42L;

    /** The value a flat field holds. */
    private static final float FLAT = 0.37F;

    /** How many uniform values are added to make one bell shaped value. */
    private static final int BELL_TERMS = 4;

    /** How many equal bands the output range is cut into to check it is even. */
    private static final int BANDS = 4;

    /** The largest difference from an equal share a band may have. */
    private static final double SHARE_TOLERANCE = 0.03;

    /** The number of tied values at the minimum in the plateau field. */
    private static final int PLATEAU_SIZE = 600;

    /** The number of values above the plateau. */
    private static final int ABOVE_SIZE = 400;

    /** The lowest value above the plateau. */
    private static final float ABOVE_FLOOR = 0.1F;

    /** The lowest output a fair plateau may map to. */
    private static final float PLATEAU_LOW = 0.2F;

    /** The highest output a fair plateau may map to. */
    private static final float PLATEAU_HIGH = 0.4F;

    /** Tolerance of the float comparisons. */
    private static final float EPSILON = 1e-6F;

    /** Builds a bell shaped field from a fixed seed. */
    private static float[] bell() {
        final var random = new Random(SEED);
        final var values = new float[SIZE];
        for (var index = 0; index < SIZE; index++) {
            var sum = 0.0F;
            for (var term = 0; term < BELL_TERMS; term++) {
                sum += random.nextFloat();
            }
            values[index] = sum;
        }
        return values;
    }

    /** Asserts that a higher input never gets a lower output. */
    private static void assertMonotone(float[] before, float[] after) {
        final var order = new Integer[before.length];
        Arrays.setAll(order, index -> index);
        Arrays.sort(order, (left, right) -> Float.compare(before[left], before[right]));
        for (var rank = 1; rank < order.length; rank++) {
            assertTrue(after[order[rank]] >= after[order[rank - 1]] - EPSILON, "rank " + rank);
        }
    }

    @Test
    void aFlatFieldIsLeftAsItIs() {
        final var values = new float[SIZE];
        Arrays.fill(values, FLAT);

        FieldEqualiser.spread(values);

        for (var value : values) {
            assertEquals(FLAT, value, 0.0F);
        }
    }

    @Test
    void aSingleValueAndAnEmptyFieldAreLeftAsTheyAre() {
        final var single = new float[] {FLAT};
        final var empty = new float[0];

        FieldEqualiser.spread(single);
        FieldEqualiser.spread(empty);

        assertEquals(FLAT, single[0], 0.0F);
        assertEquals(0, empty.length);
    }

    @Test
    void aNaNValueIsRejected() {
        final var values = new float[] {0.1F, Float.NaN, 0.5F};

        assertThrows(IllegalArgumentException.class, () -> FieldEqualiser.spread(values));
    }

    @Test
    void theResultDependsOnlyOnTheValues() {
        final var first = bell();
        final var second = bell();

        FieldEqualiser.spread(first);
        FieldEqualiser.spread(second);

        assertArrayEquals(first, second);
    }

    @Test
    void theOrderOfTheValuesIsKept() {
        final var before = bell();
        final var after = before.clone();

        FieldEqualiser.spread(after);

        assertMonotone(before, after);
    }

    @Test
    void aTiePlateauAtTheMinimumTakesTheMiddleOfItsShares() {
        final var values = new float[PLATEAU_SIZE + ABOVE_SIZE];
        for (var index = PLATEAU_SIZE; index < values.length; index++) {
            values[index] = ABOVE_FLOOR + (index - PLATEAU_SIZE) / (float) ABOVE_SIZE;
        }
        final var before = values.clone();

        FieldEqualiser.spread(values);

        assertTrue(values[0] > PLATEAU_LOW && values[0] < PLATEAU_HIGH, "plateau at " + values[0]);
        for (var index = 1; index < PLATEAU_SIZE; index++) {
            assertEquals(values[0], values[index], 0.0F);
        }
        assertTrue(values[values.length - 1] > values[0]);
        assertMonotone(before, values);
    }

    @Test
    void aBellShapedFieldBecomesRoughlyUniform() {
        final var values = bell();

        FieldEqualiser.spread(values);

        final var bands = new int[BANDS];
        for (var value : values) {
            assertTrue(value >= 0.0F && value <= 1.0F);
            bands[Math.min((int) (value * BANDS), BANDS - 1)]++;
        }
        for (var band : bands) {
            assertEquals(1.0 / BANDS, band / (double) SIZE, SHARE_TOLERANCE);
        }
    }
}
