package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.ZoomSteps;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FxColors;
import com.feuerschvenger.perlinsedge.infra.fx.utils.GrayscaleTiles;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Locale;

/**
 * Provides helper methods for drawing isometric tiles and resources.
 * Handles both normal rendering and debug visualization modes.
 */
public final class FxTileDrawingHelper {
    private static final double OUTLINE_WIDTH = 1.2;
    private static final double WATER_SCALE_FACTOR = 1.01;
    /** Half the width and height of the top diamond of a tile sprite, in art pixels. */
    private static final double DECAL_TILE_HALF_WIDTH = 16.0;
    private static final double DECAL_TILE_HALF_HEIGHT = 8.0;
    private static final double DEBUG_TEXT_OFFSET = 5.0;
    private static final double DEBUG_TEXT_Y_OFFSET_FACTOR = 1.2;
    private static final Font DEBUG_FONT = new Font("Arial", 10);

    // Prevent instantiation
    private FxTileDrawingHelper() {}

    /**
     * Draws the height and resource text of the debug view on a tile whose grey diamond is already drawn.
     *
     * @param gc GraphicsContext to draw on
     * @param x Screen X coordinate of the tile top vertex
     * @param y Screen Y coordinate of the tile top vertex
     * @param zoom Current zoom level
     * @param tile Tile to describe
     */
    public static void drawDebugTileText(GraphicsContext gc, double x, double y, double zoom, Tile tile) {
        final double halfH = AppConfig.getInstance().graphics().getTileHalfHeight() * zoom;
        drawDebugText(gc, x, y, zoom, halfH, GrayscaleTiles.normalized(tile.getHeight()), tile);
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
        final Color baseColor = FxColors.toFx(tile.getColor());
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
     * Draws a tile sprite whose top vertex sits on the given point.
     * The sprite is drawn at an integer scale on whole pixels, so its pixels stay crisp and neighbours meet exactly.
     *
     * @param gc GraphicsContext to draw on
     * @param tileSprite the 32 pixel wide block sprite, top diamond first and side faces below
     * @param screenX Screen X coordinate of the tile top vertex
     * @param screenY Screen Y coordinate of the tile top vertex
     * @param zoom Current zoom level
     */
    public static void drawTileSprite(GraphicsContext gc, Image tileSprite, double screenX, double screenY, double zoom) {
        final double scale = ZoomSteps.artScale(zoom);
        final double width = tileSprite.getWidth() * scale;
        final double height = tileSprite.getHeight() * scale;
        gc.drawImage(tileSprite, Math.rint(screenX - width / 2), Math.rint(screenY), width, height);
    }

    /**
     * Draws a small decal sprite on the top diamond of a tile sprite, with its bottom centre at an offset from the
     * diamond centre.
     * The offset is given along the two isometric axes as fractions of the tile, so it maps to a point inside the
     * diamond for any value up to one half. The decal is drawn at an integer scale on whole pixels like the tile.
     *
     * @param gc GraphicsContext to draw on
     * @param decal the decal sprite
     * @param screenX Screen X coordinate of the tile top vertex
     * @param screenY Screen Y coordinate of the tile top vertex
     * @param zoom Current zoom level
     * @param offsetU offset along the first isometric axis, a fraction of the tile
     * @param offsetV offset along the second isometric axis, a fraction of the tile
     */
    public static void drawDecal(GraphicsContext gc, Image decal, double screenX, double screenY, double zoom,
                                 double offsetU, double offsetV) {
        final double scale = ZoomSteps.artScale(zoom);
        final double halfW = DECAL_TILE_HALF_WIDTH * scale;
        final double halfH = DECAL_TILE_HALF_HEIGHT * scale;
        final double anchorX = screenX + (offsetU - offsetV) * halfW;
        final double anchorY = screenY + halfH + (offsetU + offsetV) * halfH;
        final double width = decal.getWidth() * scale;
        final double height = decal.getHeight() * scale;
        gc.drawImage(decal, Math.rint(anchorX - width / 2), Math.rint(anchorY - height), width, height);
    }

    /**
     * Draws a resource sprite with its bottom centre on the tile centre.
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
        final double scale = ZoomSteps.artScale(zoom);
        final double spriteWidth = resourceSprite.getWidth() * scale;
        final double spriteHeight = resourceSprite.getHeight() * scale;

        final double baseY = screenY + halfTileHeight;
        gc.drawImage(resourceSprite, Math.rint(screenX - spriteWidth / 2), Math.rint(baseY - spriteHeight),
                spriteWidth, spriteHeight);
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

    private static void drawDebugText(GraphicsContext gc, double x, double y, double zoom,
                                      double halfH, double normalizedHeight, Tile tile) {
        final String heightText = String.format(Locale.ROOT, "%.2f", tile.getHeight());
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

}