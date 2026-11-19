package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.crafting.processing.StationProfiles;

/**
 * A campfire that cooks meat slowly, one piece at a time. It burns logs with time, lit only while it has fuel, and
 * lies on the ground, so it does not block movement.
 */
public class Campfire extends ProcessingStation {

    /**
     * Creates a new campfire at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Campfire(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.CAMPFIRE, false, StationProfiles.campfire());
    }
}
