package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Arrays;
import java.util.Objects;

/**
 * A* over the grid on a precomputed cost per tile. Moves go in eight directions: stepping onto a tile costs the cost of
 * that tile, times the square root of two on a diagonal, and a diagonal step is refused when either of the two
 * orthogonal tiles it cuts across is blocked, so a path never squeezes between two blocked corners. A cost of
 * {@link Float#POSITIVE_INFINITY} blocks a tile. The heuristic is the octile distance times the lowest cost of any
 * tile, so it never overestimates, and the path is the cheapest one, only while every open tile costs at least that
 * lowest cost. The constructor checks it and refuses a cost array that breaks it, or that holds a NaN. A heuristic
 * weight above 1 trades that guarantee for speed: the heuristic is multiplied by it, so the search expands far fewer
 * tiles and finds a path that costs at most the weight times the cheapest one. Each tile is
 * expanded at most once per search, so the rounding of the heap's float keys cannot send a search back to a tile it
 * has settled.
 * <p>
 * One instance serves many searches over the cost array it was built with. The instance keeps its own copy of that
 * array, so a caller that goes on writing to the array it passed does not change the searches; a caller that wants
 * later searches to see a new cost, such as a penalty around a path it just carved, says so with
 * {@link #setCost(int, float)}. The arrays that a search fills are never cleared: each carries a run number, and a
 * tile whose number differs from the current run counts as untouched.
 */
final class LeastCostPath {

    /** What {@link #find(int, int, int[])} returns when it writes no path. */
    public static final int NOT_FOUND = -1;

