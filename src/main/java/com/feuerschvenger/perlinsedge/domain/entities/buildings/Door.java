package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Door building.
 * Doors can be toggled between solid (closed) and not solid (open).
 */
public class Door extends Building {
    private boolean isOpen;

    // ==================================================================
    //  Constructor
    // ==================================================================

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

    public boolean isOpen() {
        return isOpen;
    }

    public void toggleOpen() {
        this.isOpen = !this.isOpen;
        this.isSolid = !this.isOpen; // Toggle solidity
        System.out.println("Door at (" + tileX + ", " + tileY + ") is now " + (isOpen ? "OPEN" : "CLOSED"));
    }

    @Override
    public boolean onInteract(com.feuerschvenger.perlinsedge.domain.world.model.Tile tile) {
        toggleOpen();
        return true;
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the door using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double width = 40 * zoom;
        double height = 60 * zoom;

        double drawY = screenY - height / 2 - (10 * zoom);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);

        if (isOpen) {
            gc.setFill(Color.web("#ADD8E6", 0.5));
            gc.fillRect(screenX - width / 2, drawY, width, height);
            // Draw a simple frame when open
            gc.setStroke(Color.BROWN);
            gc.strokeRect(screenX - width / 2, drawY, width, height);
        } else {
            gc.setFill(Color.BROWN);
            gc.fillRect(screenX - width / 2, drawY, width, height);
            gc.strokeRect(screenX - width / 2, drawY, width, height);
            gc.setFill(Color.GOLD);
            gc.fillOval(screenX + width / 2 - (10 * zoom), drawY + height / 2 - (5 * zoom), 5 * zoom, 5 * zoom);
        }
    }

}
