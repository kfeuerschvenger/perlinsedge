package com.feuerschvenger.perlinsedge.infra.noise;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSource;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSourceFactory;

/**
 * Adapts the vendored FastNoiseLite library to the domain {@link NoiseSource} port.
 * Always generates Perlin noise.
 */
public final class FastNoiseLiteAdapter implements NoiseSource {

    /** Factory that the application passes to the map generator. */
    public static final NoiseSourceFactory FACTORY = FastNoiseLiteAdapter::new;

    /** The golden-ratio increment that SplitMix64 adds to the seed. */
    private static final long SEED_INCREMENT = 0x9E37_79B9_7F4A_7C15L;

    /** The multipliers of the two SplitMix64 mixing rounds. */
    private static final long FIRST_MULTIPLIER = 0xBF58_476D_1CE4_E5B9L;
    private static final long SECOND_MULTIPLIER = 0x94D0_49BB_1331_11EBL;

    /** The right shifts of the two mixing rounds and of the final round. */
    private static final int FIRST_SHIFT = 30;
    private static final int SECOND_SHIFT = 27;
    private static final int FINAL_SHIFT = 31;

    /** Distance between the two 32-bit halves of a 64-bit word. */
    private static final int HALF_WORD_SHIFT = 32;

    private final FastNoiseLite noise;

    /**
     * Builds a Perlin noise source.
     *
     * @param seed     64-bit seed, folded to the 32 bits the library takes without discarding the high half
     * @param settings frequency and fractal parameters
     */
    public FastNoiseLiteAdapter(long seed, NoiseSettings settings) {
        noise = new FastNoiseLite(foldSeed(seed));
        noise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        noise.SetFrequency(settings.frequency());

        if (settings.octaves() > 0) {
            noise.SetFractalType(toLibrary(settings.fractal()));
            noise.SetFractalOctaves(settings.octaves());
            noise.SetFractalGain(settings.gain());
            noise.SetFractalLacunarity(settings.lacunarity());
        }
    }

    /**
     * Folds a 64-bit seed into 32 bits with every input bit contributing.
     * A narrowing cast ignores the upper 32 bits, and xoring the two halves still collapses every seed whose halves
     * are equal to 0 and maps a seed and its half-swapped twin to the same value. The seed goes through the
     * SplitMix64 finaliser first, which spreads each input bit over the whole word, and the two halves of that
     * result are then combined.
     *
     * @param seed the 64-bit seed
     * @return a 32-bit seed that depends on every bit of the input
     */
    static int foldSeed(long seed) {
        long z = seed + SEED_INCREMENT;
        z = (z ^ (z >>> FIRST_SHIFT)) * FIRST_MULTIPLIER;
        z = (z ^ (z >>> SECOND_SHIFT)) * SECOND_MULTIPLIER;
        z ^= z >>> FINAL_SHIFT;
        return (int) (z ^ (z >>> HALF_WORD_SHIFT));
    }

    @Override
    public float getNoise(float x, float y) {
        return noise.GetNoise(x, y);
    }

    private static FastNoiseLite.FractalType toLibrary(FractalKind kind) {
        return switch (kind) {
            case NONE -> FastNoiseLite.FractalType.None;
            case FBM -> FastNoiseLite.FractalType.FBm;
            case RIDGED -> FastNoiseLite.FractalType.Ridged;
        };
    }
}
