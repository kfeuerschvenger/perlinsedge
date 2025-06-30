package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a fence building.
 * Fences are solid obstacles but often visually permeable.
 */
public class Fence extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new fence at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Fence(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FENCE, true, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the fence using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double postWidth = 10 * zoom;
        double postHeight = 40 * zoom;
        double barHeight = 10 * zoom;
        double totalWidth = 50 * zoom;

        double drawY = screenY - postHeight;

        gc.setFill(Color.DARKGOLDENROD);
        gc.setStroke(Color.BROWN);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(screenX - totalWidth / 2, drawY, postWidth, postHeight);
        gc.strokeRect(screenX - totalWidth / 2, drawY, postWidth, postHeight);

        gc.fillRect(screenX + totalWidth / 2 - postWidth, drawY, postWidth, postHeight);
        gc.strokeRect(screenX + totalWidth / 2 - postWidth, drawY, postWidth, postHeight);

        double barY = drawY + postHeight * 0.3;
        gc.fillRect(screenX - totalWidth / 2, barY, totalWidth, barHeight);
        gc.strokeRect(screenX - totalWidth / 2, barY, totalWidth, barHeight);

        double lowerBarY = drawY + postHeight * 0.7;
        gc.fillRect(screenX - totalWidth / 2, lowerBarY, totalWidth, barHeight);
        gc.strokeRect(screenX - totalWidth / 2, lowerBarY, totalWidth, barHeight);
    }

}
