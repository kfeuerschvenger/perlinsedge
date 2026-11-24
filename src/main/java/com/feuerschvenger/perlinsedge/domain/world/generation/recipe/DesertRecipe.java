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

/** The recipe of the desert: sand and hills with water high in the height range, oases and palms and cacti. */
final class DesertRecipe {

    private static final float HEIGHT_FREQUENCY = 0.04F;
    private static final int HEIGHT_OCTAVES = 6;
    private static final float HEIGHT_GAIN = 0.6F;
    private static final float RIVER_DENSITY = 0.003F;
    private static final int TILES_PER_OASIS = 5000;
    private static final int OASIS_RADIUS = 2;
    private static final int OASIS_SITE_ATTEMPTS = 400;
    private static final float FLORA_DENSITY = 0.10F;
    private static final float FLORA_DRY_FACTOR = 0.25F;
    private static final float FLORA_WET_FACTOR = 2.5F;
    private static final float FLORA_MAX_CHANCE = 0.35F;

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
                RecipeParts.noise(
                        RecipeParts.height(HEIGHT_FREQUENCY, HEIGHT_OCTAVES, FractalKind.RIDGED, HEIGHT_GAIN),
                        Set.of()),
                RecipeParts.ground(GroundRules.DESERT),
                RecipeParts.rivers(RIVER_DENSITY, TileType.SHALLOW_WATER),
                new OasisPass(new OasisParams(TILES_PER_OASIS, OASIS_RADIUS, OASIS_SITE_ATTEMPTS)),
                RecipeParts.rock(),
                RecipeParts.stone(),
                new DesertFloraPass(new DesertFloraParams(FLORA_DENSITY, FLORA_DRY_FACTOR,
                        FLORA_WET_FACTOR, FLORA_MAX_CHANCE)),
                RecipeParts.spawn()));
    }
}
