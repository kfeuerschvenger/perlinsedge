package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Fence building.
 * Fences are solid obstacles but often visually permeable.
 */
public class Fence extends Building {
    public Fence(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FENCE, true, true); // Solid, Destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double doubleY, double zoom) {
        // Fallback drawing: Simple vertical posts with a horizontal bar
        double postWidth = 10 * zoom;
        double postHeight = 40 * zoom;
        double barHeight = 10 * zoom;
        double totalWidth = 50 * zoom; // Occupy a portion of the tile

        // Adjust Y for isometric base
        double drawY = doubleY - postHeight;

        gc.setFill(Color.DARKGOLDENROD); // Wood color
        gc.setStroke(Color.BROWN);
        gc.setLineWidth(1 * zoom);

        // Left post
        gc.fillRect(screenX - totalWidth / 2, drawY, postWidth, postHeight);
        gc.strokeRect(screenX - totalWidth / 2, drawY, postWidth, postHeight);

        // Right post
        gc.fillRect(screenX + totalWidth / 2 - postWidth, drawY, postWidth, postHeight);
        gc.strokeRect(screenX + totalWidth / 2 - postWidth, drawY, postWidth, postHeight);

        // Horizontal bar (higher up)
        double barY = drawY + postHeight * 0.3; // Position bar higher
        gc.fillRect(screenX - totalWidth / 2, barY, totalWidth, barHeight);
        gc.strokeRect(screenX - totalWidth / 2, barY, totalWidth, barHeight);

        // Another horizontal bar (lower)
        double lowerBarY = drawY + postHeight * 0.7; // Position lower bar
        gc.fillRect(screenX - totalWidth / 2, lowerBarY, totalWidth, barHeight);
        gc.strokeRect(screenX - totalWidth / 2, lowerBarY, totalWidth, barHeight);
    }
}
