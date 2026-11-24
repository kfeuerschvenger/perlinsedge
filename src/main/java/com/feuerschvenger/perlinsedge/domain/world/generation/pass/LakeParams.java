package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

/**
 * How a river that dead ends spreads into a lake: the low ground around the endpoint is flooded.
 *
 * @param maxEndpointHeight a river makes a lake only where its endpoint lies below this height, finite
 * @param minRadius         the smallest radius of a lake in tiles, at least 1
 * @param maxRadius         one more than the largest radius, above the smallest
 * @param depthTolerance    how much higher than the endpoint a land tile may be and still be flooded, finite and not
 *                          negative
 * @param surface           the terrain a flooded tile becomes, shallow water or ice
 */
public record LakeParams(float maxEndpointHeight, int minRadius, int maxRadius, float depthTolerance,
                         TileType surface) {

    /** Checks the radii are ordered, the heights are finite and the surface is shallow water or ice. */
    public LakeParams {
        if (!Float.isFinite(maxEndpointHeight)) {
            throw new IllegalArgumentException("the endpoint height limit must be finite: " + maxEndpointHeight);
        }
        if (minRadius < 1 || maxRadius <= minRadius) {
            throw new IllegalArgumentException("the lake radii need 1 <= min < max: " + minRadius + " and "
                    + maxRadius);
        }
        if (!Float.isFinite(depthTolerance) || depthTolerance < 0F) {
            throw new IllegalArgumentException("the depth tolerance must be finite and not negative: "
                    + depthTolerance);
        }
        Objects.requireNonNull(surface, "surface");
        if (surface != TileType.SHALLOW_WATER && surface != TileType.ICE) {
            throw new IllegalArgumentException("the lake surface must be shallow water or ice: " + surface);
        }
    }
}
