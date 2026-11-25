package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
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
    /** How many seeds the sweep of the small sizes draws, from a stream seeded the day the sweep was added. */
    private static final int SWEEP_SEEDS = 30;
    private static final long SWEEP_SOURCE = 20_261_125L;

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

    /**
     * The minimum of five islands holds for the seeds that once lost one, because the fifth disc found no place or
     * its coast fused with another, at the sides where it happened.
     */
    @ParameterizedTest
    @CsvSource({"50,171", "100,40", "100,218"})
    void anIslandsMapKeepsFiveIslandsForTheSeedsThatLostOneToCrowding(int size, long seed) {
        final var map = generator.generate(new MapRequest(MapType.ISLANDS, seed, size, size));

        final var islands = WaterStats.landMasses(map).countAtLeast(WaterStats.MIN_LAND_TILES);

        assertTrue(islands >= MIN_ISLANDS, "seed " + seed + " size " + size + " has " + islands + " islands");
    }

    /** The Mediterranean keeps a real river for the seeds whose rivers once dead ended in a tile or two. */
    @ParameterizedTest
    @CsvSource({"150,150,69", "150,150,46", "150,150,221", "200,120,67", "200,120,179", "200,120,144"})
    void aMediterraneanMapKeepsARealRiverForTheSeedsThatLeftStubs(int width, int height, long seed) {
        final var map = generator.generate(new MapRequest(MapType.MEDITERRANEAN, seed, width, height));

        assertDoesNotThrow(() -> WaterInvariants.mediterraneanKeepsItsRivers(map),
                "seed " + seed + " " + width + "x" + height);
    }

    /**
     * A wide sweep of seeds at the small sizes: every islands map keeps five islands, and its share of land when it is
     * square, and every Mediterranean map keeps its rivers, so a regression that only a few seeds in a hundred show is
     * still caught.
     */
    @ParameterizedTest
    @MethodSource("sweepSizes")
    void theSmallSizesHoldTheirIslandsAndRiversOnEverySeedOfTheSweep(int width, int height) {
        for (var seed : FixedSeeds.sweep(SWEEP_SOURCE, SWEEP_SEEDS)) {
            final var islands = generator.generate(new MapRequest(MapType.ISLANDS, seed, width, height));
            final var label = "seed " + seed + " " + width + "x" + height;
            final var count = WaterStats.landMasses(islands).countAtLeast(WaterStats.MIN_LAND_TILES);

            assertTrue(count >= MIN_ISLANDS, label + " has " + count + " islands");
            if (width == height) {
                // A wide map holds the minimum of five islands in a smaller share of the land, which is not a defect
                assertDoesNotThrow(() -> WaterInvariants.islandsShareTheMapWithTheSea(islands,
                        new MapSpec(MapType.ISLANDS, seed, width)), label);
            }
            final var sea = generator.generate(new MapRequest(MapType.MEDITERRANEAN, seed, width, height));
            assertDoesNotThrow(() -> WaterInvariants.mediterraneanKeepsItsRivers(sea), label);
        }
    }

    /** Returns the sizes of the sweep: a square of 100, one of 150 and the wide map of 200 by 120. */
    private static Stream<Arguments> sweepSizes() {
        return Stream.of(Arguments.of(ONE_HUNDRED, ONE_HUNDRED), Arguments.of(MEDIUM, MEDIUM),
                Arguments.of(WIDE_WIDTH, WIDE_HEIGHT));
    }

    /**
     * The forest and the mountains hold the invariants of their type on every seed of a sweep at the small sizes and on
     * the wide map, and the common ones too on the squares, whose quotas the invariants scale by the side. A share of
     * trees, a free glade at the spawn and a spawn region that holds most of the land have to survive a map that sees
     * only a few periods of the noise.
     */
    @ParameterizedTest
    @EnumSource(value = MapType.class, names = {"DENSE_FOREST", "MOUNTAINOUS"})
    void theContentMapsHoldTheirInvariantsOnEverySeedOfTheSmallSizeSweep(MapType type) {
        final var source = MapSource.current();
        final var perType = MapInvariants.perType().get(type);
        final var common = MapInvariants.common(source);
        for (var seed : FixedSeeds.sweep(SWEEP_SOURCE, SWEEP_SEEDS)) {
            requireInvariants(type, seed, SMALL, SMALL, perType, common);
            requireInvariants(type, seed, MEDIUM, MEDIUM, perType, common);
            requireInvariants(type, seed, WIDE_WIDTH, WIDE_HEIGHT, perType, List.of());
        }
    }

    /** Generates a map and checks the per type invariants and then the common ones, naming the map in a failure. */
    private void requireInvariants(MapType type, long seed, int width, int height, List<MapInvariant> perType,
                                   List<MapInvariant> common) {
        final var map = generator.generate(new MapRequest(type, seed, width, height));
        final var spec = new MapSpec(type, seed, Math.min(width, height));
        final var label = type + "-" + seed + "-" + width + "x" + height;
        for (var invariant : Stream.concat(perType.stream(), common.stream()).toList()) {
            assertDoesNotThrow(() -> invariant.check(map, spec), label);
        }
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
