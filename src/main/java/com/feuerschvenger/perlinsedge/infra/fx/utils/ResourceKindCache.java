package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Remembers the art kind that {@link ResourceKinds} picks for each tile, so the water look-around runs once per resource
 * instead of once per frame. The cache is tied to one map, map type and world seed and empties itself when any of them
 * changes, which covers a map rebuild.
 */
public final class ResourceKindCache {

    private TileMap map;
    private MapType mapType;
    private long seed;
    private String[] kinds = new String[0];

    /** Forgets the remembered kinds and lets go of the map, so a discarded world is not kept alive by the cache. */
    public void invalidate() {
        map = null;
        mapType = null;
        seed = 0L;
        kinds = new String[0];
    }

    /**
     * Returns the preferred art kind of a resource, looking around for water only the first time a tile is asked about.
     *
     * @param tileMap      the map the resource stands on
     * @param type         the map type
     * @param resourceType the resource type
     * @param x            the tile column
     * @param y            the tile row
     * @param worldSeed    the world seed
     * @return the preferred art kind, or null when the biome leaves the choice to the plain variant pick
     */
    public String preferredKind(TileMap tileMap, MapType type, ResourceType resourceType, int x, int y, long worldSeed) {
        if (!ResourceKinds.dependsOnWater(type, resourceType)) {
            return null;
        }
        if (tileMap != map || type != mapType || worldSeed != seed) {
            map = tileMap;
            mapType = type;
            seed = worldSeed;
            kinds = new String[tileMap.getWidth() * tileMap.getHeight()];
        }
        if (x < 0 || y < 0 || x >= tileMap.getWidth() || y >= tileMap.getHeight()) {
            return ResourceKinds.preferredKind(type, resourceType, ResourceKinds.isNearWater(tileMap, x, y), x, y, worldSeed);
        }
        final int index = y * tileMap.getWidth() + x;
        String kind = kinds[index];
        if (kind == null) {
            kind = ResourceKinds.preferredKind(type, resourceType, ResourceKinds.isNearWater(tileMap, x, y), x, y, worldSeed);
            kinds[index] = kind;
        }
        return kind;
    }
}
