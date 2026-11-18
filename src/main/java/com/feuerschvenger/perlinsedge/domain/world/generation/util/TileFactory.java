package com.feuerschvenger.perlinsedge.domain.world.generation.util;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/**
 * Factory for creating TileType instances based on environmental parameters.
 * Uses deterministic noise-based variation to break uniformity in terrain generation.
 */
public final class TileFactory {
    // Height thresholds for terrain types
    private static final float DEEP_WATER_THRESHOLD = -0.5F;
    private static final float SHALLOW_WATER_THRESHOLD = -0.3F;
    private static final float SAND_THRESHOLD = -0.2F;
    private static final float GRASS_THRESHOLD = 0.3F;
    private static final float HILL_THRESHOLD = 0.6F;
    private static final float MOUNTAIN_THRESHOLD = 0.7F;

    // Moisture thresholds
    private static final float SWAMP_MOISTURE = 0.6F;
    private static final float FOREST_MOISTURE = 0.6F;

    // Variation constants
    private static final float MAX_VARIATION = 0.025F;
    private static final float VARIATION_RANGE = 0.05F;
    /** Weights of the tile coordinates in the hash that drives the variation. */
    private static final long VARIATION_HASH_X_WEIGHT = 31L;
    private static final long VARIATION_HASH_Y_WEIGHT = 17L;
    /** The hash is cut into this many steps, and the largest step maps to the full variation range. */
    private static final long VARIATION_STEPS = 1001L;
    private static final float VARIATION_LAST_STEP = 1000F;

    // Swamp thresholds
    private static final float SWAMP_DEEP_WATER_THRESHOLD = -0.4F;
    private static final float SWAMP_SHALLOW_WATER_THRESHOLD = -0.1F;
    private static final float SWAMP_LOWLAND_THRESHOLD = 0.2F;

    // Islands thresholds
    private static final float ISLANDS_DEEP_WATER_THRESHOLD = -0.6F;
    private static final float ISLANDS_SHALLOW_WATER_THRESHOLD = -0.4F;
    private static final float ISLANDS_SAND_THRESHOLD = -0.2F;

    // Desert thresholds: the desert floor is high, so its water lies at the top of the range
    private static final float DESERT_DEEP_WATER_THRESHOLD = 0.15F;
    private static final float DESERT_SHALLOW_WATER_THRESHOLD = 0.2F;

    // Tundra thresholds
    private static final float TUNDRA_DEEP_WATER_THRESHOLD = -0.4F;
    private static final float TUNDRA_SHALLOW_WATER_THRESHOLD = -0.2F;
    private static final float TUNDRA_INLAND_THRESHOLD = 0.2F;
    /** Temperatures below which deep and shallow water freeze. */
    private static final float TUNDRA_DEEP_FREEZE_TEMPERATURE = 0.35F;
    private static final float TUNDRA_SHALLOW_FREEZE_TEMPERATURE = 0.4F;
    /** Inland ground is snow below this temperature or above this height. */
    private static final float TUNDRA_SNOW_TEMPERATURE = 0.5F;
    private static final float TUNDRA_SNOW_HEIGHT = 0.5F;
    /** Inland ground wet enough and low enough grows forest. */
    private static final float TUNDRA_FOREST_MOISTURE = 0.5F;
    private static final float TUNDRA_FOREST_MAX_HEIGHT = 0.4F;
    /** The transition ground between the coast and the inland is snow below this temperature. */
    private static final float TUNDRA_TRANSITION_SNOW_TEMPERATURE = 0.45F;

    // Rivers thresholds
    private static final float RIVERS_DEEP_WATER_THRESHOLD = -0.6F;
    private static final float RIVERS_SHALLOW_WATER_THRESHOLD = -0.5F;
    private static final float RIVERS_SWAMP_MAX_HEIGHT = 0.3F;

    // Mediterranean thresholds
    private static final float MEDITERRANEAN_DEEP_WATER_THRESHOLD = -0.4F;
    private static final float MEDITERRANEAN_SHALLOW_WATER_THRESHOLD = -0.2F;

    // Mountainous thresholds: the water lies high, between the peaks
    private static final float MOUNTAINOUS_DEEP_WATER_THRESHOLD = 0.2F;
    private static final float MOUNTAINOUS_SHALLOW_WATER_THRESHOLD = 0.3F;
    private static final float MOUNTAINOUS_GRASS_THRESHOLD = 0.4F;

    // Default biome thresholds
    private static final float DEFAULT_LOWLAND_THRESHOLD = 0.2F;
    private static final float DEFAULT_FOREST_TEMPERATURE = 0.7F;

    private TileFactory() {
        throw new AssertionError("TileFactory is a utility class and should not be instantiated");
    }

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

