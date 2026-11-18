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
    private static final double LINE_WIDTH = 1.5;
    /** Samples taken inside one pixel of the minimap, on a 2 by 2 grid. */
    private static final int SAMPLES_PER_PIXEL = 4;
    /** Where the samples of a pixel lie along one axis, as a fraction of the pixel. */
    private static final double[] SAMPLE_OFFSETS = {0.25, 0.75};

    // Color constants
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
    private double scaledTileW = BASE_TILE_WIDTH;
    private double scaledTileH = BASE_TILE_HEIGHT;
    private double offsetX;
    private double offsetY;

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
     * instance, so calling this every frame is cheap.
     *
     * @param newMap The new TileMap to display
     */
    public void updateMap(TileMap newMap) {
        if (newMap == this.map) return;

        this.map = newMap;
        calculateScaling();
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

    private void calculateScaling() {
        if (map == null) return;

        final int mapWidth = map.getWidth();
        final int mapHeight = map.getHeight();

        // Calculate required scale
        final double mapPixelWidth = (mapWidth + mapHeight) * BASE_TILE_WIDTH;
        final double mapPixelHeight = (mapWidth + mapHeight) * BASE_TILE_HEIGHT;

        final double maxScaleX = (canvas.getWidth() - PADDING) / mapPixelWidth;
        final double maxScaleY = (canvas.getHeight() - PADDING) / mapPixelHeight;

        // Not a clamp: on a map too large for the canvas the fitting scale is below the minimum, and a clamp whose
        // upper bound is below its lower bound throws
        final double fittingScale = Math.min(maxScaleX, maxScaleY);
        final double scale = Math.max(MIN_SCALE, fittingScale);
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

    }

    /**
     * Paints the map into an off-screen pixel buffer and keeps it as an image.
     * This is the only place that walks the map, and it runs once per map instead of once per frame.
     */
    private void rebuildTileLayer() {
        final int imageWidth = Math.max(1, (int) canvas.getWidth());
        final int imageHeight = Math.max(1, (int) canvas.getHeight());
        final int[] pixels = samplePixels(map, imageWidth, imageHeight, scaledTileW, scaledTileH, offsetX, offsetY);

        if (tileLayer == null || tileLayer.getWidth() != imageWidth || tileLayer.getHeight() != imageHeight) {
            tileLayer = new WritableImage(imageWidth, imageHeight);
        }
        tileLayer.getPixelWriter().setPixels(0, 0, imageWidth, imageHeight,
                PixelFormat.getIntArgbInstance(), pixels, 0, imageWidth);
        tileLayerDirty = false;
    }

    /**
     * Colours every pixel of the minimap by asking which tile lies under it, instead of stamping each tile onto the
     * pixels. Stamping an isometric map as rectangles of one tile's width and height paints only a checkerboard of the
     * rectangle grid, because tiles that neighbour each other on the map sit diagonally on that grid, so the cells in
     * between stay unpainted and the background shows through as dots. Sampling per pixel covers every pixel of
     * the map exactly once, whatever the scale. Each pixel averages a 2 by 2 grid of samples, which smooths the
     * downscale and gives the edge of the map a partial alpha instead of a staircase.
     *
     * @param map        the map to paint
     * @param imageWidth width of the buffer in pixels
     * @param imageHeight height of the buffer in pixels
     * @param tileW      half the width of a tile on the minimap
     * @param tileH      half the height of a tile on the minimap
     * @param offsetX    x of the top corner of tile (0, 0)
     * @param offsetY    y of the top corner of tile (0, 0)
     * @return the pixels in 0xAARRGGBB layout, transparent where the map is not
     */
    static int[] samplePixels(TileMap map, int imageWidth, int imageHeight, double tileW, double tileH,
                              double offsetX, double offsetY) {
        final int[] pixels = new int[imageWidth * imageHeight];
        final int width = map.getWidth();
        final int height = map.getHeight();

        final Rgba[] samples = new Rgba[SAMPLES_PER_PIXEL];
        for (int py = 0; py < imageHeight; py++) {
            for (int px = 0; px < imageWidth; px++) {
                for (int sample = 0; sample < SAMPLES_PER_PIXEL; sample++) {
                    final double isoX = px + SAMPLE_OFFSETS[sample % 2] - offsetX;
                    final double isoY = py + SAMPLE_OFFSETS[sample / 2] - offsetY;
                    final double compX = isoX / tileW;
                    final double compY = isoY / tileH;
                    final int tileX = (int) Math.floor((compX + compY) / 2.0);
                    final int tileY = (int) Math.floor((compY - compX) / 2.0);
                    final boolean onMap = tileX >= 0 && tileY >= 0 && tileX < width && tileY < height;
                    samples[sample] = onMap ? map.getTile(tileX, tileY).getColor() : null;
                }
                pixels[py * imageWidth + px] = blend(samples);
            }
        }
        return pixels;
    }

    /**
     * Averages the samples of one pixel. The colour is weighted by alpha, as a premultiplied average is, so a
     * translucent tile weighs less than an opaque one and the colour is the one the covered area really has. The alpha
     * is the sum of the alphas over all the samples of the pixel, so a pixel half off the map is half transparent and
     * keeps the exact colour of the tiles under it.
     *
     * @param samples the colours under the pixel, null for a sample that lies off the map
     * @return the average in 0xAARRGGBB layout, 0 when nothing is covered
     */
    static int blend(Rgba[] samples) {
        double alpha = 0;
        double red = 0;
        double green = 0;
        double blue = 0;
        for (Rgba color : samples) {
            if (color == null) {
                continue;
            }
            alpha += color.a();
            red += color.r() * color.a();
            green += color.g() * color.a();
            blue += color.b() * color.a();
        }
        if (alpha <= 0) {
            return 0;
        }
        return toArgb(new Rgba(red / alpha, green / alpha, blue / alpha, alpha / samples.length));
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
        return (int) Math.round(Math.clamp(component, 0.0, 1.0) * 255.0);
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