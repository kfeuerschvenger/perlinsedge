package com.feuerschvenger.perlinsedge.infra.noise;

/**
 * Adapter for the FastNoiseLite noise generator to conform to the INoiseGenerator interface.
 * This allows the use of FastNoiseLite in a context that expects an INoiseGenerator.
 */
public class FastNoiseLiteAdapter implements INoiseGenerator {

    private final FastNoiseLite fastNoiseLite;

    /**
     * Constructs a FastNoiseLiteAdapter with the specified seed.
     *
     * @param seed The seed for the FastNoiseLite noise generator.
     */
    public FastNoiseLiteAdapter(int seed) {
        this.fastNoiseLite = new FastNoiseLite(seed);
    }

    @Override
    public float getNoise(float x, float y) {
        return fastNoiseLite.GetNoise(x, y);
    }

    @Override
    public void setSeed(int seed) {
        fastNoiseLite.SetSeed(seed);
    }

    @Override
    public void setFrequency(float frequency) {
        fastNoiseLite.SetFrequency(frequency);
    }

    @Override
    public void setNoiseType(Object noiseType) {
        if (noiseType instanceof FastNoiseLite.NoiseType) {
            fastNoiseLite.SetNoiseType((FastNoiseLite.NoiseType) noiseType);
        } else {
            throw new IllegalArgumentException("Invalid noiseType provided. Expected FastNoiseLite.NoiseType.");
        }
    }

    @Override
    public void setFractalType(Object fractalType) {
        if (fractalType instanceof FastNoiseLite.FractalType) {
            fastNoiseLite.SetFractalType((FastNoiseLite.FractalType) fractalType);
        } else {
            throw new IllegalArgumentException("Invalid fractalType provided. Expected FastNoiseLite.FractalType.");
        }
    }

    @Override
    public void setFractalOctaves(int octaves) {
        fastNoiseLite.SetFractalOctaves(octaves);
    }

    @Override
    public void setFractalGain(float gain) {
        fastNoiseLite.SetFractalGain(gain);
    }

    @Override
    public void setFractalLacunarity(float lacunarity) {
        fastNoiseLite.SetFractalLacunarity(lacunarity);
    }

}