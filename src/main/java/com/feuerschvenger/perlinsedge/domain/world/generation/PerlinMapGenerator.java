package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.generation.util.TileFactory;
import com.feuerschvenger.perlinsedge.domain.utils.Doubles;
import com.feuerschvenger.perlinsedge.domain.utils.Hashes;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Generates tile maps using Perlin noise with configurable terrain features,
 * resource distribution, and river systems.
 */
public class PerlinMapGenerator implements IMapGenerator {

    private static final int NUM_ISLANDS = 3;

    // Offsets of the seed of each noise field, so the fields of one map are unrelated to each other
    private static final long MOISTURE_SEED_OFFSET = 2;
    private static final long STONE_SEED_OFFSET = 3;
    private static final long CRYSTALS_SEED_OFFSET = 4;
    private static final long TREE_SEED_OFFSET = 5;

    // A noise value runs from minus one to one: adding one and halving it gives a value from zero to one
    private static final float NOISE_RANGE_SPAN = 2F;

    // Island shaping: where the centres lie, as a share of the map, and how the height is lifted around them
    private static final int ISLAND_CENTER_AXES = 2;
    private static final float ISLAND_CENTER_MIN = 0.2F;
    private static final float ISLAND_CENTER_SPAN = 0.6F;
    private static final float ISLAND_DISTANCE_SCALE = 2F;
    private static final float ISLAND_HEIGHT_GAIN = 1.5F;
    private static final float ISLAND_LIFT = 0.7F;
    private static final float ISLAND_SEA_OFFSET = 0.5F;

    // Height below which a tile is turned into water, per map type
    private static final float BASE_DEEP_WATER_LEVEL = -0.4F;
    private static final float BASE_SHALLOW_WATER_LEVEL = -0.2F;
    private static final float RIVERS_MAP_DEEP_WATER_LEVEL = -0.6F;
    private static final float RIVERS_MAP_SHALLOW_WATER_LEVEL = -0.5F;
    private static final float ISLANDS_DEEP_WATER_LEVEL = -0.6F;
    private static final float ISLANDS_SHALLOW_WATER_LEVEL = -0.4F;
    private static final float ISLANDS_SAND_LEVEL = -0.2F;
    private static final float TUNDRA_DEEP_WATER_LEVEL = -0.4F;
    private static final float TUNDRA_SHALLOW_WATER_LEVEL = -0.2F;
    /** Shallow water of the tundra freezes where the temperature is below this. */
    private static final float TUNDRA_FREEZE_TEMPERATURE = 0.3F;

    // Configuration holder
    private final AppConfig.GenerationConfig config = AppConfig.getInstance().generationConfig();

    private final NoiseSourceFactory noiseFactory;

    // Noise generator cache
    private NoiseSource heightNoise;
    private NoiseSource temperatureNoise;
    private NoiseSource moistureNoise;
    private NoiseSource stoneNoise;
    private NoiseSource crystalsNoise;
    private NoiseSource treeNoise;

