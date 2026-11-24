package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
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
    /** The most iron ore any single map may have, a scarce share of the thousands of rocky tiles of a range. */
    private static final int MAX_IRON = 150;
    /** The most crystals any single map may have. */
    private static final int MAX_CRYSTALS = 40;
    /** A map with at least this many rocky tiles has iron ore, as the chance of none is far below one in a million. */
    private static final int MUCH_ROCK = 2_000;
    /** The share of land a map may cover with resources: open ground stays the common case. */
    private static final double MAX_RESOURCE_SHARE = 0.45;
    /** The fewest trees a desert has on a map of {@link #DESERT_REFERENCE_AREA} tiles, scaled to the area of others. */
    private static final int MIN_DESERT_TREES = 300;
    /** The area the minimum number of desert trees is given for, a map of 150 by 150 tiles. */
    private static final int DESERT_REFERENCE_AREA = 22_500;

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
                MapInvariants::oreLiesOnRockOnly, MapInvariants::oreIsScarce, MapInvariants::muchRockHasIron,
                MapInvariants::mostLandIsOpen, deterministic(source));
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
        byType.put(MapType.DESERT, List.of(MapInvariants::desertTreesStandOnSand, MapInvariants::hasIronAndCrystals));
        byType.put(MapType.MOUNTAINOUS, List.of(MapInvariants::hasIronAndCrystals));
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

    private static void oreLiesOnRockOnly(TileMap map, MapSpec spec) {
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                final var resource = tile.getWorldResource();
                final var isOre = resource != null
                        && (resource.getType() == ResourceType.IRON_ORE || resource.getType() == ResourceType.CRYSTALS);
                require(!isOre || MapStats.isRocky(tile.getType()),
                        "ore stands on " + tile.getType() + " at (" + x + "," + y + ")");
            }
        }
    }

    private static void oreIsScarce(TileMap map, MapSpec spec) {
        final var iron = MapStats.count(map, ResourceType.IRON_ORE);
        final var crystals = MapStats.count(map, ResourceType.CRYSTALS);
        require(iron <= MAX_IRON, "too much iron ore: " + iron);
        require(crystals <= MAX_CRYSTALS, "too many crystals: " + crystals);
    }

    private static void muchRockHasIron(TileMap map, MapSpec spec) {
        final var rocky = MapStats.rockyTiles(map);
        require(rocky < MUCH_ROCK || MapStats.count(map, ResourceType.IRON_ORE) > 0,
                "a map with " + rocky + " rocky tiles has no iron ore");
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

    private static void hasIronAndCrystals(TileMap map, MapSpec spec) {
        require(MapStats.count(map, ResourceType.IRON_ORE) > 0, "the map has no iron ore");
        require(MapStats.count(map, ResourceType.CRYSTALS) > 0, "the map has no crystals");
    }

    private static void desertTreesStandOnSand(TileMap map, MapSpec spec) {
        var trees = 0;
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                final var isTree = tile.hasResource() && tile.getWorldResource().getType() == ResourceType.TREE;
                trees += isTree ? 1 : 0;
                require(!isTree || tile.getType() == TileType.SAND,
                        "a desert tree stands on " + tile.getType() + " at (" + x + "," + y + ")");
            }
        }
        require(trees >= MIN_DESERT_TREES * spec.size() * spec.size() / DESERT_REFERENCE_AREA,
                "the desert has only " + trees + " trees");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
