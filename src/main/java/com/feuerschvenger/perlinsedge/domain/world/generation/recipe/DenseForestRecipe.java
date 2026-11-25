package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/**
 * The recipe of the dense forest: rolling forest on wet ground with a few rivers and thick groves of trees. Its
 * invariants hold from a shorter side of {@value MapRecipes#MIN_CONTENT_SIDE} tiles, on a map at most twice as long as
 * it is wide.
 */
final class DenseForestRecipe {

    private static final float HEIGHT_FREQUENCY = 0.02F;
    private static final int HEIGHT_OCTAVES = 4;
    private static final float HEIGHT_GAIN = 0.4F;
    private static final float TREE_FREQUENCY = 0.04F;
    private static final float RIVER_DENSITY = 0.01F;
    /** The lowest 10% of the map is sea, whatever the noise, so a small map has a land of one piece to walk on. */
    private static final float WATER_SHARE = 0.10F;
    /** The height at which the ground rule of the dense forest turns the sea into land. */
    private static final float SEA_LEVEL = GroundRules.DENSE_FOREST_SHALLOW;
    /**
     * Trees cover this share of the land, in the groves, on what the clearings and the trails left, which is why they
     * run first: every map, big or small, ends with the same share of trees, inside the 28% to 42% of the land.
     */
    private static final float TREE_SHARE = 0.36F;
    private static final float ROCKY_SHARE = 0.03F;
    private static final float ROCKY_HEIGHT_BIAS = 0F;
    private static final int ROCKY_FOOTHILL_REACH = 0;
    private static final int IRON_PER_MILLE = 40;
    private static final int IRON_MIN = 55;
    private static final int IRON_MAX = 105;
    /** Clearings 45 tiles apart, 3 to 6 tiles in radius, and a spawn clearing of 7, which keep 2 tiles from water. */
    private static final int CLEARING_SPACING = 45;
    private static final int CLEARING_MIN_RADIUS = 3;
    private static final int CLEARING_MAX_RADIUS = 6;
    private static final int SPAWN_CLEARING_RADIUS = 7;
    private static final int CLEARING_CLEARANCE = 2;
    /** Land cut off by water is left alone: no trail could reach a clearing there. */
    private static final int NO_REGION_CLEARINGS = 0;
    /** Trails wind around the groves the tree noise promises, never cross water and close two loops. */
    private static final float TRAIL_NOISE_WEIGHT = 2F;
    private static final float TRAIL_TREE_COST = 4F;
    private static final int TRAIL_EXTRA_PER_100K = 2;
    private static final int TRAIL_MAX_WIDTH = 2;
    /** Two clearings are joined directly when they are at most this far apart, two and a half times the spacing. */
    private static final int TRAIL_REACH = 112;
    /**
     * The most trees per 100,000 tiles of map, a guard that the share of the land above never reaches. This is a count
     * against the map, not against the land, and it assumes the land is at least 84% of a dense forest map, so 35,000
     * trees stay under the 42% of the land that closes the 28% to 42% band the map type holds. The sea level keeps the
     * water near 10% of the map, rivers and lakes included, and a recipe change that makes it more than 16% has to
     * lower this.
     */
    private static final int TREES_MAX = 35_000;

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
                RecipeParts.seaLevel(WATER_SHARE, SEA_LEVEL),
                RecipeParts.ground(GroundRules.DENSE_FOREST),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.greenHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                RecipeParts.clearings(CLEARING_SPACING, CLEARING_MIN_RADIUS, CLEARING_MAX_RADIUS,
                        SPAWN_CLEARING_RADIUS, CLEARING_CLEARANCE, NO_REGION_CLEARINGS),
                RecipeParts.trails(TRAIL_NOISE_WEIGHT, TRAIL_TREE_COST, Float.POSITIVE_INFINITY, TRAIL_EXTRA_PER_100K,
                        TRAIL_MAX_WIDTH, TRAIL_REACH),
                RecipeParts.vegetationByShare(TREE_SHARE),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.fertileTrees(TREES_MAX)),
                RecipeParts.forestFloor()));
    }
}