    // River generation constants
    private static final int[][] RIVER_DIRECTIONS = {
            {0, -1}, {1, -1}, {1, 0}, {1, 1},
            {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };
    private static final int[] NO_RIVER_POINT = new int[0];
    private static final int MAX_RIVER_LENGTH = 300;
    private static final int MIN_RIVER_LENGTH = 100;
    /** Lowest height a river may start at, and the height a source tile has to exceed. */
    private static final float RIVER_SOURCE_MIN_HEIGHT = 0.6F;
    /** Share of the potential sources that become rivers, before the river density of the map type scales it. */
    private static final float RIVER_SOURCE_SHARE = 0.1F;
    /** A river has to be this long before it may branch, and branches with this chance per step after that. */
    private static final int TRIBUTARY_MIN_RIVER_LENGTH = 30;
    private static final float TRIBUTARY_CHANCE = 0.01F;
    private static final int TRIBUTARY_MIN_LENGTH = 20;
    private static final int TRIBUTARY_LENGTH_SPREAD = 50;
    /** A river that dead-ends makes a lake only below this height. */
    private static final float LAKE_MAX_ENDPOINT_HEIGHT = 0.4F;
    /** Both sides of the oasis margin count, so a span has to hold the margin twice. */
    private static final int MARGIN_SIDES = 2;
    private static final int LAKE_RADIUS_MIN = 2;
    private static final int LAKE_RADIUS_MAX = 5;
    // How much higher than the river endpoint a land tile may be and still be flooded by the lake
    private static final float LAKE_DEPTH_TOLERANCE = 0.05F;
    private static final float ISLAND_FALLOFF_POWER = 2.0F;

    // Multiplier of the tree density of the generation config, per map type
    private static final float TREE_FACTOR_DEFAULT = 1.0F;
    private static final float TREE_FACTOR_MEDITERRANEAN = 1.8F;
    private static final float TREE_FACTOR_TUNDRA = 1.5F;
    private static final float TREE_FACTOR_ISLANDS = 1.6F;

    // Iron ore and crystals: hills and mountains only, scattered thinly over the rock so they stay a find. Each rocky
    // tile is ore with a chance of 6 in a thousand, which is about one ore for every hundred and seventy rocky tiles.
    // Crystals need the crystal noise to pass its threshold first and then a chance of their own of 50 in a thousand
    // on the rocky tiles of those patches, and a tile that passes both is crystals, not ore. They are rarer than the
    // ore while the patches cover less than about an eighth of the rock, which the stone threshold of the generation config decides.
    // The chance is a hash of the tile, the seed and a salt of each resource, so a map is the same every time it is
    // generated from a seed. The salts make the two chances independent of each other, and a tile that passes both
    // becomes crystals. Both counts are proportional to the area of rocky terrain, so a map with a little rock still
    // has a few and a mountain range has some dozens.
    private static final int IRON_PER_MILLE = 6;
    private static final int CRYSTALS_PER_MILLE = 50;
    private static final int CHANCE_SCALE = 1000;
    // Salts of the tile hash, one for each resource that is placed by chance
    private static final long IRON_SALT = 0x5DEE_CE66_D1CE_4E5BL;
    private static final long CRYSTALS_SALT = 0xC0DE_D00D_B16B_00B5L;
    // Bits the tile hash is folded by, to feed its upper and lower halves back into the mix
    private static final int HASH_FOLD = 32;

    // Desert: one oasis per this many tiles of map, each a pocket of shallow water of this radius in tiles
    private static final int OASIS_AREA = 5000;
    private static final int OASIS_RADIUS = 2;
    private static final int OASIS_SITE_ATTEMPTS = 400;
    // Sand within this many tiles of water carries palms, thickly; the rest carries cacti, thinly
    public static final int OASIS_PALM_REACH = 2;
    private static final float DESERT_DRY_FACTOR = 0.25F;
    private static final float DESERT_WET_FACTOR = 2.5F;
    private static final float MAX_TREE_CHANCE = 0.35F;

    private float[][] islandCenters;

    /**
     * Creates a generator.
     *
     * @param noiseFactory builds the noise fields, which keeps the noise library outside the domain
     */
    public PerlinMapGenerator(NoiseSourceFactory noiseFactory) {
        this.noiseFactory = Objects.requireNonNull(noiseFactory, "noiseFactory");
    }

    @Override
    public TileMap generateMap(int width, int height, MapGenerationContext context) {
        config.update(context);
        var map = new TileMap(width, height);

        initializeNoiseGenerators();

        if (config.getMapType() == MapType.ISLANDS) {
            initializeIslandCenters();
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

        if (config.getMapType() == MapType.DESERT) {
            carveOases(map);
            plantDesertTrees(map);
        }

        return map;
    }

    // ==================================================================
    //  INITIALIZATION
    // ==================================================================

    private void initializeNoiseGenerators() {
        heightNoise = createNoiseGenerator(config.getSeed(), config.getHeightNoiseConfig());
        temperatureNoise = createNoiseGenerator(config.getSeed() + 1, config.getTemperatureNoiseConfig());
        moistureNoise = createNoiseGenerator(config.getSeed() + MOISTURE_SEED_OFFSET, config.getMoistureNoiseConfig());
        stoneNoise = createNoiseGenerator(config.getSeed() + STONE_SEED_OFFSET, config.getStoneNoiseConfig());
        crystalsNoise = createNoiseGenerator(config.getSeed() + CRYSTALS_SEED_OFFSET, config.getCrystalsNoiseConfig());
        treeNoise = createNoiseGenerator(config.getSeed() + TREE_SEED_OFFSET, config.getTreeNoiseConfig());
    }

    private NoiseSource createNoiseGenerator(long seed, AppConfig.NoiseConfig noiseConfig) {
        return noiseFactory.create(seed, new NoiseSettings(
                noiseConfig.getFrequency(),
                noiseConfig.getOctaves(),
                noiseConfig.getFractalType(),
                noiseConfig.getGain(),
                noiseConfig.getLacunarity()));
    }

    // ==================================================================
    //  TERRAIN GENERATION
    // ==================================================================

    private void generateTerrain(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                float heightValue = heightNoise.getNoise((float) x, (float) y);
                float tempValue = normalizeNoise(temperatureNoise.getNoise((float) x, (float) y));
                float moistureValue = normalizeNoise(moistureNoise.getNoise((float) x, (float) y));

                // Apply island shaping for island maps
                if (config.getMapType() == MapType.ISLANDS) {
                    heightValue = applyIslandEffect(x, y, width, height, heightValue);
                }

                var tileType = TileFactory.fromHeight(
                        x, y, heightValue, tempValue, moistureValue, config.getMapType(), config.getSeed()
                );

                var tile = new Tile(x, y, tileType);
                tile.setHeight(heightValue);

                assignTileResource(tile, x, y);
                map.setTile(x, y, tile);
            }
        }
    }

