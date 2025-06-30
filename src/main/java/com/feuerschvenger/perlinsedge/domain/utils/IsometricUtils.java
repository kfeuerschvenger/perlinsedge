package com.feuerschvenger.perlinsedge.domain.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.function.Predicate;

/**
 * Utility class for isometric coordinate transformations and position calculations.
 * Provides methods for converting between screen and tile coordinates, finding adjacent positions,
 * and validating map positions.
 */
public final class IsometricUtils {

    // Direction constants for cardinal and diagonal movements
    public static final int[][] CARDINAL_DIRECTIONS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    public static final int[][] ALL_DIRECTIONS = {
            {0, 1}, {1, 0}, {0, -1}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    // Private constructor to prevent instantiation
    private IsometricUtils() {}

    // ==================================================================
    //  Coordinate Transformation Methods
    // ==================================================================

    /**
     * Converts screen coordinates to isometric tile coordinates.
     *
     * @param screenX        Screen X coordinate
     * @param screenY        Screen Y coordinate
     * @param cameraX        Camera X position
     * @param cameraY        Camera Y position
     * @param zoom           Current zoom level
     * @param halfTileWidth  Half tile width (unzoomed)
     * @param halfTileHeight Half tile height (unzoomed)
     * @return Array containing integer tile coordinates [tileX, tileY]
     */
    public static int[] screenToTile(double screenX, double screenY,
                                     double cameraX, double cameraY,
                                     double zoom,
                                     double halfTileWidth, double halfTileHeight) {
        // Convert to world coordinates relative to camera
        double worldX = screenX - cameraX;
        double worldY = screenY - cameraY;

        // Apply zoom scaling
        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        // Perform isometric transformation
        int tileX = (int) Math.floor((worldX / scaledHalfWidth + worldY / scaledHalfHeight) / 2);
        int tileY = (int) Math.floor((worldY / scaledHalfHeight - worldX / scaledHalfWidth) / 2);

        return new int[]{tileX, tileY};
    }

    /**
     * Converts screen coordinates to fractional isometric tile coordinates.
     * Useful for precise positioning and interpolation.
     *
     * @param screenX        Screen X coordinate
     * @param screenY        Screen Y coordinate
     * @param cameraX        Camera X position
     * @param cameraY        Camera Y position
     * @param zoom           Current zoom level
     * @param halfTileWidth  Half tile width (unzoomed)
     * @param halfTileHeight Half tile height (unzoomed)
     * @return Array containing fractional tile coordinates [tileX, tileY]
     */
    public static double[] screenToTileFractional(double screenX, double screenY,
                                                  double cameraX, double cameraY,
                                                  double zoom,
                                                  double halfTileWidth, double halfTileHeight) {
        // Convert to world coordinates relative to camera
        double worldX = screenX - cameraX;
        double worldY = screenY - cameraY;

        // Apply zoom scaling
        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        // Perform precise isometric transformation
        double tileX = (worldX / scaledHalfWidth + worldY / scaledHalfHeight) / 2;
        double tileY = (worldY / scaledHalfHeight - worldX / scaledHalfWidth) / 2;

        return new double[]{tileX, tileY};
    }

    /**
     * Converts isometric tile coordinates to screen coordinates.
     *
     * @param tileX          Tile X coordinate
     * @param tileY          Tile Y coordinate
     * @param cameraX        Camera X position
     * @param cameraY        Camera Y position
     * @param zoom           Current zoom level
     * @param halfTileWidth  Half tile width (unzoomed)
     * @param halfTileHeight Half tile height (unzoomed)
     * @return Array containing screen coordinates [screenX, screenY]
     */
    public static double[] tileToScreen(double tileX, double tileY,
                                        double cameraX, double cameraY,
                                        double zoom,
                                        double halfTileWidth, double halfTileHeight) {
        // Apply zoom scaling
        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        // Perform inverse isometric transformation
        double screenX = (tileX - tileY) * scaledHalfWidth + cameraX;
        double screenY = (tileX + tileY) * scaledHalfHeight + cameraY;

        return new double[]{screenX, screenY};
    }

    /**
     * Converts screen coordinates to isometric tile coordinates with edge adjustment.
     * Provides more accurate tile selection near tile edges.
     *
     * @param screenX           Screen X coordinate
     * @param screenY           Screen Y coordinate
     * @param cameraX           Camera X position
     * @param cameraY           Camera Y position
     * @param zoom              Current zoom level
     * @param halfTileWidth     Half tile width (unzoomed)
     * @param halfTileHeight    Half tile height (unzoomed)
     * @param edgeTolerancePixels Edge tolerance in pixels
     * @return Array containing adjusted tile coordinates [tileX, tileY]
     */
    public static int[] screenToTileAdjusted(double screenX, double screenY,
                                             double cameraX, double cameraY,
                                             double zoom,
                                             double halfTileWidth, double halfTileHeight,
                                             double edgeTolerancePixels) {
        // Get base tile coordinates
        int[] baseTile = screenToTile(screenX, screenY, cameraX, cameraY, zoom, halfTileWidth, halfTileHeight);

        // Calculate scaled dimensions
        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        // Calculate tile center position
        double tileCenterX = (baseTile[0] - baseTile[1]) * scaledHalfWidth + cameraX;
        double tileCenterY = (baseTile[0] + baseTile[1]) * scaledHalfHeight + cameraY;

        // Calculate click offset from tile center
        double offsetX = screenX - tileCenterX;
        double offsetY = screenY - (tileCenterY + scaledHalfHeight);

        // Apply zoom-adjusted tolerance
        double tolerance = edgeTolerancePixels * zoom;

        // Adjust for edge regions
        return adjustTileForEdgeRegions(
                baseTile[0], baseTile[1],
                offsetX, offsetY,
                scaledHalfWidth, scaledHalfHeight,
                tolerance
        );
    }

    // ==================================================================
    //  Position Adjustment Methods
    // ==================================================================

    /**
     * Adjusts tile coordinates based on click position relative to edge regions.
     *
     * @param baseX       Base tile X coordinate
     * @param baseY       Base tile Y coordinate
     * @param offsetX     Click X offset from tile center
     * @param offsetY     Click Y offset from tile center
     * @param halfWidth   Scaled half tile width
     * @param halfHeight  Scaled half tile height
     * @param tolerance   Edge tolerance value
     * @return Adjusted tile coordinates [tileX, tileY]
     */
    private static int[] adjustTileForEdgeRegions(int baseX, int baseY,
                                                  double offsetX, double offsetY,
                                                  double halfWidth, double halfHeight,
                                                  double tolerance) {
        // Calculate boundary thresholds
        double topThreshold = -halfHeight + tolerance;
        double bottomThreshold = halfHeight - tolerance;
        double leftThreshold = -halfWidth + tolerance;
        double rightThreshold = halfWidth - tolerance;

        // Top edge region (closer to tile above)
        boolean inTopRegion = offsetY < topThreshold && Math.abs(offsetX) < halfWidth - tolerance;
        if (inTopRegion) return new int[]{baseX, baseY - 1};

        // Bottom edge region (closer to tile below)
        boolean inBottomRegion = offsetY > bottomThreshold && Math.abs(offsetX) < halfWidth - tolerance;
        if (inBottomRegion) return new int[]{baseX, baseY + 1};

        // Left edge region (closer to left tile)
        boolean inLeftRegion = offsetX < leftThreshold && Math.abs(offsetY) < halfHeight - tolerance;
        if (inLeftRegion) return new int[]{baseX - 1, baseY};

        // Right edge region (closer to right tile)
        boolean inRightRegion = offsetX > rightThreshold && Math.abs(offsetY) < halfHeight - tolerance;
        if (inRightRegion) return new int[]{baseX + 1, baseY};

        // Default to base tile if not near edges
        return new int[]{baseX, baseY};
    }

    // ==================================================================
    //  Position Validation and Search Methods
    // ==================================================================

    /**
     * Finds an adjacent position meeting specific criteria.
     *
     * @param x          Starting X coordinate
     * @param y          Starting Y coordinate
     * @param map        TileMap instance
     * @param validator  Predicate to validate tile suitability
     * @param directions Directions to check
     * @return Valid adjacent position or original position if none found
     */
    public static int[] findAdjacentPosition(int x, int y, TileMap map,
                                             Predicate<Tile> validator,
                                             int[][] directions) {
        // Check each direction for valid adjacent position
        for (int[] direction : directions) {
            int newX = x + direction[0];
            int newY = y + direction[1];

            if (isWithinMapBounds(newX, newY, map)) {
                Tile tile = map.getTile(newX, newY);
                if (validator.test(tile)) {
                    return new int[]{newX, newY};
                }
            }
        }

        // Return original position if no valid adjacent found
        return new int[]{x, y};
    }

    /**
     * Checks if a position has sufficient valid neighbors.
     *
     * @param x            X coordinate
     * @param y            Y coordinate
     * @param map          TileMap instance
     * @param validator    Predicate to validate neighbors
     * @param minNeighbors Minimum valid neighbors required
     * @param directions   Directions to check
     * @return true if position has insufficient valid neighbors (surrounded), false otherwise
     */
    public static boolean isPositionSurrounded(int x, int y, TileMap map,
                                               Predicate<Tile> validator,
                                               int minNeighbors,
                                               int[][] directions) {
        int validCount = 0;

        for (int[] direction : directions) {
            int neighborX = x + direction[0];
            int neighborY = y + direction[1];

            if (isWithinMapBounds(neighborX, neighborY, map)) {
                Tile neighbor = map.getTile(neighborX, neighborY);
                if (validator.test(neighbor)) {
                    validCount++;

                    // Early exit if minimum neighbors reached
                    if (validCount >= minNeighbors) {
                        return false;
                    }
                }
            }
        }

        return validCount < minNeighbors;
    }

    /**
     * Checks if coordinates are within map boundaries.
     *
     * @param x    X coordinate
     * @param y    Y coordinate
     * @param map  TileMap instance
     * @return true if coordinates are within map bounds, false otherwise
     */
    public static boolean isWithinMapBounds(int x, int y, TileMap map) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }

}