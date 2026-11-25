package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

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

    /** The share of the land the player can walk to from the spawn at the least, whatever cuts the land. */
    private static final double MIN_SPAWN_REGION_SHARE = 0.30;
    /** The fewest and the most rivers on a rivers map, which the recipe keeps at both sizes. */
    private static final int MIN_RIVER_COMPONENTS = 2;
    private static final int MAX_RIVER_COMPONENTS = 4;
    /** The share of the map the rivers cover at the least. */
    private static final double MIN_RIVER_SHARE = 0.012;
    /** The share of a side that the bounding box of a river spans at the least, so it crosses the map. */
    private static final double MIN_RIVER_SPAN_SHARE = 0.70;
    /** The most of the map that is water other than rivers, and the most of the land that is sand, on a rivers map. */
    private static final double MAX_RIVERS_MAP_STILL_WATER_SHARE = 0.02;
    private static final double MAX_RIVERS_MAP_SAND_SHARE = 0.10;
    /** How many tiles from a river the gallery forest is measured, and how much thicker than the map it is. */
    private static final int GALLERY_REACH = 3;
    private static final double MIN_GALLERY_FACTOR = 1.5;

    /** The share of the longest coast side that is water at the least, on a Mediterranean map. */
    private static final double MIN_COAST_EDGE_WATER_SHARE = 0.80;
    /** The most inland water bodies, which touch no edge, a Mediterranean map holds. */
    private static final int MAX_MEDITERRANEAN_INLAND_BODIES = 6;

    /** The fewest and the most land masses of at least {@link WaterStats#MIN_LAND_TILES} tiles on an islands map. */
    private static final int MIN_ISLANDS = 6;
    private static final int MAX_ISLANDS = 18;
    /** The fewest islands that are small, from {@link WaterStats#MIN_LAND_TILES} to this many tiles. */
    private static final int MIN_SMALL_ISLANDS = 3;
    private static final int MAX_SMALL_ISLAND_TILES = 1_500;
    /** The share of the map that is land, at the least and the most. */
    private static final double MIN_ISLANDS_LAND_SHARE = 0.22;
    private static final double MAX_ISLANDS_LAND_SHARE = 0.50;
    /** The share of the land the largest island holds at the most, on a map of 300 by 300 tiles or more. */
    private static final double MAX_LARGEST_ISLAND_SHARE = 0.40;
    /** The area, in tiles, from which the largest island is held to its share; a smaller map holds too few islands. */
    private static final long FULL_SIZE_AREA = 90_000L;

    private WaterInvariants() {
    }

    static void islandsAreSeveralAndSomeAreSmall(TileMap map, MapSpec spec) {
        final var land = WaterStats.landMasses(map);
        MapInvariants.requireScaled("island", land.countAtLeast(WaterStats.MIN_LAND_TILES), spec, MIN_ISLANDS,
                MAX_ISLANDS);
        MapInvariants.requireScaled("small island", land.countWithin(WaterStats.MIN_LAND_TILES,
                MAX_SMALL_ISLAND_TILES), spec, MIN_SMALL_ISLANDS, Integer.MAX_VALUE);
    }

    static void islandsShareTheMapWithTheSea(TileMap map, MapSpec spec) {
        final var landTiles = MapStats.landTiles(map);
        final var landShare = landTiles / (double) (map.getWidth() * map.getHeight());
        MapInvariants.require(landShare >= MIN_ISLANDS_LAND_SHARE && landShare <= MAX_ISLANDS_LAND_SHARE,
                "land covers " + landShare + " of the map");
        if ((long) spec.size() * spec.size() >= FULL_SIZE_AREA) {
            final var largest = WaterStats.landMasses(map).largest() / (double) landTiles;
            MapInvariants.require(largest <= MAX_LARGEST_ISLAND_SHARE,
                    "the largest island holds " + largest + " of the land");
        }
    }

    static void theSpawnIsOnTheLargestIsland(TileMap map, MapSpec spec) {
        final var land = WaterStats.landMasses(map);
        final var spawn = map.getSpawn().orElseThrow();
        final var spawnIsland = land.sizeOf(land.labelAt(spawn.x(), spawn.y()));
        MapInvariants.require(spawnIsland == land.largest(), "the spawn is on an island of " + spawnIsland
                + " tiles but the largest holds " + land.largest());
    }

    static void mediterraneanHasALongCoastOnOneEdge(TileMap map, MapSpec spec) {
        final var share = WaterStats.wettestEdgeShare(map);
        MapInvariants.require(share >= MIN_COAST_EDGE_WATER_SHARE,
                "the wettest edge is only " + share + " water");
    }

    static void mediterraneanHasFewInlandWaterBodies(TileMap map, MapSpec spec) {
        final var inland = WaterStats.waterBodies(map).countInland(WaterStats.MIN_BODY_TILES, Integer.MAX_VALUE);
        MapInvariants.requireAtMostScaled("inland water body", inland, spec, MAX_MEDITERRANEAN_INLAND_BODIES);
    }

    static void theSpawnRegionHoldsMostOfTheLand(TileMap map, MapSpec spec) {
        final var share = WaterStats.spawnRegionShareOfLand(map);
        MapInvariants.require(share >= MIN_SPAWN_REGION_SHARE,
                "the spawn region holds only " + share + " of the land");
    }

    static void riversAreFewLongAndConnected(TileMap map, MapSpec spec) {
        final var rivers = WaterStats.rivers(map);
        MapInvariants.require(rivers.count() >= MIN_RIVER_COMPONENTS && rivers.count() <= MAX_RIVER_COMPONENTS,
                rivers.count() + " river components, expected " + MIN_RIVER_COMPONENTS + " to "
                        + MAX_RIVER_COMPONENTS);
        for (var label = 1; label <= rivers.count(); label++) {
            final var span = Math.max(rivers.spanX(label) / (double) map.getWidth(),
                    rivers.spanY(label) / (double) map.getHeight());
            MapInvariants.require(span >= MIN_RIVER_SPAN_SHARE,
                    "river " + label + " spans only " + span + " of a side");
        }
        final var share = MapStats.count(map, TileType.RIVER) / (double) (map.getWidth() * map.getHeight());
        MapInvariants.require(share >= MIN_RIVER_SHARE, "rivers cover only " + share + " of the map");
    }

    static void riversMapKeepsItsWaterInTheRivers(TileMap map, MapSpec spec) {
        final var area = (double) map.getWidth() * map.getHeight();
        final var stillWater = WaterStats.waterShare(map) - MapStats.count(map, TileType.RIVER) / area;
        MapInvariants.require(stillWater <= MAX_RIVERS_MAP_STILL_WATER_SHARE,
                "water other than rivers covers " + stillWater + " of the map");
        final var sand = MapStats.count(map, TileType.SAND) / (double) MapStats.landTiles(map);
        MapInvariants.require(sand <= MAX_RIVERS_MAP_SAND_SHARE, "sand covers " + sand + " of the land");
    }

    static void theForestGrowsAlongTheRivers(TileMap map, MapSpec spec) {
        final var near = new boolean[map.getWidth() * map.getHeight()];
        markAroundRivers(map, near);
        var nearLand = 0;
        var nearTrees = 0;
        for (var index = 0; index < near.length; index++) {
            final var tile = map.getTile(index % map.getWidth(), index / map.getWidth());
            if (near[index] && tile.getType().isLand()) {
                nearLand++;
                nearTrees += hasTree(tile) ? 1 : 0;
            }
        }
        final var mapShare = MapStats.count(map, ResourceType.TREE) / (double) MapStats.landTiles(map);
        final var alongRivers = nearTrees / (double) Math.max(1, nearLand);
        MapInvariants.require(alongRivers >= MIN_GALLERY_FACTOR * mapShare, "trees cover " + alongRivers
                + " of the land near the rivers and " + mapShare + " of the land of the map");
    }

    private static boolean hasTree(Tile tile) {
        return tile.hasResource() && tile.getWorldResource().getType() == ResourceType.TREE;
    }

    /** Marks every tile within the gallery reach of a river tile. */
    private static void markAroundRivers(TileMap map, boolean[] near) {
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                if (map.getTile(x, y).getType() == TileType.RIVER) {
                    markSquare(map, near, x, y);
                }
            }
        }
    }

    private static void markSquare(TileMap map, boolean[] near, int x, int y) {
        for (var dy = -GALLERY_REACH; dy <= GALLERY_REACH; dy++) {
            for (var dx = -GALLERY_REACH; dx <= GALLERY_REACH; dx++) {
                if (map.isValidCoordinate(x + dx, y + dy)) {
                    near[(y + dy) * map.getWidth() + x + dx] = true;
                }
            }
        }
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
