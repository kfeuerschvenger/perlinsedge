package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.config.AppConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class FxBuildingDrawingHelper {

    /**
     * Draws a building through {@link FxBuildingPainter}.
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
        FxBuildingPainter.draw(gc, building, screenCords[0], screenCords[1], zoom);
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

}
