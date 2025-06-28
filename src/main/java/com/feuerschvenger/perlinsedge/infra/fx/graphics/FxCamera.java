package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Camera;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import javafx.scene.canvas.Canvas;

/**
 * JavaFX camera tailored for isometric tile maps, providing efficient panning, zooming,
 * and tile-centered repositioning with boundary constraints.
 */
public class FxCamera extends Camera {
    private final AppConfig config;

    public FxCamera(double x, double y, double zoom) {
        super(x, y, zoom);
        this.config = AppConfig.getInstance();
    }

    /**
     * Translates the camera by the specified deltas, constraining movement
     * within the map boundaries.
     * @param dx horizontal translation
     * @param dy vertical translation
     * @param canvas reference canvas for viewport dimensions
     */
    public void pan(double dx, double dy, Canvas canvas) {
        double proposedX = x + dx;
        double proposedY = y + dy;

        Bounds bounds = calculateBounds(canvas);
        double clampedX = clamp(proposedX, bounds.minX(), bounds.maxX());
        double clampedY = clamp(proposedY, bounds.minY(), bounds.maxY());

        if (clampedX != x || clampedY != y) {
            x = clampedX;
            y = clampedY;
        }
    }

    /**
     * Zooms the camera while maintaining focus point.
     *
     * @param factor zoom multiplier (1.1 to zoom in, 0.9 to zoom out)
     * @param centerX X coordinate of zoom focus
     * @param centerY Y coordinate of zoom focus
     */
    public void zoom(double factor, double centerX, double centerY) {
        double originalZoom = zoom;
        double targetZoom = clamp(zoom * factor,
                config.core().getMinZoom(),
                config.core().getMaxZoom());

        if (targetZoom == originalZoom) return;

        double scale = targetZoom / originalZoom;
        x = (x - centerX) * scale + centerX;
        y = (y - centerY) * scale + centerY;
        zoom = targetZoom;
    }

    /**
     * Centers camera on specified isometric tile.
     *
     * @param tileX column index of target tile
     * @param tileY row index of target tile
     * @param canvas reference canvas for viewport dimensions
     */
    public void centerOnTile(double tileX, double tileY, Canvas canvas) {
        double viewportCenterX = canvas.getWidth() * 0.5;
        double viewportCenterY = canvas.getHeight() * 0.5;

        double worldX = (tileX - tileY) * config.graphics().getTileHalfWidth() * zoom;
        double worldY = (tileX + tileY) * config.graphics().getTileHalfHeight() * zoom;

        double dx = viewportCenterX - worldX - x;
        double dy = viewportCenterY - worldY - y;
        pan(dx, dy, canvas);
    }

    /**
     * Calculates visible tile bounds in current viewport.
     *
     * @param canvasWidth viewport width
     * @param canvasHeight viewport height
     * @param mapWidth total map width in tiles
     * @param mapHeight total map height in tiles
     * @return array of visible bounds {minTileX, maxTileX, minTileY, maxTileY}
     */
    public int[] getVisibleTileBounds(double canvasWidth, double canvasHeight,
                                      int mapWidth, int mapHeight) {
        // Convert screen corners to tile coordinates
        double[] topLeft = toTileCoords(0, 0);
        double[] topRight = toTileCoords(canvasWidth, 0);
        double[] bottomLeft = toTileCoords(0, canvasHeight);
        double[] bottomRight = toTileCoords(canvasWidth, canvasHeight);

        // Find min/max coordinates
        double minX = min(topLeft[0], topRight[0], bottomLeft[0], bottomRight[0]);
        double maxX = max(topLeft[0], topRight[0], bottomLeft[0], bottomRight[0]);
        double minY = min(topLeft[1], topRight[1], bottomLeft[1], bottomRight[1]);
        double maxY = max(topLeft[1], topRight[1], bottomLeft[1], bottomRight[1]);

        // Add padding and clamp to map boundaries
        final int padding = 3;
        int minTileX = clamp((int) Math.floor(minX) - padding, mapWidth);
        int maxTileX = clamp((int) Math.ceil(maxX) + padding, mapWidth);
        int minTileY = clamp((int) Math.floor(minY) - padding, mapHeight);
        int maxTileY = clamp((int) Math.ceil(maxY) + padding, mapHeight);

        return new int[]{minTileX, maxTileX, minTileY, maxTileY};
    }

    // --- Private Helpers ---

    private double[] toTileCoords(double screenX, double screenY) {
        return IsometricUtils.screenToTileFractional(
                screenX, screenY,
                x, y, zoom,
                config.graphics().getTileHalfWidth(),
                config.graphics().getTileHalfHeight()
        );
    }

    private Bounds calculateBounds(Canvas canvas) {
        CornerPoints corners = computeCornerPoints(zoom);
        double totalWidth = corners.maxX() - corners.minX();
        double totalHeight = corners.maxY() - corners.minY();

        double viewportWidth = canvas.getWidth();
        double viewportHeight = canvas.getHeight();

        double minX, maxX, minY, maxY;

        if (totalWidth < viewportWidth) {
            double centerOffsetX = (viewportWidth - totalWidth) * 0.5;
            minX = centerOffsetX - corners.minX();
            maxX = minX;
        } else {
            minX = viewportWidth - corners.maxX();
            maxX = -corners.minX();
        }

        if (totalHeight < viewportHeight) {
            double centerOffsetY = (viewportHeight - totalHeight) * 0.5;
            minY = centerOffsetY - corners.minY();
            maxY = minY;
        } else {
            minY = viewportHeight - corners.maxY();
            maxY = -corners.minY();
        }

        return new Bounds(minX, maxX, minY, maxY);
    }

    private CornerPoints computeCornerPoints(double zoomLevel) {
        int w = config.world().getWidth() - 1;
        int h = config.world().getHeight() - 1;
        double halfTileW = config.graphics().getTileHalfWidth() * zoomLevel;
        double halfTileH = config.graphics().getTileHalfHeight() * zoomLevel;

        double x1 = 0, y1 = 0;
        double x2 = (w - h) * halfTileW;
        double y2 = (w + h) * halfTileH;
        double x3 = -h * halfTileW;
        double y3 = h * halfTileH;
        double x4 = w * halfTileW;
        double y4 = w * halfTileH;

        double minX = min(x1, x2, x3, x4);
        double maxX = max(x1, x2, x3, x4);
        double minY = min(y1, y2, y3, y4);
        double maxY = max(y1, y2, y3, y4);

        return new CornerPoints(minX, maxX, minY, maxY);
    }

    // --- Utility Functions ---

    private static double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static double max(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (Math.min(value, max));
    }

    private static int clamp(int value, int max) {
        return value < 0 ? 0 : (Math.min(value, max));
    }

    // --- DTO Records ---
    private record Bounds(double minX, double maxX, double minY, double maxY) {}
    private record CornerPoints(double minX, double maxX, double minY, double maxY) {}

}
