package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.events.IMapGenerationListener;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.generation.IMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGeneratorFactory;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import com.feuerschvenger.perlinsedge.ui.view.MapConfigPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Manages map generation, configuration, and enemy spawning.
 * Handles map creation based on configuration parameters and player position.
 */
@Slf4j
public class MapManager implements IMapGenerationListener {
    // Configuration constants
    private static final int MIN_SPAWN_DISTANCE = 10; // Minimum distance from player for enemy spawn
    private static final int MIN_WALKABLE_NEIGHBORS = 2; // Minimum adjacent walkable tiles for valid position

    /** Stands for "no position found", so a search never returns null. */
    private static final int[] NO_POSITION = new int[0];

    // Tile validator predicate
    private static final Predicate<Tile> SPAWN_VALIDATOR =
            tile -> tile != null && tile.isWalkable();

    // Dependencies
    private final GameStateManager gameState;
    private final MapConfigPanel mapConfigPanel;
    private final MapGeneratorFactory mapGeneratorFactory;
    /** Draws the candidate tiles of the enemy spawn search. */
    private final Random random = new Random();

    // State
    @Getter
    private MapGenerationContext currentMapContext;

    public MapManager(GameStateManager gameState, MapConfigPanel mapConfigPanel) {
        this(gameState, mapConfigPanel, new MapGeneratorFactory(FastNoiseLiteAdapter.FACTORY));
    }

    /**
     * Creates a manager with the factory that builds its generators, so a test can supply a generator that fails.
     *
     * @param gameState           shared game state
     * @param mapConfigPanel      panel that shows the values of the generator that built the map
     * @param mapGeneratorFactory builds the generator for a map type
     */
    MapManager(GameStateManager gameState, MapConfigPanel mapConfigPanel, MapGeneratorFactory mapGeneratorFactory) {
        this.gameState = gameState;
        this.mapConfigPanel = mapConfigPanel;
        this.mapGeneratorFactory = mapGeneratorFactory;
    }

