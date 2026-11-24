package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeastCostPathTest {

    private static final char WALL = '#';
    private static final double DIAGONAL = Math.sqrt(2.0);
    private static final double TOLERANCE = 1e-3;
    private static final int RANDOM_SIDE = 20;
    private static final int QUERIES = 40;
    private static final long SEED = 20_261_124L;
    private static final int WALL_PERCENT = 15;
    private static final int PERCENT = 100;
    private static final float[] RANDOM_COSTS = {1F, 2F, 3F, 5F};
    private static final int BIG_SIDE = 300;
    private static final int BIG_COSTS = 4;
    private static final int SHORT_BUFFER = 2;
    private static final float CHEAPER_THAN_LOWEST = 0.5F;

    /** Reads a picture: {@code .} costs 1, {@code #} is blocked and a digit is its own cost. */
    private static float[] costs(String... rows) {
        final var cells = String.join("", rows);
        final var result = new float[cells.length()];
        for (var index = 0; index < result.length; index++) {
            final var cell = cells.charAt(index);
            if (cell == WALL) {
                result[index] = Float.POSITIVE_INFINITY;
            } else {
                result[index] = cell == '.' ? 1F : cell - '0';
            }
        }
        return result;
    }

    private static boolean isBlocked(float[] cost, int index) {
        return cost[index] == Float.POSITIVE_INFINITY;
    }

    /** Sums the cost of a path and checks every step is a legal move. */
    private static double costOf(float[] cost, int width, int[] path, int length) {
        var total = 0.0;
        for (var position = 1; position < length; position++) {
            final var from = path[position - 1];
            final var to = path[position];
            final var dx = to % width - from % width;
            final var dy = to / width - from / width;
            assertTrue(Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && from != to, "step " + from + " to " + to);
            assertFalse(isBlocked(cost, to), "path enters a blocked tile " + to);
            if (dx != 0 && dy != 0) {
                assertFalse(isBlocked(cost, from + dx) || isBlocked(cost, from + dy * width), "cuts a corner");
                total += cost[to] * DIAGONAL;
            } else {
                total += cost[to];
            }
        }
        return total;
    }

    /** The reference: Dijkstra over the same moves with a linear scan for the nearest tile. */
    private static double reference(float[] cost, int width, int height, int start, int goal) {
        if (isBlocked(cost, start) || isBlocked(cost, goal)) {
            return Double.POSITIVE_INFINITY;
        }
        final var best = new double[cost.length];
        final var done = new boolean[cost.length];
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        best[start] = 0;
        for (var round = 0; round < cost.length; round++) {
            final var node = nearestOpen(best, done);
            if (best[node] == Double.POSITIVE_INFINITY) {
                break;
            }
            done[node] = true;
            relaxAround(cost, width, height, best, node);
        }
        return best[goal];
    }

    private static int nearestOpen(double[] best, boolean[] done) {
        var node = -1;
        for (var index = 0; index < best.length; index++) {
            if (!done[index] && (node < 0 || best[index] < best[node])) {
                node = index;
            }
        }
        return node;
    }

    private static void relaxAround(float[] cost, int width, int height, double[] best, int node) {
        for (var dy = -1; dy <= 1; dy++) {
            for (var dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && isOnGrid(width, height, node % width + dx, node / width + dy)) {
                    relaxMove(cost, width, best, node, dx, dy);
                }
            }
        }
    }

    private static boolean isOnGrid(int width, int height, int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private static void relaxMove(float[] cost, int width, double[] best, int node, int dx, int dy) {
        final var target = node + dy * width + dx;
        final var diagonal = dx != 0 && dy != 0;
        if (isBlocked(cost, target) || diagonal && (isBlocked(cost, node + dx) || isBlocked(cost, node + dy * width))) {
            return;
        }
        final var step = diagonal ? cost[target] * DIAGONAL : cost[target];
        best[target] = Math.min(best[target], best[node] + step);
    }

    private static float[] randomCosts(SplittableRandom random, int size) {
        final var result = new float[size];
        for (var index = 0; index < size; index++) {
            result[index] = random.nextInt(PERCENT) < WALL_PERCENT
                    ? Float.POSITIVE_INFINITY
                    : RANDOM_COSTS[random.nextInt(RANDOM_COSTS.length)];
        }
        return result;
    }

    @Test
    void aStraightCorridorGivesTheStraightPath() {
        final var cost = costs(".....");
        final var path = new int[cost.length];

        final var length = new LeastCostPath(cost.length, 1, cost, 1F).find(0, cost.length - 1, path);

        assertEquals(cost.length, length);
        assertArrayEquals(IntStream.range(0, cost.length).toArray(), path);
    }

    @Test
    void aSearchFromATileToItselfIsOneTile() {
        final var cost = costs("...", "...");
        final var path = new int[cost.length];

        assertEquals(1, new LeastCostPath(3, 2, cost, 1F).find(4, 4, path));
        assertEquals(4, path[0]);
    }

    @Test
    void anOpenGridIsCrossedOnTheDiagonal() {
        final var cost = costs("....", "....", "....", "....");
        final var path = new int[cost.length];

        final var length = new LeastCostPath(4, 4, cost, 1F).find(0, 15, path);

        assertEquals(4, length);
        assertArrayEquals(new int[] {0, 5, 10, 15}, Arrays.copyOf(path, length));
    }

    @Test
    void aWallIsWalkedAround() {
        final var cost = costs(
                "..#..",
                "..#..",
                "..#..",
                ".....");
        final var path = new int[cost.length];

        final var length = new LeastCostPath(5, 4, cost, 1F).find(0, 4, path);

        assertTrue(length > 0);
        assertEquals(0, path[0]);
        assertEquals(4, path[length - 1]);
        costOf(cost, 5, path, length);
    }

    @Test
    void aWallAcrossTheWholeMapMeansNoPath() {
        final var cost = costs(
                "..#..",
                "..#..",
                "..#..");
        final var path = new int[cost.length];

        assertEquals(LeastCostPath.NOT_FOUND, new LeastCostPath(5, 3, cost, 1F).find(0, 4, path));
    }

    @Test
    void aBlockedStartOrGoalMeansNoPath() {
        final var cost = costs(".#.");
        final var path = new int[cost.length];
        final var search = new LeastCostPath(3, 1, cost, 1F);

        assertEquals(LeastCostPath.NOT_FOUND, search.find(1, 2, path));
        assertEquals(LeastCostPath.NOT_FOUND, search.find(0, 1, path));
    }

    @Test
    void anExpensiveStripIsAvoidedWhenACheaperDetourExists() {
        final var cost = costs(
                ".9.",
                ".9.",
                ".9.",
                "...");
        final var path = new int[cost.length];

        final var length = new LeastCostPath(3, 4, cost, 1F).find(0, 2, path);

        for (var position = 0; position < length; position++) {
            assertEquals(1F, cost[path[position]], "tile " + path[position]);
        }
    }

    @Test
    void aDiagonalBetweenTwoBlockedCornersIsRefused() {
        final var cost = costs(
                ".#",
                "#.");
        final var path = new int[cost.length];

        assertEquals(LeastCostPath.NOT_FOUND, new LeastCostPath(2, 2, cost, 1F).find(0, 3, path));
    }

    @Test
    void aDiagonalBesideOneBlockedCornerIsRefusedToo() {
        final var cost = costs(
                "..",
                "#.");
        final var path = new int[cost.length];

        final var length = new LeastCostPath(2, 2, cost, 1F).find(0, 3, path);

        assertEquals(3, length);
        assertArrayEquals(new int[] {0, 1, 3}, Arrays.copyOf(path, length));
    }

    @Test
    void theResultMatchesADijkstraReferenceOnRandomGrids() {
        final var random = new SplittableRandom(SEED);
        final var size = RANDOM_SIDE * RANDOM_SIDE;
        final var cost = randomCosts(random, size);
        final var search = new LeastCostPath(RANDOM_SIDE, RANDOM_SIDE, cost, 1F);
        final var path = new int[size];

        for (var query = 0; query < QUERIES; query++) {
            final var start = random.nextInt(size);
            final var goal = random.nextInt(size);

            final var length = search.find(start, goal, path);

            final var expected = reference(cost, RANDOM_SIDE, RANDOM_SIDE, start, goal);
            if (expected == Double.POSITIVE_INFINITY) {
                assertEquals(LeastCostPath.NOT_FOUND, length, "query " + query);
            } else {
                assertEquals(start, path[0]);
                assertEquals(goal, path[length - 1]);
                assertEquals(expected, costOf(cost, RANDOM_SIDE, path, length), TOLERANCE, "query " + query);
            }
        }
    }

    @Test
    void oneInstanceGivesTheSamePathAsAFreshOneAfterManySearches() {
        final var random = new SplittableRandom(SEED);
        final var size = RANDOM_SIDE * RANDOM_SIDE;
        final var cost = randomCosts(random, size);
        final var reused = new LeastCostPath(RANDOM_SIDE, RANDOM_SIDE, cost, 1F);
        final var reusedPath = new int[size];
        final var freshPath = new int[size];

        for (var query = 0; query < QUERIES; query++) {
            final var start = random.nextInt(size);
            final var goal = random.nextInt(size);

            final var reusedLength = reused.find(start, goal, reusedPath);
            final var freshLength = new LeastCostPath(RANDOM_SIDE, RANDOM_SIDE, cost, 1F).find(start, goal, freshPath);

            assertEquals(freshLength, reusedLength, "query " + query);
            if (freshLength > 0) {
                assertArrayEquals(Arrays.copyOf(freshPath, freshLength), Arrays.copyOf(reusedPath, reusedLength));
            }
        }
    }

    @Test
    void theSearcherKeepsItsOwnCopyOfTheCostArray() {
        final var cost = costs("...", "...", "...");
        final var path = new int[cost.length];
        final var search = new LeastCostPath(3, 3, cost, 1F);
        final var before = search.find(0, 2, path);
        final var straight = Arrays.copyOf(path, before);

        cost[1] = Float.POSITIVE_INFINITY;
        final var after = search.find(0, 2, path);

        assertArrayEquals(IntStream.range(0, before).toArray(), straight);
        assertEquals(before, after);
        assertArrayEquals(straight, Arrays.copyOf(path, after));
    }

    @Test
    void aNewSearcherSeesTheChangedCosts() {
        final var cost = costs("...", "...", "...");
        final var path = new int[cost.length];
        cost[1] = Float.POSITIVE_INFINITY;

        final var length = new LeastCostPath(3, 3, cost, 1F).find(0, 2, path);

        assertTrue(length > 0);
        assertNotEquals(1, path[1]);
    }

    @Test
    void aLongSearchOnALargeMapReachesTheOppositeCorner() {
        final var random = new SplittableRandom(SEED);
        final var cost = new float[BIG_SIDE * BIG_SIDE];
        for (var index = 0; index < cost.length; index++) {
            cost[index] = 1F + random.nextInt(BIG_COSTS);
        }
        final var path = new int[cost.length];

        final var length = new LeastCostPath(BIG_SIDE, BIG_SIDE, cost, 1F).find(0, cost.length - 1, path);

        assertTrue(length >= BIG_SIDE && length < BIG_SIDE * 2, "length " + length);
        assertEquals(cost.length - 1, path[length - 1]);
    }

    @Test
    void aBufferTooSmallForThePathIsRefused() {
        final var cost = costs(".....");
        final var search = new LeastCostPath(5, 1, cost, 1F);
        final var small = new int[SHORT_BUFFER];

        assertThrows(IllegalArgumentException.class, () -> search.find(0, 4, small));
    }

    @Test
    void tilesOutsideTheGridAreRefused() {
        final var cost = costs("...");
        final var search = new LeastCostPath(3, 1, cost, 1F);
        final var path = new int[cost.length];

        assertThrows(IndexOutOfBoundsException.class, () -> search.find(-1, 0, path));
        assertThrows(IndexOutOfBoundsException.class, () -> search.find(0, 3, path));
    }

    @Test
    void theConstructorRefusesInconsistentArguments() {
        final var cost = costs("...");

        assertThrows(NullPointerException.class, () -> new LeastCostPath(3, 1, null, 1F));
        assertThrows(IllegalArgumentException.class, () -> new LeastCostPath(2, 1, cost, 1F));
        assertThrows(IllegalArgumentException.class, () -> new LeastCostPath(0, 1, cost, 1F));
        assertThrows(IllegalArgumentException.class, () -> new LeastCostPath(3, 1, cost, 0F));
        assertThrows(IllegalArgumentException.class, () -> new LeastCostPath(3, 1, cost, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new LeastCostPath(3, 1, cost, Float.POSITIVE_INFINITY));
    }

    @Test
    void aCostThatIsNaNOrBelowTheLowestCostIsRefused() {
        final var cheap = costs("...");
        cheap[1] = CHEAPER_THAN_LOWEST;
        final var notANumber = costs("...");
        notANumber[1] = Float.NaN;

        assertThrows(IllegalArgumentException.class, () -> new LeastCostPath(3, 1, cheap, 1F));
        assertThrows(IllegalArgumentException.class, () -> new LeastCostPath(3, 1, notANumber, 1F));
    }
}
