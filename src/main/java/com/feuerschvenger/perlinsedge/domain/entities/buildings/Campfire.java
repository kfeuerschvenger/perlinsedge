package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a Campfire building in the game.
 * Campfires are non-solid structures that can provide light or a cooking point.
 */
public class Campfire extends Building {

    /**
     * Creates a new campfire at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Campfire(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.CAMPFIRE, false, true);
    }
}
