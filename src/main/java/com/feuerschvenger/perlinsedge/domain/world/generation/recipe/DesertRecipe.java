package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.DesertFloraParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.DesertFloraPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRules;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.OasisParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.OasisPass;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.List;
import java.util.Set;

/** The recipe of the desert: sand and rocky ground, a trimmed sea, a few rare oases and palms and cacti. */
final class DesertRecipe {

    private static final float HEIGHT_FREQUENCY = 0.04F;
    private static final int HEIGHT_OCTAVES = 6;
    private static final float HEIGHT_GAIN = 0.6F;
    /** The tree noise only orders the sand where the balancing pass adds palms and cacti, in small groves. */
    private static final float TREE_FREQUENCY = 0.05F;
    /** The sea keeps its few largest bodies and every pond below the minimum turns to sand. */
    private static final int WATER_KEEP_PER_100K = 4;
    private static final int WATER_MIN_KEPT_TILES = 30;
    private static final int TILES_PER_OASIS = 15_000;
    private static final int OASIS_RADIUS = 2;
    private static final int OASIS_SITE_ATTEMPTS = 400;
    private static final float FLORA_DENSITY = 0.10F;
    private static final float FLORA_DRY_FACTOR = 0.25F;
    private static final float FLORA_WET_FACTOR = 2.5F;
    private static final float FLORA_MAX_CHANCE = 0.35F;
    private static final float ROCKY_SHARE = 0.12F;
    private static final float ROCKY_HEIGHT_BIAS = 0F;
    private static final int ROCKY_FOOTHILL_REACH = 0;
    private static final int IRON_PER_MILLE = 30;
    private static final int IRON_MIN = 70;
    private static final int IRON_MAX = 110;

    /** Not instantiable: the class only builds the recipe. */
    private DesertRecipe() {
    }

    /**
     * Builds the recipe.
     *
     * @return the recipe of {@link MapType#DESERT}
     */
    static MapRecipe create() {
        return new MapRecipe(MapType.DESERT, List.of(
                RecipeParts.noiseWithTrees(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.RIDGED, HEIGHT_GAIN),
                        Set.of(), TREE_FREQUENCY),
                RecipeParts.ground(GroundRules.DESERT),
                RecipeParts.waterBodyLimit(WATER_KEEP_PER_100K, WATER_MIN_KEPT_TILES, TileType.SAND),
                new OasisPass(new OasisParams(TILES_PER_OASIS, OASIS_RADIUS, OASIS_SITE_ATTEMPTS)),
                RecipeParts.rockyZones(ROCKY_SHARE, ROCKY_HEIGHT_BIAS, ROCKY_FOOTHILL_REACH, RecipeParts.sandHosts()),
                RecipeParts.rock(IRON_PER_MILLE),
                RecipeParts.stone(),
                new DesertFloraPass(new DesertFloraParams(FLORA_DENSITY, FLORA_DRY_FACTOR,
                        FLORA_WET_FACTOR, FLORA_MAX_CHANCE)),
                RecipeParts.spawn(),
                RecipeParts.balance(IRON_MIN, IRON_MAX, RecipeParts.sandTrees())));
    }
}
