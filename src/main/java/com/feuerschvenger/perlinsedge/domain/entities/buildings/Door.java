package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a Door building.
 * Doors can be toggled between solid (closed) and not solid (open).
 */
@Slf4j
public class Door extends Building {
    /** True while the door is open and does not block movement. */
    @Getter
    private boolean isOpen;

    /**
     * Creates a new door at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Door(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.DOOR, true, true);
        this.isOpen = false;
    }

    /** Flips the door between open and closed and updates its solidity. */
    public void toggleOpen() {
        this.isOpen = !this.isOpen;
        this.isSolid = !this.isOpen;
        log.debug("Door at ({}, {}) is now {}", tileX, tileY, isOpen ? "OPEN" : "CLOSED");
    }

    @Override
    public boolean onInteract(Tile tile) {
        toggleOpen();
        return true;
    }
}
