package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a workbench building where players can craft items.
 * Workbenches are solid structures that block movement.
 */
public class Workbench extends Building {

    /**
     * Creates a new workbench at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Workbench(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.WORKBENCH, true, true);
    }
}
