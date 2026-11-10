package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a wall structure in the game world.
 * Walls are solid structures that block movement and provide visual barriers.
 */
public class Wall extends Building {

    /**
     * Creates a new wall structure at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Wall(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.WALL, true, true);
    }
}
