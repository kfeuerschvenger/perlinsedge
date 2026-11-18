package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Camera;
import com.feuerschvenger.perlinsedge.domain.utils.Doubles;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import javafx.scene.canvas.Canvas;

/**
 * JavaFX camera tailored for isometric tile maps, providing efficient panning, zooming,
 * and tile-centered repositioning with boundary constraints.
 */
public class FxCamera extends Camera {

    /** Tiles kept around the visible area so a tile just outside the edge is still drawn. */
    private static final int VISIBLE_TILE_PADDING = 3;
    /** Fraction of the viewport, or of the space left over, that puts something in the middle. */
    private static final double CENTER_FACTOR = 0.5;

    private final AppConfig config;

    /**
     * Creates a camera. The zoom is snapped to the step grid by {@link Camera#setZoom(double)}, so a camera never
     * starts at a zoom the art cannot be drawn at.
     *
     * @param x    initial horizontal offset, rounded to a whole pixel
     * @param y    initial vertical offset, rounded to a whole pixel
     * @param zoom initial zoom
     */
    public FxCamera(double x, double y, double zoom) {
        super(Math.rint(x), Math.rint(y), zoom);
        this.config = AppConfig.getInstance();
        ZoomSteps.requireValidRange(config.core().getMinZoom(), config.core().getMaxZoom(),
                config.core().getZoomStep());
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

        var bounds = calculateBounds(canvas.getWidth(), canvas.getHeight());
        double clampedX = clamp(proposedX, bounds.minX(), bounds.maxX());
        double clampedY = clamp(proposedY, bounds.minY(), bounds.maxY());

        clampedX = Math.rint(clampedX);
        clampedY = Math.rint(clampedY);
        if (Doubles.differs(clampedX, x) || Doubles.differs(clampedY, y)) {
            x = clampedX;
            y = clampedY;
        }
    }

    /**
     * Moves the zoom one whole step while keeping the focus point still.
     * The zoom only takes the levels of {@link ZoomSteps}, so the art stays at an integer scale, and the camera offset
     * is rounded to whole pixels so every sprite lands on the pixel grid and then kept inside the map bounds of the new zoom.
     *
     * @param direction positive to zoom in, negative to zoom out
     * @param centerX   X coordinate of the zoom focus
     * @param centerY   Y coordinate of the zoom focus
     * @param canvas    reference canvas for viewport dimensions
     */
    public void zoomStep(int direction, double centerX, double centerY, Canvas canvas) {
        zoomStep(direction, centerX, centerY, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Moves the zoom one whole step like {@link #zoomStep(int, double, double, Canvas)}, for a viewport given by its size.
     *
     * @param direction      positive to zoom in, negative to zoom out
     * @param centerX        X coordinate of the zoom focus
     * @param centerY        Y coordinate of the zoom focus
     * @param viewportWidth  viewport width
     * @param viewportHeight viewport height
     */
    public void zoomStep(int direction, double centerX, double centerY, double viewportWidth, double viewportHeight) {
        double originalZoom = zoom;
        double targetZoom = ZoomSteps.move(zoom, direction,
                config.core().getMinZoom(), config.core().getMaxZoom(), config.core().getZoomStep());

        if (Doubles.areEqual(targetZoom, originalZoom)) {
            return;
        }

        double scale = targetZoom / originalZoom;
        zoom = targetZoom;
        final var bounds = calculateBounds(viewportWidth, viewportHeight);
        x = Math.rint(clamp((x - centerX) * scale + centerX, bounds.minX(), bounds.maxX()));
        y = Math.rint(clamp((y - centerY) * scale + centerY, bounds.minY(), bounds.maxY()));
    }

    /**
     * Centers camera on specified isometric tile.
     *
     * @param tileX column index of target tile
     * @param tileY row index of target tile
     * @param canvas reference canvas for viewport dimensions
     */
    public void centerOnTile(double tileX, double tileY, Canvas canvas) {
        double viewportCenterX = canvas.getWidth() * CENTER_FACTOR;
        double viewportCenterY = canvas.getHeight() * CENTER_FACTOR;

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
        var minTileX = clamp((int) Math.floor(minX) - VISIBLE_TILE_PADDING, mapWidth);
        var maxTileX = clamp((int) Math.ceil(maxX) + VISIBLE_TILE_PADDING, mapWidth);
        var minTileY = clamp((int) Math.floor(minY) - VISIBLE_TILE_PADDING, mapHeight);
        var maxTileY = clamp((int) Math.ceil(maxY) + VISIBLE_TILE_PADDING, mapHeight);

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

    /** Works out the range the camera offset may take at the current zoom for a viewport of the given size. */
    private Bounds calculateBounds(double viewportWidth, double viewportHeight) {
        var corners = computeCornerPoints(zoom);
        double totalWidth = corners.maxX() - corners.minX();
        double totalHeight = corners.maxY() - corners.minY();

        double minX;
        double maxX;
        double minY;
        double maxY;

        if (totalWidth < viewportWidth) {
            double centerOffsetX = (viewportWidth - totalWidth) * CENTER_FACTOR;
            minX = centerOffsetX - corners.minX();
            maxX = minX;
        } else {
            minX = viewportWidth - corners.maxX();
            maxX = -corners.minX();
        }

        if (totalHeight < viewportHeight) {
            double centerOffsetY = (viewportHeight - totalHeight) * CENTER_FACTOR;
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

        double x1 = 0;
        double y1 = 0;
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
        return Math.clamp(value, min, max);
    }

    private static int clamp(int value, int max) {
        return value < 0 ? 0 : (Math.min(value, max));
    }

    // --- DTO Records ---
    private record Bounds(double minX, double maxX, double minY, double maxY) {}
    private record CornerPoints(double minX, double maxX, double minY, double maxY) {}

}
