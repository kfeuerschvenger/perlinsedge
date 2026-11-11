package com.feuerschvenger.perlinsedge.domain.world.generation;

/** A two-dimensional noise field. Implementations live outside the domain, behind a {@link NoiseSourceFactory}. */
@FunctionalInterface
public interface NoiseSource {

    /**
     * Samples the noise field.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return the noise value, normalised between -1.0 and 1.0
     */
    float getNoise(float x, float y);
}
