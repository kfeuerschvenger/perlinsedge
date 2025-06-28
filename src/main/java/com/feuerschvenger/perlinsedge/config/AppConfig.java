package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLite;

import java.util.Random;

/**
 * Centralized configuration hub with nested configuration classes.
 * Uses singleton pattern for global access to application settings.
 */
public class AppConfig {
    private static AppConfig instance;

    // Configuration sections
    private final CoreConfig core = new CoreConfig();
    private final WorldConfig world = new WorldConfig();
    private final GraphicsConfig graphics = new GraphicsConfig();
    private final PlayerConfig player = new PlayerConfig();
    private final DebugConfig debug = new DebugConfig();
    private final GameMechanicsConfig gameMechanics = new GameMechanicsConfig();
    private final GenerationConfig generationConfig = new GenerationConfig();

    private AppConfig() {}

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    // Configuration accessors
    public CoreConfig core() { return core; }
    public WorldConfig world() { return world; }
    public GraphicsConfig graphics() { return graphics; }
    public PlayerConfig player() { return player; }
    public DebugConfig debug() { return debug; }
    public GameMechanicsConfig gameMechanics() { return gameMechanics; }
    public GenerationConfig generationConfig() { return generationConfig; }

    // ==================================================================
    //  CONFIGURATION CLASSES
    // ==================================================================

    /**
     * Core application settings (window, zoom, navigation)
     */
    public static class CoreConfig {
        private static final boolean fullscreen = true;
        private static final int windowWidth = 1024;
        private static final int windowHeight = 768;
        private static final double initialZoom = 1.0;
        private static final double minZoom = 1.0;
        private static final double maxZoom = 2.0;
        private static final double panSpeed = 30.0;

        // Getters
        public boolean isFullscreen() { return fullscreen; }
        public int getWindowWidth() { return windowWidth; }
        public int getWindowHeight() { return windowHeight; }
        public double getInitialZoom() { return initialZoom; }
        public double getMinZoom() { return minZoom; }
        public double getMaxZoom() { return maxZoom; }
        public double getPanSpeed() { return panSpeed; }
    }

    /**
     * World generation parameters (terrain, resources, enemies)
     */
    public static class WorldConfig {
        // Dimensions
        private static final int width = 150;
        private static final int height = 150;

        // Map properties
        private long defaultSeed = System.currentTimeMillis();
        private MapType currentMapType = MapType.DARK_FOREST;

        // Enemy settings
        private static final int baseSlimesCount = 30;
        private static final int baseMimicsCount = 3;
        private static final int minEnemyDistance = 30;
        private static final int maxPositioningAttempts = 3000;

        // Noise parameters
        private static final float heightNoiseFrequency = 0.01f;
        private static final int heightNoiseOctaves = 3;
        private static final float temperatureNoiseFrequency = 0.02f;
        private static final int temperatureNoiseOctaves = 3;
        private static final float moistureNoiseFrequency = 0.03f;
        private static final int moistureNoiseOctaves = 3;

        // Resource thresholds
        private static final float stonePatchFrequency = 0.06f;
        private static final float stoneThreshold = 0.79f;
        private static final float crystalsPatchFrequency = 0.09f;
        private static final float crystalsThreshold = 0.80f;
        private static final float baseTreeDensity = 0.05f;
        private static final float baseRiverDensity = 0.03f;

        // Getters and setters
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public long getDefaultSeed() { return defaultSeed; }
        public void setDefaultSeed(long seed) { this.defaultSeed = seed; }
        public MapType getCurrentMapType() { return currentMapType; }
        public void setCurrentMapType(MapType mapType) { this.currentMapType = mapType; }
        public int getBaseSlimesCount() { return baseSlimesCount; }
        public int getBaseMimicsCount() { return baseMimicsCount; }
        public int getMinEnemyDistance() { return minEnemyDistance; }
        public int getMaxPositioningAttempts() { return maxPositioningAttempts; }
        public float getHeightNoiseFrequency() { return heightNoiseFrequency; }
        public int getHeightNoiseOctaves() { return heightNoiseOctaves; }
        public float getTemperatureNoiseFrequency() { return temperatureNoiseFrequency; }
        public int getTemperatureNoiseOctaves() { return temperatureNoiseOctaves; }
        public float getMoistureNoiseFrequency() { return moistureNoiseFrequency; }
        public int getMoistureNoiseOctaves() { return moistureNoiseOctaves; }
        public float getStonePatchFrequency() { return stonePatchFrequency; }
        public float getStoneThreshold() { return stoneThreshold; }
        public float getCrystalsPatchFrequency() { return crystalsPatchFrequency; }
        public float getCrystalsThreshold() { return crystalsThreshold; }

