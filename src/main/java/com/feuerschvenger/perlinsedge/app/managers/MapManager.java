package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Creature;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureFactory;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.Species;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Manages map generation, configuration, and enemy spawning.
 * Handles map creation based on configuration parameters and player position.
 */
@Slf4j
public class MapManager {
    // Configuration constants
    private static final int MIN_WALKABLE_NEIGHBORS = 2; // Minimum adjacent walkable tiles for valid position

    /** Stands for "no position found", so a search never returns null. */
    private static final int[] NO_POSITION = new int[0];

    // Tile validator predicate
    private static final Predicate<Tile> SPAWN_VALIDATOR =
            tile -> tile != null && tile.isWalkable();

    // Dependencies
    private final GameStateManager gameState;
    private final MapGenerator mapGenerator;
    /** Draws the candidate tiles of the creature spawn search. */
    private final Random random = new Random();
    /** Builds the creatures that are spawned. */
    private final CreatureFactory creatureFactory = new CreatureFactory();

    // State
    @Getter
    private MapRequest currentRequest;

    public MapManager(GameStateManager gameState) {
        this(gameState, new MapGenerator(FastNoiseLiteAdapter.FACTORY));
    }

    /**
     * Creates a manager with its own generator, so a test can supply one that fails.
     *
     * @param gameState    shared game state
     * @param mapGenerator generates the maps
     */
    MapManager(GameStateManager gameState, MapGenerator mapGenerator) {
        this.gameState = gameState;
        this.mapGenerator = mapGenerator;
    }

    /**
     * Creates the request of a map of a type, a seed and the size of the world.
     *
     * @param mapType Type of map to generate
     * @param seed    Random seed for generation
     * @return the request, whose settings all come from the recipe of the type and nothing from the current map
     */
    public MapRequest createMapRequest(MapType mapType, long seed) {
        final var world = AppConfig.getInstance().world();
        return new MapRequest(mapType, seed, world.getWidth(), world.getHeight());
    }

    /**
     * Generates a new map for a request.
     * The request becomes the current one only once the map exists, so a generation that throws leaves the request
     * that still matches the map on screen.
     *
     * @param request what to generate
     */
    public void generateNewMap(MapRequest request) {
        logMapGeneration(request);

        final TileMap newMap = mapGenerator.generate(request, MapManager::logPassTime);

        this.currentRequest = request;
        install(newMap, request.seed());
    }

    /**
     * Stores a generated map and clears the entities and the hit times of the resources that belonged to the previous
     * one. Enemies are not spawned here: the caller spawns them once the player has a position on the new map, which
     * keeps them from being placed twice and from landing next to the player.
     */
    private void install(TileMap newMap, long seed) {
        gameState.setCurrentMap(newMap);
        gameState.setWorldSeed(seed);
        gameState.setDroppedItems(new ArrayList<>());
        gameState.setCreatures(new ArrayList<>());
        gameState.getResourceHitTimes().clear();
        gameState.getDamageNumbers().clear();
    }

    /**
     * Spawns the monsters based on configuration settings.
     * Ensures safe positioning relative to player.
     */
    public void spawnCreatures() {
        List<Creature> creatures = new ArrayList<>();
        TileMap map = gameState.getCurrentMap();

        int[] playerPosition = getPlayerPosition();
        // Every creature takes a tile of its own: a tile held by one already spawned is never picked again
        final var taken = new CreatureOccupancy();
        final var world = AppConfig.getInstance().world();
        final int width = map.getWidth();
        final int height = map.getHeight();
        spawnSpecies(creatures, Species.SLIME, world.getSlimesCount(width, height), playerPosition, map, taken);
        spawnSpecies(creatures, Species.MIMIC, world.getMimicsCount(width, height), playerPosition, map, taken);
        spawnSpecies(creatures, Species.SKELETON, world.getSkeletonsCount(width, height), playerPosition, map, taken);

        gameState.setCreatures(creatures);
        logSpawnSummary(creatures.size());
    }

    /**
     * Spawns multiple creatures of a species, built by the factory.
     */
    private void spawnSpecies(List<Creature> creatures, Species species, int count,
                              int[] playerPosition, TileMap map, CreatureOccupancy taken) {
        logSpawnAttempt(species.name(), count);

        for (var i = 0; i < count; i++) {
            int[] position = findSafePosition(playerPosition, map, taken);
            if (position.length > 0) {
                final var creature = creatureFactory.create(species, position[0], position[1]);
                taken.sync(creature);
                creatures.add(creature);
            } else {
                logSpawnFailure(species.name(), i);
            }
        }
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
    private int[] findSafePosition(int[] playerPosition, TileMap map, CreatureOccupancy taken) {
        final var world = AppConfig.getInstance().world();
        final int maxAttempts = world.getMaxPositioningAttempts();
        final int safeRadius = world.getStartSafeRadius(map.getWidth(), map.getHeight());

        for (var attempt = 0; attempt < maxAttempts; attempt++) {
            var x = random.nextInt(map.getWidth());
            var y = random.nextInt(map.getHeight());

            if (taken.ownerOf(x, y) == null && isValidSpawnPosition(x, y, playerPosition, safeRadius, map)) {
                return new int[]{x, y};
            }
        }
        return NO_POSITION;
    }

    /**
     * Validates a potential spawn position.
     */
    private static boolean isValidSpawnPosition(int x, int y, int[] playerPosition, int safeRadius, TileMap map) {
        var tile = map.getTile(x, y);
        if (tile == null || !tile.isWalkable()) {
            return false;
        }

        if (isPositionSurrounded(x, y, map)) {
            return false;
        }

        if (playerPosition != null) {
            double distance = Math.hypot((double) x - playerPosition[0], (double) y - playerPosition[1]);
            return distance >= safeRadius;
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

    private void logMapGeneration(MapRequest request) {
        log.info("Generating {} map with seed {}", request.type().name(), request.seed());
    }

    /** Logs how long a generation pass took, for the passes of a map that is taking too long. */
    private static void logPassTime(String pass, long nanos) {
        log.debug("Generation pass {} took {} us", pass, TimeUnit.NANOSECONDS.toMicros(nanos));
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
