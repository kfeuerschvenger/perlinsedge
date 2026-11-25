package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The invariants of generated maps: a common list that every type must satisfy and a per type list that holds what
 * only one identity guarantees.
 */
public final class MapInvariants {

    /** How far around the spawn the ground has to be free of resources, as the recipes set it. */
    private static final int SPAWN_RADIUS = 3;
    /** The area the guaranteed resource counts are given for, a map of 300 by 300 tiles. */
    private static final long QUOTA_AREA = 90_000L;
    /** The fewest iron ore tiles and the most, on a map of {@link #QUOTA_AREA} tiles. */
    private static final int MIN_IRON = 40;
    private static final int MAX_IRON = 100;
    /** The fewest crystal tiles and the most, on a map of {@link #QUOTA_AREA} tiles. */
    private static final int MIN_CRYSTALS = 10;
    private static final int MAX_CRYSTALS = 30;
    /** The fewest stone tiles and the most, on a map of {@link #QUOTA_AREA} tiles. */
    private static final int MIN_STONE = 200;
    private static final int MAX_STONE = 600;
    /** The fewest trees on a map of {@link #QUOTA_AREA} tiles. */
    private static final int MIN_TREES = 400;
    /** The share of the land that is rocky ground at the least, on every map type. */
    private static final double MIN_ROCKY_SHARE = 0.01;
    /** The rocky share of the land of the types that expose rock, the desert and the mountains. */
    private static final double MIN_EXPOSED_ROCKY_SHARE = 0.08;
    /** The share of the land that holds a tree in a dense forest, from the sparse side to the thick side. */
    private static final double MIN_FOREST_TREE_SHARE = 0.28;
    private static final double MAX_FOREST_TREE_SHARE = 0.42;
    /** The share of the land under forest floor in a dense forest at the least. */
    private static final double MIN_FOREST_FLOOR_SHARE = 0.35;
    /** The share of the map that is ice at the least, in the tundra. */
    private static final double MIN_TUNDRA_ICE_SHARE = 0.15;
    /** The share of the land that is snow at the least, in the tundra. */
    private static final double MIN_TUNDRA_SNOW_SHARE = 0.10;
    /** The share of land a map may cover with resources: open ground stays the common case. */
    private static final double MAX_RESOURCE_SHARE = 0.45;

    private MapInvariants() {
    }

    /**
     * Builds the invariants every map type satisfies.
     *
     * @param source where maps come from, which the determinism check uses to generate a second copy
     * @return the invariants
     */
    public static List<MapInvariant> common(MapSource source) {
        return List.of(MapInvariants::hasTheRequestedSize, MapInvariants::everyTileIsInPlace,
                MapInvariants::theSpawnIsOnFreeGround, MapInvariants::theSpawnRadiusIsFreeOfResources,
                MapInvariants::theSpawnIsInTheLargestRegion, MapInvariants::noResourceOnWaterOrIce,
                MapInvariants::oreLiesOnRockyGroundOnly, MapInvariants::resourcesMeetTheirQuotas,
                MapInvariants::rockyGroundExists, MapInvariants::forestFloorFollowsTheTrees,
                MapInvariants::mostLandIsOpen, WaterInvariants::theSpawnRegionHoldsMostOfTheLand,
                deterministic(source));
    }

    /**
     * Builds the invariants of each map type.
     *
     * @return the invariants by type, with an entry for every type, as a map that cannot be modified
     */
    public static Map<MapType, List<MapInvariant>> perType() {
        final var byType = new EnumMap<MapType, List<MapInvariant>>(MapType.class);
        for (var type : MapType.values()) {
            byType.put(type, List.of());
        }
        byType.put(MapType.DENSE_FOREST,
                List.of(MapInvariants::denseForestHasThickGroves, MapInvariants::denseForestHasForestFloor));
        byType.put(MapType.DESERT, List.of(MapInvariants::desertTreesStandOnSand, MapInvariants::rockIsExposed,
                WaterInvariants::desertHasFewWaterBodies, WaterInvariants::desertHasRareOases));
        byType.put(MapType.MOUNTAINOUS, List.of(MapInvariants::rockIsExposed));
        byType.put(MapType.RIVERS, List.of(WaterInvariants::riversAreFewLongAndConnected,
                WaterInvariants::riversMapKeepsItsWaterInTheRivers, WaterInvariants::theForestGrowsAlongTheRivers));
        byType.put(MapType.TUNDRA, List.of(MapInvariants::tundraIsFrozenAndSnowy, MapInvariants::riversExist));
        byType.put(MapType.MEDITERRANEAN, List.of(MapInvariants::riversExist));
        return Collections.unmodifiableMap(byType);
    }

    private static void hasTheRequestedSize(TileMap map, MapSpec spec) {
        require(map.getWidth() == spec.size() && map.getHeight() == spec.size(),
                "expected " + spec.size() + "x" + spec.size() + " but got " + map.getWidth() + "x" + map.getHeight());
    }

