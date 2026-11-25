package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How much of the map is under water, whatever the height of the noise turned out to be.
 *
 * @param waterShare the share of the tiles of the map that end up below the level, from 0 up to but not including 1
 * @param level      the height the ground rule of the map type counts as the top of its water, which the lowest
 *                   {@code waterShare} of the tiles are brought below; a finite number
 */
public record SeaLevelParams(float waterShare, float level) {

    /** Checks the share is below 1 and the level is a number. */
    public SeaLevelParams {
        if (Float.isNaN(waterShare) || waterShare < 0F || waterShare >= 1F) {
            throw new IllegalArgumentException("the water share must be from 0 up to but below 1: " + waterShare);
        }
        if (!Float.isFinite(level)) {
            throw new IllegalArgumentException("the sea level must be a finite number: " + level);
        }
    }
}
