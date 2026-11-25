package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the dense forest: rolling forest on wet ground with a few rivers and thick groves of trees. */
final class DenseForestRecipe {

    private static final float HEIGHT_FREQUENCY = 0.02F;
    private static final int HEIGHT_OCTAVES = 4;
    private static final float HEIGHT_GAIN = 0.4F;
    private static final float TREE_FREQUENCY = 0.04F;
    private static final float RIVER_DENSITY = 0.01F;
    private static final float TREE_DENSITY = 0.43F;
    private static final float ROCKY_SHARE = 0.03F;
    private static final float ROCKY_HEIGHT_BIAS = 0F;
    private static final int ROCKY_FOOTHILL_REACH = 0;
    private static final int IRON_PER_MILLE = 40;
    private static final int IRON_MIN = 55;
    private static final int IRON_MAX = 105;
    /**
     * The most trees per 100,000 tiles of map. This is a count against the map, not against the land, and it assumes
     * the land is at least 76% of a dense forest map, so 32,000 trees stay under the 42% of the land that closes the
     * 28% to 42% band the map type holds, which the density alone only calibrates. A recipe change that makes the
     * water more than a quarter of the map has to lower it.
     */
    private static final int TREES_MAX = 32_000;

    /** Not instantiable: the class only builds the recipe. */
    private DenseForestRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#DENSE_FOREST}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.DENSE_FOREST, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.FBM, HEIGHT_GAIN),
                        Set.of(), TREE_FREQUENCY),
                RecipeParts.ground(GroundRules.DENSE_FOREST),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.greenHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.fertileTrees(TREES_MAX)),
                RecipeParts.forestFloor()));
    }
}
