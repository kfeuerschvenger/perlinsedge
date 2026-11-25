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
 *                          each one closing a loop
 * @param maxWidth          the widest trail in tiles, from {@value Carve#MIN_STROKE} to {@value Carve#MAX_STROKE}; each
 *                          trail has a width of its own up to this
 * @param maxEdgeLength     the longest straight distance in tiles between two clearings that may be joined directly,
 *                          from 1 to {@value SiteEdges#MAX_LENGTH}
 */
public record TrailParams(float noiseWeight, float treeCost, float mountainCost, int extraEdgesPer100k, int maxWidth,
                          int maxEdgeLength) {

    /** Checks the costs, the counts and the sizes are usable. */
    public TrailParams {
        if (!Float.isFinite(noiseWeight) || noiseWeight < 0F) {
            throw new IllegalArgumentException("the noise weight must be finite and not negative: " + noiseWeight);
        }
        if (!Float.isFinite(treeCost) || treeCost < 0F) {
            throw new IllegalArgumentException("the tree cost must be finite and not negative: " + treeCost);
        }
        if (Float.isNaN(mountainCost) || mountainCost < 1F) {
            throw new IllegalArgumentException("the mountain cost must be at least 1 or infinite: " + mountainCost);
        }
        if (extraEdgesPer100k < 0) {
            throw new IllegalArgumentException("the extra edges cannot be negative: " + extraEdgesPer100k);
        }
        if (maxWidth < Carve.MIN_STROKE || maxWidth > Carve.MAX_STROKE) {
            throw new IllegalArgumentException("the trail width must be from " + Carve.MIN_STROKE + " to "
                    + Carve.MAX_STROKE + ": " + maxWidth);
        }
        if (maxEdgeLength < 1 || maxEdgeLength > SiteEdges.MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "the longest edge must be from 1 to " + SiteEdges.MAX_LENGTH + ": " + maxEdgeLength);
        }
    }
}
