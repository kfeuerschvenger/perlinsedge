package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceBalancePassTest {

    private static final int SIDE = 30;
    private static final int AREA = SIDE * SIDE;
    private static final int PER_100K = 100_000;
    private static final int ALL_OF_THE_ZONE = 1000;
    private static final int TENTH_OF_THE_ZONE = 100;
    private static final int BIG_ZONE_SIDE = 10;
    private static final int SMALL_ZONE_SIDE = 5;
    private static final int SMALL_ZONE_FROM = 20;
    private static final int ROCKY_TILES = BIG_ZONE_SIDE * BIG_ZONE_SIDE + SMALL_ZONE_SIDE * SMALL_ZONE_SIDE;
    private static final int BLOCKED_TILES = 2;
    private static final float NEAR_THE_TOP = 0.8F;
    private static final long SEED = 5L;
    private static final long OTHER_SEED = 6L;

    /** Converts a count on the test map into the quota that asks for it: counts per 100,000 tiles of map. */
    private static int per100k(int count) {
        return (int) Math.ceil(count * (double) PER_100K / AREA);
    }

    private static ResourceQuota rocky(ResourceType type, int min, int max, int cap) {
        return new ResourceQuota(type, per100k(min), max == ResourceQuota.UNLIMITED ? max : per100k(max),
                ResourceQuota.Host.ROCKY, cap);
    }

    private static void balance(GenerationContext context, ResourceQuota... quotas) {
        new ResourceBalancePass(new BalanceParams(List.of(quotas))).apply(context);
    }

    /** Water everywhere but two rocky zones: a big one at the upper left and a small one further down and right. */
    private static GenerationContext twoZones(long seed) {
        final var context = TestContexts.of(MapType.DESERT, seed, SIDE, SIDE, TestContexts.FLAT);
        TestContexts.fillGround(context, TileType.SHALLOW_WATER);
        fillSquare(context, 0, BIG_ZONE_SIDE);
        fillSquare(context, SMALL_ZONE_FROM, SMALL_ZONE_SIDE);
        return context;
    }

    private static void fillSquare(GenerationContext context, int from, int side) {
        for (var y = from; y < from + side; y++) {
            for (var x = from; x < from + side; x++) {
                context.setGround(context.index(x, y), TileType.ROCKY_GROUND);
            }
        }
    }

    private static boolean inBigZone(GenerationContext context, int index) {
        return context.xOf(index) < BIG_ZONE_SIDE && context.yOf(index) < BIG_ZONE_SIDE;
    }

    private static int countInBigZone(GenerationContext context, ResourceType type) {
        var count = 0;
        for (var index = 0; index < context.size(); index++) {
            count += inBigZone(context, index) && context.resourceAt(index) == type ? 1 : 0;
        }
        return count;
    }

    @Test
    void aResourceAboveItsMaximumIsThinnedDownToIt() {
        final var context = twoZones(SEED);
        for (var index = 0; index < context.size(); index++) {
            if (context.groundAt(index) == TileType.ROCKY_GROUND) {
                context.setResource(index, ResourceType.IRON_ORE);
            }
        }
        final var kept = 7;
        context.setResource(0, ResourceType.STONE);

        balance(context, rocky(ResourceType.IRON_ORE, 0, kept, ALL_OF_THE_ZONE));

        assertEquals(kept, TestContexts.countResource(context, ResourceType.IRON_ORE));
        assertEquals(ResourceType.STONE, context.resourceAt(0), "another resource is not touched");
    }

    @Test
    void aMissingResourceGoesToTheLargestZoneFirst() {
        final var context = twoZones(SEED);
        final var missing = 10;

        balance(context, rocky(ResourceType.IRON_ORE, missing, ResourceQuota.UNLIMITED, ALL_OF_THE_ZONE));

        assertTrue(TestContexts.countResource(context, ResourceType.IRON_ORE) >= missing);
        assertEquals(TestContexts.countResource(context, ResourceType.IRON_ORE),
                countInBigZone(context, ResourceType.IRON_ORE));
    }

    @Test
    void aZoneGivesAtMostItsCapInTheFirstRoundAndTheRestSpillsToTheNextZone() {
        final var context = twoZones(SEED);
        final var bigCap = BIG_ZONE_SIDE * BIG_ZONE_SIDE * TENTH_OF_THE_ZONE / ALL_OF_THE_ZONE;
        final var smallCap = SMALL_ZONE_SIDE * SMALL_ZONE_SIDE * TENTH_OF_THE_ZONE / ALL_OF_THE_ZONE;
        final var missing = bigCap + smallCap;

        balance(context, rocky(ResourceType.IRON_ORE, missing, ResourceQuota.UNLIMITED, TENTH_OF_THE_ZONE));

        assertEquals(missing, TestContexts.countResource(context, ResourceType.IRON_ORE));
        assertEquals(bigCap, countInBigZone(context, ResourceType.IRON_ORE));
    }

    @Test
    void aSecondRoundFillsTheZonesBeyondTheirCapWhenTheResourceIsStillMissing() {
        final var context = twoZones(SEED);
        final var missing = 60;

        balance(context, rocky(ResourceType.STONE, missing, ResourceQuota.UNLIMITED, TENTH_OF_THE_ZONE));

        assertTrue(TestContexts.countResource(context, ResourceType.STONE) >= missing);
    }

    @Test
    void theResourceStopsWhenTheRockHasNoFreeTileLeft() {
        final var context = twoZones(SEED);

        balance(context, rocky(ResourceType.STONE, AREA, ResourceQuota.UNLIMITED, ALL_OF_THE_ZONE));

        assertEquals(ROCKY_TILES, TestContexts.countResource(context, ResourceType.STONE));
    }

    @Test
    void occupiedReservedAndNonRockyTilesAreNeverUsed() {
        final var context = twoZones(SEED);
        final var occupied = context.index(1, 1);
        final var reserved = context.index(2, 2);
        context.setResource(occupied, ResourceType.TREE);
        context.reserve(reserved);

        balance(context, rocky(ResourceType.STONE, AREA, ResourceQuota.UNLIMITED, ALL_OF_THE_ZONE));

        assertEquals(ResourceType.TREE, context.resourceAt(occupied));
        assertNull(context.resourceAt(reserved));
        assertEquals(ROCKY_TILES - BLOCKED_TILES, TestContexts.countResource(context, ResourceType.STONE));
        for (var index = 0; index < context.size(); index++) {
            assertTrue(context.resourceAt(index) == null || context.groundAt(index) == TileType.ROCKY_GROUND);
        }
    }

    @Test
    void aMapWithNoRockLeavesARockyResourceMissingWithoutFailing() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);

        balance(context, rocky(ResourceType.IRON_ORE, 1, ResourceQuota.UNLIMITED, ALL_OF_THE_ZONE));

        assertEquals(0, TestContexts.countResource(context, ResourceType.IRON_ORE));
    }

    @Test
    void missingTreesGoToTheFreeFertileTilesWhereTheTreeNoiseIsHighest() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        final var tree = context.defineField(NoiseField.TREE);
        for (var index = 0; index < AREA; index++) {
            tree[index] = (float) index / AREA;
        }
        context.setGround(AREA - 1, TileType.ROCKY_GROUND);
        final var missing = 90;

        balance(context, new ResourceQuota(ResourceType.TREE, per100k(missing), ResourceQuota.UNLIMITED,
                ResourceQuota.Host.FERTILE, 1));

        assertEquals(missing, TestContexts.countResource(context, ResourceType.TREE));
        assertNull(context.resourceAt(AREA - 1), "rocky ground is not fertile");
        for (var index = 0; index < AREA; index++) {
            if (context.resourceAt(index) == ResourceType.TREE) {
                assertTrue(tree[index] >= NEAR_THE_TOP, "a tree grew where the noise is " + tree[index]);
            }
        }
    }

    @Test
    void treesNeverGrowOnReservedTiles() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        TestContexts.fillField(context, NoiseField.TREE, NEAR_THE_TOP);
        for (var index = 0; index < AREA; index++) {
            context.reserve(index);
        }

        balance(context, new ResourceQuota(ResourceType.TREE, PER_100K, ResourceQuota.UNLIMITED,
                ResourceQuota.Host.FERTILE, 1));

        assertEquals(0, TestContexts.countResource(context, ResourceType.TREE));
    }

    @Test
    void aMapWithTreesAboveAnUnlimitedMaximumKeepsThemAll() {
        final var context = TestContexts.of(SIDE, SIDE);
        TestContexts.fillGround(context, TileType.GRASS);
        TestContexts.fillField(context, NoiseField.TREE, NEAR_THE_TOP);
        for (var index = 0; index < AREA; index++) {
            context.setResource(index, ResourceType.TREE);
        }

        balance(context, new ResourceQuota(ResourceType.TREE, 1, ResourceQuota.UNLIMITED,
                ResourceQuota.Host.FERTILE, 1));

        assertEquals(AREA, TestContexts.countResource(context, ResourceType.TREE));
    }

    @Test
    void theSameSeedGivesTheSameBalanceAndAnotherSeedGivesAnother() {
        final var first = twoZones(SEED);
        final var second = twoZones(SEED);
        final var other = twoZones(OTHER_SEED);
        final var quota = rocky(ResourceType.IRON_ORE, 20, ResourceQuota.UNLIMITED, TENTH_OF_THE_ZONE);

        balance(first, quota);
        balance(second, quota);
        balance(other, quota);

        assertEquals(Arrays.asList(first.resources()), Arrays.asList(second.resources()));
        assertNotEquals(Arrays.asList(first.resources()), Arrays.asList(other.resources()));
    }
}
