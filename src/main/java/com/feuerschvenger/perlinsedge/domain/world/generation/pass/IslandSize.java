package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How big the islands of a map are and how far apart they lie. Every island draws its own radius between the two
 * bounds, skewed towards the small end, so a map has a few large islands and several small ones.
 *
 * @param minRadius the radius of the smallest island in tiles, at least 1
 * @param maxRadius the radius of the largest island in tiles, not below the smallest
 * @param spacing   how far apart two islands lie, as a multiple of the sum of their two radii: 1 lets the lifted discs
 *                  of two islands just touch and more leaves a stretch of sea between them, finite and above 0
 */
public record IslandSize(int minRadius, int maxRadius, float spacing) {

    /** Checks the radii are positive and ordered and the spacing is a positive number. */
    public IslandSize {
        if (minRadius < 1) {
            throw new IllegalArgumentException("the smallest island radius must be at least 1: " + minRadius);
        }
        if (maxRadius < minRadius) {
            throw new IllegalArgumentException(
                    "the largest island radius must not be below the smallest: " + maxRadius + " and " + minRadius);
        }
        if (!Float.isFinite(spacing) || spacing <= 0F) {
            throw new IllegalArgumentException("the island spacing must be finite and above 0: " + spacing);
        }
    }
}
