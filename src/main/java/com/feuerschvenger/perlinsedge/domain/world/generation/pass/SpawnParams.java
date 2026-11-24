package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * Where the player starts.
 *
 * @param freeRadius how far around the spawn, in tiles, the ground has to be walkable and free of resources, from 0 to
 *                   {@link #MAX_FREE_RADIUS}
 */
public record SpawnParams(int freeRadius) {

    /** The largest free radius, which keeps the square that is checked around every candidate small. */
    public static final int MAX_FREE_RADIUS = 16;

    /** Checks the radius is in range. */
    public SpawnParams {
        if (freeRadius < 0 || freeRadius > MAX_FREE_RADIUS) {
            throw new IllegalArgumentException("the free radius must be from 0 to " + MAX_FREE_RADIUS + ": "
                    + freeRadius);
        }
    }
}
