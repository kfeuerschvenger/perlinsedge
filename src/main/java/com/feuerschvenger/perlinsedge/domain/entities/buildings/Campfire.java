package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Representa un Campfire (Fogata).
 * Las fogatas son no sólidas y pueden proporcionar luz o un punto de cocción.
 */
public class Campfire extends Building {
    public Campfire(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.CAMPFIRE, false, true); // No sólido, Destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        // Dibujado básico para una fogata
        double baseWidth = 30 * zoom;
        double baseHeight = 10 * zoom;
        double fireHeight = 25 * zoom;
        double woodHeight = 10 * zoom;

        // Base de la fogata (troncos)
        gc.setFill(Color.BROWN.darker().darker());
        gc.fillOval(screenX - baseWidth / 2, screenY - baseHeight / 2 + woodHeight, baseWidth, baseHeight);

        // Llamas
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
