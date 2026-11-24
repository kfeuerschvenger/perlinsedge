package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShufflesTest {

    private static final int SIZE = 50;
    private static final int SAMPLE = 10;
    private static final long SEED = 7L;
    private static final long OTHER_SEED = 8L;
    private static final int SMALL = 5;
    private static final int TRIALS = 20_000;
    private static final double TOLERANCE = 0.15;

    private static int[] sequence(int size) {
        final var values = new int[size];
        Arrays.setAll(values, index -> index);
        return values;
    }

    @Test
    void aShuffleKeepsTheSameElements() {
        final var values = sequence(SIZE);

        Shuffles.shuffle(values, SIZE, new SplittableRandom(SEED));

        assertFalse(Arrays.equals(sequence(SIZE), values));
        Arrays.sort(values);
        assertArrayEquals(sequence(SIZE), values);
    }

    @Test
    void theSameSeedGivesTheSameOrderAndAnotherSeedADifferentOne() {
        final var first = sequence(SIZE);
        final var again = sequence(SIZE);
        final var other = sequence(SIZE);

        Shuffles.shuffle(first, SIZE, new SplittableRandom(SEED));
        Shuffles.shuffle(again, SIZE, new SplittableRandom(SEED));
        Shuffles.shuffle(other, SIZE, new SplittableRandom(OTHER_SEED));

        assertArrayEquals(first, again);
        assertFalse(Arrays.equals(first, other));
    }

    @Test
    void aShuffleOfAPrefixLeavesTheRestAlone() {
        final var values = sequence(SIZE);

        Shuffles.shuffle(values, SAMPLE, new SplittableRandom(SEED));

        for (var index = SAMPLE; index < SIZE; index++) {
            assertEquals(index, values[index]);
        }
    }

    @Test
    void aPartialShuffleFillsTheFrontWithADistinctSample() {
        final var values = sequence(SIZE);

        Shuffles.partial(values, SIZE, SAMPLE, new SplittableRandom(SEED));

        final var sample = Arrays.copyOf(values, SAMPLE);
        assertEquals(SAMPLE, Arrays.stream(sample).distinct().count());
        Arrays.sort(values);
        assertArrayEquals(sequence(SIZE), values);
    }

    @Test
    void aPartialShuffleOfNothingChangesNothing() {
        final var values = sequence(SIZE);

        Shuffles.partial(values, SIZE, 0, new SplittableRandom(SEED));

        assertArrayEquals(sequence(SIZE), values);
    }

    @Test
    void everyElementIsEquallyLikelyToLeadTheSample() {
        final var random = new SplittableRandom(SEED);
        final var leads = new int[SMALL];

        for (var trial = 0; trial < TRIALS; trial++) {
            final var values = sequence(SMALL);
            Shuffles.partial(values, SMALL, 1, random);
            leads[values[0]]++;
        }

        final var expected = (double) TRIALS / SMALL;
        for (var element = 0; element < SMALL; element++) {
            assertEquals(expected, leads[element], expected * TOLERANCE, "element " + element);
        }
    }

    @Test
    void invalidArgumentsAreRefused() {
        final var values = sequence(SMALL);
        final var random = new SplittableRandom(SEED);

        assertThrows(IllegalArgumentException.class, () -> Shuffles.shuffle(values, SMALL + 1, random));
        assertThrows(IllegalArgumentException.class, () -> Shuffles.shuffle(values, -1, random));
        assertThrows(IllegalArgumentException.class, () -> Shuffles.partial(values, SMALL, SMALL + 1, random));
        assertThrows(IllegalArgumentException.class, () -> Shuffles.partial(values, SMALL, -1, random));
        assertThrows(NullPointerException.class, () -> Shuffles.shuffle(null, 0, random));
        assertThrows(NullPointerException.class, () -> Shuffles.shuffle(values, SMALL, null));
    }
}
