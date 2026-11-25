package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The trails run over small maps of grass with the sites placed by hand, so where a trail goes can be told. */
class TrailNetworkPassTest {

    private static final int SIDE = 60;
    private static final int ROW = 10;
    private static final int LEFT = 5;
    private static final int RIGHT = 40;
    private static final int LONG_REACH = 100;
    private static final int NO_EXTRA = 0;
    private static final int PLENTY_EXTRA = 30_000;
    private static final float FLAT_WEIGHT = 0F;
    private static final float NO_TREE_COST = 0F;
    private static final float TREE_COST = 4F;
    private static final float WALL = Float.POSITIVE_INFINITY;
    private static final float PASS_COST = 10F;
    private static final long SEED = 3L;
    /** The columns of the wall that crosses the map in the tests of water and mountains. */
    private static final int WALL_FIRST = 20;
    private static final int WALL_LAST = 24;
    private static final int WALL_MIDDLE = 22;
    /** The band of thick groves across the map in the test of the groves, with a gap in the column of the sites. */
    private static final int GROVE_FIRST = 10;
    private static final int GROVE_LAST = 15;
    private static final int GROVE_GAP = 25;
    private static final float THICK = 1F;
    private static final float STRONG_WEIGHT = 50F;
    /** What clearing a resource costs in the test of the resources, far more than a detour of a few tiles. */
    private static final float STRONG_COST = 50F;
    private static final int NORTH_ROW = 5;
    private static final int SOUTH_ROW = 25;
    private static final int TRIANGLE_APEX_X = 25;
    private static final int TRIANGLE_APEX_Y = 45;
    private static final int SEEDS = 24;
    private static final int OFF_ROW = ROW + 1;
    /** The row of the only opening in the wall of stones, three rows from the straight line between the sites. */
    private static final int OPENING_ROW = ROW + 3;
    /** A diagonal step through the opening also clears the corner tile it cuts, one on each side of the opening. */
    private static final int MOST_CORNERS = 2;

    private static GenerationContext grass(long seed) {
        final var context = TestContexts.of(MapType.DENSE_FOREST, seed, SIDE, SIDE, TestContexts.FLAT);
        TestContexts.fillGround(context, TileType.GRASS);
        return context;
    }

    private static TrailParams params(float noiseWeight, float treeCost, float mountainCost, int extra, int width) {
        return new TrailParams(noiseWeight, treeCost, mountainCost, extra, width, LONG_REACH);
    }

    private static void apply(GenerationContext context, TrailParams params) {
        new TrailNetworkPass(params).apply(context);
    }

    private static int reservedCount(GenerationContext context) {
        var count = 0;
        for (var index = 0; index < context.size(); index++) {
            count += context.isReserved(index) ? 1 : 0;
        }
        return count;
    }

    /** Tells whether the reserved tiles join two tiles through steps along the four directions. */
    private static boolean areJoined(GenerationContext context, int from, int to) {
        final var seen = new boolean[context.size()];
        final var queue = new int[context.size()];
        var head = 0;
        var tail = 0;
        queue[tail] = from;
        tail++;
        seen[from] = true;
        while (head < tail) {
            final var index = queue[head];
            head++;
            final var x = context.xOf(index);
            final var y = context.yOf(index);
            final var neighbours = new int[]{x > 0 ? index - 1 : index, x < SIDE - 1 ? index + 1 : index,
                y > 0 ? index - SIDE : index, y < SIDE - 1 ? index + SIDE : index};
            for (var neighbour : neighbours) {
                if (!seen[neighbour] && context.isReserved(neighbour)) {
                    seen[neighbour] = true;
                    queue[tail] = neighbour;
                    tail++;
                }
            }
        }
        return seen[to];
    }

    private static void siteAt(GenerationContext context, int x, int y) {
        context.sites().add(context.index(x, y));
        context.reserve(context.index(x, y));
    }

    @Test
    void aTrailJoinsTwoSitesThroughTilesThatAreReservedAndClearedOfResources() {
        final var context = grass(SEED);
        for (var index = 0; index < context.size(); index++) {
            context.setResource(index, ResourceType.TREE);
        }
        siteAt(context, LEFT, ROW);
        siteAt(context, RIGHT, ROW);

        apply(context, params(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1));

        assertTrue(areJoined(context, context.index(LEFT, ROW), context.index(RIGHT, ROW)));
        for (var index = 0; index < context.size(); index++) {
            assertEquals(context.isReserved(index) ? null : ResourceType.TREE, context.resourceAt(index),
                    "tile " + index);
        }
    }

