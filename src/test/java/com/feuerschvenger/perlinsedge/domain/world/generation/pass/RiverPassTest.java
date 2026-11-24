package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rivers run over a ramp: the height falls by a fixed step along every row, so downhill is always towards the
 * right edge, the sources are the columns on the left, and the end of the ramp is a dead end.
 */
class RiverPassTest {

    private static final int WIDTH = 24;
    private static final int HEIGHT = 12;
    private static final float TOP = 1F;
    private static final float STEP = 0.04F;
    private static final float SOURCE_MIN = 0.6F;
    private static final int LAST_SOURCE_COLUMN = 9;
    private static final float MANY_RIVERS = 0.5F;
    private static final float ONE_RIVER = 0.0001F;
    private static final int SHORT_MIN = 3;
    private static final int SHORT_MAX = 6;
    private static final int LONG_MIN = 30;
    private static final int LONG_MAX = 40;
    private static final int NO_BRANCH_BEFORE = 1_000;
    private static final int BRANCH_MIN = 3;
    private static final int BRANCH_SPREAD = 3;
    private static final float ALWAYS = 1F;
    private static final float NEVER = 0F;
    private static final float LAKE_MAX_HEIGHT = 0.4F;
    private static final int LAKE_MIN_RADIUS = 2;
    private static final int LAKE_MAX_RADIUS = 4;
    private static final float LAKE_TOLERANCE = 0.05F;
    private static final int WATER_COLUMN = 12;
    private static final int FLOODED_ROWS = 5;
    private static final float FLAT = 0.5F;
    private static final float LOWEST = 0F;
    private static final long BRANCH_SEEDS = 10L;
    private static final long OTHER_SEED = 77L;
    private static final long SEED = 5L;
    private static final int LANE_ROW = 5;
    private static final float WALL = 2F;
    private static final int FUNNEL_WIDTH = 6;
    private static final int FUNNEL_HEIGHT = 3;
    private static final int FIRST_ROW = 0;
    private static final int MIDDLE_ROW = 1;
    private static final int SOURCE_COLUMN = 0;
    private static final int FORK_COLUMN = 2;
    private static final float FUNNEL_SOURCE_MIN = 0.9F;
    private static final float MAIN_HEIGHT = 0.8F;
    private static final float BRANCH_HEIGHT = 0.85F;
    private static final float JOIN_HEIGHT = 0.5F;
    private static final float BEYOND_HEIGHT = 0.3F;

    private static GenerationContext ramp(long seed) {
        final var context = TestContexts.of(MapType.RIVERS, seed, WIDTH, HEIGHT, TestContexts.FLAT);
        final var heights = context.defineField(NoiseField.HEIGHT);
        for (var y = 0; y < HEIGHT; y++) {
            for (var x = 0; x < WIDTH; x++) {
                heights[context.index(x, y)] = TOP - STEP * x;
            }
        }
        TestContexts.fillGround(context, TileType.GRASS);
        return context;
    }

    private static RiverParams params(float density, int minLength, int maxLength, float branchChance,
                                      TileType surface) {
        return params(SOURCE_MIN, density, minLength, maxLength, branchChance, surface);
    }

