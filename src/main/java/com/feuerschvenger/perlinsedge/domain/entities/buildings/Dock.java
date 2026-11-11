package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a Dock building.
 * Docks provide a walkable surface over water, typically extending from land.
 */
public class Dock extends Building {

    /**
     * Creates a new dock at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Dock(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.DOCK, false, true);
    }
}
