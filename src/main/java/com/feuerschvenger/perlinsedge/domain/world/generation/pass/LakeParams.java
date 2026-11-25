package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

/**
 * How a river that dead ends spreads into a lake: the low ground around the endpoint is flooded.
 *
 * @param maxEndpointShare a river makes a lake only where its endpoint is among this share of the land, counted from
 *                         the lowest tile up, from 0 to 1; 0 never makes a lake and 1 makes one at every dead end
 * @param minRadius        the smallest radius of a lake in tiles, at least 1
 * @param maxRadius        one more than the largest radius, above the smallest
 * @param depthTolerance   how much higher than the endpoint a land tile may be and still be flooded, finite and not
 *                         negative
 * @param surface          the terrain a flooded tile becomes, shallow water or ice
 */
public record LakeParams(float maxEndpointShare, int minRadius, int maxRadius, float depthTolerance,
                         TileType surface) {

    /** Checks the radii are ordered, the share is a share, the tolerance is finite and the surface is water or ice. */
    public LakeParams {
        if (!(maxEndpointShare >= 0F && maxEndpointShare <= 1F)) {
            throw new IllegalArgumentException("the endpoint share must be from 0 to 1: " + maxEndpointShare);
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
