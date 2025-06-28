package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Furnace building.
 * Furnaces are solid and can be interacted with (e.g., for smelting).
 */
public class Furnace extends Building {
    public Furnace(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FURNACE, true, true); // Solid, Destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double doubleY, double zoom) {
        // Fallback drawing: Gray block with a red-orange opening
        double size = 50 * zoom;
        double openingWidth = 20 * zoom;
        double openingHeight = 15 * zoom;

        // Adjust Y for isometric perspective
        double drawY = doubleY - size / 2;

        // Furnace body
        gc.setFill(Color.DARKGRAY);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);
        gc.fillRect(screenX - size / 2, drawY, size, size);
        gc.strokeRect(screenX - size / 2, drawY, size, size);

        // Furnace opening (fire effect)
        gc.setFill(Color.ORANGERED);
        gc.fillRect(screenX - openingWidth / 2, drawY + (size * 0.6), openingWidth, openingHeight);

        gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.7)); // Little glow
        gc.fillOval(screenX - openingWidth / 4, drawY + (size * 0.6) + (openingHeight * 0.2), openingWidth / 2, openingHeight / 2);
    }
}
