package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A gate in a fence. It runs along one axis of its tile, set by its rotation, and joins the fences at both ends of that
 * axis. Closed, it blocks the player and the enemies like a fence; open, the tile can be walked through. Only the player
 * opens and closes it, by clicking it without the hammer in hand: the enemies have no way to, so for them a closed gate
 * is a wall.
 */
@Slf4j
public class FenceGate extends Building {
    /** True while the gate is open and does not block movement. */
    @Getter
    private boolean isOpen;

    /**
     * Creates a closed gate at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public FenceGate(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FENCE_GATE, true, true);
    }

    /**
     * Tells whether the gate joins a fence on a side of its tile.
     *
     * @param dx tile offset along X of the neighbour, -1, 0 or 1
     * @param dy tile offset along Y of the neighbour, -1, 0 or 1
     * @return true when the neighbour lies on the axis the gate runs along: the X axis for rotation 0, the Y axis for 1
     */
    public boolean runsToward(int dx, int dy) {
        return rotation == 0 ? dx != 0 && dy == 0 : dy != 0 && dx == 0;
    }

    /** Flips the gate between open and closed and updates its solidity. */
    public void toggleOpen() {
        this.isOpen = !this.isOpen;
        this.isSolid = !this.isOpen;
        log.debug("Gate at ({}, {}) is now {}", tileX, tileY, isOpen ? "OPEN" : "CLOSED");
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean onInteract(Tile tile) {
        toggleOpen();
        // The shadow of an open gate is not the one of a closed gate
        tile.buildingChanged();
        return true;
    }
}
