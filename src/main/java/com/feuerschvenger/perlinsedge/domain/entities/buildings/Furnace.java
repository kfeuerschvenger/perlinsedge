package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a furnace building where players can smelt ores.
 * Furnaces are solid structures that block movement.
 */
public class Furnace extends Building {

    /**
     * Creates a new furnace at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Furnace(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FURNACE, true, true);
    }
}
