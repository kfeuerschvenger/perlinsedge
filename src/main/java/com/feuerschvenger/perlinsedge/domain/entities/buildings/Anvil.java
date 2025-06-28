package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Representa un Anvil (Yunque) de herrería.
 * Los yunques son sólidos y se utilizan para craftear.
 */
public class Anvil extends Building {
    public Anvil(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.ANVIL, true, true); // Sólido, Destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        // Dibujado básico para un yunque
        double baseWidth = 40 * zoom;
        double baseHeight = 15 * zoom;
        double bodyWidth = 25 * zoom;
        double bodyHeight = 20 * zoom;
        double topWidth = 35 * zoom;
        double topHeight = 10 * zoom;

        // Base
        gc.setFill(Color.DARKGRAY.darker());
        gc.fillRect(screenX - baseWidth / 2, screenY - baseHeight, baseWidth, baseHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - baseWidth / 2, screenY - baseHeight, baseWidth, baseHeight);

        // Cuerpo principal
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(screenX - bodyWidth / 2, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - bodyWidth / 2, screenY - baseHeight - bodyHeight, bodyWidth, bodyHeight);

        // Parte superior (superficie de golpeo)
        gc.setFill(Color.GRAY);
        gc.fillRoundRect(screenX - topWidth / 2, screenY - baseHeight - bodyHeight - topHeight, topWidth, topHeight, 5 * zoom, 5 * zoom);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRoundRect(screenX - topWidth / 2, screenY - baseHeight - bodyHeight - topHeight, topWidth, topHeight, 5 * zoom, 5 * zoom);
    }
}
