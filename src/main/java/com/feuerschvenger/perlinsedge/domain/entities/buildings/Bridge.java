package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Bridge building.
 * Bridges allow movement over normally unwalkable tiles (like water).
 */
public class Bridge extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new bridge at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Bridge(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.BRIDGE, false, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the bridge using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double length = 80 * zoom;
        double width = 30 * zoom;
        double thickness = 10 * zoom;

        double drawY = screenY - thickness / 2;

        gc.setFill(Color.SADDLEBROWN);
        gc.setStroke(Color.DARKGOLDENROD);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(screenX - length / 2, drawY - width / 2, length, width);
        gc.strokeRect(screenX - length / 2, drawY - width / 2, length, width);

        double plankSpacing = 15 * zoom;
        for (double i = 0; i < length; i += plankSpacing) {
            gc.strokeLine(screenX - length / 2 + i, drawY - width / 2, screenX - length / 2 + i, drawY + width / 2);
        }
    }

}