    private static final int NO_PARENT = -1;
    private static final double DIAGONAL = Math.sqrt(2.0);
    private static final int[] STEP_X = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] STEP_Y = {-1, -1, -1, 0, 0, 1, 1, 1};

    private final int width;
    private final int height;
    private final float[] cost;
    private final double minCost;
    private final double heuristicWeight;
    private final double[] distance;
    private final int[] parent;
    private final int[] stamp;
    private final int[] closed;
    private final IntMinHeap heap = new IntMinHeap();
    private int run;
    private int goalX;
    private int goalY;

    /**
     * Creates a searcher.
     *
     * @param width   the number of columns
     * @param height  the number of rows
     * @param cost    the cost of entering each tile, copied, with {@link Float#POSITIVE_INFINITY} for a blocked tile
     *                and at least {@code minCost} for every other tile
     * @param minCost the lowest cost of any open tile, above 0, which scales the heuristic
     * @throws IllegalArgumentException when the size does not match the cost array, the lowest cost is not positive and
     *                                  finite, or a cost is NaN or below the lowest cost
     */
    LeastCostPath(int width, int height, float[] cost, float minCost) {
        this(width, height, cost, minCost, 1F);
    }

    /**
     * Creates a searcher whose path may cost up to a factor more than the cheapest one, in exchange for a faster
     * search.
     *
     * @param width           the number of columns
     * @param height          the number of rows
     * @param cost            the cost of entering each tile, copied, with {@link Float#POSITIVE_INFINITY} for a blocked
     *                        tile and at least {@code minCost} for every other tile
     * @param minCost         the lowest cost of any open tile, above 0, which scales the heuristic
     * @param heuristicWeight how much the heuristic is multiplied by, from 1 for the cheapest path up; the path found
     *                        costs at most this many times the cheapest one
     * @throws IllegalArgumentException when the size does not match the cost array, the lowest cost is not positive and
     *                                  finite, a cost is NaN or below the lowest cost, or the weight is below 1 or not
     *                                  finite
     */
    LeastCostPath(int width, int height, float[] cost, float minCost, float heuristicWeight) {
        Objects.requireNonNull(cost, "cost");
        if (!Float.isFinite(heuristicWeight) || heuristicWeight < 1F) {
            throw new IllegalArgumentException("the heuristic weight must be finite and at least 1: "
                    + heuristicWeight);
        }
        if (width <= 0 || height <= 0 || cost.length != width * height) {
            throw new IllegalArgumentException(
                    "the cost array has " + cost.length + " tiles, not " + width + "x" + height);
        }
        if (!Float.isFinite(minCost) || minCost <= 0F) {
            throw new IllegalArgumentException("the lowest cost must be positive and finite: " + minCost);
        }
        this.width = width;
        this.height = height;
        this.cost = copyChecked(cost, minCost);
        this.minCost = minCost;
        this.heuristicWeight = heuristicWeight;
        this.distance = new double[cost.length];
        this.parent = new int[cost.length];
        this.stamp = new int[cost.length];
        this.closed = new int[cost.length];
    }

    /** Copies the costs, refusing a cost that is NaN or lower than the lowest cost, which would break the heuristic. */
    private static float[] copyChecked(float[] cost, float minCost) {
        final var copy = new float[cost.length];
        for (var index = 0; index < cost.length; index++) {
            if (Float.isNaN(cost[index]) || cost[index] < minCost) {
                throw new IllegalArgumentException("the cost " + cost[index] + " of tile " + index
                        + " is NaN or below the lowest cost " + minCost);
            }
            copy[index] = cost[index];
        }
        return copy;
    }

    /**
     * Finds the cheapest path between two tiles and writes it to {@code out}, start first and goal last.
     *
     * @param start the first tile
     * @param goal  the last tile
     * @param out   receives the tiles of the path, so it must be long enough for it
     * @return the number of tiles written, 1 when the start is the goal, or {@link #NOT_FOUND}, with {@code out}
     * untouched, when either end is blocked or no route exists
     * @throws IndexOutOfBoundsException when a tile is outside the grid
     * @throws IllegalArgumentException  when the path does not fit in {@code out}
     */
    int find(int start, int goal, int[] out) {
        Objects.checkIndex(start, cost.length);
        Objects.checkIndex(goal, cost.length);
        if (isBlocked(start) || isBlocked(goal)) {
            return NOT_FOUND;
        }
        beginRun();
        goalY = goal / width;
        goalX = goal - goalY * width;
        touch(start, 0.0, NO_PARENT);
        while (!heap.isEmpty()) {
            final var node = heap.pop();
            if (closed[node] == run) {
                continue;
            }
            if (node == goal) {
                return unwind(start, goal, out);
            }
            closed[node] = run;
            expand(node);
        }
        return NOT_FOUND;
    }

    /**
     * Changes the cost of entering a tile for the searches that follow. The new cost obeys the rules of the
     * constructor, so the heuristic stays valid: it is not NaN and not below the lowest cost, and it may be
     * {@link Float#POSITIVE_INFINITY} to block the tile.
     *
     * @param index the tile
     * @param value the new cost
     * @throws IndexOutOfBoundsException when the tile is outside the grid
     * @throws IllegalArgumentException  when the cost is NaN or below the lowest cost
     */
    void setCost(int index, float value) {
        Objects.checkIndex(index, cost.length);
        if (Float.isNaN(value) || value < minCost) {
            throw new IllegalArgumentException(
                    "the cost " + value + " of tile " + index + " is NaN or below the lowest cost " + minCost);
        }
        cost[index] = value;
    }

    private boolean isBlocked(int index) {
        return cost[index] >= Float.POSITIVE_INFINITY;
    }

    /** The priority of a tile: the distance reached so far plus the heuristic, rounded to the heap's float key. */
    private float keyOf(int index) {
        return (float) (distance[index] + heuristic(index));
    }

    /** Starts a search: empties the heap and moves to a new run number, clearing the stamps only when it wraps. */
    private void beginRun() {
        heap.clear();
        run++;
        if (run == 0) {
            Arrays.fill(stamp, 0);
            Arrays.fill(closed, 0);
            run = 1;
        }
    }

    /** The octile distance between a tile and the goal of the running search, scaled by the lowest cost. */
    private double heuristic(int index) {
        final var y = index / width;
        final var dx = Math.abs(index - y * width - goalX);
        final var dy = Math.abs(y - goalY);
        final var straight = Math.max(dx, dy) - Math.min(dx, dy);
        return heuristicWeight * minCost * (straight + DIAGONAL * Math.min(dx, dy));
    }

    /** Records a better way to reach a tile and queues it. */
    private void touch(int index, double reached, int from) {
        distance[index] = reached;
        parent[index] = from;
        stamp[index] = run;
        heap.push(index, keyOf(index));
    }

    /** Relaxes the eight neighbours of a tile. */
    private void expand(int node) {
        final var y = node / width;
        final var x = node - y * width;
        for (var direction = 0; direction < STEP_X.length; direction++) {
            final var nx = x + STEP_X[direction];
            final var ny = y + STEP_Y[direction];
            if (isOnGrid(nx, ny) && canStep(x, y, nx, ny)) {
                relax(node, ny * width + nx, STEP_X[direction] != 0 && STEP_Y[direction] != 0);
            }
        }
    }

    private boolean isOnGrid(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /** Tells whether a move is open: the target is open and a diagonal does not cut a blocked corner. */
    private boolean canStep(int x, int y, int nx, int ny) {
        if (isBlocked(ny * width + nx)) {
            return false;
        }
        final var diagonal = nx != x && ny != y;
        return !diagonal || !(isBlocked(y * width + nx) || isBlocked(ny * width + x));
    }

    /** Queues a neighbour when this way to it is cheaper than the one known. */
    private void relax(int node, int neighbour, boolean diagonal) {
        final var step = diagonal ? cost[neighbour] * DIAGONAL : cost[neighbour];
        final var reached = distance[node] + step;
        if (closed[neighbour] != run && (stamp[neighbour] != run || reached < distance[neighbour])) {
            touch(neighbour, reached, node);
        }
    }

    /** Writes the path from the start to the goal by following the parents back from the goal. */
    private int unwind(int start, int goal, int[] out) {
        var length = 1;
        for (var node = goal; node != start; node = parent[node]) {
            length++;
        }
        if (out.length < length) {
            throw new IllegalArgumentException("the path has " + length + " tiles and does not fit in " + out.length);
        }
        var node = goal;
        for (var position = length - 1; position >= 0; position--) {
            out[position] = node;
            node = parent[node];
        }
        return length;
    }
}
