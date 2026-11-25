package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.BalanceParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.EdgeCoastParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.EdgeCoastPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ForestFloorParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ForestFloorPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ClearingParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ClearingPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRule;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.HeightBandParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.HeightBandPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.LakeParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.NoiseFieldsParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.NoiseFieldsPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RiverParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ResourceBalancePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.ResourceQuota;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RiverCrossingParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RiverCrossingPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RiverPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RockResourceParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RockResourcePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RockyZoneParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RockyZonePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.SeaLevelParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.SeaLevelPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.SpawnParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.SpawnPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.StoneParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.StonePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.TrailNetworkPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.TrailParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.TributaryParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.VegetationParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.VegetationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.WaterBodyLimitPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.WaterBodyParams;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The settings and passes that every recipe shares, so a recipe lists only what makes its map different: its height
 * profile, its ground rule, how many rivers and trees it has and the passes only it uses.
 */
final class RecipeParts {

    // Noise: lacunarity of every fractal, and the settings of the climate fields and the patch fields
    private static final float LACUNARITY = 2F;
    private static final float CLIMATE_GAIN = 0.5F;
    private static final int CLIMATE_OCTAVES = 3;
    private static final float TEMPERATURE_FREQUENCY = 0.02F;
    private static final float MOISTURE_FREQUENCY = 0.03F;
    /** The rock noise varies slowly, so the rocky zones it picks are blobs of some tens of tiles. */
    private static final float ROCK_FREQUENCY = 0.035F;
    private static final float CRYSTALS_FREQUENCY = 0.09F;
    private static final float PATCH_FREQUENCY = 0.02F;

    /** The largest nudge of the height of a tile before the ground rule sees it, which breaks up straight borders. */
    private static final float DITHER_AMPLITUDE = 0.025F;

    // Rock: the share of rocky ground that holds each resource before the balancing pass brings the counts into range
    private static final int CRYSTALS_PER_MILLE = 40;
    private static final float CRYSTALS_THRESHOLD = 0.62F;
    private static final int STONE_PER_MILLE = 120;
    private static final int LOOSE_STONE_PER_TEN_THOUSAND = 4;

    /** The ground that rocky zones can replace in the maps that grow trees. */
    private static final Set<TileType> GREEN_HOSTS = EnumSet.of(TileType.GRASS, TileType.SAVANNA, TileType.SNOW,
            TileType.SWAMP);
    /** The ground that rocky zones replace in the desert. */
    private static final Set<TileType> SAND_HOSTS = EnumSet.of(TileType.SAND);

    // Quotas per 100,000 tiles of map, inside the guaranteed ranges of 40 to 100 iron, 10 to 30 crystals, 200 to 600
    // stone and at least 400 trees per 300x300 map
    private static final int CRYSTALS_MIN = 14;
    private static final int CRYSTALS_MAX = 32;
    private static final int STONE_MIN = 240;
    private static final int STONE_MAX = 620;
    private static final int TREES_MIN = 470;
    private static final int ROCKY_CAP_IRON = 80;
    private static final int ROCKY_CAP_CRYSTALS = 50;
    private static final int ROCKY_CAP_STONE = 250;

    // The forest floor reaches one tile beyond the trees
    private static final int FOREST_FLOOR_REACH = 1;

    // The player starts with this many tiles around them free of resources
    private static final int SPAWN_FREE_RADIUS = 3;

    // Rivers: the same everywhere but their density and the surface of their lakes
    /** A river starts on the highest tenth of the land, which every height profile has, unlike a fixed height. */
    private static final float RIVER_SOURCE_SHARE = 0.10F;
    private static final int RIVER_MIN_LENGTH = 100;
    private static final int RIVER_MAX_LENGTH = 300;
    private static final int TRIBUTARY_MIN_RIVER_LENGTH = 30;
    private static final float TRIBUTARY_CHANCE = 0.01F;
    private static final int TRIBUTARY_MIN_LENGTH = 20;
    private static final int TRIBUTARY_LENGTH_SPREAD = 50;
    /** A river that dead ends in the lowest 40% of the land floods a lake. */
    private static final float LAKE_MAX_ENDPOINT_SHARE = 0.4F;
    private static final int LAKE_MIN_RADIUS = 2;
    private static final int LAKE_MAX_RADIUS = 5;
    private static final float LAKE_DEPTH_TOLERANCE = 0.05F;

    /** Not instantiable: the class only holds static helpers. */
    private RecipeParts() {
    }

    /**
     * Describes the noise of the height of a map.
     *
     * @param frequency the scale of the pattern
     * @param octaves   the number of fractal layers
     * @param fractal   how the layers combine
     * @param gain      the amplitude factor between layers
     * @return the settings
     */
    static NoiseSettings height(float frequency, int octaves, FractalKind fractal, float gain) {
        return new NoiseSettings(frequency, octaves, fractal, gain, LACUNARITY);
    }

