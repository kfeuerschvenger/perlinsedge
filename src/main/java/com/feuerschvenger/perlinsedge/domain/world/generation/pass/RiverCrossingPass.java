package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.random.RandomGenerator;

/**
 * Cuts a few long rivers that cross the whole map from one edge to the opposite one. Every river is the cheapest
 * path over the land between a land tile near the edge it enters and one near the opposite edge, where a tile costs
 * more the higher it lies relative to the rest of the land and the drier it is, so the river follows the valleys and
 * bends towards the wet ground. The rivers are carved two tiles wide and have no ford: the player crosses them on
 * bridges.
 * <p>
 * Three things keep the rivers apart and the land in one large piece. They all run the same way, across the map from
 * top to bottom or from left to right as the seed decides, in lanes along the two sides of the map, so two rivers do
 * not cross; the land around a carved river costs more, so the next river does not run in its bed and rivers stay
 * separate; and the middle of the map across the rivers costs more too, so the largest strip of land, where the player
 * starts, stays wide. Afterwards each river receives a few short tributaries that run from high land far from every
 * river to the nearest river tile.
 * <p>
 * The pass runs on plain arrays and one {@link LeastCostPath} that it reuses for every search. It draws from one seeded
 * stream and does nothing on a map with no land, so it repeats for a seed.
 */
public final class RiverCrossingPass implements GenerationPass {

    private static final String NAME = "river-crossing";
    private static final long SALT = 0x0C20_55E2_5EED_0001L;
    /** The area, in tiles, that {@code riversPer100k} counts rivers in. */
    private static final double DENSITY_AREA_TILES = 100_000.0;
    /** What a tile of flat, low, wet land costs, and the lowest cost of any tile, which scales the heuristic. */
    private static final float BASE_COST = 1F;
    /** The land within this many tiles of a carved river costs more, so the next river keeps its distance. */
    private static final int BAND_RADIUS = 3;
    private static final float BAND_PENALTY = 6F;
    /** The middle of the map across the rivers costs more, so the strip of land the player starts on stays wide. */
    private static final double CORRIDOR_START = 0.30;
    private static final double CORRIDOR_END = 0.70;
    private static final float CORRIDOR_PENALTY = 3F;
    /** The lanes along the two sides of the map, as shares of the span across the rivers. */
    private static final double FIRST_LANE_START = 0.05;
    private static final double FIRST_LANE_END = 0.30;
    private static final double SECOND_LANE_START = 0.70;
    private static final double SECOND_LANE_END = 0.95;
    private static final int LANE_SIDES = 2;
    /** How far a river moves from the middle of its slot in a lane, as a share of the slot. */
    private static final double SLOT_JITTER = 0.3;
    private static final double HALF = 0.5;
    /** A river enters the land at the nearest land tile within this many rows of the edge. */
    private static final int EDGE_DEPTH = 10;
    /** A river enters the land at most this share of the edge away from its slot. */
    private static final double EDGE_SPAN_SHARE = 0.12;
    /** Tributaries start in the highest share of the land that lies far from every river. */
    private static final double TRIBUTARY_SOURCE_SHARE = 0.05;
    private static final int TRIBUTARY_MIN_REACH = 12;
    private static final int NO_TILE = -1;

    private final RiverCrossingParams params;

