package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;

import java.util.Objects;

/**
 * Decides the terrain of every tile from the height field and the climate fields its ground rule declares. The height
 * each tile is classified with is nudged by a small amount that depends only on the tile, which breaks up the borders;
 * the nudge is not written back to the height field. A climate field the rule does not declare is not required, and
 * the rule receives {@link #NEUTRAL_CLIMATE} in its place.
 */
public final class GroundPass implements GenerationPass {

    /** The value a rule receives for a climate field it did not declare, the middle of the range 0 to 1. */
    public static final float NEUTRAL_CLIMATE = 0.5F;

    private static final String NAME = "ground";
    private static final long DITHER_SALT = 0x0061_20D1_7E12_0001L;
    /** The dither takes the top 24 bits of the tile hash, which is as many as a float holds exactly. */
    private static final int DITHER_SHIFT = 40;
    private static final double DITHER_STEPS = 16_777_216.0;
    /** A value from 0 to 1 becomes one from -1 to 1 by doubling it and taking one off. */
    private static final double SIGNED_SPAN = 2.0;
    private static final double SIGNED_OFFSET = 1.0;

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
        final var rule = params.rule();
        final var heights = context.field(NoiseField.HEIGHT);
        final var temperatures = climate(context, rule, NoiseField.TEMPERATURE);
        final var moistures = climate(context, rule, NoiseField.MOISTURE);
        final double amplitude = params.ditherAmplitude();
        for (var y = 0; y < context.height(); y++) {
            for (var x = 0; x < context.width(); x++) {
                final var index = context.index(x, y);
                final var nudge = amplitude > 0.0 ? amplitude * dither(context, x, y) : 0.0;
                final var type = rule.classify((float) (heights[index] + nudge), valueAt(temperatures, index),
                        valueAt(moistures, index));
                context.setGround(index, type);
            }
        }
    }

    /** Returns the array of a climate field the rule declares, or null when the rule does not read it. */
    private static float[] climate(GenerationContext context, GroundRule rule, NoiseField field) {
        return rule.climateFields().contains(field) ? context.field(field) : null;
    }

    /** Returns the value of a tile in a climate array, or the neutral constant when the rule did not ask for it. */
    private static float valueAt(float[] values, int index) {
        return values == null ? NEUTRAL_CLIMATE : values[index];
    }

    /** Returns a value from -1 up to but not including 1 that depends only on the tile and the seed. */
    private static double dither(GenerationContext context, int x, int y) {
        final var unit = (context.tileHash(x, y, DITHER_SALT) >>> DITHER_SHIFT) / DITHER_STEPS;
        return unit * SIGNED_SPAN - SIGNED_OFFSET;
    }
}
