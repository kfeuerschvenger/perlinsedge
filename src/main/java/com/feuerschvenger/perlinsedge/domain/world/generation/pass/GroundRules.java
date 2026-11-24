package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.EnumSet;
import java.util.Set;

/**
 * The ground rule of each map type. Every rule holds the whole classification of its type: where the sea, the shore,
 * the ice and the land begin are all decided by one set of thresholds, so no later step has to correct the result. The
 * height picks the band a tile belongs to, with the bands checked from the lowest up. Within some bands the moisture
 * or the temperature picks between two terrains, such as forest or grass on wet or dry ground. Each rule declares the
 * climate fields it reads, and the others reach it as a constant.
 */
public final class GroundRules {

    // Heights shared by several rules
    private static final float COAST_LEVEL = 0F;
    private static final float GRASS_LIMIT = 0.3F;
    private static final float ROCKY_START = 0.6F;
    private static final float MOUNTAIN_START = 0.7F;
    private static final float WET_MOISTURE = 0.6F;

    // Dense forest: sea up to the shore, forest where it is wet and below the rocky ground
    private static final float DENSE_FOREST_DEEP = -0.4F;
    private static final float DENSE_FOREST_SHALLOW = -0.2F;

    // Swamp: the shallows reach further than in the other maps; between them and the lowland limit the ground is
    // swamp where it is wet and grass where it is dry, and above the lowland the height alone gives grass or forest
    private static final float SWAMP_DEEP = -0.4F;
    private static final float SWAMP_SHALLOW = -0.1F;
    private static final float SWAMP_LOWLAND = 0.2F;

    // Islands: deeper sea and a band of sand around every island
    private static final float ISLANDS_DEEP = -0.6F;
    private static final float ISLANDS_SHALLOW = -0.4F;
    private static final float ISLANDS_SAND = -0.2F;

    // Sea shared by the maps whose own water lies high in the range: the sea below these heights comes first, and the
    // type's own water lies above them
    private static final float HIGH_WATER_SEA_DEEP = -0.4F;
    private static final float HIGH_WATER_SEA_SHALLOW = -0.2F;

    // Desert: the floor is high, so its water lies at the top of the range
    private static final float DESERT_DEEP = 0.15F;
    private static final float DESERT_SHALLOW = 0.2F;

    // Tundra: the sea freezes where it is cold, the inland is snow where it is cold or high
    private static final float TUNDRA_DEEP = -0.4F;
    private static final float TUNDRA_SHALLOW = -0.2F;
    private static final float TUNDRA_INLAND = 0.2F;
    /** Shallow water freezes below this temperature. */
    private static final float TUNDRA_FREEZE_TEMPERATURE = 0.3F;
    /** Inland ground is snow below this temperature or above this height. */
    private static final float TUNDRA_SNOW_TEMPERATURE = 0.5F;
    private static final float TUNDRA_SNOW_HEIGHT = 0.5F;
    /** Inland ground wet enough and low enough grows forest. */
    private static final float TUNDRA_FOREST_MOISTURE = 0.5F;
    private static final float TUNDRA_FOREST_MAX_HEIGHT = 0.4F;
    /** The ground between the frozen coast and the inland is snow below this temperature. */
    private static final float TUNDRA_TRANSITION_SNOW_TEMPERATURE = 0.45F;

    // Rivers: deep sea only at the bottom, swamp in the wet lowland
    private static final float RIVERS_DEEP = -0.6F;
    private static final float RIVERS_SHALLOW = -0.5F;
    private static final float RIVERS_SWAMP_MAX_HEIGHT = 0.3F;

    // Mediterranean: a sandy shore and grass up to the rocky ground
    private static final float MEDITERRANEAN_DEEP = -0.4F;
    private static final float MEDITERRANEAN_SHALLOW = -0.2F;

    // Mountainous: the water lies high, between the peaks
    private static final float MOUNTAINOUS_DEEP = 0.2F;
    private static final float MOUNTAINOUS_SHALLOW = 0.3F;
    private static final float MOUNTAINOUS_GRASS_LIMIT = 0.4F;

    private static final Set<NoiseField> MOISTURE_ONLY = EnumSet.of(NoiseField.MOISTURE);
    private static final Set<NoiseField> TEMPERATURE_AND_MOISTURE = EnumSet.of(NoiseField.TEMPERATURE,
            NoiseField.MOISTURE);

    /**
     * Sea, then forest where it is wet below the rocky ground, then grass, rocky ground and mountains. Reads the
     * moisture.
     */
    public static final GroundRule DENSE_FOREST = new GroundRule(MOISTURE_ONLY,
            (float height, float temperature, float moisture) -> denseForest(height, moisture));

    /** Sea with wide shallows, swamp or grass by moisture in the lowland, then grass or forest by height. */
    public static final GroundRule SWAMP = new GroundRule(MOISTURE_ONLY,
            (float height, float temperature, float moisture) -> swamp(height, moisture));

