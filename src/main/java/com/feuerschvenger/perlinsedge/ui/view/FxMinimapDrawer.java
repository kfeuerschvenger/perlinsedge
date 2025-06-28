package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renders a minimap representation of the game world with camera view indication.
 * Handles coordinate transformations between main view and minimap.
 */
public class FxMinimapDrawer {
    // Constants
    private static final double BASE_TILE_WIDTH = 4;
    private static final double BASE_TILE_HEIGHT = 2;
    private static final double MIN_SCALE = 0.05;
    private static final double PADDING = 20;
    private static final double CAMERA_VIEW_OPACITY = 0.3;
    private static final double BACKGROUND_OPACITY = 0.8;
    private static final double LINE_WIDTH = 1.5;

    // Color constants
    private static final Color BACKGROUND_COLOR = Color.rgb(30, 30, 30, BACKGROUND_OPACITY);
    private static final Color CAMERA_FILL_COLOR = Color.rgb(200, 200, 200, CAMERA_VIEW_OPACITY);
    private static final Color CAMERA_STROKE_COLOR = Color.WHITE;

    // Configuration references
    private final AppConfig.GraphicsConfig graphicsConfig = AppConfig.getInstance().graphics();

    // Component references
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final FxCamera camera;
    private final Canvas mainGameCanvas;

    // State
    private TileMap map;
    private double scale = 1.0;
    private double scaledTileW = BASE_TILE_WIDTH;
    private double scaledTileH = BASE_TILE_HEIGHT;
    private double offsetX;
    private double offsetY;
    private int sampleStep = 1;

    public FxMinimapDrawer(Canvas canvas, FxCamera camera, Canvas mainGameCanvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.camera = camera;
        this.mainGameCanvas = mainGameCanvas;
    }

    /**
     * Renders the minimap with current map and camera view.
     */
    public void draw() {
        clearCanvas();
        drawBackground();

        if (map == null) return;

        drawMapTiles();
        drawCameraView();
    }

    /**
     * Updates the map reference and recalculates scaling.
     *
     * @param newMap The new TileMap to display
     */
    public void updateMap(TileMap newMap) {
        this.map = newMap;
        calculateScaling();
    }

    /**
     * Converts minimap coordinates to world tile coordinates.
     *
     * @param miniMapX Minimap X coordinate
     * @param miniMapY Minimap Y coordinate
     * @return Tile coordinates [tileX, tileY]
     */
    public double[] miniMapToTile(double miniMapX, double miniMapY) {
        double isoX = miniMapX - offsetX;
        double isoY = miniMapY - offsetY;

        double compX = isoX / scaledTileW;
        double compY = isoY / scaledTileH;

        return new double[] {
                (compX + compY) / 2.0,
                (compY - compX) / 2.0
        };
    }

    public Canvas getCanvas() {
        return canvas;
    }

    // ==================================================================
    //  PRIVATE HELPER METHODS
    // ==================================================================

    private void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawBackground() {
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void calculateScaling() {
        if (map == null) return;

        final int mapWidth = map.getWidth();
        final int mapHeight = map.getHeight();

        // Calculate required scale
        final double mapPixelWidth = (mapWidth + mapHeight) * BASE_TILE_WIDTH;
        final double mapPixelHeight = (mapWidth + mapHeight) * BASE_TILE_HEIGHT;

        final double maxScaleX = (canvas.getWidth() - PADDING) / mapPixelWidth;
        final double maxScaleY = (canvas.getHeight() - PADDING) / mapPixelHeight;

        scale = Math.max(MIN_SCALE, Math.min(maxScaleX, maxScaleY));
        scaledTileW = BASE_TILE_WIDTH * scale;
        scaledTileH = BASE_TILE_HEIGHT * scale;

        // Calculate offsets for centering
        final double mapCenterXTile = mapWidth / 2.0;
        final double mapCenterYTile = mapHeight / 2.0;

        final double isoMapCenterX = (mapCenterXTile - mapCenterYTile) * scaledTileW;
        final double isoMapCenterY = (mapCenterXTile + mapCenterYTile) * scaledTileH;

        final double canvasCenterX = canvas.getWidth() / 2.0;
        final double canvasCenterY = canvas.getHeight() / 2.0;

        offsetX = canvasCenterX - isoMapCenterX;
        offsetY = canvasCenterY - isoMapCenterY;

        // Determine sampling step based on map size
        final int totalTiles = mapWidth * mapHeight;
        if (totalTiles > 50000) sampleStep = 4;
        else if (totalTiles > 20000) sampleStep = 3;
        else if (totalTiles > 10000) sampleStep = 2;
        else sampleStep = 1;
    }

    private void drawMapTiles() {
        final int width = map.getWidth();
        final int height = map.getHeight();
        final double drawWidth = Math.max(1, scaledTileW * sampleStep);
        final double drawHeight = Math.max(1, scaledTileH * sampleStep);

        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                final double[] pos = calculateTilePosition(x, y);
                gc.setFill(map.getTile(x, y).getColor());
                gc.fillRect(pos[0], pos[1], drawWidth, drawHeight);
            }
        }
    }

    private double[] calculateTilePosition(int x, int y) {
        return new double[] {
                (x - y) * scaledTileW + offsetX,
                (x + y) * scaledTileH + offsetY
        };
    }

    private void drawCameraView() {
        final double[] corners = calculateCameraCorners();
        final double[] polygonX = new double[4];
        final double[] polygonY = new double[4];

        // Convert corners to minimap coordinates
        for (int i = 0; i < 4; i++) {
            final double[] tilePos = screenToTile(corners[i*2], corners[i*2 + 1]);
            final double[] miniPos = tileToMiniMap(tilePos[0], tilePos[1]);
            polygonX[i] = miniPos[0];
            polygonY[i] = miniPos[1];
        }

        // Draw camera view polygon
        gc.setFill(CAMERA_FILL_COLOR);
        gc.fillPolygon(polygonX, polygonY, 4);

        gc.setStroke(CAMERA_STROKE_COLOR);
        gc.setLineWidth(LINE_WIDTH);
        gc.strokePolygon(polygonX, polygonY, 4);
    }

    private double[] calculateCameraCorners() {
        final double width = mainGameCanvas.getWidth();
        final double height = mainGameCanvas.getHeight();

        return new double[] {
                0, 0,          // Top left
                width, 0,       // Top right
                width, height,  // Bottom right
                0, height       // Bottom left
        };
    }

    private double[] screenToTile(double screenX, double screenY) {
        final double zoom = camera.getZoom();
        final double camOffsetX = camera.getX();
        final double camOffsetY = camera.getY();

        final double tileWidthHalf = graphicsConfig.getTileHalfWidth() * zoom;
        final double tileHeightHalf = graphicsConfig.getTileHalfHeight() * zoom;

        final double relX = screenX - camOffsetX;
        final double relY = screenY - camOffsetY;

        final double tileX = (relX / tileWidthHalf + relY / tileHeightHalf) / 2.0;
        final double tileY = (relY / tileHeightHalf - relX / tileWidthHalf) / 2.0;

        return new double[]{tileX, tileY};
    }

    private double[] tileToMiniMap(double tileX, double tileY) {
        return new double[] {
                (tileX - tileY) * scaledTileW + offsetX,
                (tileX + tileY) * scaledTileH + offsetY
        };
    }

}