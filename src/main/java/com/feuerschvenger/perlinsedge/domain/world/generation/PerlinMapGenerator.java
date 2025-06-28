package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.generation.util.TileFactory;
import com.feuerschvenger.perlinsedge.domain.world.model.*;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLite;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import com.feuerschvenger.perlinsedge.infra.noise.INoiseGenerator;

import java.util.*;

/**
 * Generates tile maps using Perlin noise with configurable terrain features,
 * resource distribution, and river systems.
 */
public class PerlinMapGenerator implements IMapGenerator {

    private static final int NUM_ISLANDS = 3;

    // Configuration holder
    AppConfig.GenerationConfig config = AppConfig.getInstance().generationConfig();

    // Noise generator cache
    private INoiseGenerator heightNoise;
    private INoiseGenerator temperatureNoise;
    private INoiseGenerator moistureNoise;
    private INoiseGenerator stoneNoise;
    private INoiseGenerator crystalsNoise;
    private INoiseGenerator treeNoise;

    // River generation constants
    private static final int[][] RIVER_DIRECTIONS = {
            {0, -1}, {1, -1}, {1, 0}, {1, 1},
            {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };
    private static final int MAX_RIVER_LENGTH = 300;
    private static final int MIN_RIVER_LENGTH = 100;
    private static final int LAKE_RADIUS_MIN = 2;
    private static final int LAKE_RADIUS_MAX = 5;
    private static final float ISLAND_FALLOFF_POWER = 2.0f;

    private float[][] islandCenters;

    @Override
    public TileMap generateMap(int width, int height, MapGenerationContext context) {
        config.update(context);
        TileMap map = new TileMap(width, height);

        initializeNoiseGenerators();

        if (config.getMapType() == MapType.ISLANDS) {
            initializeIslandCenters(width, height);
        }

        generateTerrain(map);

        // Apply specialized water handling for different map types
        switch (config.getMapType()) {
            case ISLANDS:
                applyIslandWaterLevels(map);
                break;
            case TUNDRA:
                applyTundraWaterLevels(map);
                break;
            case RIVERS:
                applyRiverWaterLevels(map);
                break;
            default:
                applyBaseWaterLevels(map);
        }

        if (config.getRiverDensity() > 0) {
            generateRiverSystems(map);
        }

        return map;
    }

    // ==================================================================
    //  INITIALIZATION
    // ==================================================================

    private void initializeNoiseGenerators() {
        heightNoise = createNoiseGenerator(config.getSeed(), config.getHeightNoiseConfig());
        temperatureNoise = createNoiseGenerator(config.getSeed() + 1, config.getTemperatureNoiseConfig());
        moistureNoise = createNoiseGenerator(config.getSeed() + 2, config.getMoistureNoiseConfig());
        stoneNoise = createNoiseGenerator(config.getSeed() + 3, config.getStoneNoiseConfig());
        crystalsNoise = createNoiseGenerator(config.getSeed() + 4, config.getCrystalsNoiseConfig());
        treeNoise = createNoiseGenerator(config.getSeed() + 5, config.getTreeNoiseConfig());
    }

    private INoiseGenerator createNoiseGenerator(long seed, AppConfig.NoiseConfig noiseConfig) {
        INoiseGenerator generator = new FastNoiseLiteAdapter((int) seed);
        generator.setNoiseType(FastNoiseLite.NoiseType.Perlin);
        generator.setFrequency(noiseConfig.getFrequency());

        if (noiseConfig.getOctaves() > 0) {
            generator.setFractalType(noiseConfig.getFractalType());
            generator.setFractalOctaves(noiseConfig.getOctaves());
            generator.setFractalGain(noiseConfig.getGain());
            generator.setFractalLacunarity(noiseConfig.getLacunarity());
        }

        return generator;
    }

    // ==================================================================
    //  TERRAIN GENERATION
    // ==================================================================

    private void generateTerrain(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float heightValue = heightNoise.getNoise(x, y);
                float tempValue = normalizeNoise(temperatureNoise.getNoise(x, y));
                float moistureValue = normalizeNoise(moistureNoise.getNoise(x, y));

                // Apply island shaping for island maps
                if (config.getMapType() == MapType.ISLANDS) {
                    heightValue = applyIslandEffect(x, y, width, height, heightValue);
                }

                TileType tileType = TileFactory.fromHeight(
                        x, y, heightValue, tempValue, moistureValue, config.getMapType(), config.getSeed()
                );

                Tile tile = new Tile(x, y, tileType);
                tile.setHeight(heightValue);

                assignTileResource(tile, x, y);
                map.setTile(x, y, tile);
            }
        }
    }

    private float normalizeNoise(float value) {
        return (value + 1) / 2;
    }

    private void initializeIslandCenters(int width, int height) {
        Random rand = config.getRandom();
        islandCenters = new float[NUM_ISLANDS][2];

        for (int i = 0; i < NUM_ISLANDS; i++) {
            islandCenters[i][0] = 0.2f + rand.nextFloat() * 0.6f; // x (20-80%)
            islandCenters[i][1] = 0.2f + rand.nextFloat() * 0.6f; // y (20-80%)
        }
    }

    private float applyIslandEffect(int x, int y, int width, int height, float heightValue) {
        if (islandCenters == null) {
            return heightValue;
        }
        float nx = (float) x / width;
        float ny = (float) y / height;
        float maxEffect = 0f;

        for (float[] center : islandCenters) {
            float dx = nx - center[0];
            float dy = ny - center[1];
            float distance = (float) Math.sqrt(dx * dx + dy * dy) * 2f;

            float effect = 1f - Math.min(1f, distance);
            effect = (float) Math.pow(effect, ISLAND_FALLOFF_POWER);
            maxEffect = Math.max(maxEffect, effect);
        }

        return heightValue * maxEffect * 1.5f + (maxEffect * 0.7f - 0.5f);
    }

    private void assignTileResource(Tile tile, int x, int y) {
        if (!tile.getType().isLand()) return;

        float treeDensityFactor = 1.0f;
        switch (config.getMapType()) {
            case MEDITERRANEAN:
                treeDensityFactor = 1.8f;  // Más árboles en Mediterranean
                break;
            case TUNDRA:
                treeDensityFactor = 1.5f;  // Más árboles en Tundra
                break;
            case DESERT:
                treeDensityFactor = 1.3f;  // Más árboles en Desierto
                break;
            case ISLANDS:
                treeDensityFactor = 1.6f;  // Más árboles en Islas
                break;
        }

        // Resource assignment priority: Crystals > Stone > Trees
        float crystalsValue = normalizeNoise(crystalsNoise.getNoise(x, y));
        if (crystalsValue > config.getCrystalsThreshold()) {
            tile.setWorldResource(new Resource(ResourceType.CRYSTALS, config.getRandom().nextLong()));
            return;
        }

        float stoneValue = normalizeNoise(stoneNoise.getNoise(x, y));
        if (stoneValue > config.getStoneThreshold()) {
            tile.setWorldResource(new Resource(ResourceType.STONE, config.getRandom().nextLong()));
            return;
        }

        if (tile.getType().isFertile()) {
            float treeValue = normalizeNoise(treeNoise.getNoise(x, y));
            float threshold = 1.0f - (config.getTreeDensity() * treeDensityFactor);

            // Para desierto, árboles solo en oasis (GRASS)
            if (config.getMapType() == MapType.DESERT) {
                if (tile.getType() == TileType.GRASS && treeValue > threshold) {
                    tile.setWorldResource(new Resource(ResourceType.TREE, config.getRandom().nextLong()));
                }
            }
            // Para otros biomas
            else if (treeValue > threshold) {
                tile.setWorldResource(new Resource(ResourceType.TREE, config.getRandom().nextLong()));
            }
        }
    }

    // ==================================================================
    //  WATER SYSTEMS
    // ==================================================================

    private void applyBaseWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();

                if (tileHeight < -0.4f) {
                    tile.setType(TileType.DEEP_WATER);
                    tile.setWorldResource(null);
                } else if (tileHeight < -0.2f) {
                    tile.setType(TileType.SHALLOW_WATER);
                    tile.setWorldResource(null);
                }
            }
        }
    }

    private void applyRiverWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();

                // Only override water tiles
                if (tileHeight < -0.6f) {
                    tile.setType(TileType.DEEP_WATER);
                } else if (tileHeight < -0.5f) {
                    tile.setType(TileType.SHALLOW_WATER);
                }

                // Clear resources from water tiles
                if (!tile.getType().isLand()) {
                    tile.setWorldResource(null);
                }
            }
        }
    }

    private void applyIslandWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();

                if (tileHeight < -0.6f) {
                    tile.setType(TileType.DEEP_WATER);
                    tile.setWorldResource(null); // Solo eliminar recurso en agua
                } else if (tileHeight < -0.4f) {
                    tile.setType(TileType.SHALLOW_WATER);
                    tile.setWorldResource(null); // Solo eliminar recurso en agua
                } else if (tileHeight < -0.2f) {
                    tile.setType(TileType.SAND);
                    // ¡NO eliminar recursos en arena/tierra!
                }
            }
        }
    }

    private void applyTundraWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();
                float tempValue = normalizeNoise(temperatureNoise.getNoise(x, y));

                if (tileHeight < -0.4f) {
                    tile.setType(TileType.DEEP_WATER);
                    tile.setWorldResource(null); // Solo eliminar recurso en agua
                } else if (tileHeight < -0.2f) {
                    if (tempValue < 0.3f) {
                        tile.setType(TileType.ICE);
                        tile.setWorldResource(null); // Solo eliminar recurso en hielo/agua
                    } else {
                        tile.setType(TileType.SHALLOW_WATER);
                        tile.setWorldResource(null); // Solo eliminar recurso en agua
                    }
                }
            }
        }
    }

    private void generateRiverSystems(TileMap map) {
        List<int[]> riverSources = identifyRiverSources(map);
        int riversToCreate = calculateRiverCount(riverSources.size());
        Set<String> riverTiles = new HashSet<>();

        Collections.shuffle(riverSources, config.getRandom());

        for (int i = 0; i < Math.min(riversToCreate, riverSources.size()); i++) {
            int[] source = riverSources.get(i);
            if (isValidRiverStart(map, source, riverTiles)) {
                createRiverPath(map, source[0], source[1], riverTiles);
            }
        }

        clearResourcesFromRivers(map, riverTiles);
    }

    private List<int[]> identifyRiverSources(TileMap map) {
        List<int[]> sources = new ArrayList<>();
        int width = map.getWidth();
        int height = map.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile tile = map.getTile(x, y);
                if (tile.getType().isLand() && tile.getHeight() > 0.6f) {
                    sources.add(new int[]{x, y});
                }
            }
        }
        return sources;
    }

    private int calculateRiverCount(int potentialSources) {
        return Math.max(1, (int) (potentialSources * config.getRiverDensity() * 0.1f));
    }

    private boolean isValidRiverStart(TileMap map, int[] source, Set<String> riverTiles) {
        return map.getTile(source[0], source[1]).getType().isLand() &&
                !riverTiles.contains(source[0] + "," + source[1]);
    }

    private void createRiverPath(TileMap map, int startX, int startY, Set<String> riverTiles) {
        int x = startX, y = startY;
        int length = 0;
        int maxLength = MIN_RIVER_LENGTH + config.getRandom().nextInt(MAX_RIVER_LENGTH - MIN_RIVER_LENGTH);
        List<int[]> riverSegment = new ArrayList<>();
        float currentHeight = map.getTile(x, y).getHeight();

        if (currentHeight < 0.6f) return;

        while (length < maxLength) {
            String currentKey = x + "," + y;
            Tile currentTile = map.getTile(x, y);

            // Terminate if we reach existing water
            if (isWaterTile(currentTile)) {
                applyRiverSegment(map, riverSegment);
                return;
            }

            riverTiles.add(currentKey);
            riverSegment.add(new int[]{x, y});

            int[] nextCoord = findNextRiverPoint(map, x, y, currentHeight, riverTiles);
            if (nextCoord == null) {
                createLakeAtEndpoint(map, riverSegment, x, y);
                break;
            }

            x = nextCoord[0];
            y = nextCoord[1];
            currentHeight = map.getTile(x, y).getHeight();
            length++;

            // Occasionally create tributaries
            if (length > 30 && config.getRandom().nextFloat() < 0.01f) {
                createTributary(map, x, y, riverTiles);
            }
        }
        applyRiverSegment(map, riverSegment);
    }

    private boolean isWaterTile(Tile tile) {
        return tile.getType() == TileType.RIVER ||
                tile.getType() == TileType.SHALLOW_WATER ||
                tile.getType() == TileType.DEEP_WATER;
    }

    private int[] findNextRiverPoint(TileMap map, int x, int y, float currentHeight, Set<String> riverTiles) {
        float minHeight = currentHeight;
        List<int[]> candidates = new ArrayList<>();
        int width = map.getWidth();
        int height = map.getHeight();

        for (int[] dir : RIVER_DIRECTIONS) {
            int nx = x + dir[0], ny = y + dir[1];
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;

            Tile neighbor = map.getTile(nx, ny);
            String neighborKey = nx + "," + ny;
            float neighborHeight = neighbor.getHeight();

            if ((neighborHeight < currentHeight && !riverTiles.contains(neighborKey)) ||
                    (isWaterTile(neighbor) && neighborHeight < currentHeight)) {

                candidates.add(new int[]{nx, ny});
                if (neighborHeight < minHeight) minHeight = neighborHeight;
            }
        }

        if (candidates.isEmpty()) return null;
        return selectLowestCandidate(map, candidates, minHeight);
    }

    private int[] selectLowestCandidate(TileMap map, List<int[]> candidates, float minHeight) {
        List<int[]> bestCandidates = new ArrayList<>();
        for (int[] coord : candidates) {
            if (map.getTile(coord[0], coord[1]).getHeight() == minHeight) {
                bestCandidates.add(coord);
            }
        }
        return bestCandidates.get(config.getRandom().nextInt(bestCandidates.size()));
    }

    private void applyRiverSegment(TileMap map, List<int[]> segment) {
        for (int[] point : segment) {
            Tile tile = map.getTile(point[0], point[1]);
            if (tile.getType().isLand()) {
                tile.setType(TileType.RIVER);
                tile.setWorldResource(null);
            }
        }
    }

    private void createLakeAtEndpoint(TileMap map, List<int[]> segment, int x, int y) {
        Tile endpoint = map.getTile(x, y);
        if (endpoint.getHeight() >= 0.4f || !endpoint.getType().isLand()) return;

        int radius = LAKE_RADIUS_MIN + config.getRandom().nextInt(LAKE_RADIUS_MAX - LAKE_RADIUS_MIN);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int lx = x + dx, ly = y + dy;
                if (map.isValidCoordinate(lx, ly)) {
                    Tile tile = map.getTile(lx, ly);
                    if (tile.getType().isLand() && tile.getHeight() < endpoint.getHeight() + 0.05f) {
                        if (config.getMapType() == MapType.TUNDRA) {
                            tile.setType(TileType.ICE); // Frozen lakes in tundra
                        } else {
                            tile.setType(TileType.SHALLOW_WATER);
                        }
                        tile.setWorldResource(null);
                    }
                }
            }
        }
        applyRiverSegment(map, segment);
    }

    private void createTributary(TileMap map, int x, int y, Set<String> riverTiles) {
        int length = 20 + config.getRandom().nextInt(50);
        List<int[]> tributary = new ArrayList<>();
        float currentHeight = map.getTile(x, y).getHeight();

        if (!map.getTile(x, y).getType().isLand() || currentHeight < 0.6f) return;

        for (int i = 0; i < length; i++) {
            String currentKey = x + "," + y;
            if (riverTiles.contains(currentKey)) {
                applyRiverSegment(map, tributary);
                return;
            }

            riverTiles.add(currentKey);
            tributary.add(new int[]{x, y});

            int[] next = findNextRiverPoint(map, x, y, currentHeight, riverTiles);
            if (next == null) break;

            x = next[0];
            y = next[1];
            currentHeight = map.getTile(x, y).getHeight();
        }
        applyRiverSegment(map, tributary);
    }

    private void clearResourcesFromRivers(TileMap map, Set<String> riverTiles) {
        for (String key : riverTiles) {
            String[] coords = key.split(",");
            int x = Integer.parseInt(coords[0]), y = Integer.parseInt(coords[1]);
            Tile tile = map.getTile(x, y);
            if (tile.getType() == TileType.RIVER) {
                tile.setWorldResource(null);
            }
        }
    }

    // ==================================================================
    //  GETTERS (Interface Implementation)
    // ==================================================================

    @Override public MapType getMapType() { return config.getMapType(); }
    @Override public long getSeed() { return config.getSeed(); }
    @Override public float getHeightNoiseFrequency() { return config.getHeightNoiseConfig().getFrequency(); }
    @Override public int getHeightNoiseOctaves() { return config.getHeightNoiseConfig().getOctaves(); }
    @Override public float getTemperatureNoiseFrequency() { return config.getTemperatureNoiseConfig().getFrequency(); }
    @Override public int getTemperatureNoiseOctaves() { return config.getTemperatureNoiseConfig().getOctaves(); }
    @Override public float getMoistureNoiseFrequency() { return config.getMoistureNoiseConfig().getFrequency(); }
    @Override public int getMoistureNoiseOctaves() { return config.getMoistureNoiseConfig().getOctaves(); }
    @Override public float getStonePatchFrequency() { return config.getStoneNoiseConfig().getFrequency(); }
    @Override public float getStoneThreshold() { return config.getStoneThreshold(); }
    @Override public float getCrystalsPatchFrequency() { return config.getCrystalsNoiseConfig().getFrequency(); }
    @Override public float getCrystalsThreshold() { return config.getCrystalsThreshold(); }
    @Override public float getTreeDensity() { return config.getTreeDensity(); }
    @Override public float getRiverDensity() { return config.getRiverDensity(); }
}