        public float getTreeDensity() {
            return switch (currentMapType) {
                case DARK_FOREST -> 0.478f;
                case SWAMP -> 0.35f;
                case DESERT -> 0.25f;
                case TUNDRA -> 0.23f;
                case MEDITERRANEAN -> 0.45f;  // Aumentado de 0.05f
                case ISLANDS -> 0.35f;        // Aumentado para islas
                default -> baseTreeDensity;
            };
        }

        public float getRiverDensity() {
            return switch (currentMapType) {
                case DARK_FOREST -> 0.01f;
                case SWAMP -> 0.025f;
                case DESERT -> 0.003f;
                case RIVERS -> 0.04f;
                default -> baseRiverDensity;
            };
        }
    }

    /**
     * Rendering parameters (tile properties, minimap)
     */
    public static class GraphicsConfig {
        // Tile dimensions
        private static final int tileWidth = 64;
        private static final int tileHeight = 32;
        private static final int tileHalfWidth = tileWidth / 2;
        private static final int tileHalfHeight = tileHeight / 2;

        // Visual properties
        private static final double tileVisualBaseHeight = 8;
        private static final double minimapWidth = 300;
        private static final double minimapHeight = 180;

        // Getters
        public int getTileWidth() { return tileWidth; }
        public int getTileHeight() { return tileHeight; }
        public int getTileHalfWidth() { return tileHalfWidth; }
        public int getTileHalfHeight() { return tileHalfHeight; }
        public double getTileVisualBaseHeight() { return tileVisualBaseHeight; }
        public double getMinimapWidth() { return minimapWidth; }
        public double getMinimapHeight() { return minimapHeight; }
    }

    /**
     * Player-specific settings (sprite configuration)
     */
    public static class PlayerConfig {
        public final SpriteSheetConfig spriteSheet = new SpriteSheetConfig();

        public static class SpriteSheetConfig {
            // Sprite sheet properties
            public final String path = "/assets/entities/player/8-Direction-Character.png";
            public final int spriteWidth = 13;
            public final int spriteHeight = 17;
            public final int columns = 8;
            public final int rows = 3;
            public final int startX = 1;
            public final int startY = 30;
            public final int columnOffset = 16;
            public final int[] rowYCoordinates = {30, 53, 78};
        }
    }

    /**
     * Debugging and developer tools
     */
    public static class DebugConfig {
        private boolean enabled = false;
        private boolean grayscaleEnabled = false;
        private boolean textEnabled = false;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isGrayscaleEnabled() { return grayscaleEnabled; }
        public void setGrayscaleEnabled(boolean enabled) { this.grayscaleEnabled = enabled; }
        public boolean isTextEnabled() { return textEnabled; }
        public void setTextEnabled(boolean enabled) { this.textEnabled = enabled; }
    }

    /**
     * Game mechanics configuration (health, loot tables)
     */
    public static class GameMechanicsConfig {
        // Resource health
        private static final int TREE_MAX_HEALTH = 30;
        private static final int ROCK_MAX_HEALTH = 40;
        private static final int CRYSTAL_MAX_HEALTH = 25;
        private static final int DEFAULT_MAX_HEALTH = 50;

        // Cooldowns
        private static final long resourceHitCooldownMs = 300;

        public int getResourceBaseHealth(ResourceType type) {
            return switch (type) {
                case TREE -> TREE_MAX_HEALTH;
                case STONE -> ROCK_MAX_HEALTH;
                case CRYSTALS -> CRYSTAL_MAX_HEALTH;
                default -> DEFAULT_MAX_HEALTH;
            };
        }

