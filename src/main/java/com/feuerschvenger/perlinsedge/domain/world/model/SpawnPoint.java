package com.feuerschvenger.perlinsedge.domain.world.model;

/**
 * The tile where the player starts on a generated map.
 *
 * @param x the column of the tile
 * @param y the row of the tile
 */
public record SpawnPoint(int x, int y) {

    /** Checks the coordinates are not negative. */
    public SpawnPoint {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("a spawn point cannot be negative: " + x + "," + y);
        }
    }
}
