package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Finds the value of a field above which a given share of the selected tiles lie, without sorting. The values of the
 * selected tiles are counted into a histogram of {@value #BINS} bins in two linear passes, and the threshold is
 * interpolated inside the bin where the share is reached, so its resolution is the range of the field over
 * {@value #BINS}. A threshold relative to the tiles that qualify gives the same share on every height profile, which an
 * absolute threshold does not.
 */
final class FieldQuantile {

    /** The number of bins of the histogram. */
    private static final int BINS = 1024;

    private FieldQuantile() {
    }

    /**
     * Returns the threshold above which {@code share} of the selected tiles lie: about that share of them have a value
     * greater than or equal to the result, so a caller keeps a tile with {@code value >= threshold}. The mask is called
     * once for each tile to find the range of the values, and a second time for each tile to count them, except when
     * the answer follows from the range alone: no selected tile, a share of 0 or 1, or a single value. Tiles with the
     * same value fall in the same bin, so when many of them tie the share kept is only approximate. When every
     * selected tile has the same value, which includes a single selected tile, no value is above any other, so a
     * share below 1 keeps nothing instead of keeping every tile.
     *
     * @param values the field, indexed like every array of the context; the selected values must be finite
     * @param mask   the tiles that count
     * @param share  the share of the selected tiles to keep above the threshold, from 0 to 1
     * @return the threshold; {@link Float#POSITIVE_INFINITY} when no tile is selected, the share is 0, or every
     * selected tile has the same value and the share is below 1, so no tile passes it, and the lowest selected value
     * when the share is 1
     * @throws IllegalArgumentException when the share is not from 0 to 1 or a selected value is NaN or infinite
     */
    static float above(float[] values, IntPredicate mask, double share) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mask, "mask");
        if (!(share >= 0.0 && share <= 1.0)) {
            throw new IllegalArgumentException("the share must be from 0 to 1: " + share);
        }
        final var range = Range.of(values, mask);
        if (range.count == 0 || share <= 0.0) {
            return Float.POSITIVE_INFINITY;
        }
        if (share >= 1.0) {
            return range.min;
        }
        if (range.max <= range.min) {
            return Float.POSITIVE_INFINITY;
        }
        return interpolate(range, histogram(values, mask, range, new float[BINS]), share * range.count);
    }

    /**
     * Returns the evenly spaced quantiles of every value of a field from one histogram, so many thresholds cost the
     * two passes of a single call of {@link #above}. Entry 0 is the lowest value, entry {@code parts} the highest, and
     * entry {@code k} in between is the threshold {@code above(values, all, 1 - k / parts)} would give.
     *
     * @param values the field; every value must be finite
     * @param parts  how many equal shares to cut the tiles into, at least 1
     * @return {@code parts + 1} values that never decrease; all 0 when the field is empty. When several thresholds fall
     * in one bin, because many tiles hold almost the same value, they are set to the lowest value of that bin, so tied
     * values give tied thresholds
     * @throws IllegalArgumentException when a value is NaN or infinite, or {@code parts} is below 1
     */
    static float[] quantiles(float[] values, int parts) {
        Objects.requireNonNull(values, "values");
        if (parts < 1) {
            throw new IllegalArgumentException("the parts must be at least 1: " + parts);
        }
        final var knots = new float[parts + 1];
        final var range = Range.of(values, index -> true);
        if (range.count == 0) {
            return knots;
        }
        Arrays.fill(knots, range.min);
        if (range.max <= range.min) {
            return knots;
        }
        final var lows = new float[BINS];
        Arrays.fill(lows, Float.POSITIVE_INFINITY);
        final var counts = histogram(values, index -> true, range, lows);
        final var bins = new int[parts + 1];
        for (var part = 1; part < parts; part++) {
            final var target = (1.0 - part / (double) parts) * range.count;
            bins[part] = binReaching(counts, target);
            knots[part] = interpolate(range, counts, target);
        }
        bins[parts] = BINS - 1;
        for (var part = 1; part < parts; part++) {
            if (bins[part - 1] == bins[part] || bins[part + 1] == bins[part]) {
                knots[part] = lows[bins[part]];
            }
        }
        knots[parts] = range.max;
        return knots;
    }

    /**
     * Counts the selected tiles into the bins of the range. The array {@code lows} receives the lowest value counted
     * into each bin and must be filled with positive infinity by the caller.
     */
    private static int[] histogram(float[] values, IntPredicate mask, Range range, float[] lows) {
        final var counts = new int[BINS];
        final var scale = BINS / ((double) range.max - range.min);
        for (var index = 0; index < values.length; index++) {
            if (mask.test(index)) {
                final var bin = (int) ((values[index] - (double) range.min) * scale);
                final var slot = Math.min(bin, BINS - 1);
                counts[slot]++;
                lows[slot] = Math.min(lows[slot], values[index]);
            }
        }
        return counts;
    }

    /** Walks the bins from the top until {@code target} tiles are counted and returns the bin that reaches it. */
    private static int binReaching(int[] counts, double target) {
        var counted = 0.0;
        for (var bin = BINS - 1; bin > 0; bin--) {
            counted += counts[bin];
            if (counted >= target) {
                return bin;
            }
        }
        return 0;
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

        private static Range of(float[] values, IntPredicate mask) {
            final var range = new Range();
            for (var index = 0; index < values.length; index++) {
                if (mask.test(index)) {
                    if (!Float.isFinite(values[index])) {
                        throw new IllegalArgumentException(
                                "the value at " + index + " is not finite: " + values[index]);
                    }
                    range.min = Math.min(range.min, values[index]);
                    range.max = Math.max(range.max, values[index]);
                    range.count++;
                }
            }
            return range;
        }
    }
}