        public long getResourceHitCooldownMs() { return resourceHitCooldownMs; }
    }

    /**
     * Configuration for noise generators
     */
    public static class NoiseConfig {
        float frequency;
        int octaves;
        FastNoiseLite.FractalType fractalType;
        float gain;
        float lacunarity;

        public float getFrequency() {
            return frequency;
        }

        public void setFrequency(float frequency) {
            this.frequency = frequency;
        }

        public int getOctaves() {
            return octaves;
        }

        public void setOctaves(int octaves) {
            this.octaves = octaves;
        }

        public FastNoiseLite.FractalType getFractalType() {
            return fractalType;
        }

        public void setFractalType(FastNoiseLite.FractalType fractalType) {
            this.fractalType = fractalType;
        }

        public float getGain() {
            return gain;
        }

        public void setGain(float gain) {
            this.gain = gain;
        }

        public float getLacunarity() {
            return lacunarity;
        }

        public void setLacunarity(float lacunarity) {
            this.lacunarity = lacunarity;
        }
    }
    
    /**
     * Holds current generation parameters
     */
    public static class GenerationConfig {
        MapType mapType;
        long seed;
        Random random;
        float treeDensity;
        float riverDensity;
        float stoneThreshold;
        float crystalsThreshold;

        // Noise configurations
        NoiseConfig heightNoiseConfig;
        NoiseConfig temperatureNoiseConfig;
        NoiseConfig moistureNoiseConfig;
        NoiseConfig stoneNoiseConfig;
        NoiseConfig crystalsNoiseConfig;
        NoiseConfig treeNoiseConfig;

        public void update(MapGenerationContext context) {
            this.mapType = context.getMapType();
            this.seed = context.getSeed();
            this.random = new Random(seed);
            this.treeDensity = context.getTreeDensity();
            this.riverDensity = context.getRiverDensity();
            this.stoneThreshold = context.getStoneThreshold();
            this.crystalsThreshold = context.getCrystalsThreshold();

            // Configure noise parameters
            this.heightNoiseConfig = createHeightNoiseConfig();
            this.temperatureNoiseConfig = createNoiseConfig(
                    context.getTemperatureNoiseFrequency(),
                    context.getTemperatureNoiseOctaves()
            );
            this.moistureNoiseConfig = createNoiseConfig(
                    context.getMoistureNoiseFrequency(),
                    context.getMoistureNoiseOctaves()
            );
            this.stoneNoiseConfig = createSimpleNoiseConfig(context.getStonePatchFrequency());
            this.crystalsNoiseConfig = createSimpleNoiseConfig(context.getCrystalsPatchFrequency());
            this.treeNoiseConfig = createTreeNoiseConfig();
        }

        private NoiseConfig createHeightNoiseConfig() {
            NoiseConfig cfg = new NoiseConfig();
            cfg.frequency = 0.02f; // Default

            switch (mapType) {
                case ISLANDS:
                    cfg.frequency = 0.005f;
                    cfg.octaves = 5;
                    cfg.fractalType = FastNoiseLite.FractalType.FBm;
                    break;
                case DESERT:
                    cfg.frequency = 0.04f;
                    cfg.octaves = 6;
                    cfg.fractalType = FastNoiseLite.FractalType.Ridged;
                    cfg.gain = 0.6f;
                    break;
                case MOUNTAINOUS:
                    cfg.frequency = 0.01f;
                    cfg.octaves = 8;
                    cfg.fractalType = FastNoiseLite.FractalType.Ridged;
                    cfg.gain = 0.7f;
                    break;
                case DARK_FOREST:
                case SWAMP:
                    cfg.frequency = 0.02f;
                    cfg.octaves = 4;
                    cfg.fractalType = FastNoiseLite.FractalType.FBm;
                    cfg.gain = 0.4f;
                    break;
                case TUNDRA:
                    cfg.frequency = 0.015f;
                    cfg.octaves = 5;
                    cfg.fractalType = FastNoiseLite.FractalType.FBm;
                    cfg.gain = 0.5f;
                    break;
                case RIVERS:
                    cfg.frequency = 0.01f;
                    cfg.octaves = 6;
                    cfg.fractalType = FastNoiseLite.FractalType.FBm;
                    cfg.gain = 0.4f;
                    break;
                case MEDITERRANEAN:
                    cfg.frequency = 0.025f;
                    cfg.octaves = 5;
                    cfg.fractalType = FastNoiseLite.FractalType.FBm;
                    cfg.gain = 0.5f;
                    break;
            }
            cfg.lacunarity = 2.0f;
            return cfg;
        }

