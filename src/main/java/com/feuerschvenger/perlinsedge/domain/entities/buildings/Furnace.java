package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.crafting.processing.StationProfiles;

/**
 * A furnace that smelts iron ore into ingots with coal or charcoal and burns logs down to charcoal. It is a solid
 * structure that blocks movement.
 */
public class Furnace extends ProcessingStation {

    /**
     * Creates a new furnace at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Furnace(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FURNACE, true, StationProfiles.furnace());
    }
}
