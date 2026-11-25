package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How a long coast is cut along one edge of the map.
 *
 * @param depthTiles how many tiles in from the edge the height is lowered, at least 1; the lowering fades smoothly to
 *                   nothing at this depth
 * @param drop       how much the height is lowered at the edge itself, finite and above 0
 */
public record EdgeCoastParams(int depthTiles, float drop) {

    /** Checks the depth reaches at least one tile and the drop is a positive number. */
    public EdgeCoastParams {
        if (depthTiles < 1) {
            throw new IllegalArgumentException("the coast depth must be at least 1 tile: " + depthTiles);
        }
        if (!Float.isFinite(drop) || drop <= 0F) {
            throw new IllegalArgumentException("the coast drop must be finite and above 0: " + drop);
        }
    }
}
