package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.IslandParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.IslandShapePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.IslandSize;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.LandmassHintPass;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the islands: a deep sea with a few large islands and several small ones, ringed by sand. */
final class IslandsRecipe {

    private static final float HEIGHT_FREQUENCY = 0.005F;
    private static final int HEIGHT_OCTAVES = 5;
    private static final float HEIGHT_GAIN = 0F;
    private static final float TREE_FREQUENCY = 0.05F;
    private static final float RIVER_DENSITY = 0.03F;
    private static final float TREE_DENSITY = 0.304F;
    private static final int ISLANDS_PER_100K_TILES = 10;
    private static final int MIN_ISLANDS = 5;
    /**
     * The radii run from a small islet to a large island, no island has a radius above 30% of the shorter side of the
     * map, and the islands keep a little sea between them. A map holds at least five islands whatever its size, which
     * keeps the land of a small map in several pieces and its largest island from taking the map.
     */
    private static final int ISLAND_MIN_RADIUS = 12;
    private static final int ISLAND_MAX_RADIUS = 70;
    private static final float ISLAND_MAX_SHARE_OF_SIDE = 0.3F;
    private static final float ISLAND_SPACING = 1.05F;
    private static final float ISLAND_HEIGHT_GAIN = 0.6F;
    private static final float ISLAND_LIFT = 0.9F;
    private static final float ISLAND_SEA_OFFSET = 0.5F;
    private static final float ISLAND_FALLOFF_POWER = 1.1F;
    private static final float ROCKY_SHARE = 0.06F;
    private static final float ROCKY_HEIGHT_BIAS = 0F;
    private static final int ROCKY_FOOTHILL_REACH = 0;
    private static final int IRON_PER_MILLE = 40;
    private static final int IRON_MIN = 55;
    private static final int IRON_MAX = 105;

    /** Not instantiable: the class only builds the recipe. */
    private IslandsRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#ISLANDS}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.ISLANDS, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.FBM, HEIGHT_GAIN),
                        Set.of(), TREE_FREQUENCY),
                new IslandShapePass(new IslandParams(ISLANDS_PER_100K_TILES, MIN_ISLANDS,
                        new IslandSize(ISLAND_MIN_RADIUS, ISLAND_MAX_RADIUS, ISLAND_MAX_SHARE_OF_SIDE,
                                ISLAND_SPACING),
                        ISLAND_HEIGHT_GAIN, ISLAND_LIFT, ISLAND_SEA_OFFSET, ISLAND_FALLOFF_POWER)),
                RecipeParts.ground(GroundRules.ISLANDS),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.greenHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                RecipeParts.vegetation(TREE_DENSITY),
                new LandmassHintPass(),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.fertileTrees()),
                RecipeParts.forestFloor()));
    }
}
