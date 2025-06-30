package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a wall structure in the game world.
 * Walls are solid structures that block movement and provide visual barriers.
 * Implements custom isometric rendering to create a 3D-like appearance.
 */
public class Wall extends Building {
    // ==================================================================
    //  Rendering Constants
    // ==================================================================
    private static final double WALL_HEIGHT_FACTOR = 4.0;
    private static final double WALL_ELEVATION_OFFSET_FACTOR = 2.0;
    private static final double STROKE_WIDTH = 1.5;

    // Color definitions
    private static final Color BASE_WALL_COLOR = Color.rgb(100, 100, 100);
    private static final Color TOP_FACE_COLOR = BASE_WALL_COLOR.deriveColor(0, 1, 1.2, 1);
    private static final Color RIGHT_FACE_COLOR = BASE_WALL_COLOR.deriveColor(0, 1, 0.8, 1);
    private static final Color LEFT_FACE_COLOR = BASE_WALL_COLOR.deriveColor(0, 1, 0.9, 1);
    private static final Color OUTLINE_COLOR = Color.BLACK;

    // ==================================================================
    //  Constructor
    // ==================================================================

    /**
     * Creates a new wall structure at the specified tile position.
     *
     * @param tileX X-coordinate in tile units
     * @param tileY Y-coordinate in tile units
     */
    public Wall(int tileX, int tileY) {
        super(tileX, tileY, BuildingType.WALL, true, true);
    }

    // ==================================================================
    //  Rendering Methods
    // ==================================================================

    /**
     * Renders the wall using isometric projection.
     *
     * @param gc      Graphics context for drawing
     * @param screenX Screen X-coordinate for tile origin
     * @param screenY Screen Y-coordinate for tile origin
     * @param zoom    Current zoom level
     */
    @Override
    public void draw(GraphicsContext gc, double screenX, double screenY, double zoom) {
        final AppConfig config = AppConfig.getInstance();

        // Calculate scaled tile dimensions
        final double halfWidth = config.graphics().getTileHalfWidth() * zoom;
        final double halfHeight = config.graphics().getTileHalfHeight() * zoom;
        final double tileBaseHeight = config.graphics().getTileVisualBaseHeight() * zoom;

        // Calculate wall positioning
        final double elevationOffset = halfHeight * WALL_ELEVATION_OFFSET_FACTOR;
        final double wallTopY = screenY - elevationOffset;
        final double wallVisualHeight = tileBaseHeight * WALL_HEIGHT_FACTOR;

        // Calculate polygon points for wall faces
        final WallGeometry geometry = calculateWallGeometry(screenX, wallTopY, halfWidth, halfHeight, wallVisualHeight);

        // Render wall components
        renderLeftFace(gc, geometry, zoom);
        renderRightFace(gc, geometry, zoom);
        renderTopFace(gc, geometry, zoom);
    }

    // ==================================================================
    //  Geometry Calculation
    // ==================================================================

    /**
     * Calculates the geometric points for wall rendering.
     *
     * @param screenX         Screen X origin
     * @param wallTopY        Top Y position of wall
     * @param halfWidth       Half tile width (scaled)
     * @param halfHeight      Half tile height (scaled)
     * @param wallVisualHeight Visual height of wall
     * @return Geometry container with calculated points
     */
    private WallGeometry calculateWallGeometry(
            double screenX, double wallTopY,
            double halfWidth, double halfHeight,
            double wallVisualHeight) {

        // Upper diamond points (top of wall)
        final double topX = screenX;
        final double topY = wallTopY;
        final double rightX = topX + halfWidth;
        final double rightY = topY + halfHeight;
        final double bottomX = topX;
        final double bottomY = topY + 2 * halfHeight;
        final double leftX = topX - halfWidth;
        final double leftY = topY + halfHeight;

        // Base points (bottom of wall)
        final double baseRightY = rightY + wallVisualHeight;
        final double baseBottomY = bottomY + wallVisualHeight;
        final double baseLeftY = leftY + wallVisualHeight;

        return new WallGeometry(
                topX, topY,
                rightX, rightY,
                bottomX, bottomY,
                leftX, leftY,
                baseRightY, baseBottomY, baseLeftY
        );
    }

    // ==================================================================
    //  Component Rendering
    // ==================================================================

    /**
     * Renders the left face of the wall.
     */
    private void renderLeftFace(GraphicsContext gc, WallGeometry g, double zoom) {
        gc.setFill(LEFT_FACE_COLOR);
        gc.setStroke(OUTLINE_COLOR);
        gc.setLineWidth(STROKE_WIDTH * zoom);

        gc.beginPath();
        gc.moveTo(g.leftX, g.leftY);
        gc.lineTo(g.leftX, g.baseLeftY);
        gc.lineTo(g.bottomX, g.baseBottomY);
        gc.lineTo(g.bottomX, g.bottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();
    }

    /**
     * Renders the right face of the wall.
     */
    private void renderRightFace(GraphicsContext gc, WallGeometry g, double zoom) {
        gc.setFill(RIGHT_FACE_COLOR);
        gc.setStroke(OUTLINE_COLOR);
        gc.setLineWidth(STROKE_WIDTH * zoom);

        gc.beginPath();
        gc.moveTo(g.rightX, g.rightY);
        gc.lineTo(g.rightX, g.baseRightY);
        gc.lineTo(g.bottomX, g.baseBottomY);
        gc.lineTo(g.bottomX, g.bottomY);
        gc.closePath();
        gc.fill();
        gc.stroke();
    }

    /**
     * Renders the top face of the wall.
     */
    private void renderTopFace(GraphicsContext gc, WallGeometry g, double zoom) {
        gc.setFill(TOP_FACE_COLOR);
        gc.setStroke(OUTLINE_COLOR);
        gc.setLineWidth(STROKE_WIDTH * zoom);

        gc.beginPath();
        gc.moveTo(g.topX, g.topY);
        gc.lineTo(g.rightX, g.rightY);
        gc.lineTo(g.bottomX, g.bottomY);
        gc.lineTo(g.leftX, g.leftY);
        gc.closePath();
        gc.fill();
        gc.stroke();
    }

    // ==================================================================
    //  Geometry Data Container
    // ==================================================================

    /**
     * Container class for wall geometry points.
     * Encapsulates all coordinates needed for wall rendering.
     */
    private record WallGeometry(double topX, double topY, double rightX, double rightY, double bottomX, double bottomY,
                                double leftX, double leftY, double baseRightY, double baseBottomY, double baseLeftY) {
    }

}