    /**
     * Creates a new map generation context with current configuration.
     *
     * @param mapType Type of map to generate
     * @param seed Random seed for generation
     * @return Configured generation context
     */
    public MapGenerationContext createMapContext(MapType mapType, long seed) {
        var worldConfig = AppConfig.getInstance().world();
        var heightProfile = NoiseSettings.heightProfile(mapType);
        return new MapGenerationContext(
                mapType,
                seed,
                heightProfile.frequency(),
                heightProfile.octaves(),
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
     * The context becomes the current one only once the map exists, so a generation that throws leaves the context
     * that still matches the map on screen.
     *
     * @param context Map generation parameters
     */
    public void generateNewMap(MapGenerationContext context) {
        logMapGeneration(context);

        IMapGenerator generator = mapGeneratorFactory.createGenerator(context.getMapType());
        TileMap newMap = generator.generateMap(
                AppConfig.getInstance().world().getWidth(),
                AppConfig.getInstance().world().getHeight(),
                context
        );

        this.currentMapContext = context;
        onMapGenerated(newMap, generator);
    }

    /**
     * Callback when map generation completes.
     * Stores the map and clears the entities and the hit times of the resources that belonged to the previous one.
     * Enemies are not spawned here: the caller spawns them once the player has a position on the new map,
     * which keeps them from being placed twice and from landing next to the player.
     * The panel only mirrors the generator, so a panel that fails is logged and the new map stays in place: the map,
     * the context and the map type stay consistent with each other.
     */
    @Override
    public void onMapGenerated(TileMap newMap, IMapGenerator generator) {
        gameState.setCurrentMap(newMap);
        gameState.setWorldSeed(generator.getSeed());
        gameState.setDroppedItems(new ArrayList<>());
        gameState.setEnemies(new ArrayList<>());
        gameState.getResourceHitTimes().clear();
        gameState.getDamageNumbers().clear();

        try {
            mapConfigPanel.updateValuesFromGenerator(generator);
        } catch (RuntimeException e) {
            log.error("The map panel could not show the values of the new map", e);
        }
    }

    /**
     * Spawns enemies based on configuration settings.
     * Ensures safe positioning relative to player.
     */
    public void spawnEnemies() {
        List<Enemy> enemies = new ArrayList<>();
        TileMap map = gameState.getCurrentMap();

        int[] playerPosition = getPlayerPosition();
        // Every enemy takes a tile of its own: a tile held by one already spawned is never picked again
        final var taken = new EnemyOccupancy();
        spawnEnemyType(enemies, Slime.class, AppConfig.getInstance().world().getBaseSlimesCount(), playerPosition, map, taken);
        spawnEnemyType(enemies, Mimic.class, AppConfig.getInstance().world().getBaseMimicsCount(), playerPosition, map, taken);
        spawnEnemyType(enemies, Skeleton.class, AppConfig.getInstance().world().getBaseSkeletonsCount(), playerPosition, map, taken);

        gameState.setEnemies(enemies);
        logSpawnSummary(enemies.size());
    }

    /**
     * Spawns multiple enemies of a specific type.
     */
    private void spawnEnemyType(List<Enemy> enemies, Class<? extends Enemy> type, int count,
                                int[] playerPosition, TileMap map, EnemyOccupancy taken) {
        logSpawnAttempt(type.getSimpleName(), count);

        for (var i = 0; i < count; i++) {
            int[] position = findSafePosition(playerPosition, map, taken);
            if (position.length > 0) {
                final var enemy = createEnemy(type, position[0], position[1]);
                taken.sync(enemy);
                enemies.add(enemy);
            } else {
                logSpawnFailure(type.getSimpleName(), i);
            }
        }
    }

    /**
     * Creates an enemy instance based on type.
     */
    private static Enemy createEnemy(Class<? extends Enemy> type, int x, int y) {
        if (type == Slime.class) {
            return new Slime(x, y);
        }
        if (type == Mimic.class) {
            return new Mimic(x, y);
        }
        if (type == Skeleton.class) {
            return new Skeleton(x, y);
        }
        throw new IllegalArgumentException("Unsupported enemy type: " + type);
    }

    /**
     * Finds a safe spawn position considering:
     * - Walkable terrain
     * - Distance from player
     * - Sufficient adjacent walkable tiles
     * - A tile no other enemy holds
     *
     * @return the position, or {@link #NO_POSITION} when none was found within the attempts
     */
    private int[] findSafePosition(int[] playerPosition, TileMap map, EnemyOccupancy taken) {
        int maxAttempts = AppConfig.getInstance().world().getMaxPositioningAttempts();

        for (var attempt = 0; attempt < maxAttempts; attempt++) {
            var x = random.nextInt(map.getWidth());
            var y = random.nextInt(map.getHeight());

            if (taken.ownerOf(x, y) == null && isValidSpawnPosition(x, y, playerPosition, map)) {
                return new int[]{x, y};
            }
        }
        return NO_POSITION;
    }

    /**
     * Validates a potential spawn position.
     */
    private static boolean isValidSpawnPosition(int x, int y, int[] playerPosition, TileMap map) {
        var tile = map.getTile(x, y);
        if (tile == null || !tile.isWalkable()) {
            return false;
        }

        if (isPositionSurrounded(x, y, map)) {
            return false;
        }

        if (playerPosition != null) {
            double distance = Math.hypot((double) x - playerPosition[0], (double) y - playerPosition[1]);
            return distance >= MIN_SPAWN_DISTANCE;
        }

        return true;
    }

    /**
     * Checks if position has sufficient walkable neighbors.
     */
    private static boolean isPositionSurrounded(int x, int y, TileMap map) {
        return IsometricUtils.isPositionSurrounded(
                x, y, map,
                SPAWN_VALIDATOR,
                MIN_WALKABLE_NEIGHBORS,
                IsometricUtils.cardinalDirections()
        );
    }

    /**
     * Retrieves player position if available.
     */
    private int[] getPlayerPosition() {
        var player = gameState.getPlayer();
        return (player != null) ? new int[]{player.getCurrentTileX(), player.getCurrentTileY()} : null;
    }

    // --- Logging Methods ---

    private void logMapGeneration(MapGenerationContext context) {
        log.info("Generating {} map with seed {}", context.getMapType().name(), context.getSeed());
    }

    private void logSpawnAttempt(String enemyType, int count) {
        log.debug("Attempting to spawn {} {} enemies", count, enemyType);
    }

    private void logSpawnFailure(String enemyType, int index) {
        log.debug("Failed to find position for {} #{}", enemyType, index + 1);
    }

    private void logSpawnSummary(int total) {
        log.debug("Spawned {} total enemies", total);
    }

}