    @Test
    void aSingleSiteOrNoSiteGetsNoTrail() {
        final var none = grass(SEED);
        final var single = grass(SEED);
        siteAt(single, LEFT, ROW);

        apply(none, params(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1));
        apply(single, params(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1));

        assertEquals(0, reservedCount(none));
        assertEquals(1, reservedCount(single));
    }

    @Test
    void sitesFartherApartThanTheReachAreNotJoinedDirectly() {
        final var context = grass(SEED);
        siteAt(context, LEFT, ROW);
        siteAt(context, RIGHT, ROW);

        apply(context, new TrailParams(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1, RIGHT - LEFT - 1));

        assertEquals(2, reservedCount(context));
    }

    @Test
    void waterCutsTheLandAndNoTrailCrossesItWhileSitesOnTheSameSideAreStillJoined() {
        final var context = grass(SEED);
        wall(context, TileType.SHALLOW_WATER);
        siteAt(context, LEFT, ROW);
        siteAt(context, WALL_FIRST - 1, ROW + NORTH_ROW);
        siteAt(context, RIGHT, ROW);

        apply(context, params(FLAT_WEIGHT, NO_TREE_COST, PASS_COST, NO_EXTRA, 1));

        assertTrue(areJoined(context, context.index(LEFT, ROW), context.index(WALL_FIRST - 1, ROW + NORTH_ROW)));
        assertFalse(areJoined(context, context.index(LEFT, ROW), context.index(RIGHT, ROW)));
        for (var y = 0; y < SIDE; y++) {
            for (var x = WALL_FIRST; x <= WALL_LAST; x++) {
                assertFalse(context.isReserved(context.index(x, y)), "water at " + x + "," + y);
                assertEquals(TileType.SHALLOW_WATER, context.groundAt(context.index(x, y)));
            }
        }
    }

    @Test
    void aMountainWallThatIsClosedStopsTheTrailAndOneWithACostGetsAPass() {
        final var closed = grass(SEED);
        final var open = grass(SEED);
        wall(closed, TileType.MOUNTAIN);
        wall(open, TileType.MOUNTAIN);
        siteAt(closed, LEFT, ROW);
        siteAt(closed, RIGHT, ROW);
        siteAt(open, LEFT, ROW);
        siteAt(open, RIGHT, ROW);

        apply(closed, params(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1));
        apply(open, params(FLAT_WEIGHT, NO_TREE_COST, PASS_COST, NO_EXTRA, 1));

        assertFalse(areJoined(closed, closed.index(LEFT, ROW), closed.index(RIGHT, ROW)));
        assertEquals(WALL_LAST - WALL_FIRST + 1, TestContexts.countGround(closed, TileType.MOUNTAIN) / SIDE);
        assertTrue(areJoined(open, open.index(LEFT, ROW), open.index(RIGHT, ROW)));
        assertEquals(TileType.ROCKY_GROUND, open.groundAt(open.index(WALL_MIDDLE, ROW)));
        final var rockyTiles = TestContexts.countGround(open, TileType.ROCKY_GROUND);
        assertTrue(rockyTiles >= WALL_LAST - WALL_FIRST + 1 && rockyTiles < SIDE, "a narrow pass: " + rockyTiles);
        assertEquals(SIDE * (WALL_LAST - WALL_FIRST + 1) - rockyTiles, TestContexts.countGround(open,
                TileType.MOUNTAIN));
    }

    @Test
    void aTrailWindsAroundThickGrovesThroughTheGapWhenTheNoiseWeighsEnough() {
        final var context = grass(SEED);
        final var noise = context.defineField(NoiseField.TREE);
        for (var y = GROVE_FIRST; y <= GROVE_LAST; y++) {
            for (var x = 0; x < SIDE; x++) {
                noise[context.index(x, y)] = x == GROVE_GAP ? 0F : THICK;
            }
        }
        siteAt(context, LEFT, NORTH_ROW);
        siteAt(context, LEFT, SOUTH_ROW);

        apply(context, params(STRONG_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1));

        assertTrue(context.isReserved(context.index(GROVE_GAP, GROVE_FIRST)), "the trail uses the gap");
        assertFalse(context.isReserved(context.index(LEFT, GROVE_FIRST)), "and avoids the grove");
    }

