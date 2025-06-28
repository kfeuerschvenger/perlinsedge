package com.feuerschvenger.perlinsedge.infra.noise;

/**
 * Adapter for the FastNoiseLite noise generator to conform to the INoiseGenerator interface.
 * This allows the use of FastNoiseLite in a context that expects an INoiseGenerator.
 */
public interface INoiseGenerator {

    /**
     * Generate a 2D noise value for the given coordinates.
     * The range of noise values should be normalized, typically between -1.0 and 1.0.
     * @param x X coordinate
     * @param y Y coordinate
     * @return The noise value
     */
    float getNoise(float x, float y);

    /**
     * Set the seed for noise generation.
     * @param seed The seed to use.
     */
    void setSeed(int seed);

    /**
     * Set the frequency for noise generation.
     * This affects the scale of the noise pattern.
     * @param frequency The frequency to use, typically a positive float.
     */
    void setFrequency(float frequency);

    /**
     * Set the type of noise to generate.
     * @param noiseType The type of noise, typically an enum or constant defined in the noise library.
     */
    void setNoiseType(Object noiseType);

    /**
     * Set the type of fractal noise to generate.
     * @param fractalType The type of fractal noise, typically an enum or constant defined in the noise library.
     */
    void setFractalType(Object fractalType);

    /**
     * Set the number of octaves for fractal noise generation.
     * This determines the detail level of the fractal noise.
     * @param octaves The number of octaves, typically a positive integer.
     */
    void setFractalOctaves(int octaves);

    /**
     * Set the gain for fractal noise generation.
     * This affects the amplitude of the noise at each octave.
     * @param gain The gain value, typically a float.
     */
    void setFractalGain(float gain);

    /**
     * Set the lacunarity for fractal noise generation.
     * This affects the frequency increase between octaves.
     * @param lacunarity The lacunarity value, typically a float.
     */
    void setFractalLacunarity(float lacunarity);

}