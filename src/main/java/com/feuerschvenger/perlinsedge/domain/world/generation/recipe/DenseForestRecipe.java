package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
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
    private static final float TREE_FREQUENCY = 0.05F;
    private static final float RIVER_DENSITY = 0.01F;
    private static final float TREE_DENSITY = 0.32F;

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
                        Set.of(NoiseField.MOISTURE), TREE_FREQUENCY),
                RecipeParts.ground(GroundRules.DENSE_FOREST),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                RecipeParts.rock(),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                RecipeParts.spawn()));
    }
}
