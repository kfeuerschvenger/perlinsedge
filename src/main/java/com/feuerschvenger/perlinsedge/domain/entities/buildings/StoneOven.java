package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.crafting.processing.StationProfiles;

/**
 * A stone oven that cooks meat, burning a log for every few pieces. It is a solid structure that blocks movement.
 */
public class StoneOven extends ProcessingStation {

    /**
     * Creates a new stone oven at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public StoneOven(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.STONE_OVEN, true, StationProfiles.stoneOven());
    }
}
