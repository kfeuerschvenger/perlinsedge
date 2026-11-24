package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How the height is shaped into islands. Counts and sizes are in tiles and tiles per area, so a bigger map holds more
 * islands of the same size instead of the same islands stretched.
 *
 * @param islandsPer100kTiles how many islands there are for every hundred thousand tiles of map
 * @param minIslands          the fewest islands a map has, however small it is, at least 1
 * @param radiusTiles         the distance from an island centre at which its lift has faded to nothing, at least 1
 * @param heightGain          how much the noise height is multiplied by at the centre of an island
 * @param lift                how much an island centre is raised above the sea
 * @param seaOffset           how far the sea floor lies below the height of zero where no island reaches
 * @param falloffPower        how fast the lift fades with the distance, larger values give steeper shores, above 0
 */
public record IslandParams(int islandsPer100kTiles, int minIslands, int radiusTiles, float heightGain, float lift,
                           float seaOffset, float falloffPower) {

    /** Checks the counts and the radius are usable and the falloff is positive. */
    public IslandParams {
        if (islandsPer100kTiles < 0 || minIslands < 1 || radiusTiles < 1) {
            throw new IllegalArgumentException("the island counts and radius are out of range: " + islandsPer100kTiles
                    + " per 100k, at least " + minIslands + ", radius " + radiusTiles);
        }
        if (!(falloffPower > 0F)) {
            throw new IllegalArgumentException("the falloff power must be positive: " + falloffPower);
        }
    }
}
