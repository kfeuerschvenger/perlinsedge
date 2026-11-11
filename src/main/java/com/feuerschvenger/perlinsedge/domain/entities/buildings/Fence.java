package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a fence building.
 * Fences are solid obstacles but often visually permeable.
 */
public class Fence extends Building {

    /**
     * Creates a new fence at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Fence(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FENCE, true, true);
    }
}
