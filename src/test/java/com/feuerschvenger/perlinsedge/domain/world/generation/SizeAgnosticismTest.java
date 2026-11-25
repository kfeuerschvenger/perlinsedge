package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.generation.support.FixedSeeds;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.MapStats;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.infra.noise.FastNoiseLiteAdapter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tile is a fixed physical size, so a bigger map is more world and not a zoomed one: the share of the map that is
 * water, trees or stone stays about the same when the side doubles. The shares are the average over three fixed seeds.
 */
class SizeAgnosticismTest {

    private static final int SMALL = 150;
    private static final int LARGE = 300;
    private static final int SEEDS = 3;
    /** The largest relative difference between the shares at the two sizes. */
    private static final double TOLERANCE = 0.35;
    /** A share below this is too small for a relative comparison, so the difference must stay below it instead. */
    private static final double NEGLIGIBLE = 0.01;

    /** The average shares of the area of a map that are water, trees and stone. */
    private record Shares(double water, double trees, double stone) {
    }

    private static Shares sharesAt(MapType type, int size) {
        final var generator = new MapGenerator(FastNoiseLiteAdapter.FACTORY);
        var water = 0.0;
        var trees = 0.0;
        var stone = 0.0;
        for (var seed : FixedSeeds.ALL.subList(0, SEEDS)) {
            final var map = generator.generate(new MapRequest(type, seed, size, size));
            final var area = (double) size * size;
            water += (area - MapStats.landTiles(map)) / area;
            trees += MapStats.count(map, ResourceType.TREE) / area;
            stone += MapStats.count(map, ResourceType.STONE) / area;
        }
        return new Shares(water / SEEDS, trees / SEEDS, stone / SEEDS);
    }

    private static void assertClose(String what, MapType type, double small, double large) {
        final var difference = Math.abs(small - large);
        assertTrue(difference <= NEGLIGIBLE || difference <= TOLERANCE * Math.max(small, large),
                type + " " + what + " share is " + small + " at " + SMALL + " and " + large + " at " + LARGE);
    }

    @ParameterizedTest
    @EnumSource(MapType.class)
    void theSharesOfWaterTreesAndStoneDoNotDependOnTheSizeOfTheMap(MapType type) {
        final var small = sharesAt(type, SMALL);
        final var large = sharesAt(type, LARGE);

        assertClose("water", type, small.water(), large.water());
        assertClose("tree", type, small.trees(), large.trees());
        assertClose("stone", type, small.stone(), large.stone());
    }
}
