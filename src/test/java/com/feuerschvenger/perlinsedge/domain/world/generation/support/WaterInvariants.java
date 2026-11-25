package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * The invariants about water: how much of it there is and how it is split into bodies, rivers and land masses. Counts
 * are given for a map of 300 by 300 tiles and scaled to the area of the map under test.
 */
final class WaterInvariants {

    /** The most water bodies of at least {@link WaterStats#MIN_BODY_TILES} tiles a desert holds. */
    private static final int MAX_DESERT_BODIES = 10;
    /** The most of the map a desert covers with water. */
    private static final double MAX_DESERT_WATER_SHARE = 0.05;
    /** The fewest and the most oases a desert holds, which are the small bodies that touch no edge. */
    private static final int MIN_OASES = 2;
    private static final int MAX_OASES = 8;
    /** The most tiles an oasis has. */
    private static final int MAX_OASIS_TILES = 40;

    private WaterInvariants() {
    }

    static void desertHasFewWaterBodies(TileMap map, MapSpec spec) {
        final var bodies = WaterStats.waterBodies(map).countAtLeast(WaterStats.MIN_BODY_TILES);
        MapInvariants.requireScaled("water body", bodies, spec, 1, MAX_DESERT_BODIES);
        final var share = WaterStats.waterShare(map);
        MapInvariants.require(share <= MAX_DESERT_WATER_SHARE, "water covers " + share + " of a desert");
    }

    static void desertHasRareOases(TileMap map, MapSpec spec) {
        final var oases = WaterStats.waterBodies(map).countInland(WaterStats.MIN_BODY_TILES, MAX_OASIS_TILES);
        MapInvariants.requireScaled("oasis", oases, spec, MIN_OASES, MAX_OASES);
    }
}
