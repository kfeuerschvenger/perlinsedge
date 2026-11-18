package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import lombok.Getter;

/**
 * Represents a fence building. A fence is a solid post with a half rail toward each neighbouring fence or gate it
 * joins, see {@link FenceConnections}. The joined sides are kept on the building, so drawing it is a read.
 */
public class Fence extends Building {
    /** The joined sides as a mask of {@link FenceConnections}, 0 for a lone post. */
    @Getter
    private int connections;

    /**
     * Creates a new fence at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Fence(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FENCE, true, true);
    }

    /**
     * Sets the joined sides, which {@link FenceConnections#refresh} does whenever a neighbour changes.
     *
     * @param connections the mask of the joined sides
     */
    public void setConnections(int connections) {
        this.connections = connections;
    }
}
