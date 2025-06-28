package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Provides helper methods for drawing isometric tiles and resources.
 * Handles both normal rendering and debug visualization modes.
 */
public final class FxTileDrawingHelper {
    private static final double OUTLINE_WIDTH = 1.2;
    private static final double WATER_SCALE_FACTOR = 1.01;
    private static final double HEIGHT_NORMALIZATION_FACTOR = 2.0;
    private static final double DEBUG_TEXT_OFFSET = 5.0;
    private static final double DEBUG_TEXT_Y_OFFSET_FACTOR = 1.2;
    private static final Font DEBUG_FONT = new Font("Arial", 10);

    // Prevent instantiation
    private FxTileDrawingHelper() {}

    /**
     * Draws a tile in debug mode with grayscale height visualization and optional text overlay.
     *
     * @param gc GraphicsContext to draw on
     * @param x Screen X coordinate of tile center
     * @param y Screen Y coordinate of tile center
     * @param zoom Current zoom level
     * @param tile Tile to visualize
     */
    public static void drawDebugTile(GraphicsContext gc, double x, double y, double zoom, Tile tile) {
        final AppConfig config = AppConfig.getInstance();
        final double halfW = config.graphics().getTileHalfWidth() * zoom;
        final double halfH = config.graphics().getTileHalfHeight() * zoom;
        final float tileHeight = tile.getHeight();

        // Calculate normalized height value (0-1 range)
        double normalizedHeight = (tileHeight + 1.0) / HEIGHT_NORMALIZATION_FACTOR;
        normalizedHeight = clamp(normalizedHeight);

        // Draw tile shape with height-based color
        drawDebugTileShape(gc, x, y, halfW, halfH, normalizedHeight);

        // Draw debug text if enabled
        if (config.debug().isTextEnabled()) {
            drawDebugText(gc, x, y, zoom, halfH, normalizedHeight, tile);
        }
    }

    /**
     * Draws the isometric shape of a tile with appropriate coloring and shading.
     *
     * @param gc GraphicsContext to draw on
     * @param x Screen X coordinate of tile center
     * @param y Screen Y coordinate of tile center
     * @param zoom Current zoom level
     * @param tile Tile to draw
     */
    public static void drawTileShape(GraphicsContext gc, double x, double y, double zoom, Tile tile) {
        final AppConfig config = AppConfig.getInstance();
        double halfW = config.graphics().getTileHalfWidth() * zoom;
        double halfH = config.graphics().getTileHalfHeight() * zoom;
        final double baseHeight = config.graphics().getTileVisualBaseHeight() * zoom;
        final Color baseColor = tile.getColor();
        final boolean isWater = tile.getType().isWater();

        // Scale water tiles slightly larger
        if (isWater) {
            halfW *= WATER_SCALE_FACTOR;
            halfH *= WATER_SCALE_FACTOR;
        }

        // Calculate polygon points
        final double rightX = x + halfW;
        final double rightY = y + halfH;
        final double bottomY = y + 2 * halfH;
        final double leftX = x - halfW;
        final double leftY = y + halfH;

        final double baseRightY = rightY + baseHeight;
        final double baseBottomY = bottomY + baseHeight;
        final double baseLeftY = leftY + baseHeight;

        // Draw tile faces with appropriate shading
        drawRightFace(gc, rightX, rightY, x, bottomY, baseRightY, baseBottomY, baseColor, isWater);
        drawLeftFace(gc, leftX, leftY, x, bottomY, baseLeftY, baseBottomY, baseColor, isWater);
        drawTopFace(gc, x, y, rightX, rightY, leftX, leftY, x, bottomY, baseColor, isWater);
    }

