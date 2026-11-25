package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How big the islands of a map are and how far apart they lie. Every island draws its own radius between the two
 * bounds, skewed towards the small end, so a map has a few large islands and several small ones. The radii are in
 * tiles, so a bigger map holds more islands of the same size, but the largest radius is also capped at a share of the
 * shorter side of the map, so on a small map no single island takes the whole map and crowds the others out.
 *
 * @param minRadius       the radius of the smallest island in tiles, at least 1
 * @param maxRadius       the radius of the largest island in tiles, not below the smallest
 * @param maxShareOfSide  the largest radius an island may have as a share of the shorter side of the map, above 0 and
 *                        at most 1; the radii of a map whose side gives a smaller cap are drawn between the smallest
 *                        radius and the cap, and between the cap and itself when the cap is below the smallest
 * @param spacing         how far apart two islands lie, as a multiple of the sum of their two radii: 1 lets the lifted
 *                        discs of two islands just touch and more leaves a stretch of sea between them, finite and
 *                        above 0
 */
public record IslandSize(int minRadius, int maxRadius, float maxShareOfSide, float spacing) {

    /** Checks the radii are positive and ordered, the share is above 0 and at most 1 and the spacing is positive. */
    public IslandSize {
        if (minRadius < 1) {
            throw new IllegalArgumentException("the smallest island radius must be at least 1: " + minRadius);
        }
        if (maxRadius < minRadius) {
            throw new IllegalArgumentException(
                    "the largest island radius must not be below the smallest: " + maxRadius + " and " + minRadius);
        }
        if (!(maxShareOfSide > 0F && maxShareOfSide <= 1F)) {
            throw new IllegalArgumentException(
                    "the largest radius share of the side must be above 0 and at most 1: " + maxShareOfSide);
        }
        if (!Float.isFinite(spacing) || spacing <= 0F) {
            throw new IllegalArgumentException("the island spacing must be finite and above 0: " + spacing);
        }
    }
}
