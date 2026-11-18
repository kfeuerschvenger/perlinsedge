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

    private static final float DEFAULT_HEIGHT_FREQUENCY = 0.02F;
    private static final float DEFAULT_LACUNARITY = 2.0F;

    // Frequency, octave count and gain of the height noise of each map type
    private static final float ISLANDS_FREQUENCY = 0.005F;
    private static final int ISLANDS_OCTAVES = 5;
    private static final float DESERT_FREQUENCY = 0.04F;
    private static final int DESERT_OCTAVES = 6;
    private static final float DESERT_GAIN = 0.6F;
    private static final float MOUNTAINOUS_FREQUENCY = 0.01F;
    private static final int MOUNTAINOUS_OCTAVES = 8;
    private static final float MOUNTAINOUS_GAIN = 0.7F;
    private static final float FOREST_FREQUENCY = 0.02F;
    private static final int FOREST_OCTAVES = 4;
    private static final float FOREST_GAIN = 0.4F;
    private static final float TUNDRA_FREQUENCY = 0.015F;
    private static final int TUNDRA_OCTAVES = 5;
    private static final float TUNDRA_GAIN = 0.5F;
    private static final float RIVERS_FREQUENCY = 0.01F;
    private static final int RIVERS_OCTAVES = 6;
    private static final float RIVERS_GAIN = 0.4F;
    private static final float MEDITERRANEAN_FREQUENCY = 0.025F;
    private static final int MEDITERRANEAN_OCTAVES = 5;
    private static final float MEDITERRANEAN_GAIN = 0.5F;

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
            case ISLANDS -> new NoiseSettings(ISLANDS_FREQUENCY, ISLANDS_OCTAVES, FractalKind.FBM, 0F, DEFAULT_LACUNARITY);
            case DESERT -> new NoiseSettings(DESERT_FREQUENCY, DESERT_OCTAVES, FractalKind.RIDGED, DESERT_GAIN, DEFAULT_LACUNARITY);
            case MOUNTAINOUS -> new NoiseSettings(MOUNTAINOUS_FREQUENCY, MOUNTAINOUS_OCTAVES, FractalKind.RIDGED, MOUNTAINOUS_GAIN, DEFAULT_LACUNARITY);
            case DENSE_FOREST, SWAMP -> new NoiseSettings(FOREST_FREQUENCY, FOREST_OCTAVES, FractalKind.FBM, FOREST_GAIN, DEFAULT_LACUNARITY);
            case TUNDRA -> new NoiseSettings(TUNDRA_FREQUENCY, TUNDRA_OCTAVES, FractalKind.FBM, TUNDRA_GAIN, DEFAULT_LACUNARITY);
            case RIVERS -> new NoiseSettings(RIVERS_FREQUENCY, RIVERS_OCTAVES, FractalKind.FBM, RIVERS_GAIN, DEFAULT_LACUNARITY);
            case MEDITERRANEAN -> new NoiseSettings(MEDITERRANEAN_FREQUENCY, MEDITERRANEAN_OCTAVES, FractalKind.FBM, MEDITERRANEAN_GAIN, DEFAULT_LACUNARITY);
            default -> new NoiseSettings(DEFAULT_HEIGHT_FREQUENCY, 0, FractalKind.NONE, 0F, DEFAULT_LACUNARITY);
        };
    }
}