    private static boolean isRockyGround(TileType type) {
        return type == TileType.HILL || type == TileType.MOUNTAIN;
    }

    private static float normalizeNoise(float value) {
        return (float) (((double) value + 1) / NOISE_RANGE_SPAN);
    }

    private void initializeIslandCenters() {
        var rand = config.getRandom();
        islandCenters = new float[NUM_ISLANDS][ISLAND_CENTER_AXES];

        for (var i = 0; i < NUM_ISLANDS; i++) {
            islandCenters[i][0] = (float) (ISLAND_CENTER_MIN + (double) rand.nextFloat() * ISLAND_CENTER_SPAN); // x (20-80%)
            islandCenters[i][1] = (float) (ISLAND_CENTER_MIN + (double) rand.nextFloat() * ISLAND_CENTER_SPAN); // y (20-80%)
        }
    }

    private float applyIslandEffect(int x, int y, int width, int height, float heightValue) {
        if (islandCenters == null) {
            return heightValue;
        }
        final double nx = (double) x / width;
        final double ny = (double) y / height;
        var maxEffect = 0.0;

        for (float[] center : islandCenters) {
            final double dx = nx - center[0];
            final double dy = ny - center[1];
            final double distance = Math.sqrt(dx * dx + dy * dy) * ISLAND_DISTANCE_SCALE;

            final double effect = Math.pow(1.0 - Math.min(1.0, distance), ISLAND_FALLOFF_POWER);
            maxEffect = Math.max(maxEffect, effect);
        }

        return (float) (heightValue * maxEffect * ISLAND_HEIGHT_GAIN + (maxEffect * ISLAND_LIFT - ISLAND_SEA_OFFSET));
    }

    private void assignTileResource(Tile tile, int x, int y) {
        if (!tile.getType().isLand()) {
            return;
        }

        // Resource assignment priority: Crystals > Iron > Stone > Trees
        float crystalsValue = normalizeNoise(crystalsNoise.getNoise((float) x, (float) y));
        if (isRockyGround(tile.getType()) && crystalsValue > config.getCrystalsThreshold()
                && chancePasses(x, y, CRYSTALS_SALT, CRYSTALS_PER_MILLE)) {
            tile.setWorldResource(new Resource(ResourceType.CRYSTALS, config.getRandom().nextLong()));
            return;
        }

        // Iron is placed before stone, so the stone threshold of the generation config never changes how much there is
        if (isRockyGround(tile.getType()) && chancePasses(x, y, IRON_SALT, IRON_PER_MILLE)) {
            tile.setWorldResource(new Resource(ResourceType.IRON_ORE, config.getRandom().nextLong()));
            return;
        }

        float stoneValue = normalizeNoise(stoneNoise.getNoise((float) x, (float) y));
        if (stoneValue > config.getStoneThreshold()) {
            tile.setWorldResource(new Resource(ResourceType.STONE, config.getRandom().nextLong()));
            return;
        }

        if (tile.getType().isFertile()) {
            plantTreeOnFertileTile(tile, x, y);
        }
    }