        private NoiseConfig createTreeNoiseConfig() {
            NoiseConfig cfg = new NoiseConfig();
            cfg.frequency = (mapType == MapType.DESERT || mapType == MapType.TUNDRA) ? 0.01f : 0.05f;
            return cfg;
        }

        private NoiseConfig createNoiseConfig(float frequency, int octaves) {
            NoiseConfig cfg = new NoiseConfig();
            cfg.frequency = frequency;
            cfg.octaves = octaves;
            cfg.fractalType = FastNoiseLite.FractalType.FBm;
            cfg.gain = 0.5f;
            cfg.lacunarity = 2.0f;
            return cfg;
        }

        private NoiseConfig createSimpleNoiseConfig(float frequency) {
            NoiseConfig cfg = new NoiseConfig();
            cfg.frequency = frequency;
            return cfg;
        }

        public MapType getMapType() {
            return mapType;
        }

        public void setMapType(MapType mapType) {
            this.mapType = mapType;
        }

        public long getSeed() {
            return seed;
        }

        public void setSeed(long seed) {
            this.seed = seed;
        }

        public Random getRandom() {
            return random;
        }

        public void setRandom(Random random) {
            this.random = random;
        }

        public float getTreeDensity() {
            return treeDensity;
        }

        public void setTreeDensity(float treeDensity) {
            this.treeDensity = treeDensity;
        }

        public float getRiverDensity() {
            return riverDensity;
        }

        public void setRiverDensity(float riverDensity) {
            this.riverDensity = riverDensity;
        }

        public float getStoneThreshold() {
            return stoneThreshold;
        }

        public void setStoneThreshold(float stoneThreshold) {
            this.stoneThreshold = stoneThreshold;
        }

        public float getCrystalsThreshold() {
            return crystalsThreshold;
        }

        public void setCrystalsThreshold(float crystalsThreshold) {
            this.crystalsThreshold = crystalsThreshold;
        }

        public NoiseConfig getHeightNoiseConfig() {
            return heightNoiseConfig;
        }

        public void setHeightNoiseConfig(NoiseConfig heightNoiseConfig) {
            this.heightNoiseConfig = heightNoiseConfig;
        }

        public NoiseConfig getTemperatureNoiseConfig() {
            return temperatureNoiseConfig;
        }

        public void setTemperatureNoiseConfig(NoiseConfig temperatureNoiseConfig) {
            this.temperatureNoiseConfig = temperatureNoiseConfig;
        }

        public NoiseConfig getMoistureNoiseConfig() {
            return moistureNoiseConfig;
        }

        public void setMoistureNoiseConfig(NoiseConfig moistureNoiseConfig) {
            this.moistureNoiseConfig = moistureNoiseConfig;
        }

        public NoiseConfig getStoneNoiseConfig() {
            return stoneNoiseConfig;
        }

        public void setStoneNoiseConfig(NoiseConfig stoneNoiseConfig) {
            this.stoneNoiseConfig = stoneNoiseConfig;
        }

        public NoiseConfig getCrystalsNoiseConfig() {
            return crystalsNoiseConfig;
        }

        public void setCrystalsNoiseConfig(NoiseConfig crystalsNoiseConfig) {
            this.crystalsNoiseConfig = crystalsNoiseConfig;
        }

        public NoiseConfig getTreeNoiseConfig() {
            return treeNoiseConfig;
        }

        public void setTreeNoiseConfig(NoiseConfig treeNoiseConfig) {
            this.treeNoiseConfig = treeNoiseConfig;
        }
    }

}