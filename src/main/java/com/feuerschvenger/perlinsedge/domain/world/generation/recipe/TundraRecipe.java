package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the tundra: frozen coasts and lakes, snow and forest by temperature and moisture, and wide groves. */
final class TundraRecipe {

    private static final float HEIGHT_FREQUENCY = 0.015F;
    private static final int HEIGHT_OCTAVES = 5;
    private static final float HEIGHT_GAIN = 0.5F;
    private static final float TREE_FREQUENCY = 0.01F;
    private static final float RIVER_DENSITY = 0.03F;
    private static final float TREE_DENSITY = 0.51F;

    /** Not instantiable: the class only builds the recipe. */
    private TundraRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#TUNDRA}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.TUNDRA, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.FBM, HEIGHT_GAIN),
                        Set.of(NoiseField.TEMPERATURE, NoiseField.MOISTURE), TREE_FREQUENCY),
                RecipeParts.ground(GroundRules.TUNDRA),
                RecipeParts.rivers(RIVER_DENSITY, TileType.ICE),
                RecipeParts.rock(),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                RecipeParts.spawn()));
    }
}
