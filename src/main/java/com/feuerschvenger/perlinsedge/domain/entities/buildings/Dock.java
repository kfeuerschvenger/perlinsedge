package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Dock building.
 * Docks provide a walkable surface over water, typically extending from land.
 */
public class Dock extends Building {
    public Dock(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.DOCK, false, true); // Not solid, destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double doubleY, double zoom) {
        // Fallback drawing: Similar to bridge but maybe wider/more "platform-like"
        double width = 60 * zoom;
        double height = 40 * zoom; // Depth for isometric dock
        double plankThickness = 8 * zoom;

        // Adjust Y for isometric base
        double drawY = doubleY - height / 2;

        gc.setFill(Color.PERU); // Lighter brown for dock
        gc.setStroke(Color.BROWN);
        gc.setLineWidth(1 * zoom);

        // Draw the main dock platform (top face)
        gc.fillRect(screenX - width / 2, drawY, width, height);
        gc.strokeRect(screenX - width / 2, drawY, width, height);

        // Add horizontal planks for texture
        for (double i = 0; i < height; i += plankThickness + (2 * zoom)) {
            gc.strokeLine(screenX - width / 2, drawY + i, screenX + width / 2, drawY + i);
        }

        // Draw simple posts to support the dock (simplified isometric)
        double postSize = 10 * zoom;
        double postHeight = 15 * zoom; // Visual height below the dock
        gc.setFill(Color.DARKGOLDENROD);
        gc.fillRect(screenX - width / 2 + postSize/2, drawY + height - postHeight, postSize, postHeight);
        gc.fillRect(screenX + width / 2 - postSize*1.5, drawY + height - postHeight, postSize, postHeight);
    }
}
