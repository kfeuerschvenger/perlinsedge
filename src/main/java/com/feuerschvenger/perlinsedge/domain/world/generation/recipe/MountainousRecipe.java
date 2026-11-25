package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/**
 * The recipe of the mountains: ridged rock bands that no one can walk on, cut by narrow passes between valleys that
 * each have a clearing, with lakes high between the peaks, plenty of ore and few trees.
 */
final class MountainousRecipe {

    private static final float HEIGHT_FREQUENCY = 0.01F;
    private static final int HEIGHT_OCTAVES = 8;
    private static final float HEIGHT_GAIN = 0.7F;
    private static final float TREE_FREQUENCY = 0.05F;
    private static final float RIVER_DENSITY = 0.03F;
    /** The lowest 10% of the map holds water, in lakes between the peaks, whatever the noise and the map size. */
    private static final float WATER_SHARE = 0.10F;
    /** The height at which the ground rule of the mountains turns the water high between the peaks into ground. */
    private static final float WATER_LEVEL = 0.3F;
    private static final float TREE_DENSITY = 0.05F;
    private static final float ROCKY_SHARE = 0.12F;
    private static final float ROCKY_HEIGHT_BIAS = 1F;
    private static final int ROCKY_FOOTHILL_REACH = 2;
    private static final int IRON_PER_MILLE = 30;
    private static final int IRON_MIN = 70;
    private static final int IRON_MAX = 110;
    /** The highest 28% of the land is mountain, in ridges, which the passes of the trails cut through. */
    private static final float MOUNTAIN_SHARE = 0.28F;
    /** Clearings 40 tiles apart, 3 to 5 tiles in radius, and a spawn clearing of 6, which keep 4 tiles from rock. */
    private static final int CLEARING_SPACING = 40;
    private static final int CLEARING_MIN_RADIUS = 3;
    private static final int CLEARING_MAX_RADIUS = 5;
    private static final int SPAWN_CLEARING_RADIUS = 6;
    private static final int CLEARING_CLEARANCE = 4;
    /** A valley of at least 30 tiles gets a clearing even when the sampling missed it, so a pass reaches it. */
    private static final int VALLEY_MIN_TILES = 30;
    /** Trails cost 10 a tile through rock, so they cut a pass only where going round is far longer. */
    private static final float TRAIL_NOISE_WEIGHT = 1F;
    private static final float TRAIL_TREE_COST = 4F;
    private static final float TRAIL_MOUNTAIN_COST = 10F;
    private static final int TRAIL_EXTRA_PER_100K = 2;
    private static final int TRAIL_MAX_WIDTH = 2;
    /** Two clearings are joined directly when they are at most this far apart, two and a half times the spacing. */
    private static final int TRAIL_REACH = 100;

    /** Not instantiable: the class only builds the recipe. */
    private MountainousRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#MOUNTAINOUS}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.MOUNTAINOUS, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.RIDGED, HEIGHT_GAIN),
                        Set.of(), TREE_FREQUENCY),
                RecipeParts.seaLevel(WATER_SHARE, WATER_LEVEL),
                RecipeParts.ground(GroundRules.MOUNTAINOUS),
                RecipeParts.heightBand(MOUNTAIN_SHARE, TileType.MOUNTAIN),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.greenHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                RecipeParts.clearings(CLEARING_SPACING, CLEARING_MIN_RADIUS, CLEARING_MAX_RADIUS,
                        SPAWN_CLEARING_RADIUS, CLEARING_CLEARANCE, VALLEY_MIN_TILES),
                RecipeParts.trails(TRAIL_NOISE_WEIGHT, TRAIL_TREE_COST, TRAIL_MOUNTAIN_COST, TRAIL_EXTRA_PER_100K,
                        TRAIL_MAX_WIDTH, TRAIL_REACH),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.fertileTrees()),
                RecipeParts.forestFloor()));
    }
}
