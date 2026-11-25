package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Cuts a few long rivers that cross the whole map from one edge to the opposite one. Every river is the cheapest
 * path over the land between a land tile near the edge it enters and one near the opposite edge, where a tile costs
 * more the higher it lies relative to the rest of the land and the drier it is, so the river follows the valleys and
 * bends towards the wet ground. The rivers are carved two tiles wide and have no ford: the player crosses them on
 * bridges.
 * <p>
 * Four things keep the rivers apart and the land in one large piece. They all run the same way, across the map from
 * top to bottom or from left to right as the seed decides, in lanes along the two sides of the map. The tiles of a
 * carved river and the land within a few tiles of it are closed to every later river, so rivers never merge or cross
 * and a later river bends around an earlier one. The middle of the map across the rivers costs more, so the largest
 * strip of land, where the player starts, stays wide. And both ends of a river lie on the same piece of land: a river
 * whose ends cannot be joined is tried again at another place of its lane a few times, so a map reaches the minimum
 * number of rivers whenever its land allows, and a map too small for a river gets none.
 * <p>
 * A river runs between the same position along both edges, which is how far it lies from the start of the edge as a
 * share of the edge, so on flat ground it is straight and the cost of the ground bends it. Afterwards each river
 * receives its own few short tributaries: each one runs from high land a bounded distance from that river, and far
 * from every river and every tributary carved before it, to the nearest bank of that river that it can reach, and
 * never across the middle strip or into the keep-out band of another river.
 * <p>
 * The pass runs on plain arrays and one {@link LeastCostPath} that it reuses for every search and that holds the
 * costs. It draws from one seeded stream and does nothing on a map with no land, so it repeats for a seed.
 */
public final class RiverCrossingPass implements GenerationPass {

    private static final String NAME = "river-crossing";
    private static final long SALT = 0x0C20_55E2_5EED_0001L;
    /** The area, in tiles, that {@code riversPer100k} counts rivers in. */
    private static final double DENSITY_AREA_TILES = 100_000.0;
    /** A map whose shorter side is below this many tiles is too small for a river and gets none. */
    private static final int MIN_MAP_SPAN = 32;
    /** What a tile of flat, low, wet land costs, and the lowest cost of any tile, which scales the heuristic. */
    private static final float BASE_COST = 1F;
    /**
     * The search may find a river that costs up to this many times the cheapest one, which makes it several times
     * faster: the costs vary far above the lowest one, so the exact heuristic is weak and expands most of the map.
     */
    private static final float SEARCH_SLACK = 3F;
    /** The land within this many tiles of a carved river is closed to the rivers that follow, so rivers stay apart. */
    public static final int BAND_RADIUS = 3;
    /** The middle of the map across the rivers costs more, so the strip of land the player starts on stays wide. */
    public static final double CORRIDOR_START = 0.30;
    public static final double CORRIDOR_END = 0.70;
    private static final float CORRIDOR_PENALTY = 3F;
    /** The extra cost of a tile outside the middle strip. */
    private static final double NO_PENALTY = 0.0;
    /** The driest and the wettest moisture, between which the field is clamped. */
    private static final double DRIEST = 0.0;
    private static final double WETTEST = 1.0;
    /** The lanes along the two sides of the map, as shares of the span across the rivers. */
    private static final double FIRST_LANE_START = 0.05;
    private static final double FIRST_LANE_END = 0.30;
    private static final double SECOND_LANE_START = 0.70;
    private static final double SECOND_LANE_END = 0.95;
    private static final int LANE_SIDES = 2;
    /** How far a river moves from the middle of its slot in a lane, as a share of the slot. */
    private static final double SLOT_JITTER = 0.3;
    private static final double HALF = 0.5;
    /** How many places a river is tried at, the first in its slot and the others anywhere in its lane. */
    private static final int MAX_ATTEMPTS = 8;
    /** A river enters the land at the nearest land tile within this many rows of the edge. */
    private static final int EDGE_DEPTH = 10;
    /** A river enters the land at most this share of the edge away from its place. */
    private static final double EDGE_SPAN_SHARE = 0.12;
    /** Tributaries start in the highest share of the land that qualifies for the river they join. */
    private static final double TRIBUTARY_SOURCE_SHARE = 0.05;
    /** A tributary starts at least this many tiles from every river, so it is a stream of its own. */
    public static final int TRIBUTARY_MIN_REACH = 12;
    /** A tributary starts at most this many tiles from the river it joins, so it stays short. */
    public static final int TRIBUTARY_MAX_REACH = 30;
    private static final int NO_TILE = -1;
    /** The number of a tile that belongs to no river. */
    private static final int NO_RIVER = 0;

