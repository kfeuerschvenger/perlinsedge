package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;

/**
 * Spreads the values of a field so every part of the range 0..1 holds the same share of the tiles. A single octave of
 * noise is bell shaped: most of its values lie near the middle, so equal bands of its range hold very unequal shares
 * of the tiles. The values are mapped through a piecewise linear curve whose knots are the quantiles found by
 * {@link FieldQuantile}, which keeps the order of the values, so the smooth patches of the noise stay smooth and large
 * and only their sizes change. The result is deterministic, since it depends only on the values.
 */
final class FieldEqualiser {

    /** How many equal shares of the tiles the curve is made of. */
    private static final int STEPS = 16;

    /** Not instantiable: the class only holds a static method. */
    private FieldEqualiser() {
    }

    /**
     * Replaces every value by its place in the distribution of the field, from 0 for the lowest to 1 for the highest.
     * A field whose values are all the same is left as it is.
     *
     * @param values the field, whose values must be finite; it is changed in place
     * @throws IllegalArgumentException when a value is NaN or infinite
     */
    static void spread(float[] values) {
        Objects.requireNonNull(values, "values");
        final var knots = knotsOf(values);
        if (knots[STEPS] <= knots[0]) {
            return;
        }
        for (var index = 0; index < values.length; index++) {
            values[index] = place(knots, values[index]);
        }
    }

    /** Returns the lowest value, the thresholds of the equal shares from the lowest up, and the highest value. */
    private static float[] knotsOf(float[] values) {
        final var knots = new float[STEPS + 1];
        knots[0] = FieldQuantile.above(values, index -> true, 1.0);
        for (var step = 1; step < STEPS; step++) {
            knots[step] = FieldQuantile.above(values, index -> true, 1.0 - step / (double) STEPS);
        }
        var highest = knots[0];
        for (float value : values) {
            highest = Math.max(highest, value);
        }
        knots[STEPS] = highest;
        return knots;
    }

    /** Maps a value through the curve: the share below it, from 0 to 1. */
    private static float place(float[] knots, float value) {
        var step = 0;
        while (step < STEPS - 1 && value >= knots[step + 1]) {
            step++;
        }
        final var width = (double) knots[step + 1] - knots[step];
        final var inside = width <= 0.0 ? 0.0 : Math.clamp((value - (double) knots[step]) / width, 0.0, 1.0);
        return (float) ((step + inside) / STEPS);
    }
}
