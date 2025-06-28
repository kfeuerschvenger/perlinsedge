package com.feuerschvenger.perlinsedge.domain.utils;

/**
 * Utility class for isometric coordinate transformations.
 */
public final class IsometricUtils {

    private IsometricUtils() { } // Private constructor to prevent instantiation

    /**
     * Converts screen coordinates to isometric tile coordinates.
     *
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @param cameraX Camera X position
     * @param cameraY Camera Y position
     * @param zoom Current zoom level
     * @param halfTileWidth Half tile width (unzoomed)
     * @param halfTileHeight Half tile height (unzoomed)
     * @return Array containing tile coordinates [tileX, tileY]
     */
    public static int[] screenToTile(double screenX, double screenY,
                                     double cameraX, double cameraY,
                                     double zoom,
                                     double halfTileWidth, double halfTileHeight) {
        double worldX = screenX - cameraX;
        double worldY = screenY - cameraY;

        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        int tileX = (int) Math.floor((worldX / scaledHalfWidth + worldY / scaledHalfHeight) / 2);
        int tileY = (int) Math.floor((worldY / scaledHalfHeight - worldX / scaledHalfWidth) / 2);

        return new int[]{tileX, tileY};
    }

    /**
     * Converts screen coordinates to isometric tile coordinates with edge adjustment.
     *
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @param cameraX Camera X position
     * @param cameraY Camera Y position
     * @param zoom Current zoom level
     * @param halfTileWidth Half tile width (unzoomed)
     * @param halfTileHeight Half tile height (unzoomed)
     * @param edgeTolerancePixels Edge tolerance in pixels
     * @return Array containing adjusted tile coordinates [tileX, tileY]
     */
    public static int[] screenToTileAdjusted(double screenX, double screenY,
                                             double cameraX, double cameraY,
                                             double zoom,
                                             double halfTileWidth, double halfTileHeight,
                                             double edgeTolerancePixels) {
        // Calculate base tile coordinates
        int[] baseTile = screenToTile(screenX, screenY, cameraX, cameraY, zoom, halfTileWidth, halfTileHeight);
        int baseTileX = baseTile[0];
        int baseTileY = baseTile[1];

        // Calculate scaled dimensions
        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        // Calculate tile center position
        double tileCenterX = (baseTileX - baseTileY) * scaledHalfWidth + cameraX;
        double tileCenterY = (baseTileX + baseTileY) * scaledHalfHeight + cameraY;

        // Calculate click offset from tile center
        double offsetX = screenX - tileCenterX;
        double offsetY = screenY - (tileCenterY + scaledHalfHeight);

        // Apply zoom-adjusted tolerance
        double tolerance = edgeTolerancePixels * zoom;

        // Check edge regions and adjust tile coordinates
        return adjustTileForEdgeRegions(
                baseTileX, baseTileY,
                offsetX, offsetY,
                scaledHalfWidth, scaledHalfHeight,
                tolerance
        );
    }

    /**
     * Converts screen coordinates to fractional isometric tile coordinates.
     *
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @param cameraX Camera X position
     * @param cameraY Camera Y position
     * @param zoom Current zoom level
     * @param halfTileWidth Half tile width (unzoomed)
     * @param halfTileHeight Half tile height (unzoomed)
     * @return Array containing fractional tile coordinates [tileX, tileY]
     */
    public static double[] screenToTileFractional(double screenX, double screenY,
                                                  double cameraX, double cameraY,
                                                  double zoom,
                                                  double halfTileWidth, double halfTileHeight) {
        double worldX = screenX - cameraX;
        double worldY = screenY - cameraY;

        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        double tileX = (worldX / scaledHalfWidth + worldY / scaledHalfHeight) / 2;
        double tileY = (worldY / scaledHalfHeight - worldX / scaledHalfWidth) / 2;

        return new double[]{tileX, tileY};
    }

    /**
     * Converts isometric tile coordinates to screen coordinates.
     *
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     * @param cameraX Camera X position
     * @param cameraY Camera Y position
     * @param zoom Current zoom level
     * @param halfTileWidth Half tile width (unzoomed)
     * @param halfTileHeight Half tile height (unzoomed)
     * @return Array containing screen coordinates [screenX, screenY] for the tile's top-left corner of its bounding box.
     */
    public static double[] tileToScreen(double tileX, double tileY,
                                        double cameraX, double cameraY,
                                        double zoom,
                                        double halfTileWidth, double halfTileHeight) {
        double scaledHalfWidth = halfTileWidth * zoom;
        double scaledHalfHeight = halfTileHeight * zoom;

        double screenX = (tileX - tileY) * scaledHalfWidth + cameraX;
        double screenY = (tileX + tileY) * scaledHalfHeight + cameraY;

        return new double[]{screenX, screenY};
    }

    /**
     * Converts isometric tile coordinates to screen Y coordinate for sorting purposes.
     * This typically represents the "bottom" or "base" of the isometric tile visually.
     *
     * @param tileX Tile X coordinate
     * @param tileY Tile Y coordinate
     * @param cameraX Camera X position
     * @param cameraY Camera Y position
     * @param zoom Current zoom level
     * @param halfTileHeight Half tile height (unzoomed)
     * @return The screen Y coordinate representing the effective depth.
     */
    public static double tileToScreenY(double tileX, double tileY,
                                       double cameraX, double cameraY,
                                       double zoom,
                                       double halfTileHeight) {
        double scaledHalfHeight = halfTileHeight * zoom;
        // The Y coordinate that determines depth is essentially the sum of X and Y tile coords scaled,
        // plus the camera Y offset, plus the full height of one isometric diamond base.
        // This puts the sorting point at the bottom vertex of the diamond.
        return (tileX + tileY) * scaledHalfHeight + cameraY + scaledHalfHeight;
    }

    /**
     * Adjusts tile coordinates based on click position relative to edge regions.
     *
     * @param baseX Base tile X coordinate
     * @param baseY Base tile Y coordinate
     * @param offsetX Click X offset from tile center
     * @param offsetY Click Y offset from tile center
     * @param halfWidth Scaled half tile width
     * @param halfHeight Scaled half tile height
     * @param tolerance Edge tolerance value
     * @return Array containing adjusted tile coordinates [tileX, tileY]
     */
    private static int[] adjustTileForEdgeRegions(int baseX, int baseY,
                                                  double offsetX, double offsetY,
                                                  double halfWidth, double halfHeight,
                                                  double tolerance) {
        // Top region (closer to tile above)
        if (offsetY < -halfHeight + tolerance && Math.abs(offsetX) < halfWidth - tolerance) {
            return new int[]{baseX, baseY - 1};
        }

        // Bottom region (closer to tile below)
        if (offsetY > halfHeight - tolerance && Math.abs(offsetX) < halfWidth - tolerance) {
            return new int[]{baseX, baseY + 1};
        }

        // Left region (closer to left tile)
        if (offsetX < -halfWidth + tolerance && Math.abs(offsetY) < halfHeight - tolerance) {
            return new int[]{baseX - 1, baseY};
        }

        // Right region (closer to right tile)
        if (offsetX > halfWidth - tolerance && Math.abs(offsetY) < halfHeight - tolerance) {
            return new int[]{baseX + 1, baseY};
        }

        // Default to base tile if not near edges
        return new int[]{baseX, baseY};
    }

}