    /**
     * Builds the noise pass of a map.
     *
     * @param height        the settings of the height
     * @param climate       the climate fields the ground rule reads, a subset of temperature and moisture
     * @param treeFrequency the scale of the tree groves, low for few wide groves and high for many small ones
     * @return the pass, which also computes the rock, crystal and tree fields
     */
    static NoiseFieldsPass noiseWithTrees(NoiseSettings height, Set<NoiseField> climate, float treeFrequency) {
        final var fields = fieldsOf(height, climate);
        fields.put(NoiseField.TREE, patchNoise(treeFrequency));
        return new NoiseFieldsPass(new NoiseFieldsParams(fields));
    }

    /**
     * Builds the ground pass of a map.
     *
     * @param rule the ground rule of the map type
     * @return the pass
     */
    static GroundPass ground(GroundRule rule) {
        return new GroundPass(new GroundParams(rule, DITHER_AMPLITUDE));
    }

    /**
     * Builds the river pass of a map.
     *
     * @param density how many of the high tiles start a river
     * @param surface what the water of a lake is made of, shallow water or ice
     * @return the pass
     */
    static RiverPass rivers(float density, TileType surface) {
        return rivers(density, surface, LAKE_MAX_ENDPOINT_SHARE);
    }

    /**
     * Builds the river pass of a map with its own share of river ends that make a lake.
     *
     * @param density           how many of the high tiles start a river
     * @param surface           what the water of a lake is made of, shallow water or ice
     * @param lakeEndpointShare the share of the land whose river ends may make a lake, 0 for rivers that never do
     * @return the pass
     */
    static RiverPass rivers(float density, TileType surface, float lakeEndpointShare) {
        return rivers(density, surface, lakeEndpointShare, 0);
    }

    /**
     * Builds the river pass of a map whose rivers are all real ones: a river that dead ends before a number of tiles
     * is taken back and another source is tried.
     *
     * @param density           how many of the high tiles start a river
     * @param surface           what the water of a lake is made of, shallow water or ice
     * @param lakeEndpointShare the share of the land whose river ends may make a lake, 0 for rivers that never do
     * @param minKeptTiles      the fewest tiles a river covers to stay, at most the river length before a branch
     * @return the pass
     */
    static RiverPass rivers(float density, TileType surface, float lakeEndpointShare, int minKeptTiles) {
        final var tributaries = new TributaryParams(TRIBUTARY_MIN_RIVER_LENGTH, TRIBUTARY_CHANCE,
                TRIBUTARY_MIN_LENGTH, TRIBUTARY_LENGTH_SPREAD);
        final var lakes = new LakeParams(lakeEndpointShare, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS,
                LAKE_DEPTH_TOLERANCE, surface);
        return new RiverPass(new RiverParams(density, RIVER_SOURCE_SHARE, RIVER_MIN_LENGTH, RIVER_MAX_LENGTH,
                tributaries, lakes, minKeptTiles));
    }

    /**
     * Builds the pass that lowers the land along one edge of the map into a long coast.
     *
     * @param depthShare how far in from the edge the coast reaches, as a share of the size of the map across it
     * @param drop       how far the height drops at the edge
     * @return the pass
     */
    static EdgeCoastPass edgeCoast(float depthShare, float drop) {
        return new EdgeCoastPass(new EdgeCoastParams(depthShare, drop));
    }

    /**
     * Builds the pass that cuts rivers across the whole map from one edge to the opposite one.
     *
     * @param riversPer100k      how many rivers there are for every 100,000 tiles of map
     * @param minRivers          the fewest rivers a map has
     * @param maxRivers          the most rivers a map has
     * @param heightWeight       how much a river avoids high ground
     * @param meanderWeight      how much a river avoids dry ground
     * @param tributariesPerRiver how many short streams join each river
     * @return the pass
     */
    static RiverCrossingPass riverCrossing(int riversPer100k, int minRivers, int maxRivers, float heightWeight,
                                           float meanderWeight, int tributariesPerRiver) {
        return new RiverCrossingPass(new RiverCrossingParams(riversPer100k, minRivers, maxRivers, heightWeight,
                meanderWeight, tributariesPerRiver));
    }

    /**
     * Builds the pass that keeps a map to a few bodies of water.
     *
     * @param keepPer100k  how many of the largest bodies stay for every 100,000 tiles of map
     * @param minKeptTiles the fewest tiles a body needs to stay
     * @param fill         the land that replaces the bodies that do not stay
     * @return the pass
     */
    static WaterBodyLimitPass waterBodyLimit(int keepPer100k, int minKeptTiles, TileType fill) {
        return new WaterBodyLimitPass(new WaterBodyParams(keepPer100k, minKeptTiles, fill));
    }

