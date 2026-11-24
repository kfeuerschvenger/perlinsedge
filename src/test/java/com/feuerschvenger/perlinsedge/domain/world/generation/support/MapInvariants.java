package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The invariants of generated maps: a common list that every type must satisfy and a per type list that holds what only
 * one identity guarantees. Only properties that hold today are listed; stricter ones are added with the new pipeline.
 */
public final class MapInvariants {

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
                MapInvariants::hasAFreeWalkableTile, deterministic(source));
    }

    /**
     * Builds the invariants of each map type. Empty until the identities of the new pipeline define their own.
     *
     * @return the invariants by type, with an entry for every type, as a map that cannot be modified
     */
    public static Map<MapType, List<MapInvariant>> perType() {
        final var byType = new EnumMap<MapType, List<MapInvariant>>(MapType.class);
        for (var type : MapType.values()) {
            byType.put(type, List.of());
        }
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

    private static void hasAFreeWalkableTile(TileMap map, MapSpec spec) {
        final var spot = MapStats.nearestFreeTileToCentre(map);
        require(spot.isPresent(), "no walkable tile without a resource, so no player could start");
    }

    private static MapInvariant deterministic(MapSource source) {
        return (TileMap map, MapSpec spec) -> {
            final var again = source.generate(spec);
            for (var y = 0; y < map.getHeight(); y++) {
                for (var x = 0; x < map.getWidth(); x++) {
                    require(sameContent(map, again, x, y), "tile (" + x + "," + y + ") differs between two runs");
                }
            }
        };
    }

    /** Compares everything the generator decides for a tile: terrain, height and the resource with its seed and health. */
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
