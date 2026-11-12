package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for all constructible structures in the game world.
 * Defines core properties and behaviors common to all buildings, including position,
 * type, physical properties, and interaction mechanics. Drawing lives in the rendering layer.
 */
@Slf4j
@Getter
public abstract class Building {
    /** X-coordinate in tile units. */
    protected final int tileX;
    /** Y-coordinate in tile units. */
    protected final int tileY;
    /** Classification of the building. */
    protected final BuildingType type;
    /** Whether the building blocks movement. Not final to allow toggling (e.g., for doors). */
    protected boolean isSolid;
    /** Whether the building can be destroyed. */
    protected final boolean isDestructible;

    /**
     * Creates a new building instance at the specified tile position.
     *
     * @param tileX          X-coordinate in tile units
     * @param tileY          Y-coordinate in tile units
     * @param type           Classification of the building
     * @param isSolid        Whether the building blocks movement
     * @param isDestructible Whether the building can be destroyed
     */
    protected Building(int tileX, int tileY, BuildingType type, boolean isSolid, boolean isDestructible) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.type = type;
        this.isSolid = isSolid;
        this.isDestructible = isDestructible;
    }

    /**
     * Tells whether clicking the building does something, such as opening a door or a chest.
     * The player walks up to an interactable building before the interaction happens.
     *
     * @return true when {@link #onInteract(Tile)} handles the interaction
     */
    public boolean isInteractable() {
        return false;
    }

    /**
     * Handles player interaction with this building.
     *
     * @param tile The tile where the building is located
     * @return True if the interaction was handled, false to propagate
     */
    public boolean onInteract(Tile tile) {
        log.debug("Interacted with {} at ({}, {})", type.getDisplayName(), tileX, tileY);
        return false;
    }
}