    /**
     * Tells whether a tile passes a chance given in thousandths. The answer is a hash of the tile, the seed and the
     * salt of the resource rather than a draw from the shared random source, so it never moves the numbers the rest of
     * the generation draws. The tile goes through {@link Hashes#mix(int, int)} twice, the second time together with
     * the seed and the salt, so the neighbouring tiles, the seeds and the resources give unrelated answers.
     */
    private boolean chancePasses(int x, int y, long salt, int perMille) {
        final long tileHash = Hashes.mix(x, y) ^ (config.getSeed() + salt) * Hashes.GOLDEN_GAMMA;
        final long hash = Hashes.mix((int) (tileHash >>> HASH_FOLD), (int) tileHash);
        return Math.floorMod(hash, (long) CHANCE_SCALE) < perMille;
    }

    /** Puts a tree on a fertile tile where the tree noise passes the threshold the density of the map type sets. */
    private void plantTreeOnFertileTile(Tile tile, int x, int y) {
        // Desert: the palms and cacti are placed after the water, by plantDesertTrees
        if (config.getMapType() == MapType.DESERT) {
            return;
        }
        float treeDensityFactor = switch (config.getMapType()) {
            case MEDITERRANEAN -> TREE_FACTOR_MEDITERRANEAN;
            case TUNDRA -> TREE_FACTOR_TUNDRA;
            case ISLANDS -> TREE_FACTOR_ISLANDS;
            default -> TREE_FACTOR_DEFAULT;
        };
        float treeValue = normalizeNoise(treeNoise.getNoise((float) x, (float) y));
        double threshold = 1.0 - ((double) config.getTreeDensity() * treeDensityFactor);
        if (treeValue > threshold) {
            tile.setWorldResource(new Resource(ResourceType.TREE, config.getRandom().nextLong()));
        }
    }

    // ==================================================================
    //  DESERT OASES AND TREES
    // ==================================================================

    /**
     * Digs a few small oases into open sand: a pocket of shallow water with a deeper middle tile.
     * The number grows with the map area and every oasis needs a clear stretch of plain sand around it, so none lands
     * on a hill, on the shore of an existing lake or at the edge of the map.
     */
    private void carveOases(TileMap map) {
        final int clearRadius = OASIS_RADIUS + 1;
        final int margin = clearRadius + 1;
        if (map.getWidth() <= MARGIN_SIDES * margin || map.getHeight() <= MARGIN_SIDES * margin) {
            return;
        }
        final var random = config.getRandom();
        final int count = Math.max(1, map.getWidth() * map.getHeight() / OASIS_AREA);
        for (var i = 0; i < count; i++) {
            for (var attempt = 0; attempt < OASIS_SITE_ATTEMPTS; attempt++) {
                final int x = margin + random.nextInt(map.getWidth() - MARGIN_SIDES * margin);
                final int y = margin + random.nextInt(map.getHeight() - MARGIN_SIDES * margin);
                if (isPlainSand(map, x, y, clearRadius)) {
                    fillOasis(map, x, y);
                    break;
                }
            }
        }
    }

