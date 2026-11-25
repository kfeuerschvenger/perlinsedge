package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the Mediterranean: a long coast along one edge, a sandy shore and grassland with thick groves. */
final class MediterraneanRecipe {

    private static final float HEIGHT_FREQUENCY = 0.025F;
    private static final int HEIGHT_OCTAVES = 5;
    private static final float HEIGHT_GAIN = 0.5F;
    private static final float TREE_FREQUENCY = 0.05F;
    private static final float RIVER_DENSITY = 0.006F;
    /** The rivers make no lake at a dead end: the limit runs before the rivers, so a lake would be an extra body. */
    private static final float RIVER_LAKE_SHARE = 0F;
    /**
     * The coast reaches a fifth of the map in from one edge, 60 tiles on a map of 300, and lowers the height there by
     * 0.8, enough to flood the lowland.
     */
    private static final float COAST_DEPTH_SHARE = 0.2F;
    private static final float COAST_DROP = 0.8F;
    /** The sea keeps its few largest bodies, and the ponds the coast left behind turn to grass. */
    private static final int WATER_KEEP_PER_100K = 4;
    private static final int WATER_MIN_KEPT_TILES = 30;
    private static final float TREE_DENSITY = 0.306F;
    private static final float ROCKY_SHARE = 0.03F;
    private static final float ROCKY_HEIGHT_BIAS = 0F;
    private static final int ROCKY_FOOTHILL_REACH = 0;
    private static final int IRON_PER_MILLE = 40;
    private static final int IRON_MIN = 55;
    private static final int IRON_MAX = 105;

    /** Not instantiable: the class only builds the recipe. */
    private MediterraneanRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#MEDITERRANEAN}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.MEDITERRANEAN, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.FBM, HEIGHT_GAIN),
                        Set.of(), TREE_FREQUENCY),
                RecipeParts.edgeCoast(COAST_DEPTH_SHARE, COAST_DROP),
                RecipeParts.ground(GroundRules.MEDITERRANEAN),
                RecipeParts.waterBodyLimit(WATER_KEEP_PER_100K, WATER_MIN_KEPT_TILES, TileType.GRASS),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER, RIVER_LAKE_SHARE),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.greenHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.fertileTrees()),
                RecipeParts.forestFloor()));
    }
}
