package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.Flicker;
import com.feuerschvenger.perlinsedge.domain.world.light.HeldTorch;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycleSettings;

import java.util.Random;

/**
 * Centralized configuration hub with nested configuration classes.
 * Uses singleton pattern for global access to application settings.
 */
public class AppConfig {
    /** Classpath folder every bundled asset lives under. */
    public static final String ASSET_ROOT = "/assets";

    private static AppConfig instance;

    // Configuration sections
    private final CoreConfig core = new CoreConfig();
    private final WorldConfig world = new WorldConfig();
    private final GraphicsConfig graphics = new GraphicsConfig();
    private final DayCycleConfig dayCycle = new DayCycleConfig();
    private final LightsConfig lights = new LightsConfig();
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
    public LightsConfig lights() { return lights; }
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
        private static final boolean FULLSCREEN = true;
        private static final int WINDOW_WIDTH = 1024;
        private static final int WINDOW_HEIGHT = 768;
        private static final double INITIAL_ZOOM = 1.0;
        private static final double MIN_ZOOM = 1.0;
        private static final double MAX_ZOOM = 2.0;
        private static final double ZOOM_STEP = 0.5;
        private static final double PAN_SPEED = 30.0;

        // Getters
        public boolean isFullscreen() { return FULLSCREEN; }
        public int getWindowWidth() { return WINDOW_WIDTH; }
        public int getWindowHeight() { return WINDOW_HEIGHT; }
        public double getInitialZoom() { return INITIAL_ZOOM; }
        public double getMinZoom() { return MIN_ZOOM; }
        public double getMaxZoom() { return MAX_ZOOM; }
        /** @return the distance between two zoom levels */
        public double getZoomStep() { return ZOOM_STEP; }
        public double getPanSpeed() { return PAN_SPEED; }
    }

    /**
     * World generation parameters (terrain, resources, enemies)
     */
    public static class WorldConfig {
        // Dimensions
        private static final int WIDTH = 150;
        private static final int HEIGHT = 150;

        // Map properties
        private long defaultSeed = System.currentTimeMillis();
        private MapType currentMapType = MapType.DENSE_FOREST;

        // Enemy settings
        private static final int BASE_SLIMES_COUNT = 30;
        private static final int BASE_MIMICS_COUNT = 3;
        private static final int BASE_SKELETONS_COUNT = 6;
        private static final int MIN_ENEMY_DISTANCE = 30;
        private static final int MAX_POSITIONING_ATTEMPTS = 3000;

        // Noise parameters (the height noise comes from NoiseSettings.heightProfile)
        private static final float TEMPERATURE_NOISE_FREQUENCY = 0.02f;
        private static final int TEMPERATURE_NOISE_OCTAVES = 3;
        private static final float MOISTURE_NOISE_FREQUENCY = 0.03f;
        private static final int MOISTURE_NOISE_OCTAVES = 3;

        // Resource thresholds
        private static final float STONE_PATCH_FREQUENCY = 0.06f;
        private static final float STONE_THRESHOLD = 0.79f;
        private static final float CRYSTALS_PATCH_FREQUENCY = 0.09f;
        private static final float CRYSTALS_THRESHOLD = 0.80f;
        private static final float BASE_TREE_DENSITY = 0.05f;
        private static final float BASE_RIVER_DENSITY = 0.03f;

        // Getters and setters
        public int getWidth() { return WIDTH; }
        public int getHeight() { return HEIGHT; }
        public long getDefaultSeed() { return defaultSeed; }
        public void setDefaultSeed(long seed) { this.defaultSeed = seed; }
        public MapType getCurrentMapType() { return currentMapType; }
        public void setCurrentMapType(MapType mapType) { this.currentMapType = mapType; }
        public int getBaseSlimesCount() { return BASE_SLIMES_COUNT; }
        public int getBaseMimicsCount() { return BASE_MIMICS_COUNT; }
        public int getBaseSkeletonsCount() { return BASE_SKELETONS_COUNT; }
        public int getMinEnemyDistance() { return MIN_ENEMY_DISTANCE; }
        public int getMaxPositioningAttempts() { return MAX_POSITIONING_ATTEMPTS; }
        public float getTemperatureNoiseFrequency() { return TEMPERATURE_NOISE_FREQUENCY; }
        public int getTemperatureNoiseOctaves() { return TEMPERATURE_NOISE_OCTAVES; }
        public float getMoistureNoiseFrequency() { return MOISTURE_NOISE_FREQUENCY; }
        public int getMoistureNoiseOctaves() { return MOISTURE_NOISE_OCTAVES; }
        public float getStonePatchFrequency() { return STONE_PATCH_FREQUENCY; }
        public float getStoneThreshold() { return STONE_THRESHOLD; }
        public float getCrystalsPatchFrequency() { return CRYSTALS_PATCH_FREQUENCY; }
        public float getCrystalsThreshold() { return CRYSTALS_THRESHOLD; }

        public float getTreeDensity() {
            return switch (currentMapType) {
                case DENSE_FOREST -> 0.32f;
                case SWAMP -> 0.32f;
                case DESERT -> 0.10f;
                case TUNDRA -> 0.34f;
                case MEDITERRANEAN -> 0.17f;
                case ISLANDS -> 0.19f;
                case RIVERS -> 0.30f;
                default -> BASE_TREE_DENSITY;
            };
        }

        public float getRiverDensity() {
            return switch (currentMapType) {
                case DENSE_FOREST -> 0.01f;
                case SWAMP -> 0.025f;
                case DESERT -> 0.003f;
                case RIVERS -> 0.04f;
                default -> BASE_RIVER_DENSITY;
            };
        }
    }

    /**
     * Rendering parameters (tile properties, minimap)
     */
    public static class GraphicsConfig {
        // Tile dimensions
        private static final int TILE_WIDTH = 64;
        private static final int TILE_HEIGHT = 32;
        private static final int TILE_HALF_WIDTH = TILE_WIDTH / 2;
        private static final int TILE_HALF_HEIGHT = TILE_HEIGHT / 2;

        // Visual properties
        private static final double TILE_VISUAL_BASE_HEIGHT = 8;
        private static final double MINIMAP_WIDTH = 300;
        private static final double MINIMAP_HEIGHT = 180;

        // Getters
        public int getTileWidth() { return TILE_WIDTH; }
        public int getTileHeight() { return TILE_HEIGHT; }
        public int getTileHalfWidth() { return TILE_HALF_WIDTH; }
        public int getTileHalfHeight() { return TILE_HALF_HEIGHT; }
        public double getTileVisualBaseHeight() { return TILE_VISUAL_BASE_HEIGHT; }
        public double getMinimapWidth() { return MINIMAP_WIDTH; }
        public double getMinimapHeight() { return MINIMAP_HEIGHT; }
    }

    /**
     * The day and night cycle: the length of each phase in game seconds, where a new world starts, how the shadows of
     * the sun are quantised and the fast time of the debug key.
     */
    public static class DayCycleConfig {
        /** Game seconds the dawn lasts. The four phases follow Minecraft proportions, a cycle of twenty minutes. */
        private static final double DAWN_SECONDS = 150;
        /** Game seconds of full daylight. */
        private static final double DAY_SECONDS = 600;
        /** Game seconds the dusk lasts. */
        private static final double DUSK_SECONDS = 90;
        /** Game seconds the night lasts. */
        private static final double NIGHT_SECONDS = 360;
        /** The hour of the 24 hour clock a new world starts at: 06:00, when the dawn has ended and the light is neutral. */
        private static final double START_HOUR = 6.0;
        /** Distinct sun positions the shadows take across the day, an odd number so one is centred on noon. */
        private static final int SHADOW_STEPS = 17;
        /** Shadow length as a multiple of the height of what casts it, at noon. */
        private static final double MIN_SHADOW_LENGTH = 0.6;
        /** Shadow length as a multiple of the height of what casts it, with the sun on the horizon. */
        private static final double MAX_SHADOW_LENGTH = 2.0;
        /** Real seconds a whole day lasts while the fast time of the debug key is on. */
        private static final double FAST_DAY_REAL_SECONDS = 40;

        /** Whether the fast time of the debug key is on. */
        private boolean fastTime;

        /** The settings a {@link DayCycle} runs on, built once because the time scale is read on every update tick. */
        private final DayCycleSettings settings = new DayCycleSettings(DAWN_SECONDS, DAY_SECONDS, DUSK_SECONDS,
                NIGHT_SECONDS, 0, SHADOW_STEPS, MIN_SHADOW_LENGTH, MAX_SHADOW_LENGTH).startingAtClockHour(START_HOUR);

        /** Game seconds that pass per real second while the fast time is on, worked out once from the settings. */
        private final double fastTimeMultiplier = settings.cycleSeconds() / FAST_DAY_REAL_SECONDS;

        /** @return the settings a {@link DayCycle} runs on, the same instance every time */
        public DayCycleSettings settings() {
            return settings;
        }

        /** @return how many game seconds pass per real second while the fast time is on */
        public double getFastTimeMultiplier() {
            return fastTimeMultiplier;
        }

        /** @return true while the fast time of the debug key is on */
        public boolean isFastTime() { return fastTime; }

        /** @param fastTime true to make the day pass in about forty real seconds */
        public void setFastTime(boolean fastTime) { this.fastTime = fastTime; }

        /** @return the game seconds that pass per real second: one normally, the fast multiplier with the fast time on */
        public double timeScale() { return fastTime ? getFastTimeMultiplier() : 1.0; }
    }

    /**
     * The lights of the world: the torch the player holds and how every light is drawn.
     */
    public static class LightsConfig {
        /** How far the light of the held torch reaches on the ground in tiles. */
        private static final double TORCH_RADIUS = 4.5;
        /** The warm colour of the light of a torch. */
        private static final Rgba TORCH_COLOUR = new Rgba(1.0, 0.72, 0.38, 1.0);
        /** How strongly the held torch pushes back the dark at its brightest. */
        private static final double TORCH_INTENSITY = 1.0;
        /** The most the intensity of a crackling flame moves, as a share of it. */
        private static final double TORCH_INTENSITY_FLICKER = 0.10;
        /** The most the radius of a crackling flame moves, as a share of it. */
        private static final double TORCH_RADIUS_FLICKER = 0.05;
        /** The frequency of the slowest wave of the flame in cycles per second. */
        private static final double TORCH_FLICKER_HZ = 3.2;
        /** What the phases of the flame of the held torch are worked out from. */
        private static final long TORCH_FLICKER_SEED = 0x70C4L;
        /** The deeper orange the colour of the flame drifts to. */
        private static final Rgba FLAME_ORANGE = new Rgba(1.0, 0.58, 0.25, 1.0);
        /** The yellow the colour of the flame drifts to. */
        private static final Rgba FLAME_YELLOW = new Rgba(1.0, 0.82, 0.45, 1.0);
        /** The frequency of the slow drift of the colour of the flame in cycles per second. */
        private static final double FLAME_COLOUR_HZ = 0.7;
        /** The most the radius of the flame varies with the angle, as a share of it. */
        private static final double FLAME_SHAPE_AMPLITUDE = 0.10;
        /** How fast the lobes of the light of the flame turn, in turns per second. */
        private static final double FLAME_SHAPE_DRIFT_HZ = 0.25;
        /** The opacity of the warm overlay at the centre of a light in the deepest dark. */
        private static final double WARM_STRENGTH = 0.16;
        /** The tint strength of the deepest dark, which the warm overlay is scaled against. */
        private static final double REFERENCE_DARKNESS = 0.72;

        /** How every light is drawn, built once because the renderer reads it on every frame. */
        private final LightStyle style = new LightStyle(WARM_STRENGTH, REFERENCE_DARKNESS);

        /**
         * Creates the light of a torch held in hand, which the game state registers while a torch is selected.
         *
         * @return a new held torch light with the values of this configuration
         */
        public HeldTorch newHeldTorch() {
            return new HeldTorch(TORCH_RADIUS, TORCH_COLOUR, TORCH_INTENSITY, newFlameFlicker(TORCH_FLICKER_SEED));
        }

        /**
         * Creates the flicker of a flame: the intensity and the radius crackle, the colour drifts between a deeper
         * orange and a yellow and the edge of the light breathes. Other fire sources reuse it with a seed of their own.
         *
         * @param seed what the phases of the flame are worked out from
         * @return the flicker
         */
        public Flicker newFlameFlicker(long seed) {
            return Flicker.crackling(TORCH_INTENSITY_FLICKER, TORCH_RADIUS_FLICKER, TORCH_FLICKER_HZ, seed)
                    .withColourDrift(FLAME_ORANGE, FLAME_YELLOW, FLAME_COLOUR_HZ)
                    .withShape(FLAME_SHAPE_AMPLITUDE, FLAME_SHAPE_DRIFT_HZ);
        }

        /** @return how every light is drawn, the same instance every time */
        public LightStyle style() {
            return style;
        }
    }

    /**
     * Player-specific settings (sprite configuration)
     */
    public static class PlayerConfig {
        /**
         * The player sprite sheet: 8 direction columns by 3 frame rows of 16x24 cells.
         */
        public static final class SpriteSheetConfig {
            /** Classpath location of the sheet. */
            public static final String PATH = ASSET_ROOT + "/entities/player.png";
            /** Width of one cell in pixels. */
            public static final int SPRITE_WIDTH = 16;
            /** Height of one cell in pixels. */
            public static final int SPRITE_HEIGHT = 24;
            /** Direction columns in the sheet. */
            public static final int COLUMNS = 8;
            /** Frame rows in the sheet. */
            public static final int ROWS = 3;
            /** Pixel column of the first cell. */
            public static final int START_X = 0;
            /** Pixel row of the first cell. */
            public static final int START_Y = 0;
            /** Horizontal distance in pixels between the left edges of two neighbouring cells. */
            public static final int COLUMN_OFFSET = 16;

            private SpriteSheetConfig() {}

            /**
             * @param row the frame row, from 0 to {@link #ROWS} minus one
             * @return the pixel row of the top edge of that frame row
             */
            public static int rowY(int row) {
                return START_Y + row * SPRITE_HEIGHT;
            }
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