    private static RiverParams params(float sourceMin, float density, int minLength, int maxLength,
                                      float branchChance, TileType surface) {
        return new RiverParams(density, sourceMin, minLength, maxLength,
                new TributaryParams(0, branchChance, BRANCH_MIN, BRANCH_SPREAD),
                new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE, surface));
    }

    private static RiverParams plain(float density, int minLength, int maxLength) {
        return new RiverParams(density, SOURCE_MIN, minLength, maxLength,
                new TributaryParams(NO_BRANCH_BEFORE, NEVER, BRANCH_MIN, BRANCH_SPREAD),
                new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE,
                        TileType.SHALLOW_WATER));
    }

    private static void apply(GenerationContext context, RiverParams params) {
        new RiverPass(params).apply(context);
    }

    @Test
    void riversStartOnHighLandAndRunDownhill() {
        final var context = ramp(SEED);

        apply(context, plain(MANY_RIVERS, SHORT_MIN, SHORT_MAX));

        final var rivers = TestContexts.countGround(context, TileType.RIVER);
        assertTrue(rivers > 0, "some river was carved");
        var startsHigh = false;
        for (var index = 0; index < context.size(); index++) {
            if (context.groundAt(index) == TileType.RIVER) {
                startsHigh |= context.xOf(index) <= LAST_SOURCE_COLUMN;
            }
        }
        assertTrue(startsHigh, "a river covers a tile of the high ground it starts on");
    }

    @Test
    void aSingleRiverRunsAsManyStepsAsDrawnBetweenTheLengths() {
        final var context = ramp(SEED);

        apply(context, plain(ONE_RIVER, SHORT_MIN, SHORT_MAX));

        final var covered = TestContexts.countGround(context, TileType.RIVER);
        assertTrue(covered >= SHORT_MIN && covered < SHORT_MAX, "covered " + covered);
    }

    @Test
    void aRiverStopsWhereItMeetsWater() {
        final var context = ramp(SEED);
        for (var y = 0; y < HEIGHT; y++) {
            for (var x = WATER_COLUMN; x < WIDTH; x++) {
                context.setGround(context.index(x, y), TileType.DEEP_WATER);
            }
        }

        apply(context, plain(MANY_RIVERS, LONG_MIN, LONG_MAX));

        for (var index = 0; index < context.size(); index++) {
            if (context.xOf(index) >= WATER_COLUMN) {
                assertEquals(TileType.DEEP_WATER, context.groundAt(index));
            }
        }
        assertTrue(TestContexts.countGround(context, TileType.RIVER) > 0);
    }

    @Test
    void aRiverThatDeadEndsLowFloodsALakeAroundItsEnd() {
        final var context = ramp(SEED);

        apply(context, plain(ONE_RIVER, LONG_MIN, LONG_MAX));

        assertTrue(TestContexts.countGround(context, TileType.SHALLOW_WATER) >= FLOODED_ROWS);
        assertTrue(TestContexts.countGround(context, TileType.RIVER) > 0);
    }

    @Test
    void theSurfaceOfTheLakeIsTheOneTheRecipeNames() {
        final var context = ramp(SEED);

        apply(context, params(ONE_RIVER, LONG_MIN, LONG_MAX, NEVER, TileType.ICE));

        assertTrue(TestContexts.countGround(context, TileType.ICE) > 0);
        assertEquals(0, TestContexts.countGround(context, TileType.SHALLOW_WATER));
    }

    @Test
    void aRiverThatDeadEndsHighMakesNoLake() {
        final var context = ramp(SEED);
        final var heights = context.field(NoiseField.HEIGHT);
        for (var index = 0; index < heights.length; index++) {
            heights[index] += TOP;
        }

        apply(context, plain(ONE_RIVER, LONG_MIN, LONG_MAX));

        assertEquals(0, TestContexts.countGround(context, TileType.SHALLOW_WATER));
    }

    @Test
    void riversBranchWhenTheChanceIsCertain() {
        var withBranches = 0;
        var without = 0;
        for (var seed = 1L; seed <= BRANCH_SEEDS; seed++) {
            final var plainRiver = ramp(seed);
            final var branching = ramp(seed);

            // Branches only start where the ground is high, so the whole ramp counts as high
            apply(plainRiver, params(LOWEST, ONE_RIVER, SHORT_MAX, SHORT_MAX + 1, NEVER, TileType.SHALLOW_WATER));
            apply(branching, params(LOWEST, ONE_RIVER, SHORT_MAX, SHORT_MAX + 1, ALWAYS, TileType.SHALLOW_WATER));

            without += TestContexts.countGround(plainRiver, TileType.RIVER);
            withBranches += TestContexts.countGround(branching, TileType.RIVER);
        }

        assertTrue(withBranches > without, withBranches + " tiles with branches against " + without);
    }

    @Test
    void aRiverRunsOnPastTheStartsOfItsBranches() {
        var farthest = -1;
        for (var seed = 1L; seed <= BRANCH_SEEDS; seed++) {
            final var context = ramp(seed);

            // A branch starts at every step, so a river that stopped at the first one would end near its source
            apply(context, params(ONE_RIVER, LONG_MIN, LONG_MAX, ALWAYS, TileType.SHALLOW_WATER));

            farthest = Math.max(farthest, rightmostRiverColumn(context));
        }

        // Branches only reach a few tiles past their start, so only the river itself gets further than this
        assertTrue(farthest > LAST_SOURCE_COLUMN + BRANCH_MIN + BRANCH_SPREAD, "the farthest column is " + farthest);
    }

    @Test
    void aRiverWithASingleWayDownRunsOnPastABranchThatFindsNoOtherWay() {
        final var context = ramp(SEED);
        // One row of land is left, so the river has a single lower neighbour at every step, which a branch cannot take
        for (var y = 0; y < HEIGHT; y++) {
            for (var x = 0; x < WIDTH && y != LANE_ROW; x++) {
                context.setGround(context.index(x, y), TileType.DEEP_WATER);
                context.field(NoiseField.HEIGHT)[context.index(x, y)] = WALL;
            }
        }

        apply(context, params(ONE_RIVER, LONG_MIN, LONG_MAX, ALWAYS, TileType.SHALLOW_WATER));

        assertTrue(rightmostRiverColumn(context) > LAST_SOURCE_COLUMN + BRANCH_MIN + BRANCH_SPREAD,
                "the farthest column is " + rightmostRiverColumn(context));
    }

    @Test
    void aRiverNeverStepsOntoTheTilesOfItsOwnBranch() {
        final var context = TestContexts.of(MapType.RIVERS, SEED, FUNNEL_WIDTH, FUNNEL_HEIGHT, TestContexts.FLAT);
        final var heights = TestContexts.fillField(context, NoiseField.HEIGHT, WALL);
        TestContexts.fillGround(context, TileType.DEEP_WATER);
        land(context, heights, SOURCE_COLUMN, MIDDLE_ROW, TOP);
        land(context, heights, FORK_COLUMN - 1, MIDDLE_ROW, FUNNEL_SOURCE_MIN);
        land(context, heights, FORK_COLUMN, MIDDLE_ROW, MAIN_HEIGHT);
        land(context, heights, FORK_COLUMN, FIRST_ROW, BRANCH_HEIGHT);
        land(context, heights, FORK_COLUMN + 1, FIRST_ROW, JOIN_HEIGHT);
        land(context, heights, FORK_COLUMN + 2, FIRST_ROW, BEYOND_HEIGHT);
        // The branch covers three tiles, from the start of the fork to the join, and the river would reach the tile
        // beyond the join only by walking along the branch
        final var oneBranchOfThree = new TributaryParams(0, ALWAYS, BRANCH_MIN, 1);
        final var lakes = new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE,
                TileType.SHALLOW_WATER);

        apply(context, new RiverParams(ONE_RIVER, FUNNEL_SOURCE_MIN, LONG_MIN, LONG_MAX, oneBranchOfThree, lakes));

        assertEquals(TileType.GRASS, context.groundAt(context.index(FORK_COLUMN + 2, FIRST_ROW)));
        assertEquals(TileType.RIVER, context.groundAt(context.index(FORK_COLUMN + 1, FIRST_ROW)));
    }

    private static void land(GenerationContext context, float[] heights, int x, int y, float height) {
        context.setGround(context.index(x, y), TileType.GRASS);
        heights[context.index(x, y)] = height;
    }

    private static int rightmostRiverColumn(GenerationContext context) {
        var rightmost = -1;
        for (var index = 0; index < context.size(); index++) {
            if (context.groundAt(index) == TileType.RIVER) {
                rightmost = Math.max(rightmost, context.xOf(index));
            }
        }
        return rightmost;
    }

    @Test
    void withNoHighGroundThereAreNoRivers() {
        final var context = ramp(SEED);
        Arrays.fill(context.field(NoiseField.HEIGHT), FLAT);

        apply(context, plain(MANY_RIVERS, SHORT_MIN, SHORT_MAX));

        assertEquals(context.size(), TestContexts.countGround(context, TileType.GRASS));
    }

    @Test
    void theSameSeedCarvesTheSameRiversAndSeedsDiffer() {
        final var first = ramp(SEED);
        final var second = ramp(SEED);
        final var other = ramp(OTHER_SEED);

        apply(first, plain(MANY_RIVERS, SHORT_MIN, SHORT_MAX));
        apply(second, plain(MANY_RIVERS, SHORT_MIN, SHORT_MAX));
        apply(other, plain(MANY_RIVERS, SHORT_MIN, SHORT_MAX));

        assertArrayEquals(first.ground(), second.ground());
        assertNotEquals(Arrays.asList(first.ground()), Arrays.asList(other.ground()), "another seed gives other rivers");
    }

    @Test
    void theParametersRefuseNumbersThatCannotWork() {
        final var tributaries = new TributaryParams(0, NEVER, BRANCH_MIN, BRANCH_SPREAD);
        final var lakes = new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE,
                TileType.ICE);

        assertThrows(IllegalArgumentException.class,
                () -> new RiverParams(0F, SOURCE_MIN, SHORT_MIN, SHORT_MAX, tributaries, lakes));
        assertThrows(IllegalArgumentException.class,
                () -> new RiverParams(MANY_RIVERS, SOURCE_MIN, SHORT_MAX, SHORT_MAX, tributaries, lakes));
        assertThrows(IllegalArgumentException.class, () -> new TributaryParams(0, 2F, BRANCH_MIN, BRANCH_SPREAD));
        assertThrows(IllegalArgumentException.class, () -> new TributaryParams(0, NEVER, 0, BRANCH_SPREAD));
        assertThrows(IllegalArgumentException.class,
                () -> new LakeParams(LAKE_MAX_HEIGHT, LAKE_MAX_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE,
                        TileType.ICE));
        assertThrows(NullPointerException.class,
                () -> new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE,
                        TileType.DEEP_WATER));
        assertThrows(IllegalArgumentException.class,
                () -> new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE,
                        TileType.RIVER));
        assertThrows(IllegalArgumentException.class,
                () -> new LakeParams(LAKE_MAX_HEIGHT, LAKE_MIN_RADIUS, LAKE_MAX_RADIUS, LAKE_TOLERANCE,
                        TileType.GRASS));
        assertThrows(NullPointerException.class, () -> new RiverPass(null));
    }
}