    /**
     * Creates the pass.
     *
     * @param params how many rivers there are and how they choose their way
     */
    public RiverCrossingPass(RiverCrossingParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void apply(GenerationContext context) {
        new Crossing(context).run();
    }

    /**
     * Returns how many rivers a map of a number of tiles gets: the density times the area, rounded, between the
     * minimum and the maximum of the parameters.
     *
     * @param tiles the number of tiles of the map
     * @return the number of rivers
     */
    int riverCount(int tiles) {
        final var byArea = (int) Math.round((double) tiles * params.riversPer100k() / DENSITY_AREA_TILES);
        return Math.clamp(byArea, params.minRivers(), params.maxRivers());
    }

    /** The state of one run: the costs, the searcher, the rivers carved so far and the buffers. */
    private final class Crossing {

        private final GenerationContext context;
        private final int width;
        private final int height;
        private final float[] heights;
        private final float[] cost;
        private final RandomGenerator random;
        private final int[] path;
        private final int[] riverTiles;
        private final boolean[] inRiver;
        private final boolean[] banded;
        private final boolean vertical;
        private LeastCostPath search;
        private int riverTileCount;

        /** Prepares the arrays of a run and draws the direction the rivers run in. */
        Crossing(GenerationContext context) {
            this.context = context;
            this.width = context.width();
            this.height = context.height();
            this.heights = context.field(NoiseField.HEIGHT);
            this.cost = new float[context.size()];
            this.random = context.random(SALT);
            this.path = new int[context.size()];
            this.riverTiles = new int[context.size()];
            this.inRiver = new boolean[context.size()];
            this.banded = new boolean[context.size()];
            this.vertical = random.nextBoolean();
        }

        /** Fills the costs, cuts the rivers one by one and then adds the tributaries. */
        void run() {
            if (!buildCosts()) {
                return;
            }
            search = new LeastCostPath(width, height, cost, BASE_COST);
            final var rivers = riverCount(context.size());
            var crossed = 0;
            for (var river = 0; river < rivers; river++) {
                crossed += cross(river, rivers) ? 1 : 0;
            }
            addTributaries(crossed);
        }

        /** Fills the cost of every tile and tells whether the map has any land to run over. */
        private boolean buildCosts() {
            final var moisture = context.hasField(NoiseField.MOISTURE)
                    ? context.field(NoiseField.MOISTURE) : new float[cost.length];
            var lowest = Float.POSITIVE_INFINITY;
            var highest = Float.NEGATIVE_INFINITY;
            for (var index = 0; index < cost.length; index++) {
                if (context.groundAt(index).isLand()) {
                    lowest = Math.min(lowest, heights[index]);
                    highest = Math.max(highest, heights[index]);
                }
            }
            if (lowest > highest) {
                return false;
            }
            final var range = Math.max(Float.MIN_NORMAL, (double) highest - lowest);
            for (var index = 0; index < cost.length; index++) {
                cost[index] = context.groundAt(index).isLand()
                        ? landCost(index, ((double) heights[index] - lowest) / range, moisture[index])
                        : Float.POSITIVE_INFINITY;
            }
            return true;
        }

        /** The cost of a land tile from its height as a share of the land's range, its moisture and its place. */
        private float landCost(int index, double relativeHeight, float moisture) {
            final var wet = Math.clamp((double) moisture, 0.0, 1.0);
            return (float) (BASE_COST + (double) params.heightWeight() * relativeHeight
                    + (double) params.meanderWeight() * (1.0 - wet) + corridorPenalty(index));
        }

        /** The extra cost of a tile in the middle strip across the rivers, 0 outside it. */
        private double corridorPenalty(int index) {
            final var across = vertical ? index % width : index / width;
            final var span = vertical ? width : height;
            final var share = across / (double) Math.max(1, span - 1);
            return share >= CORRIDOR_START && share <= CORRIDOR_END ? CORRIDOR_PENALTY : 0.0;
        }

        /** Cuts one river between its two edges and tells whether it found a way. */
        private boolean cross(int river, int rivers) {
            final var span = vertical ? width : height;
            final var entryAlong = (int) Math.round(lanePosition(river, rivers) * (span - 1));
            final var exitAlong = (int) Math.round(lanePosition(river, rivers) * (span - 1));
            final var start = edgeLand(entryAlong, false);
            final var goal = edgeLand(exitAlong, true);
            if (start == NO_TILE || goal == NO_TILE) {
                return false;
            }
            final var length = search.find(start, goal, path);
            if (length == LeastCostPath.NOT_FOUND) {
                return false;
            }
            final var before = riverTileCount;
            Carve.path(width, height, path, length, Carve.MAX_STROKE, this::carve);
            for (var position = before; position < riverTileCount; position++) {
                penaliseAround(riverTiles[position]);
            }
            return true;
        }

        /**
         * Returns where along its edge a river enters or leaves, as a share of the span: the rivers are dealt to the
         * two lanes in turn, each river has a slot in its lane, and its place is the middle of the slot moved a little.
         */
        private double lanePosition(int river, int rivers) {
            final var inFirstLane = river % LANE_SIDES == 0;
            final var slots = inFirstLane ? (rivers + 1) / LANE_SIDES : rivers / LANE_SIDES;
            final var start = inFirstLane ? FIRST_LANE_START : SECOND_LANE_START;
            final var end = inFirstLane ? FIRST_LANE_END : SECOND_LANE_END;
            final var slotWidth = (end - start) / slots;
            final var middle = start + slotWidth * (river / LANE_SIDES + HALF);
            return middle + (random.nextDouble() - HALF) * SLOT_JITTER * slotWidth;
        }

        /** Returns the land tile nearest to a point of an edge, looked for in the first rows beside it. */
        private int edgeLand(int along, boolean far) {
            final var ideal = tileAt(along, 0, far);
            var best = NO_TILE;
            var bestDistance = Long.MAX_VALUE;
            for (var depth = 0; depth < EDGE_DEPTH; depth++) {
                final var candidate = nearestLandInRow(along, depth, far);
                if (candidate != NO_TILE && squaredDistance(candidate, ideal) < bestDistance) {
                    best = candidate;
                    bestDistance = squaredDistance(candidate, ideal);
                }
            }
            return best;
        }

        /** Returns the land tile of a row beside the edge nearest to a position along it, looking outwards. */
        private int nearestLandInRow(int along, int depth, boolean far) {
            final var span = vertical ? width : height;
            final var reach = (int) (span * EDGE_SPAN_SHARE);
            for (var offset = 0; offset <= reach; offset++) {
                final var before = landAt(along - offset, depth, far);
                if (before != NO_TILE) {
                    return before;
                }
                final var after = landAt(along + offset, depth, far);
                if (after != NO_TILE) {
                    return after;
                }
            }
            return NO_TILE;
        }

        /** Returns the tile at a position along an edge and a depth when it is land inside the map, else no tile. */
        private int landAt(int along, int depth, boolean far) {
            final var span = vertical ? width : height;
            if (along < 0 || along >= span) {
                return NO_TILE;
            }
            final var tile = tileAt(along, depth, far);
            return Float.isFinite(cost[tile]) ? tile : NO_TILE;
        }

        /** Returns the tile at a position along an edge and a number of rows in from it, on the near or far edge. */
        private int tileAt(int along, int depth, boolean far) {
            if (vertical) {
                return (far ? height - 1 - depth : depth) * width + along;
            }
            return along * width + (far ? width - 1 - depth : depth);
        }

        /** Returns the squared distance in tiles between two tiles. */
        private long squaredDistance(int first, int second) {
            final long dx = (long) first % width - second % width;
            final long dy = (long) first / width - second / width;
            return dx * dx + dy * dy;
        }

        /** Turns a land tile of the path into river, once, and notes it. */
        private void carve(int index) {
            if (!inRiver[index] && context.groundAt(index).isLand()) {
                context.setGround(index, TileType.RIVER);
                inRiver[index] = true;
                riverTiles[riverTileCount] = index;
                riverTileCount++;
            }
        }

        /** Makes the land around a river tile cost more for the searches that follow, once for each tile. */
        private void penaliseAround(int tile) {
            final var x = tile % width;
            final var y = tile / width;
            final var lastY = Math.min(height - 1, y + BAND_RADIUS);
            final var lastX = Math.min(width - 1, x + BAND_RADIUS);
            for (var ny = Math.max(0, y - BAND_RADIUS); ny <= lastY; ny++) {
                for (var nx = Math.max(0, x - BAND_RADIUS); nx <= lastX; nx++) {
                    penalise(ny * width + nx);
                }
            }
        }

        /** Adds the penalty to the cost of one open tile, unless it already carries it. */
        private void penalise(int index) {
            if (!banded[index] && Float.isFinite(cost[index])) {
                banded[index] = true;
                cost[index] = (float) ((double) cost[index] + BAND_PENALTY);
                search.setCost(index, cost[index]);
            }
        }

        /** Adds each river's tributaries: short streams from high land far from the rivers to the nearest river. */
        private void addTributaries(int rivers) {
            final var wanted = rivers * params.tributariesPerRiver();
            if (wanted == 0 || riverTileCount == 0) {
                return;
            }
            final var reach = DistanceTransform.chebyshev(width, height, index -> inRiver[index]);
            final IntPredicate eligible = index -> isFarLand(reach, index);
            final var threshold = FieldQuantile.above(heights, eligible, TRIBUTARY_SOURCE_SHARE);
            final var sources = new int[heights.length];
            final var candidates = collect(eligible, threshold, sources);
            for (var stream = 0; stream < wanted && candidates > 0; stream++) {
                join(sources[random.nextInt(candidates)]);
            }
        }

        /** Tells whether a tile is land that is not river and lies far enough from every river. */
        private boolean isFarLand(int[] reach, int index) {
            return !inRiver[index] && context.groundAt(index).isLand() && reach[index] >= TRIBUTARY_MIN_REACH;
        }

        /** Lists the eligible tiles at or above a height at the start of a buffer and returns how many. */
        private int collect(IntPredicate eligible, float threshold, int[] sources) {
            var found = 0;
            for (var index = 0; index < heights.length; index++) {
                if (heights[index] >= threshold && eligible.test(index)) {
                    sources[found] = index;
                    found++;
                }
            }
            return found;
        }

        /** Runs a stream from a source tile to the nearest river tile along the cheapest way. */
        private void join(int source) {
            final var goal = nearestRiverTile(source);
            final var length = search.find(source, goal, path);
            if (length != LeastCostPath.NOT_FOUND) {
                Carve.path(width, height, path, length, Carve.MIN_STROKE, this::carve);
            }
        }

        /** Returns the river tile nearest to a tile, as the crow flies. */
        private int nearestRiverTile(int source) {
            var nearest = riverTiles[0];
            var nearestDistance = Long.MAX_VALUE;
            for (var i = 0; i < riverTileCount; i++) {
                final var distance = squaredDistance(source, riverTiles[i]);
                if (distance < nearestDistance) {
                    nearest = riverTiles[i];
                    nearestDistance = distance;
                }
            }
            return nearest;
        }
    }
}
