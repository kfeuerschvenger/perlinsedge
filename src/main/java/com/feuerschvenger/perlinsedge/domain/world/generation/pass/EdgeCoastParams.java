package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How a long coast is cut along one edge of the map.
 *
 * @param depthShare how far in from the edge the height is lowered, as a share of the size of the map across that
 *                   edge, above 0 and at most 1; the lowering fades smoothly to nothing at this depth, so the sea
 *                   takes the same share of a small map and of a large one
 * @param drop       how much the height is lowered at the edge itself, finite and above 0
 */
public record EdgeCoastParams(float depthShare, float drop) {

    /** Checks the depth is a share of the map above 0 and the drop is a positive number. */
    public EdgeCoastParams {
        if (!(depthShare > 0F && depthShare <= 1F)) {
            throw new IllegalArgumentException("the coast depth share must be above 0 and at most 1: " + depthShare);
        }
        if (!Float.isFinite(drop) || drop <= 0F) {
            throw new IllegalArgumentException("the coast drop must be finite and above 0: " + drop);
        }
    }
}