    @Test
    void aTrailGoesAroundResourcesThatCostMoreToClear() {
        final var context = grass(SEED);
        for (var y = 0; y < SIDE; y++) {
            context.setResource(context.index(WALL_MIDDLE, y), ResourceType.STONE);
        }
        context.setResource(context.index(WALL_MIDDLE, OPENING_ROW), null);
        siteAt(context, LEFT, ROW);
        siteAt(context, RIGHT, ROW);

        apply(context, params(FLAT_WEIGHT, STRONG_COST, WALL, NO_EXTRA, 1));

        assertTrue(context.isReserved(context.index(WALL_MIDDLE, OPENING_ROW)), "the trail takes the opening");
        var stones = 0;
        for (var y = 0; y < SIDE; y++) {
            stones += context.resourceAt(context.index(WALL_MIDDLE, y)) == ResourceType.STONE ? 1 : 0;
        }
        assertTrue(stones >= SIDE - 1 - MOST_CORNERS, "the wall stands but for the opening and its corners: " + stones);
    }

    @Test
    void extraTrailsCloseLoopsBeyondTheTree() {
        final var tree = grass(SEED);
        final var loops = grass(SEED);
        for (var context : new GenerationContext[]{tree, loops}) {
            siteAt(context, LEFT, NORTH_ROW);
            siteAt(context, RIGHT, NORTH_ROW);
            siteAt(context, TRIANGLE_APEX_X, TRIANGLE_APEX_Y);
        }

        apply(tree, params(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1));
        apply(loops, params(FLAT_WEIGHT, NO_TREE_COST, WALL, PLENTY_EXTRA, 1));

        assertTrue(reservedCount(loops) > reservedCount(tree), "the third side of the triangle is cut");
    }

    @Test
    void aTrailIsOneTileWideWhenTheMaximumIsOneAndSometimesTwoWhenItIsTwo() {
        var sawTwoWide = false;
        for (var seed = 1L; seed <= SEEDS; seed++) {
            final var thin = grass(seed);
            final var wide = grass(seed);
            siteAt(thin, LEFT, ROW);
            siteAt(thin, RIGHT, ROW);
            siteAt(wide, LEFT, ROW);
            siteAt(wide, RIGHT, ROW);

            apply(thin, params(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 1));
            apply(wide, params(FLAT_WEIGHT, NO_TREE_COST, WALL, NO_EXTRA, 2));

            assertFalse(thin.isReserved(thin.index(LEFT + 1, OFF_ROW)), "a thin trail has no second row");
            sawTwoWide |= wide.isReserved(wide.index(LEFT + 1, OFF_ROW));
        }
        assertTrue(sawTwoWide, "some trails are two tiles wide");
    }

    @Test
    void theSameSeedCutsTheSameTrails() {
        final var first = grass(SEED);
        final var again = grass(SEED);
        for (var context : new GenerationContext[]{first, again}) {
            siteAt(context, LEFT, ROW);
            siteAt(context, RIGHT, ROW);
            siteAt(context, TRIANGLE_APEX_X, TRIANGLE_APEX_Y);
        }

        apply(first, params(STRONG_WEIGHT, TREE_COST, WALL, PLENTY_EXTRA, 2));
        apply(again, params(STRONG_WEIGHT, TREE_COST, WALL, PLENTY_EXTRA, 2));

        for (var index = 0; index < first.size(); index++) {
            assertEquals(first.isReserved(index), again.isReserved(index), "tile " + index);
        }
        assertTrue(reservedCount(first) > 3, "the trails cut something");
    }

    @Test
    void theParametersRefuseCostsCountsAndSizesThatCannotBuildATrail() {
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(-1F, 0F, WALL, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(Float.NaN, 0F, WALL, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(0F, -1F, WALL, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(0F, 0F, 0F, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(0F, 0F, Float.NaN, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(0F, 0F, WALL, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(0F, 0F, WALL, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(0F, 0F, WALL, 0, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrailParams(0F, 0F, WALL, 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TrailParams(0F, 0F, WALL, 0, 1, SiteEdges.MAX_LENGTH + 1));
        assertThrows(NullPointerException.class, () -> new TrailNetworkPass(null));
    }

    @Test
    void thePassIsNamedForTheTimings() {
        assertEquals("trails", new TrailNetworkPass(params(0F, 0F, WALL, 0, 1)).name());
    }

    /** Sets a wall of the given ground across every row, from the first column of the wall to the last. */
    private static void wall(GenerationContext context, TileType type) {
        for (var y = 0; y < SIDE; y++) {
            for (var x = WALL_FIRST; x <= WALL_LAST; x++) {
                context.setGround(context.index(x, y), type);
            }
        }
    }
}
