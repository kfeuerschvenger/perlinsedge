package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;

/**
 * Deterministic variant selection, so the same tile or resource always shows the same sprite without storing a choice.
 */
public final class SpriteVariants {

    /** Multipliers that spread the tile column and row over the seed. */
    private static final long COLUMN_HASH_PRIME = 73_856_093L;
    private static final long ROW_HASH_PRIME = 19_349_663L;

    /** Multiplier that mixes the tile hash into the world seed of a resource. */
    private static final long RESOURCE_SEED_MULTIPLIER = 0xD6E8_FEB8_6659_FD93L;

    /** Multipliers and shifts of the finaliser that turns a seed into well spread bits. */
    private static final long FIRST_MULTIPLIER = 0x9E37_79B9_7F4A_7C15L;
    private static final long SECOND_MULTIPLIER = 0xBF58_476D_1CE4_E5B9L;
    private static final int FIRST_SHIFT = 32;
    private static final int SECOND_SHIFT = 29;

    /** Not instantiable: the class only holds static rules. */
    private SpriteVariants() {}

    /**
     * Picks a variant for a tile from its patch value, so the variants follow the slow patches of the map instead of
     * scattering from tile to tile. The patch range is split into equal bands, one per variant.
     *
     * @param patch the patch value of the tile, from 0 to {@link Tile#MAX_PATCH}
     * @param count how many variants exist
     * @return an index in [0, count), or 0 when count is not above one
     * @throws IllegalArgumentException when the patch is outside 0 to {@link Tile#MAX_PATCH}
     */
    public static int forPatch(int patch, int count) {
        if (patch < 0 || patch > Tile.MAX_PATCH) {
            throw new IllegalArgumentException("the patch must be from 0 to " + Tile.MAX_PATCH + ": " + patch);
        }
        if (count <= 1) {
            return 0;
        }
        return Math.min(count - 1, patch * count / (Tile.MAX_PATCH + 1));
    }

    /**
     * Picks a variant for the resource on a tile from its coordinates and the world seed.
     * The same tile of the same world always answers the same index, across frames and reloads, while another world
     * lays the variants out differently.
     *
     * @param worldSeed the seed of the world
     * @param x         the tile column
     * @param y         the tile row
     * @param count     how many variants exist
     * @return an index in [0, count), or 0 when count is not above one
     */
    public static int forResource(long worldSeed, int x, int y, int count) {
        return forSeed(worldSeed ^ (x * COLUMN_HASH_PRIME ^ y * ROW_HASH_PRIME) * RESOURCE_SEED_MULTIPLIER, count);
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
        long h = seed * FIRST_MULTIPLIER;
        h ^= h >>> FIRST_SHIFT;
        h *= SECOND_MULTIPLIER;
        h ^= h >>> SECOND_SHIFT;
        return (int) Math.floorMod(h, (long) count);
    }
}
