package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;

/**
 * Abstract base class for all constructible buildings in the game.
 * Defines common properties and behaviors for buildings.
 */
public abstract class Building {
    protected int tileX;
    protected int tileY;
    protected BuildingType type;
    protected boolean solid;
    protected boolean destructible;

    public Building(int tileX, int tileY, BuildingType type, boolean solid, boolean destructible) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.type = type;
        this.solid = solid;
        this.destructible = destructible;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public BuildingType getType() {
        return type;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isDestructible() {
        return destructible;
    }

    /**
     * Called when the building is interacted with (e.g., clicked).
     * @param tile The tile the building is on.
     * @return True if the interaction was handled, false otherwise.
     */
    public boolean onInteract(Tile tile) {
        System.out.println("Interacted with " + type.getDisplayName() + " at (" + tileX + ", " + tileY + ")");
        return false; // Default: not handled
    }

    /**
     * Abstract method for drawing the building. Concrete classes will implement this
     * using JavaFX shapes for fallback drawing.
     * @param gc The GraphicsContext to draw on.
     * @param screenX The screen X coordinate for the tile's center.
     * @param screenY The screen Y coordinate for the tile's center.
     * @param zoom The current zoom level.
     */
    public abstract void draw(GraphicsContext gc, double screenX, double screenY, double zoom);
}