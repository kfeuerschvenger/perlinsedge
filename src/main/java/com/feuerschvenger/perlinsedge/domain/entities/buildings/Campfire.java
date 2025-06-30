package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Campfire building in the game.
 * Campfires are non-solid structures that can provide light or a cooking point.
 */
public class Campfire extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new campfire at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Campfire(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.CAMPFIRE, false, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the campfire using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double baseWidth = 30 * zoom;
        double baseHeight = 10 * zoom;
        double fireHeight = 25 * zoom;
        double woodHeight = 10 * zoom;

        gc.setFill(Color.BROWN.darker().darker());
        gc.fillOval(screenX - baseWidth / 2, screenY - baseHeight / 2 + woodHeight, baseWidth, baseHeight);

        gc.setFill(Color.ORANGE);
        gc.fillPolygon(new double[]{
                screenX, screenX + baseWidth / 4, screenX + baseWidth / 8, screenX - baseWidth / 8, screenX - baseWidth / 4
        }, new double[]{
                screenY - fireHeight, screenY - baseHeight / 2, screenY - baseHeight / 4, screenY - baseHeight / 4, screenY - baseHeight / 2
        }, 5);
        gc.setFill(Color.YELLOW);
        gc.fillPolygon(new double[]{
                screenX, screenX + baseWidth / 8, screenX - baseWidth / 8
        }, new double[]{
                screenY - fireHeight * 0.7, screenY - baseHeight / 2, screenY - baseHeight / 2
        }, 3);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeOval(screenX - baseWidth / 2, screenY - baseHeight / 2 + woodHeight, baseWidth, baseHeight);
    }

}
