package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.events.IConfigChangeListener;
import com.feuerschvenger.perlinsedge.domain.events.IMapGenerationListener;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGeneratorFactory;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Manages map generation, configuration, and enemy spawning.
 * Handles map creation based on configuration parameters and player position.
 */
public class MapManager implements IConfigChangeListener, IMapGenerationListener {
    // Configuration constants
    private static final int MIN_SPAWN_DISTANCE = 10; // Minimum distance from player for enemy spawn
    private static final int MIN_WALKABLE_NEIGHBORS = 2; // Minimum adjacent walkable tiles for valid position

    // Tile validator predicate
    private static final Predicate<Tile> SPAWN_VALIDATOR =
            tile -> tile != null && tile.isWalkable();

    // Dependencies
    private final GameStateManager gameState;
    private final MapConfigPanel mapConfigPanel;
    private final MapGeneratorFactory mapGeneratorFactory;

    // State
    private MapGenerationContext currentMapContext;

    public MapManager(GameStateManager gameState, MapConfigPanel mapConfigPanel) {
        this.gameState = gameState;
        this.mapConfigPanel = mapConfigPanel;
        this.mapGeneratorFactory = new MapGeneratorFactory();
    }

    /**
     * Creates a new map generation context with current configuration.
     *
     * @param mapType Type of map to generate
     * @param seed Random seed for generation
     * @return Configured generation context
     */
    public MapGenerationContext createMapContext(MapType mapType, long seed) {
        AppConfig.WorldConfig worldConfig = AppConfig.getInstance().world();
        return new MapGenerationContext(
                mapType,
                seed,
                worldConfig.getHeightNoiseFrequency(),
                worldConfig.getHeightNoiseOctaves(),
                worldConfig.getTemperatureNoiseFrequency(),
                worldConfig.getTemperatureNoiseOctaves(),
                worldConfig.getMoistureNoiseFrequency(),
                worldConfig.getMoistureNoiseOctaves(),
                worldConfig.getStonePatchFrequency(),
                worldConfig.getStoneThreshold(),
                worldConfig.getCrystalsPatchFrequency(),
                worldConfig.getCrystalsThreshold(),
                worldConfig.getTreeDensity(),
                worldConfig.getRiverDensity()
        );
    }

    /**
     * Generates a new map based on the provided context.
     *
     * @param context Map generation parameters
     */
    public void generateNewMap(MapGenerationContext context) {
        logMapGeneration(context);
        this.currentMapContext = context;

        IMapGenerator generator = mapGeneratorFactory.createGenerator(context.getMapType());
        TileMap newMap = generator.generateMap(
                AppConfig.getInstance().world().getWidth(),
                AppConfig.getInstance().world().getHeight(),
                context
        );

        onMapGenerated(newMap, generator);
    }

    /**
     * Callback when map generation completes.
     * Resets game state and spawns enemies.
     */
    @Override
    public void onMapGenerated(TileMap newMap, IMapGenerator generator) {
        gameState.setCurrentMap(newMap);
        gameState.setDroppedItems(new ArrayList<>());
        gameState.setEnemies(new ArrayList<>());

        spawnEnemies();
        mapConfigPanel.updateValuesFromGenerator(generator);
    }

    /**
     * Spawns enemies based on configuration settings.
     * Ensures safe positioning relative to player.
     */
    public void spawnEnemies() {
        List<Enemy> enemies = new ArrayList<>();
        TileMap map = gameState.getCurrentMap();

        int[] playerPosition = getPlayerPosition();
        spawnEnemyType(enemies, Slime.class, AppConfig.getInstance().world().getBaseSlimesCount(), playerPosition, map);
        spawnEnemyType(enemies, Mimic.class, AppConfig.getInstance().world().getBaseMimicsCount(), playerPosition, map);

        gameState.setEnemies(enemies);
        logSpawnSummary(enemies.size());
    }

