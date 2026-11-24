package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How the oases of a desert are dug.
 *
 * @param tilesPerOasis how many tiles of map there are for every oasis, at least 1; a map always has at least one
 * @param radius        the radius of an oasis in tiles, at least 1
 * @param siteAttempts  how many places are tried for each oasis before it is given up, at least 1
 */
public record OasisParams(int tilesPerOasis, int radius, int siteAttempts) {

    /** Checks every number is positive. */
    public OasisParams {
        if (tilesPerOasis < 1 || radius < 1 || siteAttempts < 1) {
            throw new IllegalArgumentException("the oasis numbers must be at least 1: " + tilesPerOasis + ", "
                    + radius + " and " + siteAttempts);
        }
    }
}