    private final RiverCrossingParams params;

    /**
     * Creates the pass.
     *
     * @param params how many rivers there are and how they choose their way
     */
    public RiverCrossingPass(RiverCrossingParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Cuts the rivers across the map, one by one, and then adds the tributaries of each. */
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

    /**
     * Tells whether a tile of a river other than the owner lies within the band radius of a tile, in the Chebyshev
     * distance, which makes the tile unfit for a tributary of the owner.
     *
     * @param riverOf the river that carved each tile of the map in reading order, {@link #NO_RIVER} for none
     * @param width   the number of columns
     * @param height  the number of rows
     * @param index   the tile
     * @param owner   the river whose tributary is looked for, whose own tiles do not count
     * @return true when a tile of another river lies that close
     */
    static boolean hasOtherRiverNear(int[] riverOf, int width, int height, int index, int owner) {
        final var x = index % width;
        final var y = index / width;
        final var lastY = Math.min(height - 1, y + BAND_RADIUS);
        final var lastX = Math.min(width - 1, x + BAND_RADIUS);
        for (var ny = Math.max(0, y - BAND_RADIUS); ny <= lastY; ny++) {
            for (var nx = Math.max(0, x - BAND_RADIUS); nx <= lastX; nx++) {
                final var river = riverOf[ny * width + nx];
                if (river != NO_RIVER && river != owner) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The state of one run: the rivers carved so far, the searcher that holds the costs and the buffers. */
    private final class Crossing {

        private final GenerationContext context;
        private final int width;
        private final int height;
        private final float[] heights;
        private final float[] moisture;
        private final RandomGenerator random;
        private final int[] path;
        /** The tiles of every river and tributary carved so far, the first {@code riverTileCount} filled. */
        private final int[] riverTiles;
        /** For each tile, the number of the river that covers it, or {@link #NO_RIVER}. */
        private final int[] riverOf;
        /** For each tile, the number of the river whose keep-out band covers it first, or {@link #NO_RIVER}. */
        private final int[] bandOf;
        private final boolean vertical;
        private double lowest;
        private double range;
        private LeastCostPath search;
        /** The pieces of land that are open to a river, read again after a river is carved, null before the first. */
        private Regions landmasses;
        /** The tiles that may start a tributary, the first {@code candidates} of them filled, kept between rivers. */
        private final int[] sources;
        /** For each tile, its distance to the river of the owner and to every river. */
        private final int[] ownerReach;
        private final int[] anyReach;
        /** The label, among the pieces of open land, of the river of the owner. */
        private int ownerLand;
        private boolean landmassesStale = true;
        private int riverTileCount;
        /** How many rivers are carved. */
        private int rivers;
        /** The number of the river that the tiles carved now belong to. */
        private int owner;

        /** Prepares the arrays of a run and draws the direction the rivers run in. */
        Crossing(GenerationContext context) {
            this.context = context;
            this.width = context.width();
            this.height = context.height();
            this.heights = context.field(NoiseField.HEIGHT);
            this.moisture = context.hasField(NoiseField.MOISTURE)
                    ? context.field(NoiseField.MOISTURE) : new float[context.size()];
            this.random = context.random(SALT);
            this.path = new int[context.size()];
            this.riverTiles = new int[context.size()];
            this.riverOf = new int[context.size()];
            this.bandOf = new int[context.size()];
            this.sources = new int[context.size()];
            this.ownerReach = new int[context.size()];
            this.anyReach = new int[context.size()];
            this.vertical = random.nextBoolean();
        }

        /** Cuts the rivers one by one, makes up for the ones that failed and then adds the tributaries. */
        void run() {
            if (Math.min(width, height) < MIN_MAP_SPAN || !measureLand()) {
                return;
            }
            search = new LeastCostPath(width, height, openCosts(), BASE_COST, SEARCH_SLACK);
            final var wanted = riverCount(context.size());
            for (var slot = 0; slot < wanted; slot++) {
                cross(slot, wanted);
            }
            for (var extra = 0; extra < wanted && rivers < params.minRivers(); extra++) {
                cross(extra, wanted);
            }
            addTributaries();
        }

        /** Finds the lowest height and the range of the heights of the land, and tells whether there is any land. */
        private boolean measureLand() {
            var low = Float.POSITIVE_INFINITY;
            var high = Float.NEGATIVE_INFINITY;
            for (var index = 0; index < context.size(); index++) {
                if (context.groundAt(index).isLand()) {
                    low = Math.min(low, heights[index]);
                    high = Math.max(high, heights[index]);
                }
            }
            lowest = low;
            range = Math.max(Float.MIN_NORMAL, (double) high - low);
            return low <= high;
        }

        /** Returns the cost of every tile for the first river: the cost of the land, infinite for anything else. */
        private float[] openCosts() {
            final var costs = new float[context.size()];
            for (var index = 0; index < costs.length; index++) {
                costs[index] = context.groundAt(index).isLand() ? landCost(index) : Float.POSITIVE_INFINITY;
            }
            return costs;
        }

        /** The cost of a land tile from its height as a share of the land's range, its moisture and its place. */
        private float landCost(int index) {
            final var relativeHeight = (heights[index] - lowest) / range;
            final var wet = Math.clamp(moisture[index], DRIEST, WETTEST);
            return (float) (BASE_COST + params.heightWeight() * relativeHeight
                    + params.meanderWeight() * (WETTEST - wet) + corridorPenalty(index));
        }

        /** The extra cost of a tile in the middle strip across the rivers, nothing outside it. */
        private double corridorPenalty(int index) {
            return inCorridor(index) ? CORRIDOR_PENALTY : NO_PENALTY;
        }

        /** Tells whether a tile lies in the middle strip across the rivers. */
        private boolean inCorridor(int index) {
            final var across = vertical ? index % width : index / width;
            final var span = vertical ? width : height;
            final var share = across / (double) Math.max(1, span - 1);
            return share >= CORRIDOR_START && share <= CORRIDOR_END;
        }

        /** Tells whether a river may still enter a tile, which is when its cost is finite. */
        private boolean isOpen(int index) {
            return Float.isFinite(search.costAt(index));
        }

        /** Tries to cut the river of a slot, first in the slot and then elsewhere in its lane, and tells if it did. */
        private boolean cross(int slot, int slots) {
            final var span = vertical ? width : height;
            for (var attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                final var along = (int) Math.round(lanePosition(slot, slots, attempt > 0) * (span - 1));
                final var start = edgeLand(along, false);
                final var goal = edgeLand(along, true);
                if (start != NO_TILE && goal != NO_TILE && onTheSameLand(start, goal) && carveRiver(start, goal)) {
                    return true;
                }
            }
            return false;
        }

        /** Tells whether two open tiles lie on the same piece of open land, reading the pieces again when needed. */
        private boolean onTheSameLand(int first, int second) {
            if (landmassesStale) {
                readLandmasses();
            }
            return landmasses.labelAt(first) == landmasses.labelAt(second);
        }

        /** Reads the pieces of open land again, into the arrays of the last reading when there is one. */
        private void readLandmasses() {
            landmasses = landmasses == null
                    ? Regions.label(width, height, this::isOpen) : landmasses.relabel(this::isOpen);
            landmassesStale = false;
        }

        /** Cuts a river along the cheapest way between two tiles, closes the land around it and tells if it ran. */
        private boolean carveRiver(int start, int goal) {
            final var length = search.find(start, goal, path);
            if (length == LeastCostPath.NOT_FOUND) {
                return false;
            }
            owner = rivers + 1;
            final var before = riverTileCount;
            Carve.path(width, height, path, length, Carve.MAX_STROKE, this::carve);
            for (var position = before; position < riverTileCount; position++) {
                closeAround(riverTiles[position]);
            }
            rivers = owner;
            landmassesStale = true;
            return true;
        }

        /**
         * Returns where along its edge a river enters and leaves, as a share of the span. The rivers are dealt to the
         * two lanes in turn and each river has a slot in its lane. A first try is the middle of the slot moved a
         * little, and a retry is anywhere in the lane.
         */
        private double lanePosition(int river, int slotCount, boolean anywhere) {
            final var inFirstLane = river % LANE_SIDES == 0;
            final var start = inFirstLane ? FIRST_LANE_START : SECOND_LANE_START;
            final var end = inFirstLane ? FIRST_LANE_END : SECOND_LANE_END;
            if (anywhere) {
                return start + random.nextDouble() * (end - start);
            }
            final var slots = inFirstLane ? (slotCount + 1) / LANE_SIDES : slotCount / LANE_SIDES;
            final var slotWidth = (end - start) / slots;
            final int slot = river / LANE_SIDES;
            final var middle = start + slotWidth * (slot + HALF);
            return middle + (random.nextDouble() - HALF) * SLOT_JITTER * slotWidth;
        }

        /** Returns the open tile nearest to a point of an edge, looked for in the first rows beside it. */
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

        /** Returns the open tile of a row beside the edge nearest to a position along it, looking outwards. */
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

        /** Returns the tile at a position along an edge and a depth when it is open inside the map, else no tile. */
        private int landAt(int along, int depth, boolean far) {
            final var span = vertical ? width : height;
            if (along < 0 || along >= span) {
                return NO_TILE;
            }
            final var tile = tileAt(along, depth, far);
            return isOpen(tile) ? tile : NO_TILE;
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

        /** Turns a land tile of the path into river of the current owner, once, and notes it. */
        private void carve(int index) {
            if (context.groundAt(index).isLand()) {
                context.setGround(index, TileType.RIVER);
                riverOf[index] = owner;
                riverTiles[riverTileCount] = index;
                riverTileCount++;
            }
        }

        /** Closes the tile and the land around it to every search that follows, as the keep-out band of the owner. */
        private void closeAround(int tile) {
            final var x = tile % width;
            final var y = tile / width;
            final var lastY = Math.min(height - 1, y + BAND_RADIUS);
            final var lastX = Math.min(width - 1, x + BAND_RADIUS);
            for (var ny = Math.max(0, y - BAND_RADIUS); ny <= lastY; ny++) {
                for (var nx = Math.max(0, x - BAND_RADIUS); nx <= lastX; nx++) {
                    close(ny * width + nx);
                }
            }
        }

        /** Blocks one tile, noting the owner of the band when no river has claimed it yet. */
        private void close(int index) {
            if (bandOf[index] == NO_RIVER) {
                bandOf[index] = owner;
            }
            search.setCost(index, Float.POSITIVE_INFINITY);
        }

        /** Gives each river its tributaries: short streams from high land to a tile of that same river. */
        private void addTributaries() {
            if (params.tributariesPerRiver() == 0 || rivers == 0) {
                return;
            }
            closeCorridor();
            measureReachFromAny();
            for (var river = 1; river <= rivers; river++) {
                owner = river;
                measureReach();
                reopen();
                readLandmasses();
                joinStreams();
                closeOwnRiver();
            }
        }

        /** Measures how far every tile is from the river of the owner. */
        private void measureReach() {
            DistanceTransform.chebyshev(width, height, index -> riverOf[index] == owner, ownerReach);
        }

        /** Measures how far every tile is from any river, once, before the first tributary is carved. */
        private void measureReachFromAny() {
            DistanceTransform.chebyshev(width, height, index -> riverOf[index] != NO_RIVER, anyReach);
        }

        /** Closes again the river of the owner, with the tributaries it just received, and the land around it. */
        private void closeOwnRiver() {
            for (var i = 0; i < riverTileCount; i++) {
                if (riverOf[riverTiles[i]] == owner) {
                    closeAround(riverTiles[i]);
                }
            }
            landmassesStale = true;
        }

        /** Blocks the middle strip across the rivers, so that no tributary is carved over the land of the player. */
        private void closeCorridor() {
            for (var index = 0; index < context.size(); index++) {
                if (inCorridor(index)) {
                    search.setCost(index, Float.POSITIVE_INFINITY);
                }
            }
        }

        /**
         * Opens the river of the owner and the land of its keep-out band, so that its tributaries can reach it. A tile
         * of the band that also lies within the band of another river stays closed, because the band was claimed by
         * the first river to reach it and a tributary there would come too close to the other one.
         */
        private void reopen() {
            for (var index = 0; index < context.size(); index++) {
                if (riverOf[index] == owner || isOwnFreeBand(index)) {
                    search.setCost(index, landCost(index));
                }
            }
        }

        /**
         * Tells whether a tile is land of the band of the owner, outside the middle strip and clear of other rivers.
         * The band lies within the band radius of the river of the owner, so a scan of the square of that radius
         * around the tile is all the clearance needs, and no distance is measured over the whole map for it.
         */
        private boolean isOwnFreeBand(int index) {
            return bandOf[index] == owner && context.groundAt(index).isLand() && !inCorridor(index)
                    && !hasOtherRiverNear(riverOf, width, height, index, owner);
        }

        /** Runs the tributaries of the owner from the highest of the tiles that may be a source for its river. */
        private void joinStreams() {
            ownerLand = landmasses.labelAt(firstTileOfOwner());
            final var threshold = FieldQuantile.above(heights, this::isSource, TRIBUTARY_SOURCE_SHARE);
            var candidates = 0;
            for (var index = 0; index < heights.length; index++) {
                if (heights[index] >= threshold && isSource(index)) {
                    sources[candidates] = index;
                    candidates++;
                }
            }
            var joined = 0;
            while (joined < params.tributariesPerRiver() && candidates > 0) {
                final var pick = random.nextInt(candidates);
                final var source = sources[pick];
                candidates--;
                sources[pick] = sources[candidates];
                joined += joinFrom(source) ? 1 : 0;
            }
        }

        /** Returns the first tile carved for the owner, which is a tile of its river whatever happens after it. */
        private int firstTileOfOwner() {
            var position = 0;
            while (riverOf[riverTiles[position]] != owner) {
                position++;
            }
            return riverTiles[position];
        }

        /**
         * Tells whether a tile may start a tributary of the owner: open to it and on the same piece of open land as
         * its river, so a way exists, at least the minimum reach from every river and at most the maximum reach from
         * the owner's river.
         */
        private boolean isSource(int index) {
            return anyReach[index] >= TRIBUTARY_MIN_REACH && ownerReach[index] <= TRIBUTARY_MAX_REACH
                    && isOpen(index) && landmasses.labelAt(index) == ownerLand;
        }

        /**
         * Runs a stream from a source tile to the nearest bank of the owner's river along the cheapest way, when the
         * tile is still a source after the streams carved before it, and tells whether it ran.
         */
        private boolean joinFrom(int source) {
            if (!isSource(source)) {
                return false;
            }
            final var goal = nearestBank(source);
            final var length = goal == NO_TILE ? LeastCostPath.NOT_FOUND : search.find(source, goal, path);
            if (length == LeastCostPath.NOT_FOUND) {
                return false;
            }
            final var before = riverTileCount;
            Carve.path(width, height, path, length, Carve.MIN_STROKE, this::carve);
            for (var position = before; position < riverTileCount; position++) {
                lowerReachAround(riverTiles[position]);
            }
            return true;
        }

        /**
         * Brings the distance to any river up to date for a tile just carved, in the square within the minimum reach
         * around it, which is as far as the distance matters to the sources.
         */
        private void lowerReachAround(int tile) {
            final var x = tile % width;
            final var y = tile / width;
            final var lastY = Math.min(height - 1, y + TRIBUTARY_MIN_REACH);
            final var lastX = Math.min(width - 1, x + TRIBUTARY_MIN_REACH);
            for (var ny = Math.max(0, y - TRIBUTARY_MIN_REACH); ny <= lastY; ny++) {
                for (var nx = Math.max(0, x - TRIBUTARY_MIN_REACH); nx <= lastX; nx++) {
                    final var index = ny * width + nx;
                    final var reach = Math.max(Math.abs(nx - x), Math.abs(ny - y));
                    if (reach < anyReach[index]) {
                        anyReach[index] = reach;
                    }
                }
            }
        }

        /**
         * Returns the tile of the owner's river nearest to a tile, as the crow flies, among the banks: the tiles of
         * that river with open ground of another kind beside them, which a stream can reach. A tile of the river in the
         * closed middle strip, or one surrounded by the rest of the river, is no bank.
         */
        private int nearestBank(int source) {
            var nearest = NO_TILE;
            var nearestDistance = Long.MAX_VALUE;
            for (var i = 0; i < riverTileCount; i++) {
                final var tile = riverTiles[i];
                if (riverOf[tile] != owner) {
                    continue;
                }
                final var distance = squaredDistance(source, tile);
                if (distance < nearestDistance && isBank(tile)) {
                    nearest = tile;
                    nearestDistance = distance;
                }
            }
            return nearest;
        }

        /** Tells whether a tile has an open tile of no river of the owner beside it, in any of the eight directions. */
        private boolean isBank(int tile) {
            final var x = tile % width;
            final var y = tile / width;
            final var lastY = Math.min(height - 1, y + 1);
            final var lastX = Math.min(width - 1, x + 1);
            for (var ny = Math.max(0, y - 1); ny <= lastY; ny++) {
                for (var nx = Math.max(0, x - 1); nx <= lastX; nx++) {
                    final var neighbour = ny * width + nx;
                    if (isOpen(neighbour) && riverOf[neighbour] != owner) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
