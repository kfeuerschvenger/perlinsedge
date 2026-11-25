package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the swamp: wide shallows, swamp on wet lowland, many rivers and thick groves of trees. */
final class SwampRecipe {

    private static final float HEIGHT_FREQUENCY = 0.02F;
    private static final int HEIGHT_OCTAVES = 4;
    private static final float HEIGHT_GAIN = 0.4F;
    private static final float TREE_FREQUENCY = 0.05F;
    private static final float RIVER_DENSITY = 0.025F;
    private static final float TREE_DENSITY = 0.32F;
    private static final float ROCKY_SHARE = 0.03F;
    private static final float ROCKY_HEIGHT_BIAS = 0F;
    private static final int ROCKY_FOOTHILL_REACH = 0;
    private static final int IRON_PER_MILLE = 40;
    private static final int IRON_MIN = 55;
    private static final int IRON_MAX = 105;

    /** Not instantiable: the class only builds the recipe. */
    private SwampRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#SWAMP}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.SWAMP, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.FBM, HEIGHT_GAIN),
                        Set.of(NoiseField.MOISTURE), TREE_FREQUENCY),
                RecipeParts.ground(GroundRules.SWAMP),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.greenHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.fertileTrees()),
                RecipeParts.forestFloor()));
    }
}
