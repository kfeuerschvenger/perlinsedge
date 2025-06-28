package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Bridge building.
 * Bridges allow movement over normally unwalkable tiles (like water).
 */
public class Bridge extends Building {
    public Bridge(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.BRIDGE, false, true); // Not solid, destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double doubleY, double zoom) {
        // Fallback drawing: Wooden planks for a bridge, stretched across the tile.
        double length = 80 * zoom; // Extend slightly beyond tile bounds
        double width = 30 * zoom;  // Width of the bridge planks
        double thickness = 10 * zoom; // Height of the bridge structure

        // Adjust Y for isometric base
        double drawY = doubleY - thickness / 2;

        gc.setFill(Color.SADDLEBROWN);
        gc.setStroke(Color.DARKGOLDENROD);
        gc.setLineWidth(1 * zoom);

        // Draw main bridge rectangle (top-down view simulation)
        gc.fillRect(screenX - length / 2, drawY - width / 2, length, width);
        gc.strokeRect(screenX - length / 2, drawY - width / 2, length, width);

        // Add some planks texture (vertical lines across the length)
        double plankSpacing = 15 * zoom;
        for (double i = 0; i < length; i += plankSpacing) {
            gc.strokeLine(screenX - length / 2 + i, drawY - width / 2, screenX - length / 2 + i, drawY + width / 2);
        }
    }
}
