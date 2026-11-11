package com.feuerschvenger.perlinsedge.domain.world.generation;

/** Port that builds {@link NoiseSource} instances, so the generator does not depend on a noise library. */
@FunctionalInterface
public interface NoiseSourceFactory {

    /**
     * Creates a noise source.
     * The full 64-bit seed is passed so an implementation can use every bit of it.
     *
     * @param seed     seed of the noise field
     * @param settings frequency and fractal parameters
     * @return a new noise source
     */
    NoiseSource create(long seed, NoiseSettings settings);
}
