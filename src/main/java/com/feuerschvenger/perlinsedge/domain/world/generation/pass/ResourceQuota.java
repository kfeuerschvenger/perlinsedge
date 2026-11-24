package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

import java.util.Objects;

/**
 * How many tiles of one resource a map holds, per 100,000 tiles of map, so the range scales with the area.
 *
 * @param type            the resource
 * @param minPer100k      the fewest tiles of the resource per 100,000 tiles of map, 0 for no minimum
 * @param maxPer100k      the most tiles per 100,000 tiles of map, from the minimum up to {@link #UNLIMITED}
 * @param host            where missing resources are added
 * @param zoneCapPerMille for a {@link Host#ROCKY} resource, the most of a rocky zone in thousandths that the first
 *                        round of adding fills with it, from 1 to 1000, so no resource takes a whole zone
 */
public record ResourceQuota(ResourceType type, int minPer100k, int maxPer100k, Host host, int zoneCapPerMille) {

    /** The maximum that stands for no maximum. */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private static final double MAP_SCALE = 100_000.0;
    private static final int MAX_CAP = 1000;

    /** Where a resource that is missing is added. */
    public enum Host {
        /** On the free tiles of rocky ground, zone by zone from the largest. */
        ROCKY,
        /** On the free fertile tiles where the tree noise is highest. */
        FERTILE
    }

    /** Checks the type and the host are set and the range and the cap are valid. */
    public ResourceQuota {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(host, "host");
        if (minPer100k < 0 || maxPer100k < minPer100k) {
            throw new IllegalArgumentException("the range " + minPer100k + " to " + maxPer100k + " is not valid");
        }
        if (zoneCapPerMille < 1 || zoneCapPerMille > MAX_CAP) {
            throw new IllegalArgumentException("the zone cap must be from 1 to " + MAX_CAP + ": " + zoneCapPerMille);
        }
    }

    /**
     * Returns the fewest tiles for a map.
     *
     * @param area the number of tiles of the map
     * @return the minimum, rounded to a whole tile
     */
    public int minFor(int area) {
        return scaled(minPer100k, area);
    }

    /**
     * Returns the most tiles for a map.
     *
     * @param area the number of tiles of the map
     * @return the maximum, rounded to a whole tile, or {@link #UNLIMITED}
     */
    public int maxFor(int area) {
        return maxPer100k == UNLIMITED ? UNLIMITED : scaled(maxPer100k, area);
    }

    private static int scaled(int per100k, int area) {
        return (int) Math.round(per100k * (double) area / MAP_SCALE);
    }
}
