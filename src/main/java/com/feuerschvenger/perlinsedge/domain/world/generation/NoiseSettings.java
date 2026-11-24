package com.feuerschvenger.perlinsedge.domain.world.generation;

/**
 * Parameters of one noise field.
 *
 * @param frequency  scale of the pattern, larger values give finer detail
 * @param octaves    number of fractal layers, 0 or less means a single plain layer
 * @param fractal    how the layers are combined, null counts as {@link FractalKind#NONE}
 * @param gain       amplitude factor between octaves
 * @param lacunarity frequency factor between octaves
 */
public record NoiseSettings(float frequency, int octaves, FractalKind fractal, float gain, float lacunarity) {

    /** Normalises a missing fractal kind. */
    public NoiseSettings {
        fractal = fractal == null ? FractalKind.NONE : fractal;
    }
}
