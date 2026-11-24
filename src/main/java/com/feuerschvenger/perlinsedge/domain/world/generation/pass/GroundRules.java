package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/**
 * The ground rule of each map type. Every rule holds the whole classification of its type: where the sea, the shore,
 * the ice and the land begin are all decided by one set of thresholds, so no later step has to correct the result.
 * Heights below a threshold belong to the band of that threshold, and the bands are checked from the lowest up.
 */
public final class GroundRules {

    // Heights shared by several rules
    private static final float COAST_LEVEL = 0F;
    private static final float GRASS_LIMIT = 0.3F;
    private static final float HILL_START = 0.6F;
    private static final float MOUNTAIN_START = 0.7F;
    private static final float WET_MOISTURE = 0.6F;

    // Dense forest: sea up to the shore, forest where it is wet and below the hills
    private static final float DENSE_FOREST_DEEP = -0.4F;
    private static final float DENSE_FOREST_SHALLOW = -0.2F;

    // Swamp: the shallows reach further than in the other maps, and the lowland is swamp where it is wet
    private static final float SWAMP_DEEP = -0.4F;
    private static final float SWAMP_SHALLOW = -0.1F;
    private static final float SWAMP_LOWLAND = 0.2F;

    // Islands: deeper sea and a band of sand around every island
    private static final float ISLANDS_DEEP = -0.6F;
    private static final float ISLANDS_SHALLOW = -0.4F;
    private static final float ISLANDS_SAND = -0.2F;

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

    // Mediterranean: a sandy shore and grass up to the hills
    private static final float MEDITERRANEAN_DEEP = -0.4F;
    private static final float MEDITERRANEAN_SHALLOW = -0.2F;

    // Mountainous: the water lies high, between the peaks
    private static final float MOUNTAINOUS_DEEP = 0.2F;
    private static final float MOUNTAINOUS_SHALLOW = 0.3F;
    private static final float MOUNTAINOUS_GRASS_LIMIT = 0.4F;

    /** Sea, then forest where it is wet below the hills, then grass, hills and mountains. */
    public static final GroundRule DENSE_FOREST = (float height, float temperature, float moisture) ->
            denseForest(height, moisture);

    /** Sea with wide shallows, swamp in the wet lowland, then grass and forest. */
    public static final GroundRule SWAMP = (float height, float temperature, float moisture) ->
            swamp(height, moisture);

    /** Deep sea, shallows, a sand shore, then grass, hills and mountains. */
    public static final GroundRule ISLANDS = (float height, float temperature, float moisture) -> islands(height);

    /** Water at the top of the height range, sand below the hills and hills above. */
    public static final GroundRule DESERT = (float height, float temperature, float moisture) -> desert(height);

    /** Frozen sea and coast, then snow, forest and grass inland by temperature and moisture. */
    public static final GroundRule TUNDRA = GroundRules::tundra;

    /** Deep sea only at the very bottom, sand, wet swamp lowland, grass and hills. */
    public static final GroundRule RIVERS = (float height, float temperature, float moisture) ->
            rivers(height, moisture);

    /** Sea, a sand shore, grass up to the hills. */
    public static final GroundRule MEDITERRANEAN = (float height, float temperature, float moisture) ->
            mediterranean(height);

    /** Water high in the range, grass at the valley floor and hills above. */
    public static final GroundRule MOUNTAINOUS = (float height, float temperature, float moisture) ->
            mountainous(height);

    /** Not instantiable: the class only holds the rules. */
    private GroundRules() {
    }

    /** The dense forest: water below the shore, forest on wet ground below the hills. */
    private static TileType denseForest(float height, float moisture) {
        if (height < DENSE_FOREST_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < DENSE_FOREST_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        if (moisture > WET_MOISTURE && height < HILL_START) {
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

    /** The islands: deep sea, shallows, sand, then the same grass, hills and mountains as the dense forest. */
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

    /** The desert: water at the top of the range, sand below the hills. */
    private static TileType desert(float height) {
        if (height < DESERT_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < DESERT_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        return height < HILL_START ? TileType.SAND : TileType.HILL;
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

    /** The rivers map: deep sea only at the bottom, sand to the coast, swamp in the wet lowland, grass and hills. */
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
        return height < GRASS_LIMIT ? TileType.GRASS : TileType.HILL;
    }

    /** The Mediterranean: sea, a sand shore up to the coast, grass up to the hills. */
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
        return height < HILL_START ? TileType.GRASS : TileType.HILL;
    }

    /** The mountains: water between the peaks, grass on the valley floor, hills above. */
    private static TileType mountainous(float height) {
        if (height < MOUNTAINOUS_DEEP) {
            return TileType.DEEP_WATER;
        }
        if (height < MOUNTAINOUS_SHALLOW) {
            return TileType.SHALLOW_WATER;
        }
        return height < MOUNTAINOUS_GRASS_LIMIT ? TileType.GRASS : TileType.HILL;
    }

    /** Grass below the grass limit, hills below the mountain start and mountains above, shared by two rules. */
    private static TileType highGround(float height) {
        if (height < GRASS_LIMIT) {
            return TileType.GRASS;
        }
        return height < MOUNTAIN_START ? TileType.HILL : TileType.MOUNTAIN;
    }
}
