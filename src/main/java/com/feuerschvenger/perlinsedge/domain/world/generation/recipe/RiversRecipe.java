package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the rivers map: grassland and swampy lowland cut by a few long rivers that cross the whole map. */
final class RiversRecipe {

    private static final float HEIGHT_FREQUENCY = 0.01F;
    private static final int HEIGHT_OCTAVES = 6;
    private static final float HEIGHT_GAIN = 0.4F;
    private static final float TREE_FREQUENCY = 0.05F;
    /** The sea is cut down to the largest one or two bodies of at least 200 tiles, and the puddles become grass. */
    private static final int WATER_KEEP_PER_100K = 2;
    private static final int WATER_MIN_KEPT_TILES = 200;
    /** Three rivers on a map of 300 by 300 tiles, never fewer than two or more than four. */
    private static final int RIVERS_PER_100K = 3;
    private static final int MIN_RIVERS = 2;
    private static final int MAX_RIVERS = 4;
    private static final float RIVER_HEIGHT_WEIGHT = 4F;
    private static final float RIVER_MEANDER_WEIGHT = 1.5F;
    private static final int TRIBUTARIES_PER_RIVER = 1;
    /** The gallery forest: within three tiles of a river the trees grow 15% of the noise range thicker. */
    private static final int GALLERY_REACH = 3;
    private static final float GALLERY_BONUS = 0.15F;
    private static final float TREE_DENSITY = 0.30F;
    private static final float ROCKY_SHARE = 0.06F;
    private static final float ROCKY_HEIGHT_BIAS = 0F;
    private static final int ROCKY_FOOTHILL_REACH = 0;
    private static final int IRON_PER_MILLE = 40;
    private static final int IRON_MIN = 55;
    private static final int IRON_MAX = 105;

    /** Not instantiable: the class only builds the recipe. */
    private RiversRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#RIVERS}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.RIVERS, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.FBM, HEIGHT_GAIN),
                        Set.of(NoiseField.MOISTURE), TREE_FREQUENCY),
                RecipeParts.ground(GroundRules.RIVERS),
                RecipeParts.waterBodyLimit(WATER_KEEP_PER_100K, WATER_MIN_KEPT_TILES, TileType.GRASS),
                RecipeParts.riverCrossing(RIVERS_PER_100K, MIN_RIVERS, MAX_RIVERS, RIVER_HEIGHT_WEIGHT,
                        RIVER_MEANDER_WEIGHT, TRIBUTARIES_PER_RIVER),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.greenHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                RecipeParts.vegetationNearWater(TREE_DENSITY, GALLERY_REACH, GALLERY_BONUS),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.fertileTrees()),
                RecipeParts.forestFloor()));
    }
}
