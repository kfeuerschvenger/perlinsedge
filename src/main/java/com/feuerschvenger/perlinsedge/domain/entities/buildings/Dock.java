package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Dock building.
 * Docks provide a walkable surface over water, typically extending from land.
 */
public class Dock extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new dock at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Dock(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.DOCK, false, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the dock using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double width = 60 * zoom;
        double height = 40 * zoom;
        double plankThickness = 8 * zoom;

        double drawY = screenY - height / 2;

        gc.setFill(Color.PERU);
        gc.setStroke(Color.BROWN);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(screenX - width / 2, drawY, width, height);
        gc.strokeRect(screenX - width / 2, drawY, width, height);

        for (double i = 0; i < height; i += plankThickness + (2 * zoom)) {
            gc.strokeLine(screenX - width / 2, drawY + i, screenX + width / 2, drawY + i);
        }

        double postSize = 10 * zoom;
        double postHeight = 15 * zoom;
        gc.setFill(Color.DARKGOLDENROD);
        gc.fillRect(screenX - width / 2 + postSize/2, drawY + height - postHeight, postSize, postHeight);
        gc.fillRect(screenX + width / 2 - postSize*1.5, drawY + height - postHeight, postSize, postHeight);
    }

}
