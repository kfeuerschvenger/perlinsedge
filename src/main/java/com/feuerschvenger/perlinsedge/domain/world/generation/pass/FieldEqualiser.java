package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;

/**
 * Spreads the values of a field so every part of the range 0..1 holds the same share of the tiles. A single octave of
 * noise is bell shaped: most of its values lie near the middle, so equal bands of its range hold very unequal shares
 * of the tiles. The values are mapped through a piecewise linear curve whose knots are the quantiles found by
 * {@link FieldQuantile#quantiles} in one histogram pass, which keeps the order of the values, so the smooth patches of
 * the noise stay smooth and large and only their sizes change. When many tiles share one value several knots fall on
 * it, and that plateau is mapped to the middle of the shares it spans, so it takes its fair place instead of the top
 * of them. The result is deterministic, since it depends only on the values.
 */
final class FieldEqualiser {

    /** How many equal shares of the tiles the curve is made of. */
    private static final int STEPS = 16;

    /** Not instantiable: the class only holds a static method. */
    private FieldEqualiser() {
    }

    /**
     * Replaces every value by its place in the distribution of the field, from 0 for the lowest to 1 for the highest.
     * A field whose values are all the same, or that is empty, is left as it is.
     *
     * @param values the field, whose values must be finite; it is changed in place
     * @throws IllegalArgumentException when a value is NaN or infinite
     */
    static void spread(float[] values) {
        Objects.requireNonNull(values, "values");
        final var knots = FieldQuantile.quantiles(values, STEPS);
        if (knots[STEPS] <= knots[0]) {
            return;
        }
        for (var index = 0; index < values.length; index++) {
            values[index] = place(knots, values[index]);
        }
    }

    /**
     * Maps a value through the curve: the share below it, from 0 to 1. A value on a knot, or on a plateau of tied
     * knots, goes to the middle of the first and last knot it sits on.
     */
    private static float place(float[] knots, float value) {
        var first = 0;
        while (first < STEPS && knots[first] < value) {
            first++;
        }
        if (Float.compare(knots[first], value) == 0) {
            final var last = lastTied(knots, first, value);
            return (float) ((first + last) / (2.0 * STEPS));
        }
        final var low = knots[first - 1];
        final var inside = (value - (double) low) / ((double) knots[first] - low);
        return (float) ((first - 1 + inside) / STEPS);
    }

    /** Returns the index of the last knot that holds the value, searching on from the first one that does. */
    private static int lastTied(float[] knots, int first, float value) {
        var last = first;
        while (last < STEPS && Float.compare(knots[last + 1], value) == 0) {
            last++;
        }
        return last;
    }
}
