package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;

import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Turns the height field into a sea with islands of several sizes. Every island has its own radius between the bounds
 * of the parameters, the largest one capped at a share of the shorter side of the map and the radii skewed towards the
 * small end so a map has a few large islands and several small ones. The islands are placed from the largest down by
 * rejection sampling: a centre is accepted when the island lies whole on the map and at least the spacing times the
 * sum of the two radii from every island placed before. An island that finds no such centre in a few attempts is tried
 * again at a smaller radius, down to the smallest radius, which is the one of the parameters on a map of ordinary size
 * and at most a tenth of the shorter side on a small one. An island that still finds no place makes the whole layout
 * start again with every radius shrunk, never with the discs touching, so the number of islands the parameters ask
 * for is kept while the map has room for it and the coasts of two islands never fuse; only an island that finds no
 * centre with every radius at the smallest is dropped. The count is the number of
 * discs placed: the pass does not promise that the ground rules and the noise leave each disc a land mass of any size,
 * which the tests measure. The height is
 * lifted around each centre by an amount that fades to nothing at the radius of that island, and where the lifts of
 * two islands meet the higher one counts. Beyond every radius the height is the sea floor, so the noise only shapes
 * the coasts.
 * <p>
 * The pass draws from one seeded stream and works on the height array and one effect array, so it repeats for a seed
 * and creates no object per tile.
 */
public final class IslandShapePass implements GenerationPass {

    private static final String NAME = "island-shape";
    private static final long SALT = 0x0015_1A4D_5EED_0001L;
    /** The area, in tiles, that {@code islandsPer100kTiles} counts islands in. */
    private static final double DENSITY_AREA_TILES = 100_000.0;
    /** A falloff power of exactly this value is a square, which is computed with a multiplication. */
    private static final float SQUARE_POWER = 2F;
    /**
     * The centres keep this share of the radius away from the edge of the map, so an island lies whole on the map: an
     * island cut by the edge would lose a random part of its area and make the share of the largest island vary.
     */
    private static final double EDGE_MARGIN_OF_RADIUS = 1.0;
    /** The share of the radius at the edge of an island, where the lift has faded to nothing. */
    private static final double FULL_SHARE = 1.0;
    /**
     * The smallest radius is at most this fraction of the shorter side of the map, so a small map still has room for
     * the minimum number of islands: five discs of this radius and larger ones fit whole on the map.
     */
    private static final int SMALLEST_SIDE_DIVISOR = 10;
    private static final double HALF = 0.5;
    /** The share of its slice that the size of an island is drawn in, around the middle of the slice. */
    private static final double SLICE_JITTER = 0.5;
    /** The margin is kept on both sides of an axis. */
    private static final int MARGIN_SIDES = 2;
    /** How many centres are tried for an island at one radius before it is tried at a smaller one. */
    private static final int PLACEMENT_ATTEMPTS = 400;
    /** An island that finds no place shrinks to this share of its radius, or by one tile when that changes nothing. */
    private static final double SHRINK_FACTOR = 0.8;
    /**
     * A layout that leaves an island no place shrinks every radius to this share, or by one tile when that changes
     * nothing: a small step, so the islands keep as much land as the map has room for.
     */
    private static final double RELAYOUT_SHRINK_FACTOR = 0.95;

    private final IslandParams params;
    private final boolean squaredFalloff;

    /**
     * Creates the pass.
     *
     * @param params the island count, size and lift
     */
    public IslandShapePass(IslandParams params) {
        this.params = Objects.requireNonNull(params, "params");
        this.squaredFalloff = Float.compare(params.falloffPower(), SQUARE_POWER) == 0;
    }

    /** Returns the name the timings report this pass under. */
    @Override
    public String name() {
        return NAME;
    }

    /** Places the islands and turns the height into the sea floor and the lift of each island over the noise. */
    @Override
    public void apply(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        final var random = context.random(SALT);
        final var shorterSide = Math.min(context.width(), context.height());
        final var largest = largestRadius(shorterSide);
        final var smallest = smallestRadius(shorterSide, largest);
        final var radii = drawRadii(random, islandCount(context.width(), context.height()), smallest, largest);
        final var effect = new float[context.size()];
        new Layout(context, random, radii.length, smallest).placeAll(radii, effect);
        for (var index = 0; index < effect.length; index++) {
            final double lifted = (double) effect[index] * params.lift() - params.seaOffset();
            heights[index] = (float) ((double) heights[index] * effect[index] * params.heightGain() + lifted);
        }
    }

