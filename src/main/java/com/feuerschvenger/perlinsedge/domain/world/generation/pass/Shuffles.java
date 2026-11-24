package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Seeded shuffles of int arrays by Fisher-Yates, for the passes that pick some tiles of a list at random, such as the
 * balancing of resources. The result depends only on the generator, so it repeats for one seed.
 */
final class Shuffles {

    private Shuffles() {
    }

    /**
     * Shuffles the first {@code length} elements of an array in place; every order is equally likely.
     *
     * @param values the array
     * @param length how many leading elements take part
     * @param random the source of randomness
     * @throws IllegalArgumentException when the length is outside the array
     */
    static void shuffle(int[] values, int length, RandomGenerator random) {
        partial(values, length, length, random);
    }

    /**
     * Moves a uniform random sample to the front: afterwards the first {@code count} elements are a random choice of
     * the first {@code length} ones, in random order, and the rest are in no defined order. It stops after
     * {@code count} steps, which is cheaper than shuffling everything when only a few tiles are needed.
     *
     * @param values the array
     * @param length how many leading elements take part
     * @param count  how many elements to put in the sample, from 0 to the length
     * @param random the source of randomness
     * @throws IllegalArgumentException when the length is outside the array or the count outside the length
     */
    static void partial(int[] values, int length, int count, RandomGenerator random) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(random, "random");
        if (length < 0 || length > values.length) {
            throw new IllegalArgumentException("the length " + length + " is outside an array of " + values.length);
        }
        if (count < 0 || count > length) {
            throw new IllegalArgumentException("the count " + count + " is outside the length " + length);
        }
        for (var position = 0; position < count; position++) {
            final var other = position + random.nextInt(length - position);
            final var held = values[position];
            values[position] = values[other];
            values[other] = held;
        }
    }
}