    private static void everyTileIsInPlace(TileMap map, MapSpec spec) {
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                require(tile != null && tile.getX() == x && tile.getY() == y && tile.getType() != null,
                        "tile (" + x + "," + y + ") is missing or holds the wrong coordinates or type");
            }
        }
    }

    private static void theSpawnIsOnFreeGround(TileMap map, MapSpec spec) {
        require(map.getSpawn().isPresent(), "the map has no spawn point");
        final var spawn = map.getSpawn().orElseThrow();
        require(MapStats.isFree(map.getTile(spawn.x(), spawn.y())),
                "the spawn " + spawn + " is not walkable ground free of resources");
    }

    private static void theSpawnRadiusIsFreeOfResources(TileMap map, MapSpec spec) {
        final var spawn = map.getSpawn().orElseThrow();
        for (var dy = -SPAWN_RADIUS; dy <= SPAWN_RADIUS; dy++) {
            for (var dx = -SPAWN_RADIUS; dx <= SPAWN_RADIUS; dx++) {
                final var tile = map.getTile(spawn.x() + dx, spawn.y() + dy);
                require(tile == null || !tile.hasResource(),
                        "a resource stands within " + SPAWN_RADIUS + " tiles of the spawn " + spawn + " at ("
                                + (spawn.x() + dx) + "," + (spawn.y() + dy) + ")");
            }
        }
    }

    private static void theSpawnIsInTheLargestRegion(TileMap map, MapSpec spec) {
        final var spawn = map.getSpawn().orElseThrow();
        final var sizes = MapStats.regionSizes(map);
        final var spawnRegion = sizes[spawn.y() * map.getWidth() + spawn.x()];
        final var largest = Arrays.stream(sizes).max().orElse(0);
        require(spawnRegion == largest, "the spawn region has " + spawnRegion + " free tiles but the largest has "
                + largest);
    }

    private static void noResourceOnWaterOrIce(TileMap map, MapSpec spec) {
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                require(!tile.hasResource() || tile.getType().isLand() && tile.getType() != TileType.ICE,
                        "a resource stands on " + tile.getType() + " at (" + x + "," + y + ")");
            }
        }
    }

    private static void oreLiesOnRockyGroundOnly(TileMap map, MapSpec spec) {
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                final var resource = tile.getWorldResource();
                final var isOre = resource != null
                        && (resource.getType() == ResourceType.IRON_ORE || resource.getType() == ResourceType.CRYSTALS);
                require(!isOre || tile.getType() == TileType.ROCKY_GROUND,
                        "ore stands on " + tile.getType() + " at (" + x + "," + y + ")");
            }
        }
    }

    private static void resourcesMeetTheirQuotas(TileMap map, MapSpec spec) {
        requireWithin(map, spec, ResourceType.IRON_ORE, MIN_IRON, MAX_IRON);
        requireWithin(map, spec, ResourceType.CRYSTALS, MIN_CRYSTALS, MAX_CRYSTALS);
        requireWithin(map, spec, ResourceType.STONE, MIN_STONE, MAX_STONE);
        requireWithin(map, spec, ResourceType.TREE, MIN_TREES, Integer.MAX_VALUE);
    }

    /** Requires the count of a resource to lie in a range given for a map of {@link #QUOTA_AREA} tiles. */
    private static void requireWithin(TileMap map, MapSpec spec, ResourceType type, int min, int max) {
        requireScaled(type.toString(), MapStats.count(map, type), spec, min, max);
    }

    /**
     * Requires a count to lie in a range given for a map of 300 by 300 tiles, scaled to the area of the map: the
     * lowest is rounded down and is at least 1, and the highest is rounded up.
     *
     * @param what  what is counted, for the message
     * @param count the number counted
     * @param spec  the map, which gives the area
     * @param min   the lowest count on a map of 300 by 300 tiles
     * @param max   the highest count on a map of 300 by 300 tiles, or {@link Integer#MAX_VALUE} for no highest
     */
    static void requireScaled(String what, long count, MapSpec spec, int min, int max) {
        final var area = (long) spec.size() * spec.size();
        final var lowest = Math.max(1L, min * area / QUOTA_AREA);
        final var highest = max == Integer.MAX_VALUE ? Long.MAX_VALUE : (max * area + QUOTA_AREA - 1) / QUOTA_AREA;
        require(count >= lowest && count <= highest, what + " count " + count + " is outside " + lowest + " to "
                + highest);
    }

    private static void rockyGroundExists(TileMap map, MapSpec spec) {
        final var land = MapStats.landTiles(map);
        final var rocky = MapStats.rockyTiles(map);
        require(rocky >= land * MIN_ROCKY_SHARE, "only " + rocky + " of " + land + " land tiles are rocky ground");
    }

    private static void forestFloorFollowsTheTrees(TileMap map, MapSpec spec) {
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                if (tile.getType() == TileType.FOREST) {
                    require(hasTreeNear(map, x, y), "forest floor without a tree near it at (" + x + "," + y + ")");
                }
                final var isFloor = tile.getType() == TileType.GRASS || tile.getType() == TileType.SAVANNA;
                require(!isFloor || !isTree(tile), "a tree stands on " + tile.getType() + " at (" + x + "," + y + ")");
            }
        }
    }

    private static boolean hasTreeNear(TileMap map, int x, int y) {
        for (var dy = -1; dy <= 1; dy++) {
            for (var dx = -1; dx <= 1; dx++) {
                final var near = map.isValidCoordinate(x + dx, y + dy) ? map.getTile(x + dx, y + dy) : null;
                if (near != null && isTree(near)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isTree(Tile tile) {
        return tile.hasResource() && tile.getWorldResource().getType() == ResourceType.TREE;
    }

    private static void denseForestHasThickGroves(TileMap map, MapSpec spec) {
        final var share = MapStats.count(map, ResourceType.TREE) / (double) MapStats.landTiles(map);
        require(share >= MIN_FOREST_TREE_SHARE && share <= MAX_FOREST_TREE_SHARE,
                "trees cover " + share + " of the land of a dense forest");
    }

    private static void denseForestHasForestFloor(TileMap map, MapSpec spec) {
        final var share = MapStats.count(map, TileType.FOREST) / (double) MapStats.landTiles(map);
        require(share >= MIN_FOREST_FLOOR_SHARE, "forest floor covers only " + share + " of the land");
    }

    private static void rockIsExposed(TileMap map, MapSpec spec) {
        final var share = MapStats.rockyTiles(map) / (double) MapStats.landTiles(map);
        require(share >= MIN_EXPOSED_ROCKY_SHARE, "rocky ground covers only " + share + " of the land");
    }

    private static void tundraIsFrozenAndSnowy(TileMap map, MapSpec spec) {
        final var area = (double) map.getWidth() * map.getHeight();
        final var ice = MapStats.count(map, TileType.ICE) / area;
        final var snow = MapStats.count(map, TileType.SNOW) / (double) MapStats.landTiles(map);
        require(ice >= MIN_TUNDRA_ICE_SHARE, "ice covers only " + ice + " of the map");
        require(snow >= MIN_TUNDRA_SNOW_SHARE, "snow covers only " + snow + " of the land");
    }

    /** Relative river sources give every height profile rivers, which an absolute source height did not. */
    private static void riversExist(TileMap map, MapSpec spec) {
        require(MapStats.count(map, TileType.RIVER) > 0, "the map has no river tile");
    }

    private static void mostLandIsOpen(TileMap map, MapSpec spec) {
        var land = 0;
        var covered = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                land += tile.getType().isLand() ? 1 : 0;
                covered += tile.getType().isLand() && tile.hasResource() ? 1 : 0;
            }
        }
        require(land > 0 && covered <= land * MAX_RESOURCE_SHARE,
                covered + " of " + land + " land tiles hold a resource");
    }

    /** The desert guarantees the minimum of trees every map type does, so only where they stand is its own. */
    private static void desertTreesStandOnSand(TileMap map, MapSpec spec) {
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                final var isTree = tile.hasResource() && tile.getWorldResource().getType() == ResourceType.TREE;
                require(!isTree || tile.getType() == TileType.SAND,
                        "a desert tree stands on " + tile.getType() + " at (" + x + "," + y + ")");
            }
        }
    }

    private static MapInvariant deterministic(MapSource source) {
        return (TileMap map, MapSpec spec) -> {
            final var again = source.generate(spec);
            require(map.getSpawn().equals(again.getSpawn()), "the spawn differs between two runs");
            for (var y = 0; y < map.getHeight(); y++) {
                for (var x = 0; x < map.getWidth(); x++) {
                    require(sameContent(map, again, x, y), "tile (" + x + "," + y + ") differs between two runs");
                }
            }
        };
    }

    /**
     * Compares everything the generator decides for a tile: terrain, height and the resource with its seed and
     * health.
     */
    private static boolean sameContent(TileMap first, TileMap second, int x, int y) {
        final var a = first.getTile(x, y);
        final var b = second.getTile(x, y);
        return a.getType() == b.getType() && Float.compare(a.getHeight(), b.getHeight()) == 0
                && sameResource(a.getWorldResource(), b.getWorldResource());
    }

    private static boolean sameResource(Resource a, Resource b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getType() == b.getType() && a.getVisualSeed() == b.getVisualSeed() && a.getHealth() == b.getHealth()
                && a.isDepleted() == b.isDepleted();
    }

    /** Fails with a message unless a condition holds. */
    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