    /**
     * Returns how many islands a map of a size gets: the density times the area, never below the minimum and never
     * above {@link IslandParams#MAX_ISLANDS}.
     *
     * @param width  the number of columns
     * @param height the number of rows
     * @return the island count
     */
    int islandCount(int width, int height) {
        final var byArea = (int) Math.round(
                (double) width * height * params.islandsPer100kTiles() / DENSITY_AREA_TILES);
        return Math.clamp(byArea, params.minIslands(), IslandParams.MAX_ISLANDS);
    }

    /**
     * Draws a radius for each island and returns them from the smallest to the largest. The radius is the smallest one
     * plus the span times {@code u * u}, which skews the islands to the small end, and the {@code u} values are
     * stratified: the island number {@code i} of {@code count} draws its {@code u} inside the slice from {@code i} to
     * {@code i + 1} of {@code count}. A map therefore always has islands of every size, with a large one among them,
     * instead of the luck of the draw giving it only small ones.
     */
    private static int[] drawRadii(RandomGenerator random, int count, int smallest, int largest) {
        final var span = largest - smallest;
        final var radii = new int[count];
        for (var i = 0; i < count; i++) {
            final var u = (i + HALF + (random.nextDouble() - HALF) * SLICE_JITTER) / count;
            radii[i] = smallest + (int) Math.round(span * u * u);
        }
        Arrays.sort(radii);
        return radii;
    }

    /**
     * Returns the largest radius on a map of a shorter side: the one of the parameters, or the share of the side
     * rounded down, so an island of that radius lies whole on the map: on a side of 65 the cap at half the side is 32,
     * a disc of 64 across, and not 33, which would not fit.
     */
    private int largestRadius(int shorterSide) {
        final var cap = Math.max(1, (int) (shorterSide * (double) params.size().maxShareOfSide()));
        return Math.min(params.size().maxRadius(), cap);
    }

    /**
     * Returns the smallest radius on a map of a shorter side: the one of the parameters, but never above the largest
     * radius and never above a tenth of the side, so a small map shrinks the floor instead of dropping islands.
     */
    private int smallestRadius(int shorterSide, int largest) {
        final var affordable = Math.max(1, shorterSide / SMALLEST_SIDE_DIVISOR);
        return Math.min(params.size().minRadius(), Math.min(largest, affordable));
    }

    /** The islands placed so far, with the centres and radii that later islands have to keep their distance from. */
    private final class Layout {

        private final GenerationContext context;
        private final RandomGenerator random;
        private final int[] centresX;
        private final int[] centresY;
        private final int[] placedRadii;
        /** The spacing of the parameters as a double, read once instead of for every pair of islands. */
        private final double spacing;
        /** The radius below which an island is not made smaller to find a place. */
        private final int smallest;
        private int placed;

        /**
         * Prepares room for the centres and radii of up to {@code count} islands, none below the smallest radius.
         *
         * @param context  the generation, which gives the size of the map
         * @param random   the stream the centres are drawn from
         * @param count    the most islands that are placed
         * @param smallest the smallest radius an island is made, in tiles
         */
        Layout(GenerationContext context, RandomGenerator random, int count, int smallest) {
            this.context = context;
            this.random = random;
            this.spacing = params.size().spacing();
            this.smallest = smallest;
            this.centresX = new int[count];
            this.centresY = new int[count];
            this.placedRadii = new int[count];
        }

        /**
         * Places the islands from the largest radius down and paints the lift of each placed one into the effect. When
         * an island finds no place even at the smallest radius, the whole layout is made again with every radius
         * shrunk, never with the discs touching, so the count of islands holds whenever the map has room for that many
         * of the smallest size and the coasts of two islands never fuse. A map with no room even then keeps the
         * islands that found one.
         */
        void placeAll(int[] radiiAscending, float[] effect) {
            final var radii = radiiAscending.clone();
            while (!layOut(radii) && shrinkAll(radii)) {
                placed = 0;
            }
            for (var i = 0; i < placed; i++) {
                paint(effect, i);
            }
        }

