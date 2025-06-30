package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;

/**
 * Abstract base class for all constructible structures in the game world.
 * Defines core properties and behaviors common to all buildings, including position,
 * type, physical properties, and interaction mechanics.
 */
public abstract class Building {
    // ==================================================================
    //  Instance Properties
    // ==================================================================
    protected final int tileX;
    protected final int tileY;
    protected final BuildingType type;
    protected boolean isSolid; // Whether the building blocks movement. Not final to allow toggling (e.g., for doors)
    protected final boolean isDestructible;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new building instance at the specified tile position.
     *
     * @param tileX         X-coordinate in tile units
     * @param tileY         Y-coordinate in tile units
     * @param type          Classification of the building
     * @param isSolid       Whether the building blocks movement
     * @param isDestructible Whether the building can be destroyed
     */
    public Building(int tileX, int tileY, BuildingType type, boolean isSolid, boolean isDestructible) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.type = type;
        this.isSolid = isSolid;
        this.isDestructible = isDestructible;
    }

    // ==================================================================
    //  Accessor Methods
    // ==================================================================

    /** @return X-coordinate in tile units */
    public int getTileX() {
        return tileX;
    }

    /** @return Y-coordinate in tile units */
    public int getTileY() {
        return tileY;
    }

    /** @return Classification of the building */
    public BuildingType getType() {
        return type;
    }

    /** @return True if this building blocks entity movement */
    public boolean isSolid() {
        return isSolid;
    }

    /** @return True if this building can be destroyed */
    public boolean isDestructible() {
        return isDestructible;
    }

    // ==================================================================
    //  Interaction Methods
    // ==================================================================

    /**
     * Handles player interaction with this building.
     *
     * @param tile The tile where the building is located
     * @return True if the interaction was handled, false to propagate
     */
    public boolean onInteract(Tile tile) {
        System.out.printf("Interacted with %s at (%d, %d)%n",
                type.getDisplayName(), tileX, tileY);
        return false;
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the building using isometric projection principles.
     * Must be implemented by concrete building types.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    public abstract void draw(GraphicsContext gc, double screenX, double screenY, double zoom);
}