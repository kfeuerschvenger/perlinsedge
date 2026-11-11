package com.feuerschvenger.perlinsedge.infra.noise;

import com.feuerschvenger.perlinsedge.domain.world.generation.FractalKind;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseSettings;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastNoiseLiteAdapterTest {

    private static final NoiseSettings PLAIN = new NoiseSettings(0.05f, 0, null, 0f, 2f);
    private static final NoiseSettings FRACTAL = new NoiseSettings(0.05f, 4, FractalKind.FBM, 0.5f, 2f);

    @Test
    void foldSeedIsDeterministic() {
        assertEquals(FastNoiseLiteAdapter.foldSeed(12_345L), FastNoiseLiteAdapter.foldSeed(12_345L));
    }

    @Test
    void seedsWithEqualHalvesNoLongerCollapseToTheSameValue() {
        long[] equalHalves = {0L, 0x1_0000_0001L, 0x7FFF_FFFF_7FFF_FFFFL, -1L, 0x1234_5678_1234_5678L};

        Set<Integer> folded = new HashSet<>();
        for (long seed : equalHalves) {
            folded.add(FastNoiseLiteAdapter.foldSeed(seed));
        }

        assertEquals(equalHalves.length, folded.size());
    }

    @Test
    void aSeedAndItsHalfSwappedTwinDiffer() {
        long seed = (7L << 32) | 3L;
        long swapped = (3L << 32) | 7L;

        assertNotEquals(FastNoiseLiteAdapter.foldSeed(seed), FastNoiseLiteAdapter.foldSeed(swapped));
    }

    @Test
    void consecutiveSeedsFoldToDistinctValues() {
        Set<Integer> folded = new HashSet<>();
        for (long seed = 0; seed < 10_000; seed++) {
            folded.add(FastNoiseLiteAdapter.foldSeed(seed));
        }

        assertEquals(10_000, folded.size());
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