    /** Deep sea, shallows, a sand shore, then grass, rocky ground and mountains. Reads no climate. */
    public static final GroundRule ISLANDS = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> islands(height));

    /**
     * Sea at the bottom, water at the top of the height range, sand below the rocky ground, rocky ground above; no
     * climate.
     */
    public static final GroundRule DESERT = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> desert(height));

    /** Frozen sea and coast, then snow, forest and grass inland by temperature and moisture. Reads both. */
    public static final GroundRule TUNDRA = new GroundRule(TEMPERATURE_AND_MOISTURE, GroundRules::tundra);

    /** Deep sea only at the very bottom, sand, wet swamp lowland, grass and rocky ground. Reads the moisture. */
    public static final GroundRule RIVERS = new GroundRule(MOISTURE_ONLY,
            (float height, float temperature, float moisture) -> rivers(height, moisture));

    /** Sea, a sand shore, grass up to the rocky ground. Reads no climate. */
    public static final GroundRule MEDITERRANEAN = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> mediterranean(height));

    /** Sea at the bottom, water high in the range, grass at the valley floor, rocky ground above. Reads no climate. */
    public static final GroundRule MOUNTAINOUS = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> mountainous(height));

    /** Not instantiable: the class only holds the rules. */
    private GroundRules() {
    }

    /** The dense forest: water below the shore, forest on wet ground below the rocky ground. */
    private static TileType denseForest(float height, float moisture) {
        if (height < DENSE_FOREST_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < DENSE_FOREST_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        if (moisture > WET_MOISTURE && height < ROCKY_START) {
            return TileType.FOREST;
        }
        return highGround(height);
    }

    /** The swamp: shallows up to the lowland, swamp where it is wet, then grass and forest. */
    private static TileType swamp(float height, float moisture) {
        if (height < SWAMP_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < SWAMP_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        if (moisture > WET_MOISTURE && height < SWAMP_LOWLAND) {
            return TileType.SWAMP;
        }
        return height < GRASS_LIMIT ? TileType.GRASS : TileType.FOREST;
    }

    /** The islands: deep sea, shallows, sand, then the same grass, rocky ground and mountains as the dense forest. */
    private static TileType islands(float height) {
        if (height < ISLANDS_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < ISLANDS_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        if (height < ISLANDS_SAND) {
            return TileType.SAND;
        }
        return highGround(height);
    }

    /** The desert: sea at the bottom, then water at the top of the range, sand below the rocky ground. */
    private static TileType desert(float height) {
        if (isHighWaterSea(height)) {
            return highWaterSea(height);
        }
        if (height < DESERT_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < DESERT_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        return height < ROCKY_START ? TileType.SAND : TileType.ROCKY_GROUND;
    }

    /** The tundra: deep water, then ice or shallows by temperature, an icy coast, snow, forest and grass. */
    private static TileType tundra(float height, float temperature, float moisture) {
        if (height < TUNDRA_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < TUNDRA_SHALLOW) {
            return temperature < TUNDRA_FREEZE_TEMPERATURE ? TileType.ICE : TileType.SHALLOW_WATER;
        }
        if (height < COAST_LEVEL) {
            return TileType.ICE;
        }
        if (height > TUNDRA_INLAND) {
            return inlandTundra(height, temperature, moisture);
        }
        return temperature < TUNDRA_TRANSITION_SNOW_TEMPERATURE ? TileType.SNOW : TileType.GRASS;
    }

    /** The tundra above the coast: snow where it is cold or high, forest where it is damp enough, grass otherwise. */
    private static TileType inlandTundra(float height, float temperature, float moisture) {
        if (temperature < TUNDRA_SNOW_TEMPERATURE || height > TUNDRA_SNOW_HEIGHT) {
            return TileType.SNOW;
        }
        if (moisture > TUNDRA_FOREST_MOISTURE && height < TUNDRA_FOREST_MAX_HEIGHT) {
            return TileType.FOREST;
        }
        return TileType.GRASS;
    }

    /**
     * The rivers map: deep sea only at the bottom, sand to the coast, swamp in the wet lowland, grass and rocky
     * ground.
     */
    private static TileType rivers(float height, float moisture) {
        if (height < RIVERS_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < RIVERS_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        if (height < COAST_LEVEL) {
            return TileType.SAND;
        }
        if (moisture > WET_MOISTURE && height < RIVERS_SWAMP_MAX_HEIGHT) {
            return TileType.SWAMP;
        }
        return height < GRASS_LIMIT ? TileType.GRASS : TileType.ROCKY_GROUND;
    }

    /** The Mediterranean: sea, a sand shore up to the coast, grass up to the rocky ground. */
    private static TileType mediterranean(float height) {
        if (height < MEDITERRANEAN_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < MEDITERRANEAN_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        if (height < COAST_LEVEL) {
            return TileType.SAND;
        }
        return height < ROCKY_START ? TileType.GRASS : TileType.ROCKY_GROUND;
    }

    /** The mountains: sea at the bottom, water between the peaks, grass on the valley floor, rocky ground above. */
    private static TileType mountainous(float height) {
        if (isHighWaterSea(height)) {
            return highWaterSea(height);
        }
        if (height < MOUNTAINOUS_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < MOUNTAINOUS_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        return height < MOUNTAINOUS_GRASS_LIMIT ? TileType.GRASS : TileType.ROCKY_GROUND;
    }

    /**
     * Tells whether a height lies in the sea of the maps whose own water lies high. Like every band of a rule, the
     * sea bands are checked against the height the ground pass dithered, not the stored height.
     */
    private static boolean isHighWaterSea(float height) {
        return height < HIGH_WATER_SEA_SHALLOW;
    }

    /** Returns the terrain of a height in the sea, deep below {@code HIGH_WATER_SEA_DEEP} and shallow above it. */
    private static TileType highWaterSea(float height) {
        return height < HIGH_WATER_SEA_DEEP ? TileType.DEEP_WATER : TileType.SHALLOW_WATER;
    }

    /** Grass below the grass limit, rocky ground below the mountain start and mountains above, shared by two rules. */
    private static TileType highGround(float height) {
        if (height < GRASS_LIMIT) {
            return TileType.GRASS;
        }
        return height < MOUNTAIN_START ? TileType.ROCKY_GROUND : TileType.MOUNTAIN;
    }
}
