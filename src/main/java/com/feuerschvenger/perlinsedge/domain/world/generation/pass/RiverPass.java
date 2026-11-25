package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.utils.Doubles;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.random.RandomGenerator;

/**
 * Carves rivers into the ground. A river starts on a land tile among the highest share of the land, so every map has
 * sources whatever its height profile, and runs downhill, always to the lowest of the
 * neighbours that are lower than the tile it stands on, until it reaches water, runs out of length or has nowhere
 * lower to go. A river that dead ends before the fewest tiles to keep is taken back and its slot goes to the next
 * source; when no river of the map reaches that length the fewest halves until one stays. A river that dead ends low
 * enough floods the low ground around its end into a lake, and a long river may send out a branch. The tiles a river
 * covers become {@link TileType#RIVER}.
 * <p>
 * The pass works on the arrays only: the tiles a river has visited are a mask over the map and a river is a buffer of
 * indices, so no object is created per tile. It runs before any resource is placed, so it has nothing to clear.
 */
public final class RiverPass implements GenerationPass {

    private static final String NAME = "rivers";
    private static final long SALT = 0x0017_E125_0A11_0001L;
    /** Share of the potential sources that become rivers, before the density of the recipe scales it. */
    private static final double SOURCE_SHARE = 0.1;
    /** The eight neighbours, clockwise from the one above. */
    private static final int[] STEP_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] STEP_Y = {-1, -1, 0, 1, 1, 1, 0, -1};
    /** Stands for "no tile" where an index is expected. */
    private static final int NO_TILE = -1;
    /** Stands for "the next step is not chosen yet" where an index is expected. */
    private static final int UNPLANNED = -2;
    /** The factor the fewest tiles to keep shrinks by when no river reaches it. */
    private static final int HALVING = 2;

    private final RiverParams params;

    /**
     * Creates the pass.
     *
     * @param params how many rivers there are, how long they run and how they branch and end
     */
    public RiverPass(RiverParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Traces the rivers from random high tiles and floods a lake at the dead end of those that end low. */
    @Override
    public void apply(GenerationContext context) {
        new Flow(context).run();
    }

    /**
     * Returns the height below which a dead end may flood a lake: the top of the lowest {@code share} of the selected
     * tiles. A share of 0 gives {@link Float#NEGATIVE_INFINITY}, so no tile lies below it and no lake is made, and a
     * share of 1 gives {@link Float#POSITIVE_INFINITY}, so every dead end makes one. On land with a single height no
     * tile lies lower than another, so any share below 1 also gives {@link Float#NEGATIVE_INFINITY} instead of
     * flooding a lake at every dead end.
     *
     * @param heights the height field
     * @param isLand  the tiles the share is counted over
     * @param share   the share of the tiles that may hold a lake, from 0 to 1
     * @return the ceiling; a tile makes a lake only when its height is below it
     */
    static float lakeCeiling(float[] heights, IntPredicate isLand, double share) {
        if (share >= 1.0) {
            return Float.POSITIVE_INFINITY;
        }
        if (share <= 0.0) {
            return Float.NEGATIVE_INFINITY;
        }
        final var ceiling = FieldQuantile.above(heights, isLand, 1.0 - share);
        return Float.isInfinite(ceiling) ? Float.NEGATIVE_INFINITY : ceiling;
    }

    /** The state of one run: the arrays the rivers are traced over and the buffers of the river being traced. */
    private final class Flow {

        private final GenerationContext context;
        private final float[] heights;
        private final boolean[] inRiver;
        private final int[] river;
        private final int[] branch;
        private final int[] candidates = new int[STEP_X.length];
        private final RandomGenerator random;
        /** The tiles a river may start on, of which the first {@code sourceCount} are filled. */
        private final int[] sources;
        /** The lowest height a river or a branch may start at: the sources are the highest share of the land. */
        private final float sourceThreshold;
        /** A dead end makes a lake only below this height: the lowest share of the land lies under it. */
        private final float lakeCeiling;
        private int sourceCount;
        /** The fewest tiles a river covers to stay, which drops when no river of the map reaches it. */
        private int keptThreshold = params.minKeptTiles();
        /** For each tile, the number of the river whose branch covers it, 0 for none. */
        private final int[] branchOwner;
        /** The number of the river being traced, counting from 1. */
        private int riverNumber;
        /** The tile the river being traced steps to next while a branch is walked, which the branch must leave free. */
        private int reserved = NO_TILE;

        /** Prepares the masks and buffers of a run. */
        Flow(GenerationContext context) {
            this.context = context;
            this.heights = context.field(NoiseField.HEIGHT);
            this.inRiver = new boolean[context.size()];
            this.branchOwner = new int[context.size()];
            this.river = new int[params.maxLength()];
            this.branch = new int[params.tributaries().maxLength()];
            this.random = context.random(SALT);
            this.sources = new int[context.size()];
            // Both thresholds are read before the first river is carved, from the land the pass starts with
            final IntPredicate isLand = index -> context.groundAt(index).isLand();
            this.sourceThreshold = FieldQuantile.above(heights, isLand, params.sourceShare());
            this.lakeCeiling = lakeCeiling(heights, isLand, params.lakes().maxEndpointShare());
        }

        /** Starts a number of rivers from random high tiles, the number scaled by the density of the recipe. */
        void run() {
            collectSources();
            if (sourceCount == 0) {
                return;
            }
            final var riverCount = Math.max(1, (int) (sourceCount * (double) params.density() * SOURCE_SHARE));
            shuffleSources();
            final var rivers = Math.min(riverCount, sourceCount);
            var used = startRivers(rivers);
            // A map with no river that is long enough is not left dry: the rule halves until some river stays
            while (used == 0 && keptThreshold > 0) {
                keptThreshold /= HALVING;
                used = startRivers(rivers);
            }
        }

        /**
         * Starts rivers from the shuffled sources until {@code rivers} of them used a slot or the sources run out, and
         * returns how many did. A river taken back as a stub leaves its slot free, so the next source gets a try.
         */
        private int startRivers(int rivers) {
            var used = 0;
            for (var i = 0; i < sourceCount && used < rivers; i++) {
                used += startRiver(sources[i]) ? 1 : 0;
            }
            return used;
        }

        /** Finds every land tile at or above the source height. */
        private void collectSources() {
            for (var index = 0; index < sources.length; index++) {
                if (context.groundAt(index).isLand() && heights[index] >= sourceThreshold) {
                    sources[sourceCount] = index;
                    sourceCount++;
                }
            }
        }

        /** Shuffles the filled part of the sources, so the rivers start from random ones. */
        private void shuffleSources() {
            for (var i = sourceCount - 1; i > 0; i--) {
                final var other = random.nextInt(i + 1);
                final var held = sources[i];
                sources[i] = sources[other];
                sources[other] = held;
            }
        }

        /**
         * Traces a river from a source unless another river already covers it, and tells whether the source used one
         * of the river slots: false only for a river that was too short to stay and was taken back.
         */
        private boolean startRiver(int source) {
            if (context.groundAt(source).isLand() && !inRiver[source]) {
                // The length is drawn only for a source that is used
                return trace(source, params.minLength() + random.nextInt(params.maxLength() - params.minLength()));
            }
            return true;
        }

        /**
         * Walks downhill from a tile and turns the tiles it passes into river, and tells whether the river stays. A
         * river that dead ends before the fewest tiles to keep makes no lake and is taken back, which is only a river
         * that has no branch, since a branch needs a longer river.
         */
        private boolean trace(int start, int maxLength) {
            riverNumber++;
            var index = start;
            var length = 0;
            var covered = 0;
            var planned = UNPLANNED;
            while (length < maxLength && isOpenTo(index)) {
                inRiver[index] = true;
                river[covered] = index;
                covered++;
                final var next = planned == UNPLANNED ? nextPoint(index) : planned;
                planned = UNPLANNED;
                if (next == NO_TILE) {
                    if (covered >= keptThreshold) {
                        makeLake(index);
                    }
                    break;
                }
                index = next;
                length++;
                // A step that reached water ends the river, so it sends out no branch and draws no chance
                if (isOpenTo(index) && branches(length)) {
                    planned = branchOff(index);
                }
            }
            return keepOrTakeBack(covered);
        }

        /** Carves the river in the buffer when it covers the fewest tiles to keep, and frees its tiles otherwise. */
        private boolean keepOrTakeBack(int covered) {
            if (covered >= keptThreshold) {
                carve(river, covered);
                return true;
            }
            for (var i = 0; i < covered; i++) {
                inRiver[river[i]] = false;
            }
            return false;
        }

        /** Tells whether the river being traced may enter a tile: one that is not water, or one of its own branch. */
        private boolean isOpenTo(int index) {
            return isOwnBranch(index) || !isWater(index);
        }

        /** Tells whether a river that has run {@code length} steps sends out a branch, drawing the chance. */
        private boolean branches(int length) {
            return length > params.tributaries().minRiverLength()
                    && random.nextFloat() < params.tributaries().chance();
        }

        /**
         * Sends a branch out from a tile of the river being traced and returns where the river itself goes next from
         * there, or {@link #NO_TILE} when the river has nowhere lower to go. That step is chosen first and kept free of
         * the branch, so the branch never takes the river's way.
         */
        private int branchOff(int start) {
            final var next = nextPoint(start);
            reserved = next;
            traceBranch(start);
            reserved = NO_TILE;
            return next;
        }

        /** Draws the length of a branch, before the start is looked at, and walks it. */
        private void traceBranch(int start) {
            final var tributaries = params.tributaries();
            walkBranch(start, tributaries.minLength() + random.nextInt(tributaries.lengthSpread()));
        }

        /**
         * Walks downhill from a point of a river and turns the tiles it passes into river, stopping at a river. The
         * tiles are noted as branch tiles of the river being traced, so that river runs on past the start of the
         * branch instead of stopping at the water the branch made.
         */
        private void walkBranch(int start, int maxLength) {
            if (!context.groundAt(start).isLand() || heights[start] < sourceThreshold) {
                return;
            }
            var index = start;
            var covered = 0;
            for (var step = 0; step < maxLength && !inRiver[index]; step++) {
                inRiver[index] = true;
                branchOwner[index] = riverNumber;
                branch[covered] = index;
                covered++;
                final var next = nextPoint(index);
                if (next == NO_TILE) {
                    break;
                }
                index = next;
            }
            carve(branch, covered);
        }

        /**
         * Picks where a river goes from a tile: one of the lowest neighbours that lie lower than the tile, are either
         * water or not covered by a river yet, are not a branch of this river and are not reserved for its next step.
         */
        private int nextPoint(int index) {
            final var current = heights[index];
            final var x = context.xOf(index);
            final var y = context.yOf(index);
            var lowest = current;
            var count = 0;
            for (var direction = 0; direction < STEP_X.length; direction++) {
                final var neighbour = neighbourOf(x + STEP_X[direction], y + STEP_Y[direction]);
                if (neighbour == NO_TILE || !isOpen(neighbour, current)) {
                    continue;
                }
                if (heights[neighbour] < lowest) {
                    lowest = heights[neighbour];
                    count = 0;
                }
                if (Doubles.areEqual(heights[neighbour], lowest)) {
                    candidates[count] = neighbour;
                    count++;
                }
            }
            return count == 0 ? NO_TILE : candidates[random.nextInt(count)];
        }

        /** Returns the index of a tile, or {@link #NO_TILE} when it lies outside the map. */
        private int neighbourOf(int x, int y) {
            return context.isOnMap(x, y) ? context.index(x, y) : NO_TILE;
        }

        /**
         * Tells whether a river may flow into a tile lower than {@code current}: water, or ground not yet covered, but
         * never a branch of its own or the tile the river keeps for its own next step.
         */
        private boolean isOpen(int index, float current) {
            return heights[index] < current && isFreeToFlow(index) && index != reserved;
        }

        /** Tells whether a river may run onto a tile: water, or ground no river covers, and never its own branch. */
        private boolean isFreeToFlow(int index) {
            return (!inRiver[index] || isWater(index)) && !isOwnBranch(index);
        }

        /** Tells whether a tile belongs to a branch of the river being traced. */
        private boolean isOwnBranch(int index) {
            return branchOwner[index] == riverNumber;
        }

        /** Tells whether a tile is water a river flows into. */
        private boolean isWater(int index) {
            return Terrain.isFlowingInto(context.groundAt(index));
        }

        /** Turns the land tiles of the first {@code count} indices of a buffer into river. */
        private void carve(int[] buffer, int count) {
            for (var i = 0; i < count; i++) {
                if (context.groundAt(buffer[i]).isLand()) {
                    context.setGround(buffer[i], TileType.RIVER);
                }
            }
        }

        /** Floods the low land around the end of a river, when the end lies low enough. */
        private void makeLake(int endpoint) {
            if (heights[endpoint] >= lakeCeiling || !context.groundAt(endpoint).isLand()) {
                return;
            }
            final var lakes = params.lakes();
            final var radius = lakes.minRadius() + random.nextInt(lakes.maxRadius() - lakes.minRadius());
            final var limit = (float) ((double) heights[endpoint] + lakes.depthTolerance());
            final var x = context.xOf(endpoint);
            final var y = context.yOf(endpoint);
            for (var dy = -radius; dy <= radius; dy++) {
                for (var dx = -radius; dx <= radius; dx++) {
                    flood(neighbourOf(x + dx, y + dy), limit, lakes.surface());
                }
            }
        }

        /** Turns a land tile lower than the limit into the surface of the lake. */
        private void flood(int index, float limit, TileType surface) {
            if (index != NO_TILE && context.groundAt(index).isLand() && heights[index] < limit) {
                context.setGround(index, surface);
            }
        }
    }
}
