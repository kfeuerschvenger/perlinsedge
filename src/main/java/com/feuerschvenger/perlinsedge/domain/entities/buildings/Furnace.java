package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a furnace building where players can smelt ores.
 * Workbenches are solid structures that block movement but provide smelting functionality.
 */
public class Furnace extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new furnace at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Furnace(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.FURNACE, true, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the furnace using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double size = 50 * zoom;
        double openingWidth = 20 * zoom;
        double openingHeight = 15 * zoom;

        double drawY = screenY - size / 2;

        gc.setFill(Color.DARKGRAY);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);
        gc.fillRect(screenX - size / 2, drawY, size, size);
        gc.strokeRect(screenX - size / 2, drawY, size, size);

        gc.setFill(Color.ORANGERED);
        gc.fillRect(screenX - openingWidth / 2, drawY + (size * 0.6), openingWidth, openingHeight);

        gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.7));
        gc.fillOval(screenX - openingWidth / 4, drawY + (size * 0.6) + (openingHeight * 0.2), openingWidth / 2, openingHeight / 2);
    }

}
