package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;

import java.util.Objects;

/**
 * Raises or lowers the whole height field so that a given share of the map lies below a level, which gives every seed
 * and every map size the same amount of water.
 * <p>
 * The height thresholds of a ground rule are absolute, and a small map sees only a few periods of the noise, so without
 * this a map can be nearly all water or nearly all land depending on where its window of the noise falls, and the water
 * can cut the land into pieces the player cannot walk between.
 * <p>
 * The pass adds one number to every height: the one that moves the quantile of the map that leaves the water share
 * below it onto the level. It runs before the ground pass, reads and writes the height only, draws no random numbers
 * and leaves a map whose heights are all equal alone, so it repeats for a seed.
 */
public final class SeaLevelPass implements GenerationPass {

    private static final String NAME = "sea-level";
    /** The whole map as a share, so the water share is taken from it to find the share that lies above the sea. */
    private static final double WHOLE_SHARE = 1.0;

    private final SeaLevelParams params;

    /**
     * Creates the pass.
     *
     * @param params the share of the map under water and the level that counts as its surface
     */
    public SeaLevelPass(SeaLevelParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Shifts the height field so the water share of the map lies below the level. */
    @Override
    public void apply(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        final var threshold = FieldQuantile.above(heights, index -> true, WHOLE_SHARE - params.waterShare());
        if (!Float.isFinite(threshold)) {
            return;
        }
        final var shift = (double) params.level() - threshold;
        for (var index = 0; index < heights.length; index++) {
            heights[index] = (float) (heights[index] + shift);
        }
    }
}
