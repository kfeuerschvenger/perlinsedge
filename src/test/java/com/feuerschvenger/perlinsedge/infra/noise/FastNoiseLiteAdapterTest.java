package com.feuerschvenger.perlinsedge.infra.noise;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastNoiseLiteAdapterTest {

    private static final NoiseSettings PLAIN = new NoiseSettings(0.05f, 0, null, 0f, 2f);
    private static final NoiseSettings FRACTAL = new NoiseSettings(0.05f, 4, FractalKind.FBM, 0.5f, 2f);

    @Test
    void foldSeedKeepsTheLowHalfWhenTheHighHalfIsZero() {
        assertEquals(12_345, FastNoiseLiteAdapter.foldSeed(12_345L));
    }

    @Test
    void foldSeedLetsTheHighHalfChangeTheResult() {
        assertNotEquals(FastNoiseLiteAdapter.foldSeed(12_345L), FastNoiseLiteAdapter.foldSeed(12_345L | (7L << 40)));
    }

    @Test
    void sameSeedAndSettingsGiveTheSameNoise() {
        var a = new FastNoiseLiteAdapter(99L, FRACTAL);
        var b = new FastNoiseLiteAdapter(99L, FRACTAL);

        assertEquals(a.getNoise(10.5f, 20.25f), b.getNoise(10.5f, 20.25f));
    }

    @Test
    void differentSeedsGiveDifferentNoise() {
        var a = new FastNoiseLiteAdapter(1L, PLAIN);
        var b = new FastNoiseLiteAdapter(2L, PLAIN);

        assertNotEquals(a.getNoise(10.5f, 20.25f), b.getNoise(10.5f, 20.25f));
    }

    @Test
    void fractalOctavesChangeTheField() {
        var plain = new FastNoiseLiteAdapter(5L, PLAIN);
        var fractal = new FastNoiseLiteAdapter(5L, FRACTAL);

        assertNotEquals(plain.getNoise(31.5f, 17.5f), fractal.getNoise(31.5f, 17.5f));
    }

    @Test
    void noiseStaysInTheNormalisedRange() {
        var source = FastNoiseLiteAdapter.FACTORY.create(77L, FRACTAL);

        for (int i = 0; i < 200; i++) {
            float value = source.getNoise(i * 1.7f, i * 0.9f);
            assertTrue(value >= -1.01f && value <= 1.01f, "value " + value);
        }
    }
}
