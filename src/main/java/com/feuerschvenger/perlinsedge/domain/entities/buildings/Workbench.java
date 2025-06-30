package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a workbench building where players can craft items.
 * Workbenches are solid structures that block movement but provide crafting functionality.
 */
public class Workbench extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new workbench at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Workbench(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.WORKBENCH, true, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the workbench using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double tableWidth = 60 * zoom;
        double tableHeight = 10 * zoom;
        double legWidth = 8 * zoom;
        double legHeight = 30 * zoom;

        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(screenX - tableWidth / 2, screenY - tableHeight - legHeight, tableWidth, tableHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - tableWidth / 2, screenY - tableHeight - legHeight, tableWidth, tableHeight);

        gc.setFill(Color.BROWN);
        gc.fillRect(screenX - tableWidth / 2 + (legWidth / 2), screenY - legHeight, legWidth, legHeight);
        gc.fillRect(screenX + tableWidth / 2 - (legWidth * 1.5), screenY - legHeight, legWidth, legHeight);
        gc.fillRect(screenX - (legWidth / 2), screenY - legHeight + (legWidth * 0.7), legWidth, legHeight);
        gc.fillRect(screenX + tableWidth / 2 - (legWidth / 2) - (legWidth * 0.7), screenY - legHeight + (legWidth * 0.7), legWidth, legHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - tableWidth / 2 + (legWidth / 2), screenY - legHeight, legWidth, legHeight);
        gc.strokeRect(screenX + tableWidth / 2 - (legWidth * 1.5), screenY - legHeight, legWidth, legHeight);
    }

}
