package com.feuerschvenger.perlinsedge.domain.world.model;

import lombok.Getter;

import java.util.Optional;

/**
 * Represents a 2D grid of tiles with efficient coordinate-based access.
 * Provides bounds-checked operations for safe tile manipulation.
 */
public class TileMap {
    // Dimensions (Immutable)
    @Getter
    private final int width;
    @Getter
    private final int height;

    // Tile Storage
    private final Tile[][] tiles;

    /** The tile where the player starts, or null when the map was built without one. */
    private final SpawnPoint spawn;

    /** The number of changes the map has seen, see {@link #getRevision()}. */
    private long revision;

    /** The labelling of the walkable tiles, created by the first query. */
    private WalkableRegions walkableRegions;

    /**
     * Constructs a TileMap with specified dimensions.
     * @param width  Width of the tile map (number of tiles in the X direction)
     * @param height Height of the tile map (number of tiles in the Y direction)
     */
    public TileMap(int width, int height) {
        this(width, height, null);
    }

    /**
     * Constructs a TileMap with specified dimensions and the tile where the player starts.
     * @param width  Width of the tile map (number of tiles in the X direction)
     * @param height Height of the tile map (number of tiles in the Y direction)
     * @param spawn  The starting tile, or null when the map has none
     * @throws IllegalArgumentException when the spawn point lies outside the map
     */
    public TileMap(int width, int height, SpawnPoint spawn) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        if (spawn != null && !isInside(width, height, spawn.x(), spawn.y())) {
            throw new IllegalArgumentException("the spawn point " + spawn + " is outside the " + width + "x" + height
                    + " map");
        }
        this.spawn = spawn;
    }

    /**
     * Returns the tile where the player starts on this map.
     *
     * @return the spawn point, or empty when the map was built without one
     */
    public Optional<SpawnPoint> getSpawn() {
        return Optional.ofNullable(spawn);
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
     * @throws IllegalStateException when the tile is held by another map, which is checked before anything changes
     */
    public boolean setTile(int x, int y, Tile tile) {
        if (isValidCoordinate(x, y)) {
            final Tile replaced = tiles[x][y];
            if (tile != null) {
                // Refuses a tile of another map before the grid is touched
                tile.attachTo(this);
            }
            tiles[x][y] = tile;
            // The tile that was replaced no longer reports its changes here
            if (replaced != null && replaced != tile) {
                replaced.detach();
            }
            markChanged();
            return true;
        }
        return false;
    }

    /**
     * Returns a counter that goes up whenever a tile is put into the map or what stands on a tile changes: its
     * resource or building is set, removed or replaced, or its terrain type changes. Reading never changes it. A
     * consumer that keeps something worked out from the tiles compares it with the value it saw last to know whether
     * the map still is what it was worked out from.
     *
     * @return the number of changes the map has seen
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Returns the index of the walkable regions of this map, which stays right as the map changes because it relabels
     * itself when the revision moved. The same instance is returned every time.
     *
     * @return the region index of this map
     */
    public WalkableRegions getWalkableRegions() {
        if (walkableRegions == null) {
            walkableRegions = new WalkableRegions(this);
        }
        return walkableRegions;
    }

    /** Counts one change of the map, which a tile calls when its contents change. */
    void markChanged() {
        revision++;
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
        return isInside(width, height, x, y);
    }

    /** Checks a coordinate against given dimensions, so the constructor never calls an overridable method. */
    private static boolean isInside(int width, int height, int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

}
