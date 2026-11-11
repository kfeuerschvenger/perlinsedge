package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerlinMapGeneratorTest {

    private static final int SIZE = 48;

    private static MapGenerationContext context(MapType type, long seed, float heightFrequency, int heightOctaves) {
        return new MapGenerationContext(type, seed, heightFrequency, heightOctaves, 0.02f, 3, 0.03f, 3,
                0.06f, 0.79f, 0.09f, 0.80f, 0.3f, 0.0f);
    }

    private static MapGenerationContext defaults(MapType type, long seed) {
        NoiseSettings profile = NoiseSettings.heightProfile(type);
        return context(type, seed, profile.frequency(), profile.octaves());
    }

    private static TileMap generate(MapGenerationContext context) {
        return new PerlinMapGenerator(FastNoiseLiteAdapter.FACTORY).generateMap(SIZE, SIZE, context);
    }

    private static float[] heights(TileMap map) {
        float[] heights = new float[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                heights[y * SIZE + x] = map.getTile(x, y).getHeight();
            }
        }
        return heights;
    }

    private static boolean sameTerrain(TileMap a, TileMap b) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (a.getTile(x, y).getHeight() != b.getTile(x, y).getHeight()
                        || a.getTile(x, y).getType() != b.getTile(x, y).getType()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void sameSeedAndContextProduceTheSameTerrain() {
        assertTrue(sameTerrain(generate(defaults(MapType.DARK_FOREST, 42L)), generate(defaults(MapType.DARK_FOREST, 42L))));
    }

    @Test
    void seedsThatDifferOnlyInTheHighBitsProduceDifferentTerrain() {
        long low = 12_345L;
        long high = low | (7L << 40);

        assertTrue((int) low == (int) high, "precondition: a narrowing cast cannot tell these seeds apart");
        assertTrue(!sameTerrain(generate(defaults(MapType.DARK_FOREST, low)), generate(defaults(MapType.DARK_FOREST, high))));
    }

    @Test
    void theFullLongSeedReachesEveryNoiseSource() {
        List<Long> seeds = new ArrayList<>();
        NoiseSourceFactory recording = (seed, settings) -> {
            seeds.add(seed);
            return (x, y) -> 0f;
        };
        long seed = 0x1234_5678_9ABC_DEF0L;

        new PerlinMapGenerator(recording).generateMap(8, 8, defaults(MapType.DARK_FOREST, seed));

        assertEquals(List.of(seed, seed + 1, seed + 2, seed + 3, seed + 4, seed + 5), seeds);
    }

    @Test
    void heightFrequencyFromTheContextChangesTheTerrain() {
        TileMap coarse = generate(context(MapType.DARK_FOREST, 7L, 0.01f, 4));
        TileMap fine = generate(context(MapType.DARK_FOREST, 7L, 0.08f, 4));

        assertNotEquals(java.util.Arrays.hashCode(heights(coarse)), java.util.Arrays.hashCode(heights(fine)));
    }

    @Test
    void heightOctavesFromTheContextChangesTheTerrain() {
        TileMap smooth = generate(context(MapType.DARK_FOREST, 7L, 0.02f, 1));
        TileMap detailed = generate(context(MapType.DARK_FOREST, 7L, 0.02f, 8));

        assertNotEquals(java.util.Arrays.hashCode(heights(smooth)), java.util.Arrays.hashCode(heights(detailed)));
    }

    @Test
    void generatorReportsTheContextHeightSettings() {
        PerlinMapGenerator generator = new PerlinMapGenerator(FastNoiseLiteAdapter.FACTORY);
        generator.generateMap(8, 8, context(MapType.TUNDRA, 3L, 0.033f, 6));

        assertEquals(0.033f, generator.getHeightNoiseFrequency());
        assertEquals(6, generator.getHeightNoiseOctaves());
    }

    @Test
    void generatorRequiresANoiseFactory() {
        assertThrows(NullPointerException.class, () -> new PerlinMapGenerator(null));
    }
}
