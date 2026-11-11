package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a Bridge building.
 * Bridges allow movement over normally unwalkable tiles (like water).
 */
public class Bridge extends Building {

    /**
     * Creates a new bridge at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Bridge(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.BRIDGE, false, true);
    }
}