    /** Tells whether every tile within a radius of a point is sand, so an oasis fits without cutting into other ground. */
    private static boolean isPlainSand(TileMap map, int x, int y, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                final var tile = map.getTile(x + dx, y + dy);
                if (tile == null || tile.getType() != TileType.SAND) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Turns a disc of sand into shallow water with a deep centre and clears the resources on it. */
    private static void fillOasis(TileMap map, int centerX, int centerY) {
        for (int dx = -OASIS_RADIUS; dx <= OASIS_RADIUS; dx++) {
            for (int dy = -OASIS_RADIUS; dy <= OASIS_RADIUS; dy++) {
                final int squared = dx * dx + dy * dy;
                if (squared > OASIS_RADIUS * OASIS_RADIUS + 1) {
                    continue;
                }
                final var tile = map.getTile(centerX + dx, centerY + dy);
                tile.setType(squared == 0 ? TileType.DEEP_WATER : TileType.SHALLOW_WATER);
                tile.setWorldResource(null);
            }
        }
    }

    /**
     * Scatters the palms and cacti of the desert over open sand, thinly in the dry sand and thickly within
     * {@link #OASIS_PALM_REACH} tiles of any water, so the trees gather around the oases, lakes and rivers.
     * The renderer draws these tree resources as palms near water and as cacti elsewhere.
     */
    private void plantDesertTrees(TileMap map) {
        final var random = config.getRandom();
        final double dryChance = Math.min(MAX_TREE_CHANCE, (double) config.getTreeDensity() * DESERT_DRY_FACTOR);
        final double wetChance = Math.min(MAX_TREE_CHANCE, (double) config.getTreeDensity() * DESERT_WET_FACTOR);
        for (var y = 0; y < map.getHeight(); y++) {
            for (var x = 0; x < map.getWidth(); x++) {
                final var tile = map.getTile(x, y);
                if (tile.getType() != TileType.SAND || tile.hasResource()) {
                    continue;
                }
                final double chance = hasWaterWithin(map, x, y, OASIS_PALM_REACH) ? wetChance : dryChance;
                if (random.nextFloat() < chance) {
                    tile.setWorldResource(new Resource(ResourceType.TREE, random.nextLong()));
                }
            }
        }
    }

    /** Tells whether any water tile lies within a square radius of a point. */
    private static boolean hasWaterWithin(TileMap map, int x, int y, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                final var tile = map.getTile(x + dx, y + dy);
                if (tile != null && tile.getType().isWater()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================================================================
    //  WATER SYSTEMS
    // ==================================================================

    private static void applyBaseWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();

                if (tileHeight < BASE_DEEP_WATER_LEVEL) {
                    tile.setType(TileType.DEEP_WATER);
                    tile.setWorldResource(null);
                } else if (tileHeight < BASE_SHALLOW_WATER_LEVEL) {
                    tile.setType(TileType.SHALLOW_WATER);
                    tile.setWorldResource(null);
                }
            }
        }
    }

    private static void applyRiverWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();

                // Only override water tiles
                if (tileHeight < RIVERS_MAP_DEEP_WATER_LEVEL) {
                    tile.setType(TileType.DEEP_WATER);
                } else if (tileHeight < RIVERS_MAP_SHALLOW_WATER_LEVEL) {
                    tile.setType(TileType.SHALLOW_WATER);
                }

