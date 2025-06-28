package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import javafx.scene.paint.Color;
import java.util.Objects;

/**
 * Represents a game world tile with position, terrain properties, and optional resource.
 */
public class Tile {
    // Coordinate Properties (Immutable)
    private final int x, y;

    // Terrain Properties
    private float height;
    private TileType type;

    // Resource Association
    private Resource resource;

    // Building Association
    private Building building;

    /**
     * Constructs a Tile at the specified coordinates with the given type.
     * The height is initialized to 0 and no resource is associated by default.
     * @param x    The x-coordinate of the tile.
     * @param y    The y-coordinate of the tile.
     * @param type The type of terrain for this tile.
     */
    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        // Default state
        this.height = 0;
        this.resource = null;
        this.building = null;
    }

    // ==================================================================
    //  Coordinate Getters
    // ==================================================================

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // ==================================================================
    //  Terrain Getters and Setters
    // ==================================================================

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
    }

    public Color getColor() {
        return type.getColor();
    }

    // ==================================================================
    //  Resource Management
    // ==================================================================

    public Resource getWorldResource() {
        return resource;
    }

    public void setWorldResource(Resource resource) {
        this.resource = resource;
    }

    public boolean hasResource() {
        return resource != null && !resource.isDepleted();
    }

    // ==================================================================
    //  Building Management
    // ==================================================================

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public boolean hasBuilding() {
        return building != null;
    }

    /**
     * Checks if this tile is walkable, considering its type and any building present.
     * This method is crucial for pathfinding and player movement.
     * @return True if the tile can be walked on, false otherwise.
     */
    public boolean isWalkable() {
        // If there's a building that is solid, the tile is not walkable.
        if (hasBuilding() && building.isSolid()) {
            return false;
        }

        // Bridges and Docks explicitly make tiles walkable, even if the base tile type is not (e.g., water).
        if (hasBuilding() && (building.getType() == BuildingType.BRIDGE || building.getType() == BuildingType.DOCK)) {
            return true; // These buildings override tile type walkability
        }

        if (hasResource()) {
            return false; // Tiles with resources are not walkable
        }

        // Otherwise, rely on the base tile type's walkability.
        return type.isWalkable();
    }

    /**
     * Checks if this tile can have a building placed on it, considering current state.
     * This does NOT check if the player has resources, only tile validity.
     * @return True if a building can potentially be placed, false otherwise.
     */
    public boolean isBuildable() {
        // A tile is generally buildable if it doesn't have a resource and no existing building.
        // Specific building types (like Bridges/Docks) will have additional rules in BuildingManager.
        return !hasResource() && !hasBuilding();
    }

    // ==================================================================
    //  Identity Operations
    // ==================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tile)) return false;
        Tile tile = (Tile) o;
        return x == tile.x && y == tile.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

}