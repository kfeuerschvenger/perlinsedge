package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.GroundRule;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.LakeParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.NoiseFieldsParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.NoiseFieldsPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RiverParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RiverPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RockResourceParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.RockResourcePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.SpawnParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.SpawnPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.StoneParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.StonePass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.TributaryParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.VegetationParams;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.VegetationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.EnumMap;
import java.util.Set;

/**
 * The settings and passes that every recipe shares, so a recipe lists only what makes its map different: its height
 * profile, its ground rule, how many rivers and trees it has and the passes only it uses.
 */
final class RecipeParts {

    // Noise: lacunarity of every fractal, and the settings of the climate fields and the patch fields
    private static final float LACUNARITY = 2F;
    private static final float CLIMATE_GAIN = 0.5F;
    private static final int CLIMATE_OCTAVES = 3;
    private static final float TEMPERATURE_FREQUENCY = 0.02F;
    private static final float MOISTURE_FREQUENCY = 0.03F;
    private static final float STONE_FREQUENCY = 0.06F;
    private static final float CRYSTALS_FREQUENCY = 0.09F;

    /** The largest nudge of the height of a tile before the ground rule sees it, which breaks up straight borders. */
    private static final float DITHER_AMPLITUDE = 0.025F;

    // Rock: iron is about one rocky tile in 170, crystals need a patch of the crystal noise and are rarer than iron
    private static final int IRON_PER_MILLE = 6;
    private static final int CRYSTALS_PER_MILLE = 50;
    private static final float CRYSTALS_THRESHOLD = 0.80F;
    private static final float STONE_THRESHOLD = 0.79F;

    // The player starts with this many tiles around them free of resources
    private static final int SPAWN_FREE_RADIUS = 3;

    // Rivers: the same everywhere but their density and the surface of their lakes
    private static final float RIVER_SOURCE_MIN_HEIGHT = 0.6F;
    private static final int RIVER_MIN_LENGTH = 100;
    private static final int RIVER_MAX_LENGTH = 300;
    private static final int TRIBUTARY_MIN_RIVER_LENGTH = 30;
    private static final float TRIBUTARY_CHANCE = 0.01F;
    private static final int TRIBUTARY_MIN_LENGTH = 20;
    private static final int TRIBUTARY_LENGTH_SPREAD = 50;
    private static final float LAKE_MAX_ENDPOINT_HEIGHT = 0.4F;
    private static final int LAKE_MIN_RADIUS = 2;
    private static final int LAKE_MAX_RADIUS = 5;
    private static final float LAKE_DEPTH_TOLERANCE = 0.05F;

    /** Not instantiable: the class only holds static helpers. */
    private RecipeParts() {
    }

    /**
     * Describes the noise of the height of a map.
     *
     * @param frequency the scale of the pattern
     * @param octaves   the number of fractal layers
     * @param fractal   how the layers combine
     * @param gain      the amplitude factor between layers
     * @return the settings
     */
    static NoiseSettings height(float frequency, int octaves, FractalKind fractal, float gain) {
        return new NoiseSettings(frequency, octaves, fractal, gain, LACUNARITY);
    }

    /**
     * Builds the noise pass of a map that has no trees, and so no tree field.
     *
     * @param height  the settings of the height
     * @param climate the climate fields the ground rule reads, a subset of temperature and moisture
     * @return the pass, which also computes the stone and crystal fields
     */
    static NoiseFieldsPass noise(NoiseSettings height, Set<NoiseField> climate) {
        return new NoiseFieldsPass(new NoiseFieldsParams(fieldsOf(height, climate)));
    }

    /**
     * Builds the noise pass of a map with trees.
     *
     * @param height        the settings of the height
     * @param climate       the climate fields the ground rule reads, a subset of temperature and moisture
     * @param treeFrequency the scale of the tree groves, low for few wide groves and high for many small ones
     * @return the pass, which also computes the stone, crystal and tree fields
     */
    static NoiseFieldsPass noiseWithTrees(NoiseSettings height, Set<NoiseField> climate, float treeFrequency) {
        final var fields = fieldsOf(height, climate);
        fields.put(NoiseField.TREE, patchNoise(treeFrequency));
        return new NoiseFieldsPass(new NoiseFieldsParams(fields));
    }

