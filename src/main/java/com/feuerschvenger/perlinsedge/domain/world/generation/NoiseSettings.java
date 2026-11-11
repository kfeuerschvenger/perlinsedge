package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

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

    private static final float DEFAULT_HEIGHT_FREQUENCY = 0.02f;
    private static final float DEFAULT_LACUNARITY = 2.0f;

    /** Normalises a missing fractal kind. */
    public NoiseSettings {
        fractal = fractal == null ? FractalKind.NONE : fractal;
    }

    /**
     * Terrain shape of a map type: the frequency and octave count a new map starts with,
     * and the fractal kind, gain and lacunarity that the tuning panel does not expose.
     *
     * @param mapType the map type
     * @return the height noise profile of that map type
     */
    public static NoiseSettings heightProfile(MapType mapType) {
        return switch (mapType) {
            case ISLANDS -> new NoiseSettings(0.005f, 5, FractalKind.FBM, 0f, DEFAULT_LACUNARITY);
            case DESERT -> new NoiseSettings(0.04f, 6, FractalKind.RIDGED, 0.6f, DEFAULT_LACUNARITY);
            case MOUNTAINOUS -> new NoiseSettings(0.01f, 8, FractalKind.RIDGED, 0.7f, DEFAULT_LACUNARITY);
            case DENSE_FOREST, SWAMP -> new NoiseSettings(0.02f, 4, FractalKind.FBM, 0.4f, DEFAULT_LACUNARITY);
            case TUNDRA -> new NoiseSettings(0.015f, 5, FractalKind.FBM, 0.5f, DEFAULT_LACUNARITY);
            case RIVERS -> new NoiseSettings(0.01f, 6, FractalKind.FBM, 0.4f, DEFAULT_LACUNARITY);
            case MEDITERRANEAN -> new NoiseSettings(0.025f, 5, FractalKind.FBM, 0.5f, DEFAULT_LACUNARITY);
            default -> new NoiseSettings(DEFAULT_HEIGHT_FREQUENCY, 0, FractalKind.NONE, 0f, DEFAULT_LACUNARITY);
        };
    }
}
