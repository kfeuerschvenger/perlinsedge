package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;

/**
 * How the rivers of a map are made. A recipe without rivers does not list the pass at all.
 *
 * @param density         scales how many of the high land tiles become the source of a river, above 0 and finite
 * @param sourceMinHeight a river starts only on land above this height, and a branch only where the height is not
 *                        below it, finite
 * @param minLength       the fewest steps a river runs, at least 1
 * @param maxLength       one more than the most steps a river runs, above the fewest
 * @param tributaries     how a river branches
 * @param lakes           how a river that dead ends makes a lake
 */
public record RiverParams(float density, float sourceMinHeight, int minLength, int maxLength,
                          TributaryParams tributaries, LakeParams lakes) {

    /** Checks the density is positive, the lengths are ordered and the parts are set. */
    public RiverParams {
        if (!Float.isFinite(density) || density <= 0F) {
            throw new IllegalArgumentException("the river density must be finite and above 0: " + density);
        }
        if (!Float.isFinite(sourceMinHeight)) {
            throw new IllegalArgumentException("the source height must be finite: " + sourceMinHeight);
        }
        if (minLength < 1 || maxLength <= minLength) {
            throw new IllegalArgumentException("the river lengths need 1 <= min < max: " + minLength + " and "
                    + maxLength);
        }
        Objects.requireNonNull(tributaries, "tributaries");
        Objects.requireNonNull(lakes, "lakes");
    }
}
