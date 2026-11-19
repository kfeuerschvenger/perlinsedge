package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * A straw sleeping bag, the basic bed. It lies flat on the ground, so it does not block movement.
 */
public class SleepingBag extends Building {

    /**
     * Creates a new sleeping bag at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public SleepingBag(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.SLEEPING_BAG, false, true);
    }
}
