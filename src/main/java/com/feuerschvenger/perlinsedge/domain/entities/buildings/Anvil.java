package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a blacksmith's anvil in the game.
 * It is a solid, destructible building used for crafting items.
 */
public class Anvil extends Building {

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new bed at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Anvil(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.ANVIL, true, true); // Sólido, Destructible
    }

    /**
     * Renders the anvil using isometric projection principles.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        double baseWidth = 40 * zoom;
        double baseHeight = 15 * zoom;
        double bodyWidth = 25 * zoom;
        double bodyHeight = 20 * zoom;
        double topWidth = 35 * zoom;
        double topHeight = 10 * zoom;

        gc.setFill(Color.DARKGRAY.darker());
        gc.fillRect(screenX - baseWidth / 2, screenY - baseHeight, baseWidth, baseHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - baseWidth / 2, screenY - baseHeight, baseWidth, baseHeight);

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(screenX - bodyWidth / 2, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - bodyWidth / 2, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);

        gc.setFill(Color.GRAY);
        gc.fillRoundRect(screenX - topWidth / 2, screenY - baseHeight - bodyHeight - topHeight, topWidth, topHeight, 5 * zoom, 5 * zoom);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRoundRect(screenX - topWidth / 2, screenY - baseHeight - bodyHeight - topHeight, topWidth, topHeight, 5 * zoom, 5 * zoom);
    }
}
