package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;

import java.util.Objects;

/**
 * Decides the terrain of every tile from the height, temperature and moisture fields with the ground rule of the
 * recipe. The height each tile is classified with is nudged by a small amount that depends only on the tile, which
 * breaks up the borders; the nudge is not written back to the height field.
 */
public final class GroundPass implements GenerationPass {

    private static final String NAME = "ground";
    private static final long DITHER_SALT = 0x6120_D1_7E_12_00_01L;
    /** The dither takes the top 24 bits of the tile hash, which is as many as a float holds exactly. */
    private static final int DITHER_SHIFT = 40;
    private static final float DITHER_STEPS = 16_777_216F;
    /** A value from 0 to 1 becomes one from -1 to 1 by doubling it and taking one off. */
    private static final float SIGNED_SPAN = 2F;

    private final GroundParams params;

    /**
     * Creates the pass.
     *
     * @param params the ground rule and the dither
     */
    public GroundPass(GroundParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        final var temperatures = context.field(NoiseField.TEMPERATURE);
        final var moistures = context.field(NoiseField.MOISTURE);
        final var ground = context.ground();
        final var rule = params.rule();
        final var amplitude = params.ditherAmplitude();
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                final var index = context.index(x, y);
                final var nudge = amplitude == 0F ? 0F : amplitude * dither(context, x, y);
                ground[index] = rule.classify(heights[index] + nudge, temperatures[index], moistures[index]);
            }
        }
    }

    /** Returns a value from -1 up to but not including 1 that depends only on the tile and the seed. */
    private static float dither(GenerationContext context, int x, int y) {
        final var unit = (context.tileHash(x, y, DITHER_SALT) >>> DITHER_SHIFT) / DITHER_STEPS;
        return unit * SIGNED_SPAN - 1F;
    }
}
