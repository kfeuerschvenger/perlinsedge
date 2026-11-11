package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.generation.PerlinMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Chooses which art kind a resource is drawn as when a biome holds more than one kind for it.
 * A tree of the desert is a palm or a cactus: palms stand around water and cacti out in the dry sand, with a few of the
 * other kind in each place so neither looks planted by rule. The choice is a pure function of the biome, the surroundings,
 * the coordinates and the world seed, so a tree keeps its kind on every frame without storing it.
 */
public final class ResourceKinds {

    /** The art kind of a desert tree near water. */
    public static final String PALM = "palm";

    /** The art kind of a desert tree in the dry sand. */
    public static final String CACTUS = "cactus";

    /** One desert tree in this many is of the kind that does not suit its surroundings. */
    static final int ODD_ONE_IN = 6;

    private static final long SALT_ODD = 0x6A09_E667_F3BCL;

    /** Not instantiable: the class only holds static rules. */
    private ResourceKinds() {}

    /**
     * Tells whether the kind of a resource depends on whether water lies nearby, so the caller only looks around when needed.
     *
     * @param mapType      the map type
     * @param resourceType the resource type
     * @return true for the trees of the desert
     */
    public static boolean dependsOnWater(MapType mapType, ResourceType resourceType) {
        return mapType == MapType.DESERT && resourceType == ResourceType.TREE;
    }

    /**
     * Tells whether water lies within the reach that world generation gathers palms in.
     *
     * @param map the map
     * @param x   the tile column
     * @param y   the tile row
     * @return true when a water tile is within {@link PerlinMapGenerator#OASIS_PALM_REACH} tiles
     */
    public static boolean isNearWater(TileMap map, int x, int y) {
        final int reach = PerlinMapGenerator.OASIS_PALM_REACH;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                final Tile tile = map.getTile(x + dx, y + dy);
                if (tile != null && tile.getType().isWater()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Picks the art kind of a resource.
     *
     * @param mapType      the map type
     * @param resourceType the resource type
     * @param nearWater    whether water lies nearby, only read for the kinds that depend on it
     * @param x            the tile column
     * @param y            the tile row
     * @param seed         the world seed
     * @return the preferred art kind, or null when the biome leaves the choice to the plain variant pick
     */
    public static String preferredKind(MapType mapType, ResourceType resourceType, boolean nearWater, int x, int y, long seed) {
        if (!dependsOnWater(mapType, resourceType)) {
            return null;
        }
        final boolean odd = SpriteVariants.forResource(seed ^ SALT_ODD, x, y, ODD_ONE_IN) == 0;
        return nearWater != odd ? PALM : CACTUS;
    }
}
