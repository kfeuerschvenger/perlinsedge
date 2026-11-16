package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycleSettings;

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
    private final DayCycleConfig dayCycle = new DayCycleConfig();
    private final PlayerConfig player = new PlayerConfig();
    private final DebugConfig debug = new DebugConfig();
    private final GameMechanicsConfig gameMechanics = new GameMechanicsConfig();
    private final GenerationConfig generationConfig = new GenerationConfig();
    private final EnemyConfig enemies = new EnemyConfig();

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
    public DayCycleConfig dayCycle() { return dayCycle; }
    public PlayerConfig player() { return player; }
    public DebugConfig debug() { return debug; }
    public GameMechanicsConfig gameMechanics() { return gameMechanics; }
    public EnemyConfig enemies() { return enemies; }
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
        private static final double zoomStep = 0.5;
        private static final double panSpeed = 30.0;

        // Getters
        public boolean isFullscreen() { return fullscreen; }
        public int getWindowWidth() { return windowWidth; }
        public int getWindowHeight() { return windowHeight; }
        public double getInitialZoom() { return initialZoom; }
        public double getMinZoom() { return minZoom; }
        public double getMaxZoom() { return maxZoom; }
        /** @return the distance between two zoom levels */
        public double getZoomStep() { return zoomStep; }
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
        private MapType currentMapType = MapType.DENSE_FOREST;

        // Enemy settings
        private static final int baseSlimesCount = 30;
        private static final int baseMimicsCount = 3;
        private static final int baseSkeletonsCount = 6;
        private static final int minEnemyDistance = 30;
        private static final int maxPositioningAttempts = 3000;

        // Noise parameters (the height noise comes from NoiseSettings.heightProfile)
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
        public int getBaseSkeletonsCount() { return baseSkeletonsCount; }
        public int getMinEnemyDistance() { return minEnemyDistance; }
        public int getMaxPositioningAttempts() { return maxPositioningAttempts; }
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
                case DENSE_FOREST -> 0.32f;
                case SWAMP -> 0.32f;
                case DESERT -> 0.10f;
                case TUNDRA -> 0.34f;
                case MEDITERRANEAN -> 0.17f;
                case ISLANDS -> 0.19f;
                case RIVERS -> 0.30f;
                default -> baseTreeDensity;
            };
        }

        public float getRiverDensity() {
            return switch (currentMapType) {
                case DENSE_FOREST -> 0.01f;
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
     * The day and night cycle: the length of each phase in game seconds, where a new world starts, how the shadows of
     * the sun are quantised and the fast time of the debug key.
     */
    public static class DayCycleConfig {
        // Minecraft proportions: a cycle of twenty minutes
        private static final double DAWN_SECONDS = 90;
        private static final double DAY_SECONDS = 600;
        private static final double DUSK_SECONDS = 90;
        private static final double NIGHT_SECONDS = 420;
        /** A new world starts half a minute after the dawn has ended. */
        private static final double START_SECONDS = 120;
        private static final int SHADOW_STEPS = 16;
        private static final double MIN_SHADOW_LENGTH = 0.6;
        private static final double MAX_SHADOW_LENGTH = 2.0;
        /** Real seconds a whole day lasts while the fast time of the debug key is on. */
        private static final double FAST_DAY_REAL_SECONDS = 40;

        private boolean fastTime;

        /** @return the settings a {@link DayCycle} runs on */
        public DayCycleSettings settings() {
            return new DayCycleSettings(DAWN_SECONDS, DAY_SECONDS, DUSK_SECONDS, NIGHT_SECONDS, START_SECONDS,
                    SHADOW_STEPS, MIN_SHADOW_LENGTH, MAX_SHADOW_LENGTH);
        }

        /** @return how many game seconds pass per real second while the fast time is on */
        public double getFastTimeMultiplier() {
            return (DAWN_SECONDS + DAY_SECONDS + DUSK_SECONDS + NIGHT_SECONDS) / FAST_DAY_REAL_SECONDS;
        }

        /** @return true while the fast time of the debug key is on */
        public boolean isFastTime() { return fastTime; }

        /** @param fastTime true to make the day pass in about forty real seconds */
        public void setFastTime(boolean fastTime) { this.fastTime = fastTime; }

        /** @return the game seconds that pass per real second: one normally, the fast multiplier with the fast time on */
        public double timeScale() { return fastTime ? getFastTimeMultiplier() : 1.0; }
    }

    /**
     * Player-specific settings (sprite configuration)
     */
    public static class PlayerConfig {
        public final SpriteSheetConfig spriteSheet = new SpriteSheetConfig();

        public static class SpriteSheetConfig {
            // Sprite sheet properties: 8 direction columns by 3 frame rows of 16x24 cells
            public final String path = "/assets/entities/player.png";
            public final int spriteWidth = 16;
            public final int spriteHeight = 24;
            public final int columns = 8;
            public final int rows = 3;
            public final int startX = 0;
            public final int startY = 0;
            public final int columnOffset = 16;
            public final int[] rowYCoordinates = {0, 24, 48};
        }
    }

    /**
     * Debugging and developer tools
     */
    public static class DebugConfig {
        private boolean enabled = false;
        private boolean grayscaleEnabled = false;
        private boolean textEnabled = false;
        private boolean fpsOverlayEnabled = false;

        // Getters and setters
        public boolean isFpsOverlayEnabled() { return fpsOverlayEnabled; }
        public void setFpsOverlayEnabled(boolean enabled) { this.fpsOverlayEnabled = enabled; }
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
        // Resource health, balanced against the damage table of ToolStats so the right tool breaks a resource in three
        // hits and a bare hand takes about eight where a hand can do it at all
        private static final int TREE_MAX_HEALTH = 30;
        private static final int ROCK_MAX_HEALTH = 30;
        private static final int CRYSTAL_MAX_HEALTH = 25;
        private static final int IRON_ORE_MAX_HEALTH = 36;
        private static final int DEFAULT_MAX_HEALTH = 50;

        // Slime strike cycle: it squashes in front of the player, strikes, then waits before it can squash again
        private static final double SLIME_WINDUP_SECONDS = 0.6;
        private static final double SLIME_STRIKE_WAIT_SECONDS = 1.5;

        /** @return seconds a slime squashes in front of the player before it strikes */
        public double getSlimeWindupSeconds() { return SLIME_WINDUP_SECONDS; }

        /** @return seconds a slime waits after a strike before it starts the next squash */
        public double getSlimeStrikeWaitSeconds() { return SLIME_STRIKE_WAIT_SECONDS; }

        public int getResourceBaseHealth(ResourceType type) {
            return switch (type) {
                case TREE -> TREE_MAX_HEALTH;
                case STONE -> ROCK_MAX_HEALTH;
                case CRYSTALS -> CRYSTAL_MAX_HEALTH;
                case IRON_ORE -> IRON_ORE_MAX_HEALTH;
                default -> DEFAULT_MAX_HEALTH;
            };
        }
    }

    /**
     * How far each enemy type notices the player and how far it follows before giving up.
     */
    public static class EnemyConfig {
        /**
         * The three distances that govern one enemy's chase.
         *
         * @param aggroTiles  distance in tiles within which the enemy starts chasing the player
         * @param leashTiles  distance in tiles beyond which a chasing enemy gives up at once, larger than aggroTiles so
         *                    an enemy at the edge of its range does not flip between chasing and idling
         * @param loseSeconds seconds a chasing enemy keeps going while the player is outside aggroTiles, before it
         *                    gives up
         */
        public record ChaseRange(double aggroTiles, double leashTiles, double loseSeconds) {
            /** Validates that the leash encloses the aggro range. */
            public ChaseRange {
                if (aggroTiles <= 0 || leashTiles <= aggroTiles || loseSeconds < 0) {
                    throw new IllegalArgumentException("need 0 < aggroTiles < leashTiles and loseSeconds >= 0");
                }
            }
        }

        private static final ChaseRange SLIME = new ChaseRange(8.0, 12.0, 3.0);
        private static final ChaseRange SKELETON = new ChaseRange(10.0, 15.0, 4.0);
        private static final ChaseRange MIMIC = new ChaseRange(2.5, 8.0, 3.0);

        /** @return the chase distances of a slime */
        public ChaseRange slime() { return SLIME; }

        /** @return the chase distances of a skeleton */
        public ChaseRange skeleton() { return SKELETON; }

        /** @return the chase distances of a mimic, whose aggro range is the distance that wakes it */
        public ChaseRange mimic() { return MIMIC; }
    }

    /**
     * Configuration for noise generators
     */
    public static class NoiseConfig {
        float frequency;
        int octaves;
        FractalKind fractalType = FractalKind.NONE;
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

        public FractalKind getFractalType() {
            return fractalType;
        }

        public void setFractalType(FractalKind fractalType) {
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
            this.heightNoiseConfig = createHeightNoiseConfig(
                    context.getHeightNoiseFrequency(),
                    context.getHeightNoiseOctaves()
            );
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

        /**
         * Height noise: the frequency and octaves come from the context, so the tuning panel affects the terrain,
         * while the fractal kind, gain and lacunarity come from the map type profile.
         */
        private NoiseConfig createHeightNoiseConfig(float frequency, int octaves) {
            NoiseSettings profile = NoiseSettings.heightProfile(mapType);
            NoiseConfig cfg = new NoiseConfig();
            cfg.frequency = frequency;
            cfg.octaves = octaves;
            cfg.fractalType = profile.fractal();
            cfg.gain = profile.gain();
            cfg.lacunarity = profile.lacunarity();
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
            cfg.fractalType = FractalKind.FBM;
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