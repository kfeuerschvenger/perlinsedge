package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every map type has to generate on the small and on the odd sizes the game may ask for: a small square and a map that
 * is wider than it is high. A map of every type has land, a spawn and a spawn region that holds most of the land, for
 * every fixed seed.
 */
class MapSmokeTest {

    private static final int SMALL = 64;
    private static final int MEDIUM = 150;
    private static final int ONE_HUNDRED = 100;
    private static final int MIN_ISLANDS = 5;
    private static final int WIDE_WIDTH = 200;
    private static final int WIDE_HEIGHT = 120;

    private final MapGenerator generator = new MapGenerator(FastNoiseLiteAdapter.FACTORY);

    /** Returns every map type on the small square and on the wide map, which is the argument list of the smoke test. */
    private static Stream<Arguments> typesAndSizes() {
        return Arrays.stream(MapType.values()).flatMap(type -> Stream.of(
                Arguments.of(type, SMALL, SMALL), Arguments.of(type, WIDE_WIDTH, WIDE_HEIGHT)));
    }

    /** The minimum of five islands is honest on the small maps: every fixed seed has five islands of real size. */
    @ParameterizedTest
    @ValueSource(ints = {SMALL, ONE_HUNDRED, MEDIUM})
    void anIslandsMapHasAtLeastFiveIslandsOnSmallSizes(int size) {
        for (var seed : FixedSeeds.ALL) {
            final var map = generator.generate(new MapRequest(MapType.ISLANDS, seed, size, size));

            final var islands = WaterStats.landMasses(map).countAtLeast(WaterStats.MIN_LAND_TILES);

            assertTrue(islands >= MIN_ISLANDS, "seed " + seed + " size " + size + " has " + islands + " islands");
        }
    }

    /**
     * The minimum of five islands holds on a map of 100 as well, for the seeds that once left it with four because an
     * island found no place at a radius between the ones tried.
     */
    @ParameterizedTest
    @ValueSource(longs = {71_284L, 261_340L, 586_019L, 783_994L, 1_029_483L, 1_053_240L, 1_108_673L})
    void anIslandsMapOfOneHundredKeepsFiveIslandsForTheSeedsThatLostOne(long seed) {
        final var map = generator.generate(new MapRequest(MapType.ISLANDS, seed, ONE_HUNDRED, ONE_HUNDRED));

        final var islands = WaterStats.landMasses(map).countAtLeast(WaterStats.MIN_LAND_TILES);

        assertTrue(islands >= MIN_ISLANDS, "seed " + seed + " has " + islands + " islands");
    }

    /** The rivers of a Mediterranean map survive its water limit at every size, wide maps included. */
    @ParameterizedTest
    @MethodSource("mediterraneanSizes")
    void aMediterraneanMapKeepsItsRiversAtEverySize(int width, int height) {
        for (var seed : FixedSeeds.ALL) {
            final var map = generator.generate(new MapRequest(MapType.MEDITERRANEAN, seed, width, height));

            assertDoesNotThrow(() -> WaterInvariants.mediterraneanKeepsItsRivers(map),
                    "seed " + seed + " " + width + "x" + height);
        }
    }

    /** Returns the sizes of the river check of the Mediterranean: the small square, the medium one and the wide map. */
    private static Stream<Arguments> mediterraneanSizes() {
        return Stream.of(Arguments.of(SMALL, SMALL), Arguments.of(MEDIUM, MEDIUM),
                Arguments.of(WIDE_WIDTH, WIDE_HEIGHT));
    }

    /** Generates the map of every fixed seed and checks its size, its land, its spawn and its spawn region. */
    @ParameterizedTest
    @MethodSource("typesAndSizes")
    void everyTypeGeneratesLandASpawnAndASpawnRegionOnSmallAndOddSizes(MapType type, int width, int height) {
        for (var seed : FixedSeeds.ALL) {
            final var map = generator.generate(new MapRequest(type, seed, width, height));
            final var label = type + "-" + seed + "-" + width + "x" + height;

            assertEquals(width, map.getWidth(), label);
            assertEquals(height, map.getHeight(), label);
            assertTrue(MapStats.landTiles(map) > 0, label + " has land");
            assertTrue(map.getSpawn().isPresent(), label + " has a spawn");
            requireSpawnRegion(map, label);
        }
    }

    /** Checks the spawn region rule of the water invariants, naming the map in the failure. */
    private static void requireSpawnRegion(TileMap map, String label) {
        try {
            WaterInvariants.theSpawnRegionHoldsMostOfTheLand(map);
        } catch (AssertionError failure) {
            throw new AssertionError(label + ": " + failure.getMessage(), failure);
        }
    }
}
