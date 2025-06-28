package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Door building.
 * Doors can be toggled between solid (closed) and not solid (open).
 */
public class Door extends Building {
    private boolean isOpen;

    public Door(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.DOOR, true, true); // Initially closed (solid), destructible
        this.isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void toggleOpen() {
        this.isOpen = !this.isOpen;
        this.solid = !this.isOpen; // Toggle solidity
        System.out.println("Door at (" + tileX + ", " + tileY + ") is now " + (isOpen ? "OPEN" : "CLOSED"));
    }

    @Override
    public boolean onInteract(com.feuerschvenger.perlinsedge.domain.world.model.Tile tile) {
        toggleOpen();
        return true; // Interaction handled
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double doubleY, double zoom) {
        // Fallback drawing: Rectangle for a door, color changes based on state
        double width = 40 * zoom;
        double height = 60 * zoom; // Taller than walls for visual distinction

        // Adjust Y to simulate isometric depth
        double drawY = doubleY - height / 2 - (10 * zoom); // Move up slightly for perspective

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);

        if (isOpen) {
            gc.setFill(Color.web("#ADD8E6", 0.5)); // Light blue, transparent when open
            gc.fillRect(screenX - width / 2, drawY, width, height);
            // Draw a simple frame when open
            gc.setStroke(Color.BROWN);
            gc.strokeRect(screenX - width / 2, drawY, width, height);
        } else {
            gc.setFill(Color.BROWN); // Brown when closed
            gc.fillRect(screenX - width / 2, drawY, width, height);
            gc.strokeRect(screenX - width / 2, drawY, width, height); // Outline when closed
            // Add a simple handle/knob
            gc.setFill(Color.GOLD);
            gc.fillOval(screenX + width / 2 - (10 * zoom), drawY + height / 2 - (5 * zoom), 5 * zoom, 5 * zoom);
        }
    }
}