    /**
     * Builds the pass that turns zones of the ground into rocky ground.
     *
     * @param landShare  the share of the host ground that becomes rock
     * @param heightBias how much the height pulls the zones towards high ground, 0 for the noise alone
     * @param reach      how far from a mountain the ground becomes rocky, 0 for none
     * @param hosts      the ground that can become rock
     * @return the pass
     */
    static RockyZonePass rockyZones(float landShare, float heightBias, int reach, Set<TileType> hosts) {
        return new RockyZonePass(new RockyZoneParams(landShare, heightBias, reach, hosts));
    }

    /**
     * Returns the ground that rocky zones replace on a map with trees: grass, savanna, snow and swamp.
     *
     * @return a new set
     */
    static Set<TileType> greenHosts() {
        return EnumSet.copyOf(GREEN_HOSTS);
    }

    /**
     * Returns the ground that rocky zones replace in the desert.
     *
     * @return a new set
     */
    static Set<TileType> sandHosts() {
        return EnumSet.copyOf(SAND_HOSTS);
    }

    /**
     * Builds the pass that scatters iron ore and crystals over the rocky ground.
     *
     * @param ironPerMille the share of rocky ground that holds iron, in thousandths
     * @return the pass
     */
    static RockResourcePass rock(int ironPerMille) {
        return new RockResourcePass(new RockResourceParams(ironPerMille, CRYSTALS_PER_MILLE, CRYSTALS_THRESHOLD));
    }

    /**
     * Builds the pass that puts stone on the rocky ground and a few loose stones elsewhere.
     *
     * @return the pass, the same for every map type
     */
    static StonePass stone() {
        return new StonePass(new StoneParams(STONE_PER_MILLE, LOOSE_STONE_PER_TEN_THOUSAND));
    }

    /**
     * Builds the pass that brings the resources into their guaranteed ranges.
     *
     * @param ironMin the fewest iron tiles per 100,000 tiles of map
     * @param ironMax the most iron tiles per 100,000 tiles of map
     * @param trees   the quota of the trees, which names the ground that hosts the trees the pass adds
     * @return the pass
     */
    static ResourceBalancePass balance(int ironMin, int ironMax, ResourceQuota trees) {
        return new ResourceBalancePass(new BalanceParams(List.of(
                new ResourceQuota(ResourceType.IRON_ORE, ironMin, ironMax, ResourceQuota.Host.ROCKY, ROCKY_CAP_IRON),
                new ResourceQuota(ResourceType.CRYSTALS, CRYSTALS_MIN, CRYSTALS_MAX, ResourceQuota.Host.ROCKY,
                        ROCKY_CAP_CRYSTALS),
                new ResourceQuota(ResourceType.STONE, STONE_MIN, STONE_MAX, ResourceQuota.Host.ROCKY,
                        ROCKY_CAP_STONE),
                trees)));
    }

    /**
     * Builds the quota of the trees of a map whose trees stand on fertile ground, with no maximum.
     *
     * @return the quota, which asks for at least {@link #TREES_MIN} trees per 100,000 tiles
     */
    static ResourceQuota fertileTrees() {
        return fertileTrees(ResourceQuota.UNLIMITED);
    }

    /**
     * Builds the quota of the trees of a map whose trees stand on fertile ground.
     *
     * @param maxPer100k the most trees per 100,000 tiles of map, or {@link ResourceQuota#UNLIMITED}
     * @return the quota
     */
    static ResourceQuota fertileTrees(int maxPer100k) {
        return ResourceQuota.onFertile(ResourceType.TREE, TREES_MIN, maxPer100k);
    }

    /**
     * Builds the quota of the trees of the desert, the palms and cacti, which stand on sand.
     *
     * @return the quota, which asks for at least {@link #TREES_MIN} trees per 100,000 tiles, with no maximum
     */
    static ResourceQuota sandTrees() {
        return ResourceQuota.onSand(ResourceType.TREE, TREES_MIN, ResourceQuota.UNLIMITED);
    }

    /**
     * Builds the pass that floors the ground under and around the trees with forest.
     *
     * @return the pass, the same for every map with trees
     */
    static ForestFloorPass forestFloor() {
        return new ForestFloorPass(new ForestFloorParams(FOREST_FLOOR_REACH,
                EnumSet.of(TileType.GRASS, TileType.SAVANNA)));
    }

    /**
     * Builds the pass that grows trees.
     *
     * @param density how thickly the trees grow
     * @return the pass
     */
    static VegetationPass vegetation(float density) {
        return new VegetationPass(new VegetationParams(density, 0, 0F));
    }

    /**
     * Builds the pass that grows trees over a fixed share of the land, so a map holds the same share of trees whatever
     * its noise or size and whatever the passes before it reserved.
     *
     * @param share the share of the land the trees cover, from the places where the tree noise is highest
     * @return the pass
     */
    static VegetationPass vegetationByShare(float share) {
        return new VegetationPass(new VegetationParams(share, 0, 0F, true));
    }

