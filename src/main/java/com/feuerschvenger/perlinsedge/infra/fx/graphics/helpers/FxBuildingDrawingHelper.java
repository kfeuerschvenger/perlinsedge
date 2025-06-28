package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class FxBuildingDrawingHelper {

    /**
     * Draws a generic building using its abstract draw method.
     * @param building The building to draw.
     */
    public static void drawBuilding(GraphicsContext gc, Building building,
                                    double offsetX, double offsetY, double zoom
    ) {
        double[] screenCords = IsometricUtils.tileToScreen(
                building.getTileX(), building.getTileY(),
                offsetX, offsetY,
                zoom,
                AppConfig.getInstance().graphics().getTileHalfWidth(),
                AppConfig.getInstance().graphics().getTileHalfHeight()
        );
        building.draw(gc, screenCords[0], screenCords[1], zoom);
    }

    /**
     * Draws the building placement preview on the hovered tile.
     * This draws a semi-transparent colored outline of the tile.
     *
     * @param tile The tile where the preview is shown.
     * @param color The color of the preview.
     */
    public static void drawBuildingPlacementPreview(GraphicsContext gc, Tile tile, Color color,
                                                    double offsetX, double offsetY, double zoom
    ) {
        double tileHalfWidth = AppConfig.getInstance().graphics().getTileHalfWidth();
        double tileHalfHeight = AppConfig.getInstance().graphics().getTileHalfHeight();

        double screenX = (tile.getX() - tile.getY()) * tileHalfWidth * zoom + offsetX;
        double screenY = (tile.getX() + tile.getY()) * tileHalfHeight * zoom + offsetY;

        double centerY = screenY + tileHalfHeight * zoom;

        gc.save();
        gc.setGlobalAlpha(color.getOpacity());
        gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.3));
        gc.setStroke(color);
        gc.setLineWidth(2);

        gc.beginPath();
        gc.moveTo(screenX, centerY - tileHalfHeight * zoom);
        gc.lineTo(screenX + tileHalfWidth * zoom, centerY);
        gc.lineTo(screenX, centerY + tileHalfHeight * zoom);
        gc.lineTo(screenX - tileHalfWidth * zoom, centerY);
        gc.closePath();

        gc.fill();
        gc.stroke();
        gc.restore();
    }

    /**
     * Draws a black and yellow border around the canvas to indicate building mode.
     * This is drawn in screen coordinates, independent of camera transformations.
     * @param canvasWidth The width of the main canvas.
     * @param canvasHeight The height of the main canvas.
     */
    public static void drawBuildingModeBorder(GraphicsContext gc, double canvasWidth, double canvasHeight) {
        gc.save();
        gc.setTransform(1, 0, 0, 1, 0, 0);

        double borderWidth = 10;
        double stripeWidth = 5;
        double patternSize = stripeWidth * 2;

        LinearGradient stripePattern = new LinearGradient(
                0, 0, patternSize, patternSize, false, CycleMethod.REPEAT,
                new Stop(0, Color.YELLOW),
                new Stop(0.5, Color.YELLOW),
                new Stop(0.5, Color.BLACK),
                new Stop(1, Color.BLACK)
        );

        gc.setFill(stripePattern);
        gc.fillRect(0, 0, canvasWidth, borderWidth);
        gc.fillRect(0, canvasHeight - borderWidth, canvasWidth, borderWidth);
        gc.fillRect(0, borderWidth, borderWidth, canvasHeight - 2 * borderWidth);
        gc.fillRect(canvasWidth - borderWidth, borderWidth, borderWidth, canvasHeight - 2 * borderWidth);

        gc.restore();
    }

}