    /**
     * Builds the ground pass of a map.
     *
     * @param rule the ground rule of the map type
     * @return the pass
     */
    static GroundPass ground(GroundRule rule) {
        return new GroundPass(new GroundParams(rule, DITHER_AMPLITUDE));
    }

    /**
     * Builds the river pass of a map.
     *
     * @param density how many of the high tiles start a river
     * @param surface what the water of a lake is made of, shallow water or ice
     * @return the pass
     */
    static RiverPass rivers(float density, TileType surface) {
        final var tributaries = new TributaryParams(TRIBUTARY_MIN_RIVER_LENGTH, TRIBUTARY_CHANCE,
                TRIBUTARY_MIN_LENGTH, TRIBUTARY_LENGTH_SPREAD);
        final var lakes = new LakeParams(LAKE_MAX_ENDPOINT_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS,
                LAKE_DEPTH_TOLERANCE, surface);
        return new RiverPass(new RiverParams(density, RIVER_SOURCE_MIN_HEIGHT, RIVER_MIN_LENGTH, RIVER_MAX_LENGTH,
                tributaries, lakes));
    }

    /**
     * Builds the pass that scatters iron ore and crystals over the rock.
     *
     * @return the pass, the same for every map type
     */
    static RockResourcePass rock() {
        return new RockResourcePass(new RockResourceParams(IRON_PER_MILLE, CRYSTALS_PER_MILLE,
                CRYSTALS_THRESHOLD));
    }

    /**
     * Builds the pass that puts stone on the land.
     *
     * @return the pass, the same for every map type
     */
    static StonePass stone() {
        return new StonePass(new StoneParams(STONE_THRESHOLD));
    }

    /**
     * Builds the pass that grows trees.
     *
     * @param density how thickly the trees grow
     * @return the pass
     */
    static VegetationPass vegetation(float density) {
        return new VegetationPass(new VegetationParams(density));
    }

    /**
     * Builds the pass that chooses where the player starts.
     *
     * @return the pass, the same for every map type
     */
    static SpawnPass spawn() {
        return new SpawnPass(new SpawnParams(SPAWN_FREE_RADIUS));
    }

    /** The fields every map computes, the height, the stone and the crystals, and the climate fields it asks for. */
    private static EnumMap<NoiseField, NoiseSettings> fieldsOf(NoiseSettings height, Set<NoiseField> climate) {
        final var fields = new EnumMap<NoiseField, NoiseSettings>(NoiseField.class);
        fields.put(NoiseField.HEIGHT, height);
        if (climate.contains(NoiseField.TEMPERATURE)) {
            fields.put(NoiseField.TEMPERATURE, climateNoise(TEMPERATURE_FREQUENCY));
        }
        if (climate.contains(NoiseField.MOISTURE)) {
            fields.put(NoiseField.MOISTURE, climateNoise(MOISTURE_FREQUENCY));
        }
        fields.put(NoiseField.STONE, patchNoise(STONE_FREQUENCY));
        fields.put(NoiseField.CRYSTALS, patchNoise(CRYSTALS_FREQUENCY));
        return fields;
    }

    /** The noise of a climate field: smooth and fractal. */
    private static NoiseSettings climateNoise(float frequency) {
        return new NoiseSettings(frequency, CLIMATE_OCTAVES, FractalKind.FBM, CLIMATE_GAIN, LACUNARITY);
    }

    /** The noise of a patch field: one plain layer, whose single scale sets how big the patches are. */
    private static NoiseSettings patchNoise(float frequency) {
        return new NoiseSettings(frequency, 0, FractalKind.NONE, 0F, 0F);
    }
}
