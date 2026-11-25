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
    private static final float MOUNTAIN_START = 0.7F;
    private static final float WET_MOISTURE = 0.6F;

    // Dense forest: sea up to the shore, grass below the mountains
    private static final float DENSE_FOREST_DEEP = -0.4F;
    private static final float DENSE_FOREST_SHALLOW = -0.2F;

    // Swamp: the shallows reach further than in the other maps; between them and the lowland limit the ground is
    // swamp where it is wet and grass where it is dry
    private static final float SWAMP_DEEP = -0.4F;
    private static final float SWAMP_SHALLOW = -0.1F;
    private static final float SWAMP_LOWLAND = 0.2F;

    // Islands: deeper sea and a band of sand around every island
    private static final float ISLANDS_DEEP = -0.6F;
    private static final float ISLANDS_SHALLOW = -0.4F;
    private static final float ISLANDS_SAND = -0.2F;

    // Sea shared by the maps whose own water lies high in the range, and by the desert, whose only water besides the
    // sea is dug later: the sea below these heights comes first, and the type's own water lies above them
    private static final float HIGH_WATER_SEA_DEEP = -0.4F;
    private static final float HIGH_WATER_SEA_SHALLOW = -0.2F;

    // Tundra: the sea freezes where it is cold, the inland is snow where it is cold or high
    private static final float TUNDRA_DEEP = -0.4F;
    private static final float TUNDRA_SHALLOW = -0.2F;
    private static final float TUNDRA_INLAND = 0.2F;
    /** Shallow water freezes below this temperature. */
    private static final float TUNDRA_FREEZE_TEMPERATURE = 0.3F;
    /** Inland ground is snow below this temperature or above this height. */
    private static final float TUNDRA_SNOW_TEMPERATURE = 0.5F;
    private static final float TUNDRA_SNOW_HEIGHT = 0.5F;
    /** The ground between the frozen coast and the inland is snow below this temperature. */
    private static final float TUNDRA_TRANSITION_SNOW_TEMPERATURE = 0.45F;

    // Rivers: deep sea only at the bottom, swamp in the wet lowland
    private static final float RIVERS_DEEP = -0.6F;
    private static final float RIVERS_SHALLOW = -0.5F;
    private static final float RIVERS_SWAMP_MAX_HEIGHT = 0.3F;

    // Mediterranean: a sandy shore and grass up to the rocky ground
    private static final float MEDITERRANEAN_DEEP = -0.4F;
    private static final float MEDITERRANEAN_SHALLOW = -0.2F;

    // Mountainous: the water lies high, between the peaks, and the rest is valley floor
    private static final float MOUNTAINOUS_DEEP = 0.2F;
    private static final float MOUNTAINOUS_SHALLOW = 0.3F;

    private static final Set<NoiseField> MOISTURE_ONLY = EnumSet.of(NoiseField.MOISTURE);
    private static final Set<NoiseField> TEMPERATURE_ONLY = EnumSet.of(NoiseField.TEMPERATURE);

    /**
     * Sea, then grass below the mountains, which stand above it. Reads no climate: forest is not a ground but the floor
     * under the trees, which a later pass puts there.
     */
    public static final GroundRule DENSE_FOREST = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> denseForest(height));

    /** Sea with wide shallows, then swamp or grass by moisture. Reads the moisture. */
    public static final GroundRule SWAMP = new GroundRule(MOISTURE_ONLY,
            (float height, float temperature, float moisture) -> swamp(height, moisture));

    /** Deep sea, shallows, a sand shore, then grass and mountains. Reads no climate. */
    public static final GroundRule ISLANDS = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> islands(height));

    /** Sea at the bottom of the height range and sand everywhere else, with no inland water; no climate. */
    public static final GroundRule DESERT = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> desert(height));

    /** Frozen sea and coast, then snow and grass inland by temperature and height. Reads the temperature. */
    public static final GroundRule TUNDRA = new GroundRule(TEMPERATURE_ONLY,
            (float height, float temperature, float moisture) -> tundra(height, temperature));

    /** Deep sea only at the very bottom, sand, wet swamp lowland and grass. Reads the moisture. */
    public static final GroundRule RIVERS = new GroundRule(MOISTURE_ONLY,
            (float height, float temperature, float moisture) -> rivers(height, moisture));

    /** Sea, a sand shore, then grass. Reads no climate. */
    public static final GroundRule MEDITERRANEAN = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> mediterranean(height));

    /** Sea at the bottom, water high in the range, grass everywhere else. Reads no climate. */
    public static final GroundRule MOUNTAINOUS = GroundRule.ofHeight(
            (float height, float temperature, float moisture) -> mountainous(height));

    /** Not instantiable: the class only holds the rules. */
    private GroundRules() {
    }

    /** The dense forest: water below the shore, then grass and the mountains. */
    private static TileType denseForest(float height) {
        if (height < DENSE_FOREST_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < DENSE_FOREST_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        return highGround(height);
    }

    /** The swamp: shallows up to the lowland, swamp where it is wet, then grass. */
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
        return TileType.GRASS;
    }

    /** The islands: deep sea, shallows, sand, then the same grass and mountains as the dense forest. */
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

    /** The desert: sea at the bottom, sand everywhere else. The oases are dug into the sand by a later pass. */
    private static TileType desert(float height) {
        return isHighWaterSea(height) ? highWaterSea(height) : TileType.SAND;
    }

    /** The tundra: deep water, then ice or shallows by temperature, an icy coast, snow and grass. */
    private static TileType tundra(float height, float temperature) {
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
            return inlandTundra(height, temperature);
        }
        return temperature < TUNDRA_TRANSITION_SNOW_TEMPERATURE ? TileType.SNOW : TileType.GRASS;
    }

    /** The tundra above the coast: snow where it is cold or high, grass otherwise. */
    private static TileType inlandTundra(float height, float temperature) {
        return temperature < TUNDRA_SNOW_TEMPERATURE || height > TUNDRA_SNOW_HEIGHT ? TileType.SNOW : TileType.GRASS;
    }

    /**
     * The rivers map: deep sea only at the bottom, sand to the coast, swamp in the wet lowland and grass.
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
        return TileType.GRASS;
    }

    /** The Mediterranean: sea, a sand shore up to the coast, then grass. */
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
        return TileType.GRASS;
    }

    /** The mountains: sea at the bottom, water between the peaks, grass everywhere else. */
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
        return TileType.GRASS;
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

    /** Grass below the mountain start and mountains above, shared by two rules. */
    private static TileType highGround(float height) {
        return height < MOUNTAIN_START ? TileType.GRASS : TileType.MOUNTAIN;
    }
}