        /** Places every island from the largest down and tells whether all of them found a place. */
        private boolean layOut(int[] radiiAscending) {
            var unplaced = 0;
            for (var i = radiiAscending.length - 1; i >= 0; i--) {
                unplaced += place(radiiAscending[i]) ? 0 : 1;
            }
            return unplaced == 0;
        }

        /** Shrinks every radius above the smallest one, and tells whether any radius changed. */
        private boolean shrinkAll(int[] radii) {
            var changed = false;
            for (var i = 0; i < radii.length; i++) {
                if (radii[i] > smallest) {
                    radii[i] = Math.clamp((int) (radii[i] * RELAYOUT_SHRINK_FACTOR), smallest, radii[i] - 1);
                    changed = true;
                }
            }
            return changed;
        }

        /**
         * Places an island at the radius, or a smaller one when no centre keeps the spacing down to the smallest
         * radius, which is always tried, and tells if it fits.
         */
        private boolean place(int radius) {
            var candidate = radius;
            while (!tryPlace(candidate, spacing)) {
                if (candidate <= smallest) {
                    return false;
                }
                candidate = Math.clamp((int) (candidate * SHRINK_FACTOR), smallest, candidate - 1);
            }
            return true;
        }

        /** Tries centres until one keeps the given spacing, and records the island there. */
        private boolean tryPlace(int radius, double requiredSpacing) {
            final var margin = (int) Math.round(radius * EDGE_MARGIN_OF_RADIUS);
            for (var attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
                final var x = centreOnAxis(context.width(), margin);
                final var y = centreOnAxis(context.height(), margin);
                if (isClear(x, y, radius, requiredSpacing)) {
                    centresX[placed] = x;
                    centresY[placed] = y;
                    placedRadii[placed] = radius;
                    placed++;
                    return true;
                }
            }
            return false;
        }

        /** Tells whether a centre keeps a spacing from every island placed so far. */
        private boolean isClear(int x, int y, int radius, double requiredSpacing) {
            for (var i = 0; i < placed; i++) {
                final var needed = requiredSpacing * (radius + placedRadii[i]);
                final double dx = (double) x - centresX[i];
                final double dy = (double) y - centresY[i];
                if (dx * dx + dy * dy < needed * needed) {
                    return false;
                }
            }
            return true;
        }

        /** Raises the effect, where it is higher, over the disc of an island. */
        private void paint(float[] effect, int island) {
            final var radius = placedRadii[island];
            final var radiusSquared = (long) radius * radius;
            final var lastY = Math.min(context.height() - 1, centresY[island] + radius);
            final var lastX = Math.min(context.width() - 1, centresX[island] + radius);
            for (var y = Math.max(0, centresY[island] - radius); y <= lastY; y++) {
                for (var x = Math.max(0, centresX[island] - radius); x <= lastX; x++) {
                    final long dx = (long) x - centresX[island];
                    final long dy = (long) y - centresY[island];
                    final var squared = dx * dx + dy * dy;
                    if (squared < radiusSquared) {
                        final var index = context.index(x, y);
                        effect[index] = Math.max(effect[index],
                                (float) falloff(Math.sqrt((double) squared) / radius));
                    }
                }
            }
        }

        /** Returns how much of the lift remains at a share of the radius from 0 at the centre to 1 at the edge. */
        private double falloff(double shareOfRadius) {
            final var fade = FULL_SHARE - shareOfRadius;
            // StrictMath, not Math: Math.pow may differ between JVMs and CPUs in the last bit, and a map has to
            // come out the same for the same seed everywhere
            return squaredFalloff ? fade * fade : StrictMath.pow(fade, params.falloffPower());
        }

        /** Draws one coordinate between the margins, or the middle of the axis when the margins leave no room. */
        private int centreOnAxis(int length, int margin) {
            final var usableMargin = Math.min(margin, (length - 1) / MARGIN_SIDES);
            return usableMargin + random.nextInt(length - MARGIN_SIDES * usableMargin);
        }
    }
}
