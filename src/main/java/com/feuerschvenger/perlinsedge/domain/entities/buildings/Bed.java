package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Bed building in the game.
 * Beds are solid structures that can be used for resting or respawning.
 */
public class Bed extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new bed at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Bed(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.BED, true, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the bed using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double bedWidth = 50 * zoom;
        double bedLength = 70 * zoom;
        double headboardHeight = 25 * zoom;
        double headboardWidth = bedWidth * 0.8;

        double drawX = screenX;
        double drawY = screenY + (AppConfig.getInstance().graphics().getTileHalfHeight() * zoom);

        gc.setFill(Color.PERU);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);

        gc.fillRect(drawX - bedWidth / 2, drawY - bedLength, bedWidth, bedLength);
        gc.strokeRect(drawX - bedWidth / 2, drawY - bedLength, bedWidth, bedLength);

        gc.setFill(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.8));
        gc.fillRect(drawX - bedWidth / 2 + (2 * zoom), drawY - bedLength + (2 * zoom), bedWidth - (4 * zoom), bedLength - (4 * zoom));
        gc.strokeRect(drawX - bedWidth / 2 + (2 * zoom), drawY - bedLength + (2 * zoom), bedWidth - (4 * zoom), bedLength - (4 * zoom));


        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(drawX - headboardWidth / 2, drawY - bedLength - headboardHeight, headboardWidth, headboardHeight);
        gc.strokeRect(drawX - headboardWidth / 2, drawY - bedLength - headboardHeight, headboardWidth, headboardHeight);
    }

}
