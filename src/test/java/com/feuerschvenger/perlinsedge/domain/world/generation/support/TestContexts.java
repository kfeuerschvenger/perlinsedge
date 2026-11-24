package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSourceFactory;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

/** Builds the small contexts the unit tests of the generation passes work on. */
public final class TestContexts {

    /** A noise factory whose sources are zero everywhere, for contexts whose fields the test fills by hand. */
    public static final NoiseSourceFactory FLAT = (long seed, NoiseSettings settings) -> (float x, float y) -> 0F;

    private static final long DEFAULT_SEED = 1L;

    private TestContexts() {
    }

    /**
     * Creates a context of a desert map with a fixed seed and flat noise.
     *
     * @param width  the number of columns
     * @param height the number of rows
     * @return the new context
     */
    public static GenerationContext of(int width, int height) {
        return of(MapType.DESERT, DEFAULT_SEED, width, height, FLAT);
    }

    /**
     * Creates a context.
     *
     * @param type   the kind of map
     * @param seed   the seed
     * @param width  the number of columns
     * @param height the number of rows
     * @param noise  where the noise sources come from
     * @return the new context
     */
    public static GenerationContext of(MapType type, long seed, int width, int height, NoiseSourceFactory noise) {
        return new GenerationContext(new MapRequest(type, seed, width, height), noise);
    }
}
