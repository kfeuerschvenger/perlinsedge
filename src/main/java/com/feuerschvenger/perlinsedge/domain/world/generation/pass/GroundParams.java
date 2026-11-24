package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Objects;

/**
 * How the terrain of a map is decided.
 *
 * @param rule            decides the terrain of a tile from its height and climate
 * @param ditherAmplitude the largest amount, up or down, the height of a tile is nudged by before the rule sees it, so
 *                        the thresholds make ragged borders instead of smooth contour lines; 0 or more
 */
public record GroundParams(GroundRule rule, float ditherAmplitude) {

    /** Checks the rule is set and the amplitude is a number that is not negative. */
    public GroundParams {
        Objects.requireNonNull(rule, "rule");
        if (!(ditherAmplitude >= 0F) || Float.isInfinite(ditherAmplitude)) {
            throw new IllegalArgumentException("the dither amplitude must be a finite number of at least 0: "
                    + ditherAmplitude);
        }
    }
}