    /**
     * Draws a resource sprite positioned correctly on an isometric tile.
     *
     * @param gc GraphicsContext to draw on
     * @param screenX Screen X coordinate of tile top vertex
     * @param screenY Screen Y coordinate of tile top vertex
     * @param zoom Current zoom level
     * @param resourceSprite Sprite image to draw
     */
    public static void drawResourceSprite(GraphicsContext gc, double screenX, double screenY, double zoom, Image resourceSprite) {
        if (resourceSprite == null) return;

        final AppConfig config = AppConfig.getInstance();
        final double halfTileHeight = config.graphics().getTileHalfHeight() * zoom;
        final double spriteWidth = resourceSprite.getWidth() * zoom;
        final double spriteHeight = resourceSprite.getHeight() * zoom;

        final double baseY = screenY + halfTileHeight + 4;
        final double drawX = screenX - (spriteWidth / 2);
        final double drawY = baseY - spriteHeight;

        gc.drawImage(resourceSprite, drawX, drawY, spriteWidth, spriteHeight);
    }

    /**
     * Draws highlight lines for the tile currently under the mouse.
     *
     * @param centerX The X-coordinate of the tile's center.
     * @param centerY The Y-coordinate of the tile's center.
     * @param tileWidth The zoomed width of the tile.
     * @param tileHeight The zoomed height of the tile.
     */
    public static void drawTileHighlight(GraphicsContext gc, double centerX, double centerY,
                                         double tileWidth, double tileHeight, double zoom) {
        final double segmentLength = 8; // Length of the highlight segments from each corner
        final double halfWidth = tileWidth / 2; // Half of the zoomed tile width
        final double halfHeight = tileHeight / 2; // Half of the zoomed tile height

        // Calculate the four vertices of the isometric tile (diamond shape) relative to the center
        // The center (centerX, centerY) is the reference point for all vertex calculations.
        final double topX = centerX;
        final double topY = centerY - halfHeight; // Top vertex is directly above center by halfHeight
        final double rightX = centerX + halfWidth; // Right vertex is right of center by halfWidth
        final double rightY = centerY;             // Right vertex is at same Y as center
        final double bottomX = centerX;
        final double bottomY = centerY + halfHeight; // Bottom vertex is directly below center by halfHeight
        final double leftX = centerX - halfWidth; // Left vertex is left of center by halfWidth
        final double leftY = centerY;             // Left vertex is at same Y as center

        // Configure drawing style for the highlight
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);

