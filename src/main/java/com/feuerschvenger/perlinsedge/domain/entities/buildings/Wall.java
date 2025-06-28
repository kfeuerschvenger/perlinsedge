package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a Wall building.
 * Walls are solid and block movement.
 */
public class Wall extends Building {
    private static final double WALL_HEIGHT_FACTOR = 4;
    private static final double WALL_ELEVATION_OFFSET_FACTOR = 2;

    public Wall(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.WALL, true, true); // Sólida, Destructible
    }

    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        final AppConfig config = AppConfig.getInstance();
        double halfW = config.graphics().getTileHalfWidth() * zoom;
        double halfH = config.graphics().getTileHalfHeight() * zoom;
        final double tileBaseHeight = config.graphics().getTileVisualBaseHeight() * zoom;

        // Calcular el desplazamiento de elevación para que la pared flote sobre el tile
        // screenY es el punto superior del diamante del tile. Queremos que la pared empiece ligeramente por encima.
        double elevationOffset = halfH * WALL_ELEVATION_OFFSET_FACTOR;

        // Ajustar el punto superior de la pared hacia arriba
        double wallTopX = screenX;
        double wallTopY = screenY - elevationOffset; // Desplaza toda la pared hacia arriba

        // Altura visual de la pared (qué tan altas son las caras laterales)
        double wallVisualHeight = tileBaseHeight * WALL_HEIGHT_FACTOR;

        // --- Calcular los puntos de polígono para el diamante superior de la pared (desplazado hacia arriba) ---
        final double rightX = wallTopX + halfW;
        final double rightY = wallTopY + halfH;
        final double bottomX = wallTopX; // El punto inferior del diamante superior está en el mismo X que el superior
        final double bottomY = wallTopY + 2 * halfH; // Punto inferior del diamante superior
        final double leftX = wallTopX - halfW;
        final double leftY = wallTopY + halfH;

        // --- Calcular los puntos base para los bordes inferiores de la pared ---
        // Estos se proyectan hacia abajo desde los puntos del diamante superior por wallVisualHeight
        final double baseRightY = rightY + wallVisualHeight;
        final double baseBottomY = bottomY + wallVisualHeight;
        final double baseLeftY = leftY + wallVisualHeight;

        // Colores de la pared (ajusta según sea necesario para tu estilo de arte)
        Color wallColor = Color.rgb(100, 100, 100); // Un gris medio para la pared
        Color topFaceColor = wallColor.deriveColor(0, 1, 1.2, 1); // Cara superior más clara
        Color rightFaceColor = wallColor.deriveColor(0, 1, 0.8, 1); // Cara derecha más oscura
        Color leftFaceColor = wallColor.deriveColor(0, 1, 0.9, 1); // Cara izquierda con tono medio

        // Contorno
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5 * zoom); // Grosor del contorno que escala con el zoom

        // 1. Dibujar Cara Izquierda
        gc.setFill(leftFaceColor);
        gc.beginPath();
        gc.moveTo(leftX, leftY);
        gc.lineTo(leftX, baseLeftY);
        gc.lineTo(bottomX, baseBottomY);
        gc.lineTo(bottomX, bottomY);
        gc.closePath();
        gc.fill();
        gc.stroke(); // Trazar el contorno de la cara

        // 2. Dibujar Cara Derecha
        gc.setFill(rightFaceColor);
        gc.beginPath();
        gc.moveTo(rightX, rightY);
        gc.lineTo(rightX, baseRightY);
        gc.lineTo(bottomX, baseBottomY);
        gc.lineTo(bottomX, bottomY);
        gc.closePath();
        gc.fill();
        gc.stroke(); // Trazar el contorno de la cara

        // 3. Dibujar Cara Superior (forma de diamante)
        gc.setFill(topFaceColor);
        gc.beginPath();
        gc.moveTo(wallTopX, wallTopY);
        gc.lineTo(rightX, rightY);
        gc.lineTo(bottomX, bottomY);
        gc.lineTo(leftX, leftY);
        gc.closePath();
        gc.fill();
        gc.stroke(); // Trazar el contorno de la cara superior
    }
}
