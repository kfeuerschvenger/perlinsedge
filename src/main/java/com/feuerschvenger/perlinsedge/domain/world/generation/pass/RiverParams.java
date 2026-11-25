package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;

/**
 * How the rivers of a map are made. A recipe without rivers does not list the pass at all.
 *
 * @param density      scales how many of the high land tiles become the source of a river, above 0 and finite
 * @param sourceShare  the share of the land, from the highest tile down, where a river may start, and where a branch
 *                     may start too, from 0 to 1; it is relative to the height profile of the map, so a map whose
 *                     land never gets high still has sources, which an absolute height could not give it
 * @param minLength    the fewest steps a river runs, at least 1
 * @param maxLength    one more than the most steps a river runs, above the fewest
 * @param tributaries  how a river branches
 * @param lakes        how a river that dead ends makes a lake
 */
public record RiverParams(float density, float sourceShare, int minLength, int maxLength,
                          TributaryParams tributaries, LakeParams lakes) {

    /** Checks the density is positive, the share is a share, the lengths are ordered and the parts are set. */
    public RiverParams {
        if (!Float.isFinite(density) || density <= 0F) {
            throw new IllegalArgumentException("the river density must be finite and above 0: " + density);
        }
        if (!(sourceShare >= 0F && sourceShare <= 1F)) {
            throw new IllegalArgumentException("the source share must be from 0 to 1: " + sourceShare);
        }
        if (minLength < 1 || maxLength <= minLength) {
            throw new IllegalArgumentException("the river lengths need 1 <= min < max: " + minLength + " and "
                    + maxLength);
        }
        Objects.requireNonNull(tributaries, "tributaries");
        Objects.requireNonNull(lakes, "lakes");
    }
}
