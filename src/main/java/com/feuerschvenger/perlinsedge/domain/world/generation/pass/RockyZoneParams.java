package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Where rocky ground lies.
 *
 * @param landShare     the share of the host tiles, from 0 to 1, that become rocky ground, taken from the places where
 *                      the rock noise plus the height bias is highest
 * @param heightBias    how much the height adds to the rock noise when the zones are chosen, so a positive bias puts
 *                      rock on high ground and 0 leaves it to the noise alone
 * @param foothillReach how far from a mountain, in tiles of Chebyshev distance, walkable host tiles also become rocky
 *                      ground; 0 leaves the feet of mountains alone
 * @param hosts         the terrains that can turn into rocky ground, copied
 */
public record RockyZoneParams(float landShare, float heightBias, int foothillReach, Set<TileType> hosts) {

    /** Checks the share, the bias and the reach and copies the hosts. */
    public RockyZoneParams {
        if (Float.isNaN(landShare) || landShare < 0F || landShare > 1F) {
            throw new IllegalArgumentException("the rocky land share must be from 0 to 1: " + landShare);
        }
        if (!Float.isFinite(heightBias)) {
            throw new IllegalArgumentException("the height bias must be a finite number: " + heightBias);
        }
        if (foothillReach < 0) {
            throw new IllegalArgumentException("the foothill reach cannot be negative: " + foothillReach);
        }
        Objects.requireNonNull(hosts, "hosts");
        hosts = copyOf(hosts);
    }

    /**
     * Returns the terrains that can turn into rocky ground.
     *
     * @return a copy of the set of host terrains
     */
    @Override
    public Set<TileType> hosts() {
        return copyOf(hosts);
    }

    private static Set<TileType> copyOf(Set<TileType> types) {
        final var copy = EnumSet.noneOf(TileType.class);
        copy.addAll(types);
        return copy;
    }
}
