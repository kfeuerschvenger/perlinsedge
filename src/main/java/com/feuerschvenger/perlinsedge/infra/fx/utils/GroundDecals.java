package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;

/**
 * Chooses which small decal, if any, lies on a tile and where inside the tile it sits.
 * Everything is a pure function of the tile coordinates and the world seed, so a tile shows the same decal on every
 * frame without storing a choice, and a different seed scatters them differently. Decals are cosmetic: they have no
 * collision and no effect on gameplay.
 */
public final class GroundDecals {

    /** Names of the decals drawn on grass, forest, savanna, hill and swamp. */
    static final List<String> GREEN_DECALS = List.of(
            "tuft-1", "tuft-2", "tuft-3", "pebble-1", "pebble-2", "flower-1", "flower-2", "mushroom-1");

    /** Names of the decals drawn on sand and beach. */
    static final List<String> SAND_DECALS = List.of("shell-1", "pebble-1");

    /** Names of the decals drawn on the sand bed under shallow water. */
    static final List<String> UNDERWATER_DECALS = List.of("seaweed-1", "seaweed-2", "pebble-1");

    /** Every decal name the ground or a water bed can show, for the loader. */
    public static final List<String> ALL = List.of(
            "tuft-1", "tuft-2", "tuft-3", "pebble-1", "pebble-2", "flower-1", "flower-2", "mushroom-1", "shell-1",
            "seaweed-1", "seaweed-2");

    /** One shallow water tile in this many holds an underwater decal. */
    static final int UNDERWATER_ONE_IN = 6;

    /** One tile in this many holds a decal on green ground. */
    static final int GREEN_ONE_IN = 4;

    /** One tile in this many holds a decal on sand. */
    static final int SAND_ONE_IN = 8;

    /**
     * How far from the tile centre a decal may sit along each isometric axis, as a fraction of the tile.
     * A tile edge is at 0.5, so the margin keeps a decal of up to a quarter of the tile width inside the top diamond.
     */
    static final double MAX_OFFSET = 0.25;

    private static final long SALT_PICK = 0x51ED_270B_1F2DL;
    private static final long SALT_UNDERWATER = 0x1F83_D9AB_FB41L;
    private static final long SALT_U = 0x2545_F491_4F6CL;
    private static final long SALT_V = 0x7F4A_7C15_9E37L;

    private GroundDecals() {}

    /**
     * Picks the decal of a tile.
     *
     * @param type the tile type
     * @param x    the tile column
     * @param y    the tile row
     * @param seed the world seed
     * @return the decal name, or null when the tile holds none
     */
    public static String decalFor(TileType type, int x, int y, long seed) {
        List<String> pool;
        int oneIn;
        switch (type) {
            case GRASS, FOREST, SAVANNA, HILL, SWAMP -> {
                pool = GREEN_DECALS;
                oneIn = GREEN_ONE_IN;
            }
            case SAND, BEACH -> {
                pool = SAND_DECALS;
                oneIn = SAND_ONE_IN;
            }
            default -> {
                return null;
            }
        }
        long h = mix(x, y, seed, SALT_PICK);
        if (Math.floorMod(h, (long) oneIn) != 0) {
            return null;
        }
        return pool.get((int) Math.floorMod(h >>> 20, (long) pool.size()));
    }

    /**
     * Picks the decal lying on the bed of a water tile, which the renderer draws under the translucent water.
     *
     * @param type the tile type
     * @param x    the tile column
     * @param y    the tile row
     * @param seed the world seed
     * @return the decal name, or null when the tile is not shallow water or holds none
     */
    public static String underwaterDecalFor(TileType type, int x, int y, long seed) {
        if (type != TileType.SHALLOW_WATER) {
            return null;
        }
        long h = mix(x, y, seed, SALT_UNDERWATER);
        if (Math.floorMod(h, (long) UNDERWATER_ONE_IN) != 0) {
            return null;
        }
        return UNDERWATER_DECALS.get((int) Math.floorMod(h >>> 20, (long) UNDERWATER_DECALS.size()));
    }

    /**
     * Returns the offset of a tile's decal along the first isometric axis.
     *
     * @param x    the tile column
     * @param y    the tile row
     * @param seed the world seed
     * @return a fraction of the tile in [-MAX_OFFSET, MAX_OFFSET]
     */
    public static double offsetU(int x, int y, long seed) {
        return unit(mix(x, y, seed, SALT_U)) * MAX_OFFSET;
    }

    /**
     * Returns the offset of a tile's decal along the second isometric axis.
     *
     * @param x    the tile column
     * @param y    the tile row
     * @param seed the world seed
     * @return a fraction of the tile in [-MAX_OFFSET, MAX_OFFSET]
     */
    public static double offsetV(int x, int y, long seed) {
        return unit(mix(x, y, seed, SALT_V)) * MAX_OFFSET;
    }

    /** Maps a hash to [-1, 1). */
    private static double unit(long hash) {
        return ((hash >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    /** Mixes the tile coordinates, the world seed and a salt into well spread bits. */
    static long mix(int x, int y, long seed, long salt) {
        long h = x * 73_856_093L ^ y * 19_349_663L ^ seed * 0x9E37_79B9_7F4A_7C15L ^ salt;
        h = (h ^ (h >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D0_49BB_1331_11EBL;
        return h ^ (h >>> 31);
    }
}
