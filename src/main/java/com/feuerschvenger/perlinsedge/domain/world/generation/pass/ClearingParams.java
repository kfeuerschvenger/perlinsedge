package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * Where the clearings of a map are and how big. The spacing and the radii are those of a large map: a map whose
 * shorter side is under three spacings shrinks them in proportion.
 *
 * @param spacing        the least distance between the centres of two clearings in tiles, at least 1
 * @param minRadius      the smallest radius of an ordinary clearing in tiles, at least 1
 * @param maxRadius      the largest radius of an ordinary clearing in tiles, at least the smallest
 * @param spawnRadius    the radius of the clearing the player starts in, at least 1
 * @param clearance      how far a clearing is from ground that cannot be walked on, in tiles of Chebyshev distance, 0
 *                       or more; the spawn clearing keeps at least this distance and as close to its own radius as the
 *                       land allows
 * @param minRegionTiles the fewest tiles a separate piece of walkable ground has to hold to get a clearing of its own
 *                       when the sampling left it without one, so every valley that is not a pocket has a place to
 *                       stand, though a trail reaches it only when no water lies between; 0 leaves such pieces alone
 */
public record ClearingParams(int spacing, int minRadius, int maxRadius, int spawnRadius, int clearance,
                             int minRegionTiles) {

    /** Checks the spacing, the radii, the clearance and the smallest region are usable. */
    public ClearingParams {
        if (spacing < 1) {
            throw new IllegalArgumentException("the spacing must be at least 1: " + spacing);
        }
        if (minRadius < 1) {
            throw new IllegalArgumentException("the smallest radius must be at least 1: " + minRadius);
        }
        if (maxRadius < minRadius) {
            throw new IllegalArgumentException(
                    "the largest radius " + maxRadius + " is below the smallest " + minRadius);
        }
        if (spawnRadius < 1) {
            throw new IllegalArgumentException("the spawn radius must be at least 1: " + spawnRadius);
        }
        if (clearance < 0) {
            throw new IllegalArgumentException("the clearance cannot be negative: " + clearance);
        }
        if (minRegionTiles < 0) {
            throw new IllegalArgumentException("the smallest region cannot be negative: " + minRegionTiles);
        }
    }
}
