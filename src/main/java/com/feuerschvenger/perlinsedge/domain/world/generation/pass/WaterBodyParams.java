package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;

/**
 * How many bodies of water a map keeps. Counts are per area, so a bigger map keeps more bodies.
 *
 * @param keepPer100k  how many bodies are kept for every hundred thousand tiles of map, from 0 to 100000; a map keeps at
 *                     least one body whatever its size when this is above 0, and none when it is 0
 * @param minKeptTiles the fewest tiles a body needs to be kept, at least 1; a smaller body is always filled
 * @param fill         the land that replaces the water of a body that is not kept, which must be solid ground that is
 *                     not ice
 */
public record WaterBodyParams(int keepPer100k, int minKeptTiles, TileType fill) {

    /** The largest density, one body on every tile. A larger one only makes the count overflow. */
    public static final int MAX_KEEP_PER_100K = 100_000;

    /** Checks the density and the size are usable and the fill is land. */
    public WaterBodyParams {
        if (keepPer100k < 0 || keepPer100k > MAX_KEEP_PER_100K) {
            throw new IllegalArgumentException(
                    "the bodies kept per 100k tiles must be from 0 to " + MAX_KEEP_PER_100K + ": " + keepPer100k);
        }
        if (minKeptTiles < 1) {
            throw new IllegalArgumentException("a kept body needs at least 1 tile: " + minKeptTiles);
        }
        Objects.requireNonNull(fill, "fill");
        if (!fill.isLand() || fill == TileType.ICE) {
            throw new IllegalArgumentException("the fill must be solid ground that is not ice: " + fill);
        }
    }
}
