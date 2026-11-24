package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How the height is shaped into islands. Counts and sizes are in tiles and tiles per area, so a bigger map holds more
 * islands of the same size instead of the same islands stretched.
 *
 * @param islandsPer100kTiles how many islands there are for every hundred thousand tiles of map, from 0 to 100000,
 *                            which is one island on every tile; the island count of a map never exceeds
 *                            {@link #MAX_ISLANDS}
 * @param minIslands          the fewest islands a map has, however small it is, from 1 to {@link #MAX_ISLANDS}
 * @param radiusTiles         the distance from an island centre at which its lift has faded to nothing, at least 1
 * @param heightGain          how much the noise height is multiplied by at the centre of an island, finite
 * @param lift                how much an island centre is raised above the sea, finite
 * @param seaOffset           how far the sea floor lies below the height of zero where no island reaches, finite
 * @param falloffPower        how fast the lift fades with the distance, larger values give steeper shores, finite and
 *                            above 0
 */
public record IslandParams(int islandsPer100kTiles, int minIslands, int radiusTiles, float heightGain, float lift,
                           float seaOffset, float falloffPower) {

    /** The largest density, one island on every tile. A larger one only makes the island count overflow. */
    public static final int MAX_ISLANDS_PER_100K_TILES = 100_000;

    /** The most islands a map has, whatever its size and density, which keeps the centre arrays small. */
    public static final int MAX_ISLANDS = 10_000;

    /** Checks the counts and the radius are usable, the falloff is positive and every float is finite. */
    public IslandParams {
        if (islandsPer100kTiles < 0 || islandsPer100kTiles > MAX_ISLANDS_PER_100K_TILES || minIslands < 1
                || minIslands > MAX_ISLANDS || radiusTiles < 1) {
            throw new IllegalArgumentException("the island counts and radius are out of range: " + islandsPer100kTiles
                    + " per 100k, at least " + minIslands + ", radius " + radiusTiles);
        }
        requireFinite(heightGain, "height gain");
        requireFinite(lift, "lift");
        requireFinite(seaOffset, "sea offset");
        requireFinite(falloffPower, "falloff power");
        if (falloffPower <= 0F) {
            throw new IllegalArgumentException("the falloff power must be positive: " + falloffPower);
        }
    }

    /** Refuses a value that is NaN or infinite. */
    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("the " + name + " must be a finite number: " + value);
        }
    }
}
