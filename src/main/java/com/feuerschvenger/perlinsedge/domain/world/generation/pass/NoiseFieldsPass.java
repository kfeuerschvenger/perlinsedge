package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;

import java.util.Objects;

/**
 * Samples every noise field of the recipe once into its array. It is the only pass that calls a noise source, so the
 * passes after it read plain arrays. The fields that {@link NoiseField#isNormalized()} are brought from the -1..1 of
 * the noise to 0..1 here, and the patch field is then spread so that equal bands of it cover equal shares of the map.
 */
public final class NoiseFieldsPass implements GenerationPass {

    private static final String NAME = "noise-fields";
    /** A noise value runs from minus one to one, so adding one and dividing by this span gives zero to one. */
    private static final double NOISE_RANGE_SPAN = 2.0;
    private static final double NOISE_RANGE_OFFSET = 1.0;

    private final NoiseFieldsParams params;

    /**
     * Creates the pass.
     *
     * @param params the fields to compute and their noise settings
     */
    public NoiseFieldsPass(NoiseFieldsParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name of the pass. */
    @Override
    public String name() {
        return NAME;
    }

    /** Samples every field of the recipe into the context. */
    @Override
    public void apply(GenerationContext context) {
        for (var entry : params.fields().entrySet()) {
            sample(context, entry.getKey(), entry.getValue());
        }
    }

    /** Fills the array of one field from a noise source whose seed is the seed of the map plus the field offset. */
    private static void sample(GenerationContext context, NoiseField field, NoiseSettings settings) {
        final var source = context.noiseFactory().create(context.seed() + field.seedOffset(), settings);
        final var values = context.defineField(field);
        final var normalized = field.isNormalized();
        final var width = context.width();
        final var height = context.height();
        for (var y = 0; y < height; y++) {
            final var row = y * width;
            for (var x = 0; x < width; x++) {
                final var value = source.getNoise((float) x, (float) y);
                values[row + x] = normalized ? (float) ((value + NOISE_RANGE_OFFSET) / NOISE_RANGE_SPAN) : value;
            }
        }
        if (field == NoiseField.PATCH) {
            FieldEqualiser.spread(values);
        }
    }
}