        // Calculate normalized direction components for isometric edges.
        // This is based on the half-dimensions of the tile, which form the legs of a right triangle
        // whose hypotenuse is the isometric edge itself.
        final double edgeLength = Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight);
        final double normDx = halfWidth / edgeLength; // Normalized change in X along an isometric edge
        final double normDy = halfHeight / edgeLength; // Normalized change in Y along an isometric edge

        // Calculate the actual delta X and Y for a segment of 'segmentLength' along an isometric edge
        final double segmentDx = segmentLength * normDx * zoom;
        final double segmentDy = segmentLength * normDy * zoom;

        // Draw segments at each corner following the correct isometric directions
        // Each corner will have two segments extending outwards along the tile's edges.

        // Top corner (topX, topY)
        // Segments extend down-left and down-right from the top vertex
        gc.strokeLine(topX, topY, topX - segmentDx, topY + segmentDy); // Segment towards bottom-left
        gc.strokeLine(topX, topY, topX + segmentDx, topY + segmentDy); // Segment towards bottom-right

        // Right corner (rightX, rightY)
        // Segments extend up-left and down-left from the right vertex
        gc.strokeLine(rightX, rightY, rightX - segmentDx, rightY - segmentDy); // Segment towards top-left
        gc.strokeLine(rightX, rightY, rightX - segmentDx, rightY + segmentDy); // Segment towards bottom-left

        // Bottom corner (bottomX, bottomY)
        // Segments extend up-left and up-right from the bottom vertex
        gc.strokeLine(bottomX, bottomY, bottomX - segmentDx, bottomY - segmentDy); // Segment towards top-left
        gc.strokeLine(bottomX, bottomY, bottomX + segmentDx, bottomY - segmentDy); // Segment towards top-right

        // Left corner (leftX, leftY)
        // Segments extend up-right and down-right from the left vertex
        gc.strokeLine(leftX, leftY, leftX + segmentDx, leftY - segmentDy); // Segment towards top-right
        gc.strokeLine(leftX, leftY, leftX + segmentDx, leftY + segmentDy); // Segment towards bottom-right
    }

    // ==================================================================
    //  PRIVATE DRAWING HELPERS
    // ==================================================================

    private static void drawDebugTileShape(GraphicsContext gc, double x, double y,
                                           double halfW, double halfH, double normalizedHeight) {
        gc.setFill(Color.gray(normalizedHeight));
        gc.beginPath();
        gc.moveTo(x, y);
        gc.lineTo(x + halfW, y + halfH);
        gc.lineTo(x, y + 2 * halfH);
        gc.lineTo(x - halfW, y + halfH);
        gc.closePath();
        gc.fill();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.stroke();
    }

    private static void drawDebugText(GraphicsContext gc, double x, double y, double zoom,
                                      double halfH, double normalizedHeight, Tile tile) {
        final String heightText = String.format("%.2f", tile.getHeight());
        final Color textColor = (normalizedHeight > 0.5) ? Color.BLACK : Color.WHITE;
        final double textY = y + halfH * DEBUG_TEXT_Y_OFFSET_FACTOR;

        gc.setFill(textColor);
        gc.setFont(DEBUG_FONT);
        gc.setTextAlign(TextAlignment.CENTER);

        if (tile.hasResource()) {
            final String resourceChar = tile.getWorldResource().getType().getInitial();
            gc.fillText(heightText, x, textY - (DEBUG_TEXT_OFFSET * zoom));
            gc.fillText(resourceChar, x, textY + (DEBUG_TEXT_OFFSET * zoom));
        } else {
            gc.fillText(heightText, x, textY);
        }
    }

    private static void drawRightFace(GraphicsContext gc, double rightX, double rightY,
                                      double bottomX, double bottomY, double baseRightY,
                                      double baseBottomY, Color baseColor, boolean isWater) {
        final Color faceColor = isWater ? baseColor : baseColor.darker().darker();
        gc.setFill(faceColor);

        gc.beginPath();
        gc.moveTo(rightX, rightY);
        gc.lineTo(rightX, baseRightY);
        gc.lineTo(bottomX, baseBottomY);
        gc.lineTo(bottomX, bottomY);
        gc.closePath();
        gc.fill();

        if (!isWater) {
            gc.setStroke(faceColor.darker().darker());
            gc.setLineWidth(OUTLINE_WIDTH);
            gc.stroke();
        }
    }

    private static void drawLeftFace(GraphicsContext gc, double leftX, double leftY,
                                     double bottomX, double bottomY, double baseLeftY,
                                     double baseBottomY, Color baseColor, boolean isWater) {
        final Color faceColor = isWater ? baseColor : baseColor.darker();
        gc.setFill(faceColor);

        gc.beginPath();
        gc.moveTo(leftX, leftY);
        gc.lineTo(leftX, baseLeftY);
        gc.lineTo(bottomX, baseBottomY);
        gc.lineTo(bottomX, bottomY);
        gc.closePath();
        gc.fill();

        if (!isWater) {
            gc.setStroke(faceColor.darker().darker());
            gc.setLineWidth(OUTLINE_WIDTH);
            gc.stroke();
        }
    }

    private static void drawTopFace(GraphicsContext gc, double topX, double topY,
                                    double rightX, double rightY, double leftX, double leftY,
                                    double bottomX, double bottomY, Color baseColor, boolean isWater) {
        gc.setFill(baseColor);

        gc.beginPath();
        gc.moveTo(topX, topY);
        gc.lineTo(rightX, rightY);
        gc.lineTo(bottomX, bottomY);
        gc.lineTo(leftX, leftY);
        gc.closePath();
        gc.fill();

        if (!isWater) {
            gc.setStroke(baseColor.darker());
            gc.setLineWidth(OUTLINE_WIDTH);
            gc.stroke();
        } else {
            gc.setStroke(baseColor);
            gc.setLineWidth(0.5);
            gc.stroke();
        }
    }

    // ==================================================================
    //  UTILITY METHODS
    // ==================================================================

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

}