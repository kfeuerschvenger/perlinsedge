package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How the trails between the clearings find their way and how wide they are.
 *
 * @param noiseWeight       how much the tree noise adds to the cost of a tile, 0 or more, so a trail winds around the
 *                          thick groves instead of cutting straight through them
 * @param treeCost          what a tile that holds a resource adds to its cost, 0 or more, since a trail has to clear it
 * @param mountainCost      what a mountain tile costs, at least 1, or {@link Float#POSITIVE_INFINITY} for mountains no
 *                          trail can cross; a finite cost lets a trail cut a pass through a mountain
 * @param extraEdgesPer100k how many trails beyond the minimum tree are cut for every 100,000 tiles of map, 0 or more,
 *                          each one closing a loop; any value above 0 cuts at least one on a map of any size
 * @param maxWidth          the widest trail in tiles, from {@value Carve#MIN_STROKE} to {@value Carve#MAX_STROKE}; each
 *                          trail has a width of its own up to this
 * @param maxEdgeLength     the longest straight distance in tiles between two clearings that may be joined directly,
 *                          from 1 to {@value SiteEdges#MAX_LENGTH}
 */
public record TrailParams(float noiseWeight, float treeCost, float mountainCost, int extraEdgesPer100k, int maxWidth,
                          int maxEdgeLength) {

    /** Checks the costs, the counts and the sizes are usable. */
    public TrailParams {
        requireFiniteNotNegative(noiseWeight, "noise weight");
        requireFiniteNotNegative(treeCost, "tree cost");
        if (Float.isNaN(mountainCost) || mountainCost < 1F) {
            throw new IllegalArgumentException("the mountain cost must be at least 1 or infinite: " + mountainCost);
        }
        if (extraEdgesPer100k < 0) {
            throw new IllegalArgumentException("the extra edges cannot be negative: " + extraEdgesPer100k);
        }
        requireInRange(maxWidth, Carve.MIN_STROKE, Carve.MAX_STROKE, "trail width");
        requireInRange(maxEdgeLength, 1, SiteEdges.MAX_LENGTH, "longest edge");
    }

    /** Refuses a cost or weight that is negative, NaN or infinite. */
    private static void requireFiniteNotNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0F) {
            throw new IllegalArgumentException("the " + name + " must be finite and not negative: " + value);
        }
    }

    /** Refuses a count outside an inclusive range. */
    private static void requireInRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("the " + name + " must be from " + min + " to " + max + ": " + value);
        }
    }
}
