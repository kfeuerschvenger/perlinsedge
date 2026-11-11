package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
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
    private static final Color PLAYER_MARKER_COLOR = Color.rgb(255, 60, 60);
    private static final double PLAYER_MARKER_RADIUS = 2.5;

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

    // Cached tile layer, rebuilt only when the map changes
    private WritableImage tileLayer;
    private boolean tileLayerDirty = true;

    // Player marker, drawn on top of the cached layer every frame
    private boolean playerVisible;
    private int playerTileX;
    private int playerTileY;

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

        if (tileLayerDirty) {
            rebuildTileLayer();
        }
        gc.drawImage(tileLayer, 0, 0);
        drawCameraView();
        drawPlayerMarker();
    }

    /**
     * Sets the map to display. The tile layer is cached as an image and rebuilt only when the map is a different
     * instance or {@link #invalidateTileLayer()} was called, so calling this every frame is cheap.
     *
     * @param newMap The new TileMap to display
     */
    public void updateMap(TileMap newMap) {
        if (newMap == this.map) return;

        this.map = newMap;
        calculateScaling();
        tileLayerDirty = true;
    }

    /** Marks the cached tile layer as outdated, for example when the explored area of the map changes. */
    public void invalidateTileLayer() {
        tileLayerDirty = true;
    }

    /**
     * Sets the tile where the player marker is drawn on top of the cached tile layer.
     *
     * @param tileX player tile X
     * @param tileY player tile Y
     */
    public void setPlayerTile(int tileX, int tileY) {
        this.playerTileX = tileX;
        this.playerTileY = tileY;
        this.playerVisible = true;
    }

    /** Hides the player marker. */
    public void clearPlayerTile() {
        this.playerVisible = false;
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

    /**
     * Paints every sampled tile into an off-screen pixel buffer and keeps it as an image.
     * This is the only place that walks the whole map, and it runs once per map instead of once per frame.
     */
    private void rebuildTileLayer() {
        final int imageWidth = Math.max(1, (int) canvas.getWidth());
        final int imageHeight = Math.max(1, (int) canvas.getHeight());
        final int[] pixels = new int[imageWidth * imageHeight];

        final int width = map.getWidth();
        final int height = map.getHeight();
        final double drawWidth = Math.max(1, scaledTileW * sampleStep);
        final double drawHeight = Math.max(1, scaledTileH * sampleStep);

        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                final int argb = toArgb(map.getTile(x, y).getColor());
                final double posX = (x - y) * scaledTileW + offsetX;
                final double posY = (x + y) * scaledTileH + offsetY;
                fillRect(pixels, imageWidth, imageHeight, posX, posY, drawWidth, drawHeight, argb);
            }
        }

        if (tileLayer == null || tileLayer.getWidth() != imageWidth || tileLayer.getHeight() != imageHeight) {
            tileLayer = new WritableImage(imageWidth, imageHeight);
        }
        tileLayer.getPixelWriter().setPixels(0, 0, imageWidth, imageHeight,
                PixelFormat.getIntArgbInstance(), pixels, 0, imageWidth);
        tileLayerDirty = false;
    }

    private static void fillRect(int[] pixels, int imageWidth, int imageHeight,
                                 double x, double y, double width, double height, int argb) {
        final int left = Math.max(0, (int) Math.floor(x));
        final int top = Math.max(0, (int) Math.floor(y));
        final int right = Math.min(imageWidth, (int) Math.ceil(x + width));
        final int bottom = Math.min(imageHeight, (int) Math.ceil(y + height));

        for (int row = top; row < bottom; row++) {
            final int rowStart = row * imageWidth;
            for (int column = left; column < right; column++) {
                pixels[rowStart + column] = argb;
            }
        }
    }

    /**
     * Packs a colour into the 0xAARRGGBB integer layout of a pixel buffer.
     *
     * @param color colour with components from 0.0 to 1.0
     * @return the packed colour
     */
    static int toArgb(Rgba color) {
        return (channel(color.a()) << 24) | (channel(color.r()) << 16) | (channel(color.g()) << 8) | channel(color.b());
    }

    private static int channel(double component) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, component)) * 255.0);
    }

    private void drawPlayerMarker() {
        if (!playerVisible) return;

        final double[] marker = tileToMiniMap(playerTileX, playerTileY);
        gc.setFill(PLAYER_MARKER_COLOR);
        gc.fillOval(marker[0] - PLAYER_MARKER_RADIUS, marker[1] - PLAYER_MARKER_RADIUS,
                PLAYER_MARKER_RADIUS * 2, PLAYER_MARKER_RADIUS * 2);
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