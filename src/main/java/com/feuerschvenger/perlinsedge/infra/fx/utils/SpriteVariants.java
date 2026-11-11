package com.feuerschvenger.perlinsedge.infra.fx.utils;

/**
 * Deterministic variant selection, so the same tile or resource always shows the same sprite without storing a choice.
 */
public final class SpriteVariants {

    private SpriteVariants() {}

    /**
     * Picks a variant for a tile from its coordinates.
     *
     * @param x     the tile column
     * @param y     the tile row
     * @param count how many variants exist
     * @return an index in [0, count), or 0 when count is not above one
     */
    public static int forTile(int x, int y, int count) {
        return forSeed(x * 73_856_093L ^ y * 19_349_663L, count);
    }

    /**
     * Picks a variant from a seed.
     *
     * @param seed  any value that stays the same for the object
     * @param count how many variants exist
     * @return an index in [0, count), or 0 when count is not above one
     */
    public static int forSeed(long seed, int count) {
        if (count <= 1) {
            return 0;
        }
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 32;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (int) Math.floorMod(h, (long) count);
    }
}
