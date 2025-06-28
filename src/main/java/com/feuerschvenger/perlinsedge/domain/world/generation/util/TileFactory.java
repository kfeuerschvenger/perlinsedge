package com.feuerschvenger.perlinsedge.domain.world.generation.util;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Factory for creating TileType instances based on environmental parameters.
 * Uses deterministic noise-based variation to break uniformity in terrain generation.
 */
public final class TileFactory {
    // Height thresholds for terrain types
    private static final float DEEP_WATER_THRESHOLD = -0.5f;
    private static final float SHALLOW_WATER_THRESHOLD = -0.3f;
    private static final float SAND_THRESHOLD = -0.2f;
    private static final float GRASS_THRESHOLD = 0.3f;
    private static final float HILL_THRESHOLD = 0.6f;
    private static final float MOUNTAIN_THRESHOLD = 0.7f;

    // Moisture thresholds
    private static final float SWAMP_MOISTURE = 0.6f;
    private static final float FOREST_MOISTURE = 0.6f;

    // Temperature thresholds
    private static final float TUNDRA_TEMPERATURE = 0.4f;

    // Variation constants
    private static final float MAX_VARIATION = 0.025f;
    private static final float VARIATION_RANGE = 0.05f;

    /**
     * Determines the appropriate tile type based on environmental factors.
     *
     * @param x X coordinate of the tile
     * @param y Y coordinate of the tile
     * @param height Normalized height value (-1 to 1)
     * @param temperature Normalized temperature (0 to 1)
     * @param moisture Normalized moisture level (0 to 1)
     * @param mapType Biome type for specialized generation
     * @param seed World seed for deterministic variation
     * @return Appropriate TileType for the given conditions
     */
    public static TileType fromHeight(int x, int y, float height, float temperature,
                                      float moisture, MapType mapType, long seed) {
        float variedHeight = applyHeightVariation(x, y, height, seed);

        switch (mapType) {
            case DARK_FOREST:   return createDarkForestTile(variedHeight, moisture);
            case SWAMP:         return createSwampTile(variedHeight, moisture);
            case ISLANDS:       return createIslandsTile(variedHeight);
            case DESERT:        return createDesertTile(variedHeight, moisture);
            case TUNDRA:        return createTundraTile(variedHeight, temperature, moisture);
            case RIVERS:        return createRiverTile(variedHeight, moisture);
            case MEDITERRANEAN: return createMediterraneanTile(variedHeight);
            case MOUNTAINOUS:   return createMountainousTile(variedHeight);
            default:            return createDefaultTile(variedHeight, temperature);
        }
    }

    /**
     * Applies small deterministic variations to break terrain uniformity.
     */
    private static float applyHeightVariation(int x, int y, float height, long seed) {
        // Create deterministic pseudo-random variation based on position and seed
        long hash = seed + 31L * x + 17L * y;
        float variation = (hash % 1001) / 1000f * VARIATION_RANGE - MAX_VARIATION;
        return height + variation;
    }

    /**
     * Tile generation for Dark Forest biome.
     */
    private static TileType createDarkForestTile(float height, float moisture) {
        if (height < DEEP_WATER_THRESHOLD) return TileType.DEEP_WATER;
        if (height < SHALLOW_WATER_THRESHOLD) return TileType.SHALLOW_WATER;
        if (height < SAND_THRESHOLD) return TileType.SAND;
        if (moisture > FOREST_MOISTURE && height < HILL_THRESHOLD) return TileType.FOREST;
        if (height < GRASS_THRESHOLD) return TileType.GRASS;
        if (height < MOUNTAIN_THRESHOLD) return TileType.HILL;
        return TileType.MOUNTAIN;
    }

