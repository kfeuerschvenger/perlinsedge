package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a Bed building in the game.
 * Beds are solid structures that can be used for resting or respawning.
 */
public class Bed extends Building {

    /**
     * Creates a new bed at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Bed(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.BED, true, true);
    }
}
