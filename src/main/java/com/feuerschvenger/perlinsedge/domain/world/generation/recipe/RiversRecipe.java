package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the rivers map: a dry coast, swampy lowland and the densest rivers of any map. */
final class RiversRecipe {

    private static final float HEIGHT_FREQUENCY = 0.01F;
    private static final int HEIGHT_OCTAVES = 6;
    private static final float HEIGHT_GAIN = 0.4F;
    private static final float TREE_FREQUENCY = 0.05F;
    private static final float RIVER_DENSITY = 0.04F;
    private static final float TREE_DENSITY = 0.30F;

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
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                RecipeParts.rock(),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                RecipeParts.spawn()));
    }
}