                // Clear resources from water tiles
                if (!tile.getType().isLand()) {
                    tile.setWorldResource(null);
                }
            }
        }
    }

    private static void applyIslandWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();

                if (tileHeight < ISLANDS_DEEP_WATER_LEVEL) {
                    tile.setType(TileType.DEEP_WATER);
                    tile.setWorldResource(null); // Resources are only removed on water
                } else if (tileHeight < ISLANDS_SHALLOW_WATER_LEVEL) {
                    tile.setType(TileType.SHALLOW_WATER);
                    tile.setWorldResource(null); // Resources are only removed on water
                } else if (tileHeight < ISLANDS_SAND_LEVEL) {
                    tile.setType(TileType.SAND);
                    // Resources on sand and land are kept
                }
            }
        }
    }

    private void applyTundraWaterLevels(TileMap map) {
        int width = map.getWidth();
        int height = map.getHeight();

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var tile = map.getTile(x, y);
                float tileHeight = tile.getHeight();
                float tempValue = normalizeNoise(temperatureNoise.getNoise((float) x, (float) y));

                if (tileHeight < TUNDRA_DEEP_WATER_LEVEL) {
                    tile.setType(TileType.DEEP_WATER);
                    tile.setWorldResource(null); // Resources are only removed on water
                } else if (tileHeight < TUNDRA_SHALLOW_WATER_LEVEL) {
                    freezeOrFlood(tile, tempValue);
                }
            }
        }
    }

    /** Turns a tile of the tundra shoreline into ice where it is cold and into shallow water elsewhere. */
    private static void freezeOrFlood(Tile tile, float temperature) {
        // Resources are only removed on ice and water
        tile.setType(temperature < TUNDRA_FREEZE_TEMPERATURE ? TileType.ICE : TileType.SHALLOW_WATER);
        tile.setWorldResource(null);
    }

    private void generateRiverSystems(TileMap map) {
        List<int[]> riverSources = identifyRiverSources(map);
        int riversToCreate = calculateRiverCount(riverSources.size());
        Set<String> riverTiles = new HashSet<>();

        Collections.shuffle(riverSources, config.getRandom());

        for (var i = 0; i < Math.min(riversToCreate, riverSources.size()); i++) {
            int[] source = riverSources.get(i);
            if (isValidRiverStart(map, source, riverTiles)) {
                createRiverPath(map, source[0], source[1], riverTiles);
            }
        }

        clearResourcesFromRivers(map, riverTiles);
    }

    private static List<int[]> identifyRiverSources(TileMap map) {
        List<int[]> sources = new ArrayList<>();
        int width = map.getWidth();
        int height = map.getHeight();

        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                var tile = map.getTile(x, y);
                if (tile.getType().isLand() && tile.getHeight() > RIVER_SOURCE_MIN_HEIGHT) {
                    sources.add(new int[]{x, y});
                }
            }
        }
        return sources;
    }

    private int calculateRiverCount(int potentialSources) {
        return Math.max(1, (int) (potentialSources * (double) config.getRiverDensity() * RIVER_SOURCE_SHARE));
    }

    private static boolean isValidRiverStart(TileMap map, int[] source, Set<String> riverTiles) {
        return map.getTile(source[0], source[1]).getType().isLand() &&
                !riverTiles.contains(source[0] + "," + source[1]);
    }

    private void createRiverPath(TileMap map, int startX, int startY, Set<String> riverTiles) {
        // The length is drawn before the source is looked at, which keeps the random sequence of a seed unchanged
        traceRiver(map, startX, startY, MIN_RIVER_LENGTH + config.getRandom().nextInt(MAX_RIVER_LENGTH - MIN_RIVER_LENGTH),
                riverTiles);
    }

    private void traceRiver(TileMap map, int startX, int startY, int maxLength, Set<String> riverTiles) {
        int x = startX;
        int y = startY;
        float currentHeight = map.getTile(x, y).getHeight();

        if (currentHeight < RIVER_SOURCE_MIN_HEIGHT) {
            return;
        }

        var length = 0;
        List<int[]> riverSegment = new ArrayList<>();
        while (length < maxLength) {
            var currentTile = map.getTile(x, y);

            // Terminate if we reach existing water
            if (isWaterTile(currentTile)) {
                applyRiverSegment(map, riverSegment);
                return;
            }

            String currentKey = x + "," + y;
            riverTiles.add(currentKey);
            riverSegment.add(new int[]{x, y});

            int[] nextCoord = findNextRiverPoint(map, x, y, currentHeight, riverTiles);
            if (nextCoord.length == 0) {
                createLakeAtEndpoint(map, riverSegment, x, y);
                break;
            }

            x = nextCoord[0];
            y = nextCoord[1];
            currentHeight = map.getTile(x, y).getHeight();
            length++;

            // Occasionally create tributaries
            if (length > TRIBUTARY_MIN_RIVER_LENGTH && config.getRandom().nextFloat() < TRIBUTARY_CHANCE) {
                createTributary(map, x, y, riverTiles);
            }
        }
        applyRiverSegment(map, riverSegment);
    }

    private static boolean isWaterTile(Tile tile) {
        return tile.getType() == TileType.RIVER ||
                tile.getType() == TileType.SHALLOW_WATER ||
                tile.getType() == TileType.DEEP_WATER;
    }

    /** @return the next tile of a river as [x, y], or an empty array when no neighbour lies lower */
    private int[] findNextRiverPoint(TileMap map, int x, int y, float currentHeight, Set<String> riverTiles) {
        float minHeight = currentHeight;
        List<int[]> candidates = new ArrayList<>();
        for (int[] dir : RIVER_DIRECTIONS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (!map.isValidCoordinate(nx, ny)) {
                continue;
            }

            var neighbor = map.getTile(nx, ny);
            String neighborKey = nx + "," + ny;
            float neighborHeight = neighbor.getHeight();

            if ((neighborHeight < currentHeight && !riverTiles.contains(neighborKey)) ||
                    (isWaterTile(neighbor) && neighborHeight < currentHeight)) {

                candidates.add(new int[]{nx, ny});
                if (neighborHeight < minHeight) {
                    minHeight = neighborHeight;
                }
            }
        }

        if (candidates.isEmpty()) {
            return NO_RIVER_POINT;
        }
        return selectLowestCandidate(map, candidates, minHeight);
    }

    private int[] selectLowestCandidate(TileMap map, List<int[]> candidates, float minHeight) {
        List<int[]> bestCandidates = new ArrayList<>();
        for (int[] coord : candidates) {
            if (Doubles.areEqual(map.getTile(coord[0], coord[1]).getHeight(), minHeight)) {
                bestCandidates.add(coord);
            }
        }
        return bestCandidates.get(config.getRandom().nextInt(bestCandidates.size()));
    }

    private static void applyRiverSegment(TileMap map, List<int[]> segment) {
        for (int[] point : segment) {
            var tile = map.getTile(point[0], point[1]);
            if (tile.getType().isLand()) {
                tile.setType(TileType.RIVER);
                tile.setWorldResource(null);
            }
        }
    }

    private void createLakeAtEndpoint(TileMap map, List<int[]> segment, int x, int y) {
        var endpoint = map.getTile(x, y);
        if (endpoint.getHeight() >= LAKE_MAX_ENDPOINT_HEIGHT || !endpoint.getType().isLand()) {
            return;
        }

        int radius = LAKE_RADIUS_MIN + config.getRandom().nextInt(LAKE_RADIUS_MAX - LAKE_RADIUS_MIN);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                flood(map, x + dx, y + dy, (float) ((double) endpoint.getHeight() + LAKE_DEPTH_TOLERANCE));
            }
        }
        applyRiverSegment(map, segment);
    }

    /** Turns a land tile lower than the limit into shallow water, or into ice on a tundra map. */
    private void flood(TileMap map, int x, int y, float heightLimit) {
        if (!map.isValidCoordinate(x, y)) {
            return;
        }
        var tile = map.getTile(x, y);
        if (tile.getType().isLand() && tile.getHeight() < heightLimit) {
            // Frozen lakes in tundra
            tile.setType(config.getMapType() == MapType.TUNDRA ? TileType.ICE : TileType.SHALLOW_WATER);
            tile.setWorldResource(null);
        }
    }

    private void createTributary(TileMap map, int startX, int startY, Set<String> riverTiles) {
        // The length is drawn before the start is looked at, which keeps the random sequence of a seed unchanged
        traceTributary(map, startX, startY, TRIBUTARY_MIN_LENGTH + config.getRandom().nextInt(TRIBUTARY_LENGTH_SPREAD),
                riverTiles);
    }

    private void traceTributary(TileMap map, int startX, int startY, int length, Set<String> riverTiles) {
        int x = startX;
        int y = startY;
        float currentHeight = map.getTile(x, y).getHeight();

        if (!map.getTile(x, y).getType().isLand() || currentHeight < RIVER_SOURCE_MIN_HEIGHT) {
            return;
        }

        List<int[]> tributary = new ArrayList<>();
        for (var i = 0; i < length; i++) {
            String currentKey = x + "," + y;
            if (riverTiles.contains(currentKey)) {
                applyRiverSegment(map, tributary);
                return;
            }

            riverTiles.add(currentKey);
            tributary.add(new int[]{x, y});

            int[] next = findNextRiverPoint(map, x, y, currentHeight, riverTiles);
            if (next.length == 0) {
                break;
            }

            x = next[0];
            y = next[1];
            currentHeight = map.getTile(x, y).getHeight();
        }
        applyRiverSegment(map, tributary);
    }

    private static void clearResourcesFromRivers(TileMap map, Set<String> riverTiles) {
        for (String key : riverTiles) {
            String[] coords = key.split(",");
            var x = Integer.parseInt(coords[0]);
            var y = Integer.parseInt(coords[1]);
            var tile = map.getTile(x, y);
            if (tile.getType() == TileType.RIVER) {
                tile.setWorldResource(null);
            }
        }
    }

    // ==================================================================
    //  GETTERS (Interface Implementation)
    // ==================================================================

    @Override public long getSeed() { return config.getSeed(); }
}
