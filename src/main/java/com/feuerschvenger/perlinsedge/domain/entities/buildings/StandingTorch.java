package com.feuerschvenger.perlinsedge.domain.entities.buildings;

/**
 * Represents a standing torch: a decorative light source on a post.
 * Like the campfire it does not block movement and can be removed.
 */
public class StandingTorch extends Building {

    /**
     * Creates a new standing torch at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public StandingTorch(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.STANDING_TORCH, false, true);
    }
}