    /**
     * Tile generation for Swamp biome.
     */
    private static TileType createSwampTile(float height, float moisture) {
        if (height < -0.4f) return TileType.DEEP_WATER;
        if (height < -0.1f) return TileType.SHALLOW_WATER;
        if (moisture > SWAMP_MOISTURE && height < 0.2f) return TileType.SWAMP;
        if (height < GRASS_THRESHOLD) return TileType.GRASS;
        return TileType.FOREST;
    }

    /**
     * Tile generation for Islands biome.
     */
    private static TileType createIslandsTile(float height) {
        if (height < -0.6f) return TileType.DEEP_WATER;
        if (height < -0.4f) return TileType.SHALLOW_WATER;
        if (height < -0.2f) return TileType.SAND;
        if (height < GRASS_THRESHOLD) return TileType.GRASS;
        if (height < MOUNTAIN_THRESHOLD) return TileType.HILL;
        return TileType.MOUNTAIN;
    }

    /**
     * Tile generation for Desert biome.
     */
    private static TileType createDesertTile(float height, float moisture) {
        if (height < 0.15f) return TileType.DEEP_WATER;
        if (height < 0.2f) return TileType.SHALLOW_WATER;
        return height < HILL_THRESHOLD ? TileType.SAND : TileType.HILL;
    }

    /**
     * Tile generation for Tundra biome.
     */
    private static TileType createTundraTile(float height, float temperature, float moisture) {
        if (height < -0.4f) {
            return temperature < 0.35f ? TileType.ICE : TileType.DEEP_WATER;
        } else if (height < -0.2f) {
            return temperature < 0.4f ? TileType.ICE : TileType.SHALLOW_WATER;
        } else if (height < 0.0f) {
            return TileType.ICE; // Costa congelada
        } else if (height > 0.2f) {
            if (temperature < 0.5f || height > 0.5f) {
                return TileType.SNOW;
            }
            // Añadir bosques en tundra con suficiente humedad
            if (moisture > 0.5f && height < 0.4f) {
                return TileType.FOREST;
            }
            return TileType.GRASS;
        }
        // Áreas de transición
        return (temperature < 0.45f) ? TileType.SNOW : TileType.GRASS;
    }

    /**
     * Tile generation for Rivers biome.
     */
    private static TileType createRiverTile(float height, float moisture) {
        if (height < -0.6f) return TileType.DEEP_WATER;
        if (height < -0.5f) return TileType.SHALLOW_WATER;
        if (height < 0.0f) return TileType.SAND;
        if (moisture > SWAMP_MOISTURE && height < 0.3f) return TileType.SWAMP;
        if (height < GRASS_THRESHOLD) return TileType.GRASS;
        return TileType.HILL;
    }

    /**
     * Tile generation for Mediterranean biome.
     */
    private static TileType createMediterraneanTile(float height) {
        if (height < -0.4f) return TileType.DEEP_WATER;
        if (height < -0.2f) return TileType.SHALLOW_WATER;
        if (height < 0.0f) return TileType.SAND;
        if (height < HILL_THRESHOLD) return TileType.GRASS;
        return TileType.HILL;
    }

    /**
     * Tile generation for Mountainous biome.
     */
    private static TileType createMountainousTile(float height) {
        if (height < 0.2f) return TileType.DEEP_WATER;
        if (height < 0.3f) return TileType.SHALLOW_WATER;
        if (height < 0.4f) return TileType.GRASS;
        return TileType.HILL;
    }

    /**
     * Default tile generation algorithm.
     */
    private static TileType createDefaultTile(float height, float temperature) {
        if (height < DEEP_WATER_THRESHOLD) return TileType.DEEP_WATER;
        if (height < SHALLOW_WATER_THRESHOLD) return TileType.SHALLOW_WATER;
        if (height < SAND_THRESHOLD) return TileType.SAND;
        if (height < 0.2f) return (temperature > 0.7f) ? TileType.FOREST : TileType.GRASS;
        if (height < HILL_THRESHOLD) return TileType.HILL;
        return TileType.MOUNTAIN;
    }

}