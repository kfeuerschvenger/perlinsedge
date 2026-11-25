package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IslandShapePassTest {

    private static final int SIDE = 60;
    private static final int RADIUS = 6;
    private static final float SPACING = 1.05F;
    /** A share of the side at which the cap on the radius never bites on the maps of these tests. */
    private static final float NO_CAP = IslandSize.MAX_SHARE_OF_SIDE;
    /** The sizes of the islands recipe, whose five islands have to fit on the small maps too. */
    private static final int RECIPE_MIN_RADIUS = 12;
    private static final int RECIPE_MAX_RADIUS = 70;
    private static final float RECIPE_SHARE = 0.3F;
    private static final IslandSize RECIPE_SIZE =
            new IslandSize(RECIPE_MIN_RADIUS, RECIPE_MAX_RADIUS, RECIPE_SHARE, SPACING);
    /** A spacing wide enough that two islands the layout keeps apart leave a gap that touching discs would not. */
    private static final float GAP_SPACING = 1.3F;
    private static final IslandSize SPACED_SIZE =
            new IslandSize(RECIPE_MIN_RADIUS, RECIPE_MAX_RADIUS, RECIPE_SHARE, GAP_SPACING);
    private static final int MIN_GAP = 3;
    private static final int SMALLEST_MAP = 64;
    private static final long FIT_SEEDS = 20L;
    private static final IslandSize SIZE = new IslandSize(RADIUS, RADIUS, NO_CAP, SPACING);
    private static final float GAIN = 1.5F;
    private static final float LIFT = 0.7F;
    private static final float SEA = 0.5F;
    private static final float FALLOFF = 2F;
    private static final float CUBIC_FALLOFF = 3F;
    private static final float TOLERANCE = 1e-5F;
    private static final float FLAT_SEA = 0F;
    private static final float HIGH_GROUND = 0.9F;
    private static final float NOISE_STEP = 0.5F;

    private static final int SMALL = 150;
    private static final int LARGE = 300;
    private static final int TINY = 2;
    private static final int SMALL_COUNT = 3;
    private static final int LARGE_COUNT = 12;
    private static final int FLOOR_COUNT = 5;
    private static final int DENSITY = 13;
    private static final int NO_DENSITY = 0;
    private static final int MINIMUM = 1;
    private static final int THREE_ISLANDS = 3;
    private static final int FOUR_ISLANDS = 4;
    private static final int PER_100K = 100_000;
    private static final int OVER_100K = PER_100K + 1;
    /** A side of 4 is narrower than the margin of a radius of 2: the centres are drawn in the middle two tiles. */
    private static final int TINY_SIDE = 4;
    private static final int TINY_MIDDLE_FIRST = 1;
    private static final int TINY_MIDDLE_LAST = 2;
    private static final int ODD_TINY_SIDE = 3;
    private static final int ODD_SMALL_SIDE = 5;
    private static final int HALF = 2;
    private static final int SINGLE_TILE_SIDE = 1;
    private static final int OVER_MAX_ISLANDS = IslandParams.MAX_ISLANDS + 1;
    private static final int HUGE_SIDE = 2_000;

    private static final long SEED_SEA = 1L;
    private static final long SEED_CENTRE = 2L;
    private static final long SEED_NOISE = 3L;
    private static final long SEED_REPEAT = 4L;
    private static final long SEED_OTHER = 5L;
    private static final long SEED_TINY = 6L;
    private static final long SEED_MISSING = 1L;
    private static final long SEED_SPACING = 8L;

    private static final int ROOMY_SIDE = 90;
    private static final int CROWDED_SIDE = 30;
    private static final int HUGE_ROOMY_SIDE = 150;
    private static final int SEVEN_ISLANDS = 7;
    private static final int NINE_ISLANDS = 9;
    private static final int MANY_ISLANDS = 20;
    private static final float WIDE_SPACING = 2F;
    private static final int ISLET_RADIUS = 3;
    private static final int BIG_RADIUS = 20;
    private static final int ISLAND_SIZE_RATIO = 4;
    private static final int HALF_SIDE = SIDE / 2;
    /** A tenth of the side of the small map, which is the radius of 3. */
    private static final float CAP_SHARE = 0.1F;
    private static final int HUGE_RADIUS = 25;
    private static final int SIX_ISLANDS = 6;
    private static final long SHRINK_SEEDS = 10L;

    private static IslandParams params(int per100k, int minimum) {
        return new IslandParams(per100k, minimum, SIZE, GAIN, LIFT, SEA, FALLOFF);
    }

    /** One island and no more, wherever the seed puts it, so what lies around it is the sea. */
    private static IslandParams singleIsland() {
        return params(NO_DENSITY, MINIMUM);
    }

    private static GenerationContext flatHeightContext(long seed, int side, float height) {
        final var context = TestContexts.of(MapType.ISLANDS, seed, side, side, TestContexts.FLAT);
        Arrays.fill(context.defineField(NoiseField.HEIGHT), height);
        return context;
    }

    /** Returns the index of the first tile with the highest value, which is the centre of an island. */
    private static int highestIndex(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        var best = 0;
        for (var i = 1; i < heights.length; i++) {
            if (heights[i] > heights[best]) {
                best = i;
            }
        }
        return best;
    }

    private static float highest(GenerationContext context) {
        return context.field(NoiseField.HEIGHT)[highestIndex(context)];
    }

    @Test
    void theIslandCountIsADensityPerAreaWithAFloor() {
        final var pass = new IslandShapePass(params(DENSITY, MINIMUM));

        assertEquals(SMALL_COUNT, pass.islandCount(SMALL, SMALL));
        assertEquals(LARGE_COUNT, pass.islandCount(LARGE, LARGE));
        assertEquals(MINIMUM, pass.islandCount(TINY, TINY));
        assertEquals(FLOOR_COUNT, new IslandShapePass(params(DENSITY, FLOOR_COUNT)).islandCount(TINY, TINY));
        assertEquals(MINIMUM,
                new IslandShapePass(params(PER_100K, MINIMUM)).islandCount(SINGLE_TILE_SIDE, SINGLE_TILE_SIDE));
    }

    @Test
    void theIslandCountNeverExceedsTheMaximum() {
        final var pass = new IslandShapePass(params(PER_100K, MINIMUM));

        assertEquals(IslandParams.MAX_ISLANDS, pass.islandCount(HUGE_SIDE, HUGE_SIDE));
        assertEquals(IslandParams.MAX_ISLANDS,
                new IslandShapePass(params(NO_DENSITY, IslandParams.MAX_ISLANDS)).islandCount(TINY, TINY));
    }

    @Test
    void awayFromEveryIslandTheHeightIsTheSeaFloor() {
        final var context = flatHeightContext(SEED_SEA, SIDE, HIGH_GROUND);

        new IslandShapePass(singleIsland()).apply(context);

        final var heights = context.field(NoiseField.HEIGHT);
        var seaFloor = 0;
        for (var value : heights) {
            if (Math.abs(value + SEA) < TOLERANCE) {
                seaFloor++;
            }
        }
        assertTrue(seaFloor > heights.length / HALF, "sea floor tiles " + seaFloor);
    }

    @Test
    void theLiftIsHighestAtTheIslandCentreAndFadesToTheSeaAtTheRadius() {
        final var context = flatHeightContext(SEED_CENTRE, SIDE, FLAT_SEA);

        new IslandShapePass(singleIsland()).apply(context);

        final var heights = context.field(NoiseField.HEIGHT);
        final var centre = highestIndex(context);
        assertEquals(LIFT - SEA, heights[centre], TOLERANCE);
        final var cx = context.xOf(centre);
        final var cy = context.yOf(centre);
        final var edgeX = cx + RADIUS < SIDE ? cx + RADIUS : cx - RADIUS;
        assertEquals(-SEA, heights[context.index(edgeX, cy)], TOLERANCE);
    }

    @Test
    void aSquaredFalloffGivesAQuarterOfTheLiftHalfWayOut() {
        final var context = flatHeightContext(SEED_CENTRE, SIDE, FLAT_SEA);

        new IslandShapePass(singleIsland()).apply(context);

        final var centre = highestIndex(context);
        final var halfX = context.xOf(centre) + RADIUS / HALF < SIDE
                ? context.xOf(centre) + RADIUS / HALF
                : context.xOf(centre) - RADIUS / HALF;
        final var halfWay = context.field(NoiseField.HEIGHT)[context.index(halfX, context.yOf(centre))];
        assertEquals(LIFT / (HALF * HALF) - SEA, halfWay, TOLERANCE);
    }

    @Test
    void aFalloffThatIsNotASquareUsesThePowerOfTheParams() {
        final var context = flatHeightContext(SEED_CENTRE, SIDE, FLAT_SEA);
        final var cubic = new IslandParams(NO_DENSITY, MINIMUM, SIZE, GAIN, LIFT, SEA, CUBIC_FALLOFF);

        new IslandShapePass(cubic).apply(context);

        final var centre = highestIndex(context);
        final var halfX = context.xOf(centre) + RADIUS / HALF < SIDE
                ? context.xOf(centre) + RADIUS / HALF
                : context.xOf(centre) - RADIUS / HALF;
        final var halfWay = context.field(NoiseField.HEIGHT)[context.index(halfX, context.yOf(centre))];
        assertEquals(LIFT / (HALF * HALF * HALF) - SEA, halfWay, TOLERANCE);
    }

    @Test
    void theNoiseHeightScalesTheLiftAtTheCentre() {
        final var low = flatHeightContext(SEED_NOISE, SIDE, FLAT_SEA);
        final var high = flatHeightContext(SEED_NOISE, SIDE, NOISE_STEP);
        final var pass = new IslandShapePass(params(DENSITY, MINIMUM));

        pass.apply(low);
        pass.apply(high);

        assertEquals(NOISE_STEP * GAIN, highest(high) - highest(low), TOLERANCE);
    }

    @Test
    void theSameSeedGivesTheSameIslandsAndAnotherSeedMovesThem() {
        final var first = flatHeightContext(SEED_REPEAT, SIDE, FLAT_SEA);
        final var same = flatHeightContext(SEED_REPEAT, SIDE, FLAT_SEA);
        final var other = flatHeightContext(SEED_OTHER, SIDE, FLAT_SEA);
        final var pass = new IslandShapePass(params(DENSITY, THREE_ISLANDS));

        pass.apply(first);
        pass.apply(same);
        pass.apply(other);

        assertArrayEquals(first.field(NoiseField.HEIGHT), same.field(NoiseField.HEIGHT));
        assertFalse(Arrays.equals(first.field(NoiseField.HEIGHT), other.field(NoiseField.HEIGHT)));
    }

    /**
     * On a map narrower than the margins of the islands the margin gives way: every centre is drawn from the middle of
     * the map instead of from an empty range, so the pass places its islands and does not throw.
     */
    @Test
    void aMapNarrowerThanTheMarginsKeepsEveryCentreInTheMiddleOfTheMap() {
        final var context = flatHeightContext(SEED_TINY, TINY_SIDE, FLAT_SEA);

        new IslandShapePass(params(DENSITY, FOUR_ISLANDS)).apply(context);

        final var centres = centres(context);
        assertTrue(centres.length >= 1, "an island was placed");
        for (var centre : centres) {
            final var inTheMiddle = context.xOf(centre) >= TINY_MIDDLE_FIRST && context.xOf(centre) <= TINY_MIDDLE_LAST
                    && context.yOf(centre) >= TINY_MIDDLE_FIRST && context.yOf(centre) <= TINY_MIDDLE_LAST;
            assertTrue(inTheMiddle, "the centre " + centre + " lies off the middle");
        }
    }

    /** On a side that is odd the largest radius rounds down, so an island of that radius lies whole on the map. */
    @Test
    void anIslandLiesWholeOnAMapWhoseSideIsOdd() {
        for (var side : new int[] {ODD_TINY_SIDE, ODD_SMALL_SIDE}) {
            final var context = flatHeightContext(SEED_TINY, side, FLAT_SEA);
            final var heights = context.field(NoiseField.HEIGHT);

            new IslandShapePass(params(NO_DENSITY, FOUR_ISLANDS)).apply(context);

            for (var index = 0; index < context.size(); index++) {
                final var onBorder = context.xOf(index) == 0 || context.yOf(index) == 0
                        || context.xOf(index) == side - 1 || context.yOf(index) == side - 1;
                assertTrue(!onBorder || Math.abs(heights[index] + SEA) < TOLERANCE,
                        "side " + side + ": an island reaches the edge at " + index);
            }
        }
    }

    /** Returns the indices of the island centres, which are the tiles lifted by the whole lift. */
    private static int[] centres(GenerationContext context) {
        final var heights = context.field(NoiseField.HEIGHT);
        return IntStream.range(0, heights.length)
                .filter(index -> Math.abs(heights[index] - (LIFT - SEA)) < TOLERANCE).toArray();
    }

    @Test
    void islandsKeepTheirSpacingFromEachOther() {
        final var context = flatHeightContext(SEED_SPACING, ROOMY_SIDE, FLAT_SEA);
        final var wide = new IslandSize(RADIUS, RADIUS, NO_CAP, WIDE_SPACING);

        new IslandShapePass(new IslandParams(NO_DENSITY, SEVEN_ISLANDS, wide, GAIN, LIFT, SEA, FALLOFF))
                .apply(context);

        final var centres = centres(context);
        assertTrue(centres.length > 1, "several islands were placed");
        final var needed = WIDE_SPACING * HALF * RADIUS;
        for (var first = 0; first < centres.length; first++) {
            for (var second = first + 1; second < centres.length; second++) {
                final double dx = (double) context.xOf(centres[first]) - context.xOf(centres[second]);
                final double dy = (double) context.yOf(centres[first]) - context.yOf(centres[second]);
                assertTrue(Math.hypot(dx, dy) >= needed, "centres " + centres[first] + " and " + centres[second]);
            }
        }
    }

    @Test
    void aCrowdedMapDropsTheIslandsThatDoNotFit() {
        final var context = flatHeightContext(SEED_SPACING, CROWDED_SIDE, FLAT_SEA);

        new IslandShapePass(params(NO_DENSITY, MANY_ISLANDS)).apply(context);

        final var placed = centres(context).length;
        assertTrue(placed >= 1 && placed < MANY_ISLANDS, "placed " + placed + " of " + MANY_ISLANDS);
    }

    @Test
    void theRadiiRunFromSmallIsletsToLargeIslandsOnTheSameMap() {
        final var context = flatHeightContext(SEED_SPACING, HUGE_ROOMY_SIDE, FLAT_SEA);
        final var varied = new IslandSize(ISLET_RADIUS, BIG_RADIUS, NO_CAP, SPACING);

        new IslandShapePass(new IslandParams(NO_DENSITY, NINE_ISLANDS, varied, GAIN, LIFT, SEA, FALLOFF))
                .apply(context);

        final var heights = context.field(NoiseField.HEIGHT);
        final var lifted = Regions.label(context.width(), context.height(), index -> heights[index] > -SEA + TOLERANCE);
        final var sizes = lifted.sizes();
        final var smallest = Arrays.stream(sizes).skip(1).min().orElseThrow();
        final var largest = Arrays.stream(sizes).max().orElseThrow();
        assertTrue(lifted.count() >= THREE_ISLANDS, lifted.count() + " islands");
        assertTrue(largest >= ISLAND_SIZE_RATIO * smallest, "largest " + largest + " smallest " + smallest);
    }

    @Test
    void theLargestRadiusIsCappedAtAShareOfTheShorterSideOfTheMap() {
        final var context = flatHeightContext(SEED_CENTRE, HALF_SIDE, FLAT_SEA);
        final var capped = new IslandSize(RADIUS, RADIUS, CAP_SHARE, SPACING);

        new IslandShapePass(new IslandParams(NO_DENSITY, MINIMUM, capped, GAIN, LIFT, SEA, FALLOFF)).apply(context);

        final var heights = context.field(NoiseField.HEIGHT);
        final var centre = highestIndex(context);
        final var cx = context.xOf(centre);
        final var half = RADIUS / HALF;
        final var edgeX = cx + half < HALF_SIDE ? cx + half : cx - half;
        final var insideX = cx + half - 1 < HALF_SIDE ? cx + half - 1 : cx - half + 1;
        assertEquals(-SEA, heights[context.index(edgeX, context.yOf(centre))], TOLERANCE);
        assertTrue(heights[context.index(insideX, context.yOf(centre))] > -SEA + TOLERANCE);
    }

    @Test
    void aMapKeepsTheIslandsTheParametersAskForByShrinkingTheOnesThatDoNotFit() {
        final var large = new IslandSize(ISLET_RADIUS, HUGE_RADIUS, NO_CAP, SPACING);
        final var params = new IslandParams(NO_DENSITY, SIX_ISLANDS, large, GAIN, LIFT, SEA, FALLOFF);
        for (var seed = 1L; seed <= SHRINK_SEEDS; seed++) {
            final var context = flatHeightContext(seed, SIDE, FLAT_SEA);

            new IslandShapePass(params).apply(context);

            assertEquals(SIX_ISLANDS, centres(context).length, "seed " + seed);
        }
    }

    /** The floor of the radius shrinks on a small map, so the minimum of five islands holds there as it does on 300. */
    @Test
    void theMinimumOfFiveIslandsHoldsOnTheSmallMapsToo() {
        final var params = new IslandParams(NO_DENSITY, FLOOR_COUNT, RECIPE_SIZE, GAIN, LIFT, SEA, FALLOFF);
        for (var side : new int[] {SMALLEST_MAP, SMALL, LARGE}) {
            for (var seed = 1L; seed <= FIT_SEEDS; seed++) {
                final var context = flatHeightContext(seed, side, FLAT_SEA);

                new IslandShapePass(params).apply(context);

                assertEquals(FLOOR_COUNT, centres(context).length, "side " + side + " seed " + seed);
            }
        }
    }

    /**
     * A crowded map shrinks every radius and lays the islands out again, so every pair of islands keeps the spacing of
     * the parameters, instead of putting the last island down with its disc touching another. With a spacing of 1.3
     * the lifted land of two islands lies at least a third of the smallest radii apart, so five separate pieces of
     * land are left with a gap of at least three tiles between each two of them. The old last resort, which let the
     * discs touch, left gaps of a tile or none.
     */
    @Test
    void theIslandsOfTheSmallMapsKeepTheSpacingOfTheParametersWhenTheLayoutIsMadeAgain() {
        final var params = new IslandParams(NO_DENSITY, FLOOR_COUNT, SPACED_SIZE, GAIN, LIFT, SEA, FALLOFF);
        for (var side : new int[] {SMALLEST_MAP, SMALL}) {
            for (var seed = 1L; seed <= FIT_SEEDS; seed++) {
                final var context = flatHeightContext(seed, side, FLAT_SEA);

                new IslandShapePass(params).apply(context);

                final var heights = context.field(NoiseField.HEIGHT);
                final var lifted = Regions.label(side, side, index -> heights[index] > -SEA + TOLERANCE);
                final var label = "side " + side + " seed " + seed;
                assertEquals(FLOOR_COUNT, lifted.count(), label);
                assertTrue(closestGap(lifted, side) >= MIN_GAP, label + ": two islands lie " + closestGap(lifted, side)
                        + " tiles apart");
            }
        }
    }

    /** Returns the smallest Chebyshev distance, in tiles, between the land of one island and that of another. */
    private static int closestGap(Regions lifted, int side) {
        var closest = Integer.MAX_VALUE;
        for (var label = 1; label <= lifted.count(); label++) {
            final var own = label;
            final var reach = DistanceTransform.chebyshev(side, side, index -> lifted.labelAt(index) == own);
            for (var index = 0; index < reach.length; index++) {
                final var other = lifted.labelAt(index) != Regions.NO_LABEL && lifted.labelAt(index) != own;
                closest = other ? Math.min(closest, reach[index]) : closest;
            }
        }
        return closest;
    }

    @Test
    void theSizeRefusesRadiiAndSpacingsThatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(0, 1, NO_CAP, SPACING));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(2, 1, NO_CAP, SPACING));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, 0F, SPACING));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, 1.5F, SPACING));
        final var overHalf = Math.nextUp(IslandSize.MAX_SHARE_OF_SIDE);
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, overHalf, SPACING));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, Float.NaN, SPACING));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, NO_CAP, 0F));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, NO_CAP, -SPACING));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, NO_CAP, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new IslandSize(1, 1, NO_CAP, Float.POSITIVE_INFINITY));
    }

    @Test
    void needsTheHeightField() {
        final var context = TestContexts.of(MapType.ISLANDS, SEED_MISSING, SIDE, SIDE, TestContexts.FLAT);
        final var pass = new IslandShapePass(singleIsland());

        assertThrows(IllegalStateException.class, () -> pass.apply(context));
    }

    @Test
    void paramsRefuseUnusableValues() {
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(-1, 1, SIZE, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(IllegalArgumentException.class,
                () -> new IslandParams(OVER_100K, 1, SIZE, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(1, 0, SIZE, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(NullPointerException.class, () -> new IslandParams(1, 1, null, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(1, 1, SIZE, GAIN, LIFT, SEA, 0F));
        assertThrows(IllegalArgumentException.class, () -> new IslandParams(1, 1, SIZE, GAIN, LIFT, SEA, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new IslandParams(1, OVER_MAX_ISLANDS, SIZE, GAIN, LIFT, SEA, FALLOFF));
        assertThrows(IllegalArgumentException.class,
                () -> new IslandParams(1, 1, SIZE, GAIN, LIFT, SEA, Float.POSITIVE_INFINITY));
        assertThrows(NullPointerException.class, () -> new IslandShapePass(null));
    }

    @Test
    void paramsRefuseNonFiniteHeightGainLiftAndSeaOffset() {
        for (var bad : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new IslandParams(1, MINIMUM, SIZE, bad, LIFT, SEA, FALLOFF));
            assertThrows(IllegalArgumentException.class,
                    () -> new IslandParams(1, MINIMUM, SIZE, GAIN, bad, SEA, FALLOFF));
            assertThrows(IllegalArgumentException.class,
                    () -> new IslandParams(1, MINIMUM, SIZE, GAIN, LIFT, bad, FALLOFF));
        }
    }

    @Test
    void namesItselfForTheTimings() {
        assertEquals("island-shape", new IslandShapePass(singleIsland()).name());
    }
}
