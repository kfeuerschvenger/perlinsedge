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
     * Folds a 64-bit seed into 32 bits with both halves contributing.
     * A plain narrowing cast would make seeds that differ only in the upper 32 bits produce the same noise.
     *
     * @param seed the 64-bit seed
     * @return a 32-bit seed that depends on every bit of the input
     */
    static int foldSeed(long seed) {
        return (int) (seed ^ (seed >>> 32));
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
