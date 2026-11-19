package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Iron ore and crystals are scarce: a handful to a few dozen on a map, crystals rarer than iron, both only on hills and
 * mountains, and present on every map type that has some. The counts are taken over a few fixed seeds.
 */
class ResourceScarcityTest {

    private static final int SIZE = 150;
    private static final long[] SEEDS = {1L, 2L, 3L, 20_261_111L, 987_654_321L};
    /** The most iron ore any single map may have, a scarce share of the thousands of rocky tiles of a range. */
    private static final int MAX_IRON_PER_MAP = 150;
    /** The most crystals any single map may have. */
    private static final int MAX_CRYSTALS_PER_MAP = 40;
    /** The least iron ore the fixed seeds give together on a map type with rock, which makes it present. */
    private static final int MIN_IRON_PER_TYPE = 10;

    private final MapType original = AppConfig.getInstance().world().getCurrentMapType();

    @AfterEach
    void restoreMapType() {
        AppConfig.getInstance().world().setCurrentMapType(original);
    }

    /** What one map holds: its rocky tiles, its iron ore and its crystals, and whether any sits off the rock. */
    private record Counts(int rocky, int iron, int crystals, int offRock) {
    }

    private static TileMap generate(MapType type, long seed) {
        final var world = AppConfig.getInstance().world();
        world.setCurrentMapType(type);
        final NoiseSettings profile = NoiseSettings.heightProfile(type);
        final var context = new MapGenerationContext(type, seed, profile.frequency(), profile.octaves(),
                world.getTemperatureNoiseFrequency(), world.getTemperatureNoiseOctaves(),
                world.getMoistureNoiseFrequency(), world.getMoistureNoiseOctaves(),
                world.getStonePatchFrequency(), world.getStoneThreshold(),
                world.getCrystalsPatchFrequency(), world.getCrystalsThreshold(),
                world.getTreeDensity(), world.getRiverDensity());
        return new PerlinMapGenerator(FastNoiseLiteAdapter.FACTORY).generateMap(SIZE, SIZE, context);
    }

    private static Counts count(TileMap map) {
        var rocky = 0;
        var iron = 0;
        var crystals = 0;
        var offRock = 0;
        for (var y = 0; y < SIZE; y++) {
            for (var x = 0; x < SIZE; x++) {
                final Tile tile = map.getTile(x, y);
                final boolean onRock = tile.getType() == TileType.HILL || tile.getType() == TileType.MOUNTAIN;
                rocky += onRock ? 1 : 0;
                if (!tile.hasResource()) {
                    continue;
                }
                final ResourceType resource = tile.getWorldResource().getType();
                if (resource == ResourceType.IRON_ORE || resource == ResourceType.CRYSTALS) {
                    iron += resource == ResourceType.IRON_ORE ? 1 : 0;
                    crystals += resource == ResourceType.CRYSTALS ? 1 : 0;
                    offRock += onRock ? 0 : 1;
                }
            }
        }
        return new Counts(rocky, iron, crystals, offRock);
    }

    @Test
    void ironAndCrystalsAreScarceOnEveryMapAndNeverOffTheRock() {
        for (MapType type : MapType.values()) {
            for (long seed : SEEDS) {
                final Counts counts = count(generate(type, seed));
                final String where = type + " seed " + seed + " " + counts;

                assertTrue(counts.iron() <= MAX_IRON_PER_MAP, "too much iron: " + where);
                assertTrue(counts.crystals() <= MAX_CRYSTALS_PER_MAP, "too many crystals: " + where);
                assertEquals(0, counts.offRock(), "ore off the rock: " + where);
            }
        }
    }

    @Test
    void everyMapTypeWithRockHasIronAndCrystalsAndTheCrystalsAreRarer() {
        for (MapType type : MapType.values()) {
            var rocky = 0;
            var iron = 0;
            var crystals = 0;
            for (long seed : SEEDS) {
                final Counts counts = count(generate(type, seed));
                rocky += counts.rocky();
                iron += counts.iron();
                crystals += counts.crystals();
            }
            if (rocky == 0) {
                assertEquals(0, iron + crystals, type + " has no rock and so no ore");
                continue;
            }
            final String where = type + " iron " + iron + " crystals " + crystals;
            assertTrue(iron >= MIN_IRON_PER_TYPE, "iron is missing: " + where);
            assertTrue(crystals >= 1, "crystals are missing: " + where);
            assertTrue(crystals < iron, "crystals are not rarer than iron: " + where);
        }
    }

    @Test
    void theSameSeedGivesTheSameOre() {
        final Counts first = count(generate(MapType.MOUNTAINOUS, SEEDS[0]));
        final Counts second = count(generate(MapType.MOUNTAINOUS, SEEDS[0]));

        assertEquals(first, second);
    }
}