    /**
     * Spawns multiple enemies of a specific type.
     */
    private void spawnEnemyType(List<Enemy> enemies, Class<? extends Enemy> type, int count,
                                int[] playerPosition, TileMap map) {
        logSpawnAttempt(type.getSimpleName(), count);

        for (int i = 0; i < count; i++) {
            int[] position = findSafePosition(playerPosition, map);
            if (position != null) {
                enemies.add(createEnemy(type, position[0], position[1]));
            } else {
                logSpawnFailure(type.getSimpleName(), i);
            }
        }
    }

    /**
     * Creates an enemy instance based on type.
     */
    private Enemy createEnemy(Class<? extends Enemy> type, int x, int y) {
        if (type == Slime.class) return new Slime(x, y);
        if (type == Mimic.class) return new Mimic(x, y);
        throw new IllegalArgumentException("Unsupported enemy type: " + type);
    }

    /**
     * Finds a safe spawn position considering:
     * - Walkable terrain
     * - Distance from player
     * - Sufficient adjacent walkable tiles
     */
    private int[] findSafePosition(int[] playerPosition, TileMap map) {
        Random random = new Random();
        int maxAttempts = AppConfig.getInstance().world().getMaxPositioningAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = random.nextInt(map.getWidth());
            int y = random.nextInt(map.getHeight());

            if (isValidSpawnPosition(x, y, playerPosition, map)) {
                return new int[]{x, y};
            }
        }
        return null;
    }

    /**
     * Validates a potential spawn position.
     */
    private boolean isValidSpawnPosition(int x, int y, int[] playerPosition, TileMap map) {
        Tile tile = map.getTile(x, y);
        if (tile == null || !tile.isWalkable()) {
            return false;
        }

        if (isPositionSurrounded(x, y, map)) {
            return false;
        }

        if (playerPosition != null) {
            double distance = Math.hypot(x - playerPosition[0], y - playerPosition[1]);
            return !(distance < MIN_SPAWN_DISTANCE);
        }

        return true;
    }

    /**
     * Checks if position has sufficient walkable neighbors.
     */
    private boolean isPositionSurrounded(int x, int y, TileMap map) {
        return IsometricUtils.isPositionSurrounded(
                x, y, map,
                SPAWN_VALIDATOR,
                MIN_WALKABLE_NEIGHBORS,
                IsometricUtils.CARDINAL_DIRECTIONS
        );
    }

    /**
     * Handles configuration changes from UI.
     * Regenerates map with new parameters.
     */
    @Override
    public void onMapConfigChanged(MapType mapType, long seed,
                                   float heightFreq, int heightOctaves,
                                   float tempFreq, int tempOctaves,
                                   float moistureFreq, int moistureOctaves,
                                   float stoneFreq, float stoneThreshold,
                                   float crystalsFreq, float crystalsThreshold,
                                   float treeDensity, float riverDensity) {
        AppConfig.getInstance().world().setCurrentMapType(mapType);
        generateNewMap(new MapGenerationContext(
                mapType, seed,
                heightFreq, heightOctaves,
                tempFreq, tempOctaves,
                moistureFreq, moistureOctaves,
                stoneFreq, stoneThreshold,
                crystalsFreq, crystalsThreshold,
                treeDensity, riverDensity
        ));
    }

    /**
     * Retrieves player position if available.
     */
    private int[] getPlayerPosition() {
        Player player = gameState.getPlayer();
        return (player != null) ? new int[]{player.getCurrentTileX(), player.getCurrentTileY()} : null;
    }

    // --- Logging Methods ---

    private void logMapGeneration(MapGenerationContext context) {
        System.out.printf("Generating %s map with seed %d%n",
                context.getMapType().name(), context.getSeed());
    }

    private void logSpawnAttempt(String enemyType, int count) {
        System.out.printf("Attempting to spawn %d %s enemies%n", count, enemyType);
    }

    private void logSpawnFailure(String enemyType, int index) {
        System.out.printf("Failed to find position for %s #%d%n", enemyType, index + 1);
    }

    private void logSpawnSummary(int total) {
        System.out.printf("Spawned %d total enemies%n", total);
    }

    // --- Getters ---

    public MapGenerationContext getCurrentMapContext() {
        return currentMapContext;
    }

}