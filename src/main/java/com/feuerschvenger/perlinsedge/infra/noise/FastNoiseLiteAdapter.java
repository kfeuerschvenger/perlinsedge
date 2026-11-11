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
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (int) (z ^ (z >>> 32));
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