    /**
     * Builds the pass that grows trees thicker along the rivers.
     *
     * @param density    how thickly the trees grow away from the water
     * @param waterReach how many tiles from a river the trees grow thicker
     * @param waterBonus how much thicker, as a share of the tree noise range
     * @return the pass
     */
    static VegetationPass vegetationNearWater(float density, int waterReach, float waterBonus) {
        return new VegetationPass(new VegetationParams(density, waterReach, waterBonus));
    }

    /**
     * Builds the pass that gives a map the same share of water whatever its noise and size.
     *
     * @param waterShare the share of the map that lies below the level
     * @param level      the height that the ground rule of the map type counts as the top of its water
     * @return the pass
     */
    static SeaLevelPass seaLevel(float waterShare, float level) {
        return new SeaLevelPass(new SeaLevelParams(waterShare, level));
    }

    /**
     * Builds the pass that turns the highest part of the land into a band of rock.
     *
     * @param landShare the share of the land that becomes the band
     * @param band      the terrain of the band
     * @return the pass
     */
    static HeightBandPass heightBand(float landShare, TileType band) {
        return new HeightBandPass(new HeightBandParams(landShare, band));
    }

    /**
     * Builds the pass that opens clearings in the vegetation and the spawn clearing.
     *
     * @param spacing     the least distance between two clearings in tiles
     * @param minRadius   the smallest radius of an ordinary clearing
     * @param maxRadius   the largest radius of an ordinary clearing
     * @param spawnRadius the radius of the clearing the player starts in
     * @param clearance   how far the clearings keep from water and mountains
     * @param minRegion   the fewest tiles a piece of walkable ground has to hold to get a clearing when the sampling
     *                    missed it, 0 for none
     * @return the pass
     */
    static ClearingPass clearings(int spacing, int minRadius, int maxRadius, int spawnRadius, int clearance,
                                  int minRegion) {
        return new ClearingPass(new ClearingParams(spacing, minRadius, maxRadius, spawnRadius, clearance,
                minRegion));
    }

    /**
     * Builds the pass that joins the clearings with trails.
     *
     * @param noiseWeight  how much a thick grove adds to the cost of a tile
     * @param treeCost     what clearing a resource adds to the cost of a tile
     * @param mountainCost what a mountain tile costs, or {@link Float#POSITIVE_INFINITY} when no trail crosses one
     * @param extraPer100k how many trails beyond the tree there are for every 100,000 tiles of map
     * @param maxWidth     the widest trail in tiles
     * @param reach        the longest distance between two clearings a trail joins directly
     * @return the pass
     */
    static TrailNetworkPass trails(float noiseWeight, float treeCost, float mountainCost, int extraPer100k,
                                   int maxWidth, int reach) {
        return new TrailNetworkPass(new TrailParams(noiseWeight, treeCost, mountainCost, extraPer100k, maxWidth,
                reach));
    }

    /**
     * Builds the pass that chooses where the player starts.
     *
     * @return the pass, the same for every map type
     */
    static SpawnPass spawn() {
        return new SpawnPass(new SpawnParams(SPAWN_FREE_RADIUS));
    }

    /** The fields every map computes (height, rock, crystals, ground patches) and the climate fields it asks for. */
    private static EnumMap<NoiseField, NoiseSettings> fieldsOf(NoiseSettings height, Set<NoiseField> climate) {
        final var fields = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        fields.put(NoiseField.HEIGHT, height);
        if (climate.contains(NoiseField.TEMPERATURE)) {
            fields.put(NoiseField.TEMPERATURE, climateNoise(TEMPERATURE_FREQUENCY));
        }
        if (climate.contains(NoiseField.MOISTURE)) {
            fields.put(NoiseField.MOISTURE, climateNoise(MOISTURE_FREQUENCY));
        }
        fields.put(NoiseField.ROCK, patchNoise(ROCK_FREQUENCY));
        fields.put(NoiseField.CRYSTALS, patchNoise(CRYSTALS_FREQUENCY));
        fields.put(NoiseField.PATCH, patchNoise(PATCH_FREQUENCY));
        return fields;
    }

    /** The noise of a climate field: smooth and fractal. */
    private static NoiseSettings climateNoise(float frequency) {
        return new NoiseSettings(frequency, CLIMATE_OCTAVES, FractalKind.FBM, CLIMATE_GAIN, LACUNARITY);
    }

    /** The noise of a patch field: one plain layer, whose single scale sets how big the patches are. */
    private static NoiseSettings patchNoise(float frequency) {
        return new NoiseSettings(frequency, 0, FractalKind.NONE, 0F, 0F);
    }
}
