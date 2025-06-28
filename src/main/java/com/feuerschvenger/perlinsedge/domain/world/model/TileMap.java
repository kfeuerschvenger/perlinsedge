package com.feuerschvenger.perlinsedge.domain.world.model;

/**
 * Represents a 2D grid of tiles with efficient coordinate-based access.
 * Provides bounds-checked operations for safe tile manipulation.
 */
public class TileMap {
    // Dimensions (Immutable)
    private final int width;
    private final int height;

    // Tile Storage
    private final Tile[][] tiles;

    /**
     * Constructs a TileMap with specified dimensions.
     * @param width  Width of the tile map (number of tiles in the X direction)
     * @param height Height of the tile map (number of tiles in the Y direction)
     */
    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
    }

    // ==================================================================
    //  Dimensional Getters
    // ==================================================================

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // ==================================================================
    //  Tile Getters and Setters
    // ==================================================================

    /**
     * Retrieves tile at specified coordinates with bounds checking.
     *
     * @param x X coordinate (0 <= x < width)
     * @param y Y coordinate (0 <= y < height)
     * @return Tile instance or null if out of bounds
     */
    public Tile getTile(int x, int y) {
        return isValidCoordinate(x, y) ? tiles[x][y] : null;
    }

    /**
     * Sets tile at specified coordinates with bounds checking.
     *
     * @param x X coordinate (0 <= x < width)
     * @param y Y coordinate (0 <= y < height)
     * @param tile Tile instance to set (can be null)
     * @return true if tile was set, false if coordinates invalid
     */
    public boolean setTile(int x, int y, Tile tile) {
        if (isValidCoordinate(x, y)) {
            tiles[x][y] = tile;
            return true;
        }
        return false;
    }

    // ==================================================================
    //  Coordinate Validation
    // ==================================================================

    /**
     * Checks if coordinates are within map boundaries.
     *
     * @param x X coordinate to validate
     * @param y Y coordinate to validate
     * @return true if coordinates are valid, false otherwise
     */
    public boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

}