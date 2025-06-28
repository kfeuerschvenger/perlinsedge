package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Representa una Bed (Cama).
 * Las camas son sólidas y proporcionan un punto de reaparición o de guardado.
 */
public class Bed extends Building {
    public Bed(int tileX, int tileY) {
        // Las camas son generalmente sólidas y destructibles
        super(tileX, tileY, BuildingType.BED, true, true);
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        // Dibujado básico para una cama
        double bedWidth = 50 * zoom;
        double bedLength = 70 * zoom; // Length along isometric axis
        double bedHeight = 15 * zoom;
        double headboardHeight = 25 * zoom;
        double headboardWidth = bedWidth * 0.8;

        // Ajustar screenY para dibujar la base de la cama desde el punto inferior-central del tile
        // En un tile isométrico, el punto (screenX, screenY) suele ser la esquina superior del diamante.
        // Queremos la base de la cama en el "suelo" del tile.
        // Para dibujar centrado horizontalmente y al nivel del suelo:
        double drawX = screenX;
        double drawY = screenY + (AppConfig.getInstance().graphics().getTileHalfHeight() * zoom);

        // Marco de la cama
        gc.setFill(Color.PERU); // Color de madera
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1 * zoom);

        // Dibuja la base de la cama (rectángulo "isométrico" en 2D)
        // Para simplificar, lo dibujaremos como un rectángulo plano en el suelo, asumiendo una vista ligeramente superior
        gc.fillRect(drawX - bedWidth / 2, drawY - bedLength, bedWidth, bedLength);
        gc.strokeRect(drawX - bedWidth / 2, drawY - bedLength, bedWidth, bedLength);

        // Colchón
        gc.setFill(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.8));
        gc.fillRect(drawX - bedWidth / 2 + (2 * zoom), drawY - bedLength + (2 * zoom), bedWidth - (4 * zoom), bedLength - (4 * zoom));
        gc.strokeRect(drawX - bedWidth / 2 + (2 * zoom), drawY - bedLength + (2 * zoom), bedWidth - (4 * zoom), bedLength - (4 * zoom));


        // Cabecero (en la parte "superior" o "lejos" de la cama en vista isométrica)
        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(drawX - headboardWidth / 2, drawY - bedLength - headboardHeight, headboardWidth, headboardHeight);
        gc.strokeRect(drawX - headboardWidth / 2, drawY - bedLength - headboardHeight, headboardWidth, headboardHeight);
    }
}
