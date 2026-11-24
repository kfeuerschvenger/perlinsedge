package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;

/**
 * Finds the value of a field above which a given share of the selected tiles lie, without sorting. The values of the
 * selected tiles are counted into a histogram of {@value #BINS} bins in two linear passes, and the threshold is
 * interpolated inside the bin where the share is reached, so its resolution is the range of the field over
 * {@value #BINS}. A threshold relative to the tiles that qualify gives the same share on every height profile, which an
 * absolute threshold does not.
 */
final class FieldQuantile {

    /** The number of bins of the histogram. */
    static final int BINS = 1024;

    private FieldQuantile() {
    }

    /**
     * Returns the threshold above which {@code share} of the selected tiles lie: about that share of them have a value
     * at or above the result. The mask is called twice for each tile, once per pass.
     *
     * @param values the field, indexed like every array of the context
     * @param mask   the tiles that count
     * @param share  the share of the selected tiles to keep above the threshold, from 0 to 1
     * @return the threshold; {@link Float#POSITIVE_INFINITY} when no tile is selected or the share is 0, so no tile
     * passes it, and the lowest selected value when the share is 1 or every selected tile has the same value
     * @throws IllegalArgumentException when the share is not from 0 to 1
     */
    static float above(float[] values, Mask mask, double share) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mask, "mask");
        if (!(share >= 0.0 && share <= 1.0)) {
            throw new IllegalArgumentException("the share must be from 0 to 1: " + share);
        }
        final var range = Range.of(values, mask);
        if (range.count == 0 || share == 0.0) {
            return Float.POSITIVE_INFINITY;
        }
        if (share == 1.0 || range.max == range.min) {
            return range.min;
        }
        return interpolate(range, histogram(values, mask, range), share * range.count);
    }

    /** Counts the selected tiles into the bins of the range. */
    private static int[] histogram(float[] values, Mask mask, Range range) {
        final var counts = new int[BINS];
        final var scale = BINS / ((double) range.max - range.min);
        for (var index = 0; index < values.length; index++) {
            if (mask.test(index)) {
                final var bin = (int) ((values[index] - (double) range.min) * scale);
                counts[Math.min(bin, BINS - 1)]++;
            }
        }
        return counts;
    }

    /** Walks the bins from the top until {@code target} tiles are counted and places the threshold inside that bin. */
    private static float interpolate(Range range, int[] counts, double target) {
        final var binWidth = ((double) range.max - range.min) / BINS;
        var counted = 0.0;
        for (var bin = BINS - 1; bin >= 0; bin--) {
            final var inBin = counts[bin];
            if (counted + inBin >= target) {
                final var kept = (target - counted) / inBin;
                return (float) (range.min + (bin + 1.0 - kept) * binWidth);
            }
            counted += inBin;
        }
        return range.min;
    }

    /** The lowest and highest value of the selected tiles and how many there are. */
    private static final class Range {
        private float min = Float.POSITIVE_INFINITY;
        private float max = Float.NEGATIVE_INFINITY;
        private int count;

        private static Range of(float[] values, Mask mask) {
            final var range = new Range();
            for (var index = 0; index < values.length; index++) {
                if (mask.test(index)) {
                    range.min = Math.min(range.min, values[index]);
                    range.max = Math.max(range.max, values[index]);
                    range.count++;
                }
            }
            return range;
        }
    }
}