        return switch (mapType) {
            case DENSE_FOREST -> createDenseForestTile(variedHeight, moisture);
            case SWAMP -> createSwampTile(variedHeight, moisture);
            case ISLANDS -> createIslandsTile(variedHeight);
            case DESERT -> createDesertTile(variedHeight);
            case TUNDRA -> createTundraTile(variedHeight, temperature, moisture);
            case RIVERS -> createRiverTile(variedHeight, moisture);
            case MEDITERRANEAN -> createMediterraneanTile(variedHeight);
            case MOUNTAINOUS -> createMountainousTile(variedHeight);
            default -> createDefaultTile(variedHeight, temperature);
        };
    }

    /**
     * Applies small deterministic variations to break terrain uniformity.
     */
    private static float applyHeightVariation(int x, int y, float height, long seed) {
        // Create deterministic pseudo-random variation based on position and seed
        long hash = seed + VARIATION_HASH_X_WEIGHT * x + VARIATION_HASH_Y_WEIGHT * y;
        float variation = (hash % VARIATION_STEPS) / VARIATION_LAST_STEP * VARIATION_RANGE - MAX_VARIATION;
        return height + variation;
    }

    /**
     * Tile generation for Dense Forest biome.
     */
    private static TileType createDenseForestTile(float height, float moisture) {
        if (height < DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        if (height < SAND_THRESHOLD) {
            return TileType.SAND;
        }
        if (moisture > FOREST_MOISTURE && height < HILL_THRESHOLD) {
            return TileType.FOREST;
        }
        if (height < GRASS_THRESHOLD) {
            return TileType.GRASS;
        }
        if (height < MOUNTAIN_THRESHOLD) {
            return TileType.HILL;
        }
        return TileType.MOUNTAIN;
    }

    /**
     * Tile generation for Swamp biome.
     */
    private static TileType createSwampTile(float height, float moisture) {
        if (height < SWAMP_DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < SWAMP_SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        if (moisture > SWAMP_MOISTURE && height < SWAMP_LOWLAND_THRESHOLD) {
            return TileType.SWAMP;
        }
        if (height < GRASS_THRESHOLD) {
            return TileType.GRASS;
        }
        return TileType.FOREST;
    }

    /**
     * Tile generation for Islands biome.
     */
    private static TileType createIslandsTile(float height) {
        if (height < ISLANDS_DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < ISLANDS_SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        if (height < ISLANDS_SAND_THRESHOLD) {
            return TileType.SAND;
        }
        if (height < GRASS_THRESHOLD) {
            return TileType.GRASS;
        }
        if (height < MOUNTAIN_THRESHOLD) {
            return TileType.HILL;
        }
        return TileType.MOUNTAIN;
    }

    /**
     * Tile generation for Desert biome.
     */
    private static TileType createDesertTile(float height) {
        if (height < DESERT_DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < DESERT_SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        return height < HILL_THRESHOLD ? TileType.SAND : TileType.HILL;
    }

    /**
     * Tile generation for Tundra biome.
     */
    private static TileType createTundraTile(float height, float temperature, float moisture) {
        if (height < TUNDRA_DEEP_WATER_THRESHOLD) {
            return temperature < TUNDRA_DEEP_FREEZE_TEMPERATURE ? TileType.ICE : TileType.DEEP_WATER;
        } else if (height < TUNDRA_SHALLOW_WATER_THRESHOLD) {
            return temperature < TUNDRA_SHALLOW_FREEZE_TEMPERATURE ? TileType.ICE : TileType.SHALLOW_WATER;
        } else if (height < 0.0F) {
            return TileType.ICE; // Costa congelada
        } else if (height > TUNDRA_INLAND_THRESHOLD) {
            if (temperature < TUNDRA_SNOW_TEMPERATURE || height > TUNDRA_SNOW_HEIGHT) {
                return TileType.SNOW;
            }
            // Añadir bosques en tundra con suficiente humedad
            if (moisture > TUNDRA_FOREST_MOISTURE && height < TUNDRA_FOREST_MAX_HEIGHT) {
                return TileType.FOREST;
            }
            return TileType.GRASS;
        }
        // Áreas de transición
        return (temperature < TUNDRA_TRANSITION_SNOW_TEMPERATURE) ? TileType.SNOW : TileType.GRASS;
    }

    /**
     * Tile generation for Rivers biome.
     */
    private static TileType createRiverTile(float height, float moisture) {
        if (height < RIVERS_DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < RIVERS_SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        if (height < 0.0F) {
            return TileType.SAND;
        }
        if (moisture > SWAMP_MOISTURE && height < RIVERS_SWAMP_MAX_HEIGHT) {
            return TileType.SWAMP;
        }
        if (height < GRASS_THRESHOLD) {
            return TileType.GRASS;
        }
        return TileType.HILL;
    }

    /**
     * Tile generation for Mediterranean biome.
     */
    private static TileType createMediterraneanTile(float height) {
        if (height < MEDITERRANEAN_DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < MEDITERRANEAN_SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        if (height < 0.0F) {
            return TileType.SAND;
        }
        if (height < HILL_THRESHOLD) {
            return TileType.GRASS;
        }
        return TileType.HILL;
    }

    /**
     * Tile generation for Mountainous biome.
     */
    private static TileType createMountainousTile(float height) {
        if (height < MOUNTAINOUS_DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < MOUNTAINOUS_SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        if (height < MOUNTAINOUS_GRASS_THRESHOLD) {
            return TileType.GRASS;
        }
        return TileType.HILL;
    }

    /**
     * Default tile generation algorithm.
     */
    private static TileType createDefaultTile(float height, float temperature) {
        if (height < DEEP_WATER_THRESHOLD) {
            return TileType.DEEP_WATER;
        }
        if (height < SHALLOW_WATER_THRESHOLD) {
            return TileType.SHALLOW_WATER;
        }
        if (height < SAND_THRESHOLD) {
            return TileType.SAND;
        }
        if (height < DEFAULT_LOWLAND_THRESHOLD) {
            return (temperature > DEFAULT_FOREST_TEMPERATURE) ? TileType.FOREST : TileType.GRASS;
        }
        if (height < HILL_THRESHOLD) {
            return TileType.HILL;
        }
        return TileType.MOUNTAIN;
    }

}
