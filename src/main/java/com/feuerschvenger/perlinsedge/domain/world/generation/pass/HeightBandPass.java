package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;

import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Turns the highest part of the land into a band of another terrain, such as the rock bands of the mountains. The share
 * is relative to the land of the map, so every height profile gets the same share of band, and because the height of a
 * ridged map is a set of ridge lines, the band is a set of ridges.
 * <p>
 * Water and reserved tiles are never part of the band. The pass reads the height and the ground, draws no random
 * numbers and does nothing on a map with no land, so it repeats for a seed.
 */
public final class HeightBandPass implements GenerationPass {

    private static final String NAME = "height-band";

    private final HeightBandParams params;

    /**
     * Creates the pass.
     *
     * @param params the share of the land that becomes the band and the terrain of the band
     */
    public HeightBandPass(HeightBandParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Sets the band terrain on the highest share of the land. */
    @Override
    public void apply(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        final IntPredicate land = index -> context.groundAt(index).isLand() && !context.isReserved(index);
        final var threshold = FieldQuantile.above(heights, land, params.landShare());
        for (var index = 0; index < context.size(); index++) {
            if (heights[index] >= threshold && land.test(index)) {
                context.setGround(index, params.band());
            }
        }
    }
}
