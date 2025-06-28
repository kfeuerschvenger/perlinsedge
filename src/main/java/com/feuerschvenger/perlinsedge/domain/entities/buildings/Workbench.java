package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Representa un Workbench (Banco de trabajo) para craftear.
 * Los bancos de trabajo son sólidos.
 */
public class Workbench extends Building {
    public Workbench(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.WORKBENCH, true, true); // Sólido, Destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        // Dibujado básico para un banco de trabajo
        double tableWidth = 60 * zoom;
        double tableHeight = 10 * zoom;
        double legWidth = 8 * zoom;
        double legHeight = 30 * zoom;

        // Mesa
        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(screenX - tableWidth / 2, screenY - tableHeight - legHeight, tableWidth, tableHeight);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - tableWidth / 2, screenY - tableHeight - legHeight, tableWidth, tableHeight);

        // Patas
        gc.setFill(Color.BROWN);
        // Pata izquierda-superior (perspectiva isométrica)
        gc.fillRect(screenX - tableWidth / 2 + (legWidth / 2), screenY - legHeight, legWidth, legHeight);
        // Pata derecha-superior
        gc.fillRect(screenX + tableWidth / 2 - (legWidth * 1.5), screenY - legHeight, legWidth, legHeight);
        // Pata izquierda-inferior
        gc.fillRect(screenX - (legWidth / 2), screenY - legHeight + (legWidth * 0.7), legWidth, legHeight);
        // Pata derecha-inferior
        gc.fillRect(screenX + tableWidth / 2 - (legWidth / 2) - (legWidth * 0.7), screenY - legHeight + (legWidth * 0.7), legWidth, legHeight);


        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5 * zoom);
        gc.strokeRect(screenX - tableWidth / 2 + (legWidth / 2), screenY - legHeight, legWidth, legHeight);
        gc.strokeRect(screenX + tableWidth / 2 - (legWidth * 1.5), screenY - legHeight, legWidth, legHeight);
    }
}
