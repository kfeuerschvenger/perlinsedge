package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * Represents different types of terrain tiles with specific properties.
 * Each tile type has visual, terrain, and gameplay characteristics.
 */
public enum TileType {
    // Water-based tiles
    DEEP_WATER(
            Rgba.rgb(20, 40, 80),
            TerrainProperties.WATER
    ),
    SHALLOW_WATER(
            Rgba.rgb(40, 80, 120),
            TerrainProperties.WATER
    ),
    RIVER(
            Rgba.rgb(50, 90, 130),
            TerrainProperties.WATER
    ),
    COAST(
            Rgba.rgb(100, 150, 200),
            TerrainProperties.WATER
    ),
    ICE(
            Rgba.rgb(200, 220, 240),
            new TerrainProperties(true, false, true, false)
    ),

    // Land-based tiles
    BEACH(
            Rgba.rgb(200, 180, 120),
            TerrainProperties.LAND
    ),
    SAND(
            Rgba.rgb(200, 180, 120),
            new TerrainProperties(true, false, true, true)
    ),
    GRASS(
            Rgba.rgb(60, 120, 60),
            new TerrainProperties(true, false, true, true)
    ),
    FOREST(
            Rgba.rgb(40, 80, 40),
            new TerrainProperties(true, false, true, true)
    ),
    SAVANNA(
            Rgba.rgb(189, 183, 107),
            new TerrainProperties(true, false, true, true)
    ),
    HILL(
            Rgba.rgb(120, 80, 50),
            new TerrainProperties(true, false, true, true)
    ),
    MOUNTAIN(
            Rgba.rgb(90, 90, 90),
            new TerrainProperties(true, false, false, false)
    ),
    SNOW(
            Rgba.rgb(235, 235, 235),
            new TerrainProperties(true, false, true, false)
    ),
    SWAMP(
            Rgba.rgb(50, 70, 50),
            new TerrainProperties(true, false, true, true)
    );

    // Visual Properties
    private final Rgba color;

    // Terrain Properties
    private final TerrainProperties properties;

    TileType(Rgba color, TerrainProperties properties) {
        this.color = color;
        this.properties = properties;
    }

    // ==================================================================
    //  Visual Getters
    // ==================================================================

    public Rgba getColor() {
        return color;
    }

    // ==================================================================
    //  Terrain Property Getters
    // ==================================================================
    public boolean isLand() {
        return properties.isLand();
    }

    public boolean isWater() {
        return properties.isWater();
    }

    public boolean isFertile() {
        return properties.isFertile();
    }

    public boolean isWalkable() {
        return properties.isWalkable();
    }

    // ==================================================================
    //  Terrain Properties Configuration
    // ==================================================================
    private static class TerrainProperties {
        // Predefined property configurations
        static final TerrainProperties WATER =
                new TerrainProperties(false, true, false, false);
        static final TerrainProperties LAND =
                new TerrainProperties(true, false, true, false);

        private final boolean land;
        private final boolean water;
        private final boolean walkable;
        private final boolean fertile;

        TerrainProperties(boolean land, boolean water,
                          boolean walkable, boolean fertile) {
            this.land = land;
            this.water = water;
            this.walkable = walkable;
            this.fertile = fertile;
        }

        public boolean isLand() {
            return land;
        }

        public boolean isWater() {
            return water;
        }

        public boolean isWalkable() {
            return walkable;
        }

        public boolean isFertile() {
            return fertile;
        }
    }
}