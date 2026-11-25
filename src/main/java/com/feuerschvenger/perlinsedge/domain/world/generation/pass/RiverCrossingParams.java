package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How the rivers that cross a map from one edge to the opposite one are made. The count is per area, so a bigger map
 * has more rivers, within the bounds of the minimum and the maximum.
 *
 * @param riversPer100k      how many rivers there are for every hundred thousand tiles of map, from 0 to 100000
 * @param minRivers          the fewest rivers a map has, from 1 to {@link #MAX_RIVERS}
 * @param maxRivers          the most rivers a map has, from the fewest to {@link #MAX_RIVERS}
 * @param heightWeight       how much a river avoids high ground, finite and not negative; the height of a tile is taken
 *                           relative to the lowest and highest land of the map
 * @param meanderWeight      how much a river avoids dry ground, finite and not negative, so rivers bend towards the wet
 *                           parts of the map; it has no effect on a map with no moisture field
 * @param tributariesPerRiver how many short streams join each river, from 0 to {@link #MAX_TRIBUTARIES}
 */
public record RiverCrossingParams(int riversPer100k, int minRivers, int maxRivers, float heightWeight,
                                  float meanderWeight, int tributariesPerRiver) {

    /** The density at which every tile would be a river; a larger one only makes the count overflow. */
    public static final int MAX_RIVERS_PER_100K = 100_000;
    /** The most rivers a map has, however large it is. */
    public static final int MAX_RIVERS = 8;
    /** The most tributaries each river has. */
    public static final int MAX_TRIBUTARIES = 4;

    /** Checks the counts are in range and ordered and the weights are finite and not negative. */
    public RiverCrossingParams {
        requireInRange(riversPer100k, 0, MAX_RIVERS_PER_100K, "river density per 100k tiles");
        requireInRange(minRivers, 1, MAX_RIVERS, "minimum river count");
        requireInRange(maxRivers, minRivers, MAX_RIVERS, "maximum river count");
        requireInRange(tributariesPerRiver, 0, MAX_TRIBUTARIES, "tributaries per river");
        requireWeight(heightWeight, "height weight");
        requireWeight(meanderWeight, "meander weight");
    }

    /** Refuses a count outside an inclusive range. */
    private static void requireInRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("the " + name + " must be from " + min + " to " + max + ": " + value);
        }
    }

    /** Refuses a weight that is NaN, infinite or negative. */
    private static void requireWeight(float value, String name) {
        if (!Float.isFinite(value) || value < 0F) {
            throw new IllegalArgumentException("the " + name + " must be finite and not negative: " + value);
        }
    }
}
