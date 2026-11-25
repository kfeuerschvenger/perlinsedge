package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every map type has to generate on the small and on the odd sizes the game may ask for: a small square and a map that
 * is wider than it is high. A map of every type has land, a spawn and a spawn region that holds most of the land, for
 * every fixed seed.
 */
class MapSmokeTest {

    private static final int SMALL = 64;
    private static final int WIDE_WIDTH = 200;
    private static final int WIDE_HEIGHT = 120;

    private final MapGenerator generator = new MapGenerator(FastNoiseLiteAdapter.FACTORY);

    private static Stream<Arguments> typesAndSizes() {
        return Arrays.stream(MapType.values()).flatMap(type -> Stream.of(
                Arguments.of(type, SMALL, SMALL), Arguments.of(type, WIDE_WIDTH, WIDE_HEIGHT)));
    }

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
