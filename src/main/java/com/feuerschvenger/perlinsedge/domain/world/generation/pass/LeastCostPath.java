package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Arrays;
import java.util.Objects;

/**
 * A* over the grid on a precomputed cost per tile. Moves go in eight directions: stepping onto a tile costs the cost of
 * that tile, times the square root of two on a diagonal, and a diagonal step is refused when either of the two
 * orthogonal tiles it cuts across is blocked, so a path never squeezes between two blocked corners. A cost of
 * {@link Float#POSITIVE_INFINITY} blocks a tile. The heuristic is the octile distance times the lowest cost of any
 * tile, so the path is the cheapest one when no tile costs less than that lowest cost.
 * <p>
 * One instance serves many searches over the same cost array, which the caller may change between them. The arrays that
 * a search fills are never cleared: each carries a run number, and a tile whose number differs from the current run
 * counts as untouched. A search expands at most as many tiles as the grid holds, which bounds the worst case when the
 * heuristic is not admissible.
 */
final class LeastCostPath {

    /** What {@link #find(int, int, int[])} returns when no path exists. */
    static final int NOT_FOUND = -1;

    private static final int NO_PARENT = -1;
    private static final float DIAGONAL = (float) Math.sqrt(2.0);
    private static final int[] STEP_X = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] STEP_Y = {-1, -1, -1, 0, 0, 1, 1, 1};

    private final int width;
    private final int height;
    private final float[] cost;
    private final float minCost;
    private final float[] distance;
    private final int[] parent;
    private final int[] stamp;
    private final IntMinHeap heap = new IntMinHeap();
    private int run;
    private int goalX;
    private int goalY;

    /**
     * Creates a searcher.
     *
     * @param width   the number of columns
     * @param height  the number of rows
     * @param cost    the cost of entering each tile, kept by reference, with {@link Float#POSITIVE_INFINITY} for a
     *                blocked tile
     * @param minCost the lowest cost of any open tile, above 0, which scales the heuristic
     * @throws IllegalArgumentException when the size does not match the cost array or the lowest cost is not positive
     */
    LeastCostPath(int width, int height, float[] cost, float minCost) {
        Objects.requireNonNull(cost, "cost");
        if (width <= 0 || height <= 0 || cost.length != width * height) {
            throw new IllegalArgumentException(
                    "the cost array has " + cost.length + " tiles, not " + width + "x" + height);
        }
        if (!(minCost > 0F) || Float.isInfinite(minCost)) {
            throw new IllegalArgumentException("the lowest cost must be positive and finite: " + minCost);
        }
        this.width = width;
        this.height = height;
        this.cost = cost;
        this.minCost = minCost;
        this.distance = new float[cost.length];
        this.parent = new int[cost.length];
        this.stamp = new int[cost.length];
    }

    /**
     * Finds the cheapest path between two tiles and writes it to {@code out}, start first and goal last.
     *
     * @param start the first tile
     * @param goal  the last tile
     * @param out   receives the tiles of the path, so it must be long enough for it
     * @return the number of tiles written, 1 when the start is the goal, or {@link #NOT_FOUND} when either end is
     * blocked or no route exists
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
        touch(start, 0F, NO_PARENT);
        var expansions = 0;
        while (!heap.isEmpty() && expansions < cost.length) {
            final var key = heap.peekKey();
            final var node = heap.pop();
            if (key > distance[node] + heuristic(node)) {
                continue;
            }
            if (node == goal) {
                return unwind(start, goal, out);
            }
            expansions++;
            expand(node);
        }
        return NOT_FOUND;
    }

    private boolean isBlocked(int index) {
        return cost[index] == Float.POSITIVE_INFINITY;
    }

    /** Starts a search: empties the heap and moves to a new run number, clearing the stamps only when it wraps. */
    private void beginRun() {
        heap.clear();
        run++;
        if (run == 0) {
            Arrays.fill(stamp, 0);
            run = 1;
        }
    }

    /** The octile distance between a tile and the goal of the running search, scaled by the lowest cost. */
    private float heuristic(int index) {
        final var y = index / width;
        final var dx = Math.abs(index - y * width - goalX);
        final var dy = Math.abs(y - goalY);
        final var straight = Math.max(dx, dy) - Math.min(dx, dy);
        return minCost * (straight + DIAGONAL * Math.min(dx, dy));
    }

    /** Records a better way to reach a tile and queues it. */
    private void touch(int index, float reached, int from) {
        distance[index] = reached;
        parent[index] = from;
        stamp[index] = run;
        heap.push(index, reached + heuristic(index));
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
        if (stamp[neighbour] != run || reached < distance[neighbour]) {
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
