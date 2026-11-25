package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.NoiseField;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rivers run over small grass maps, with the height and the moisture drawn by hand where a test needs them. */
class RiverCrossingPassTest {

    private static final int SIDE = 60;
    private static final int SMALL_SIDE = 40;
    private static final int WIDE_SIDE = 120;
    private static final int TOO_SMALL = 24;
    private static final int MIN_SPAN = 32;
    /** The closest two rivers may come: the band is closed up to the band radius, so the next tile is one farther. */
    private static final int KEEP_OUT = RiverCrossingPass.BAND_RADIUS + 1;
    /** The tiles of a square of three by three, the one in the middle included. */
    private static final int NEIGHBOURS = 9;
    /** The index of the middle tile of that square along each axis, taken off to get the step from the middle. */
    private static final int SQUARE_CENTRE = 1;
    /** The steps to the four tiles beside a tile: left, right, up and down. */
    private static final int[] STEP_X = {-1, 1, 0, 0};
    private static final int[] STEP_Y = {0, 0, -1, 1};
    private static final int MIN_VALLEY_TILES = 40;
    /** A line between the two slots of the first lane, which both rivers of that lane would like to follow. */
    private static final int SHARED_VALLEY = 10;
    private static final int THREE = 3;
    /** The tiles along the rivers from the start of the map that are water in the test of the dead lane. */
    private static final int DEAD_LANE = 26;
    private static final long SEED = 7L;
    private static final long OTHER_SEED = 99L;
    private static final float FLAT = 0.5F;
    private static final float HIGH = 1F;
    private static final float LOW = 0F;
    /** The column and the row that the valley and the wet ground follow, inside the first lane of the rivers. */
    private static final int VALLEY = 7;
    private static final int NO_PER_AREA = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int NO_TRIBUTARIES = 0;
    private static final float STRONG = 8F;
    private static final float NONE = 0F;
    private static final double MOST = 0.7;
    /** Half the side of the pond in the middle of the map. */
    private static final int POND_HALF = 5;
    /** The rows of water along every edge in the test of a map whose edges hold no land: deeper than a river looks. */
    private static final int WATER_RIM = 12;
    /** The middle of the strip across the rivers, which no river crosses even when it may enter the strip's edges. */
    private static final double STRIP_START = 0.35;
    private static final double STRIP_END = 0.65;
    /** The tributaries to a river in the tests of the tributaries, and the seeds those tests run over. */
    private static final int TRIBUTARIES = 3;
    private static final long SEEDS = 5L;
    private static final int TILES_150 = 150 * 150;
    private static final int TILES_300 = 300 * 300;
    private static final int TILES_HUGE = 1_000 * 1_000;
    private static final int DENSITY = 3;
    private static final int MAX_BOUND = 4;
    /** The side of the small grid of the tests of the band, and its middle row and column: room for the band around. */
    private static final int BAND_GRID = 15;
    private static final int BAND_CENTRE = 7;
    /** The numbers of the two rivers that carved the grid of the tests of the band. */
    private static final int FIRST_RIVER = 1;
    private static final int SECOND_RIVER = 2;
    /** The map that squeezes two rivers within reach of one band: a rolling height field with a valley dug in it. */
    private static final int ROLLING_SEED = 90;
    private static final int ROLLING_RIVERS = 3;
    private static final int ROLLING_STREAMS = 3;
    private static final int ROLLING_SEED_MIX = 31;
    private static final int WAVE_RANGE = 6;
    private static final double WAVE_TURNS = 3.0;
    private static final double WAVE_BASE = 0.5;
    private static final double WAVE_AMPLITUDE = 0.3;
    private static final int VALLEY_HALF_WIDTH = 1;

    private static GenerationContext grass(int side, long seed) {
        final var context = TestContexts.of(MapType.RIVERS, seed, side, side, TestContexts.FLAT);
        TestContexts.fillField(context, NoiseField.HEIGHT, FLAT);
        TestContexts.fillGround(context, TileType.GRASS);
        return context;
    }

    private static RiverCrossingParams rivers(int count, float heightWeight, float meanderWeight, int tributaries) {
        return new RiverCrossingParams(NO_PER_AREA, count, count, heightWeight, meanderWeight, tributaries);
    }

    private static void apply(GenerationContext context, RiverCrossingParams params) {
        new RiverCrossingPass(params).apply(context);
    }

    private static int riverComponents(GenerationContext context) {
        return Regions.label(context.width(), context.height(),
                index -> context.groundAt(index) == TileType.RIVER).count();
    }

    private static boolean isRiver(GenerationContext context, int index) {
        return context.groundAt(index) == TileType.RIVER;
    }

    /** Tells whether a river tile lies in the given column, which a river that runs left to right does at the edge. */
    private static boolean riverTouchesColumn(GenerationContext context, int column) {
        for (var y = 0; y < context.height(); y++) {
            if (isRiver(context, context.index(column, y))) {
                return true;
            }
        }
        return false;
    }

    /** Tells whether a river tile lies in the given row, which a river that runs top to bottom does at the edge. */
    private static boolean riverTouchesRow(GenerationContext context, int row) {
        for (var x = 0; x < context.width(); x++) {
            if (isRiver(context, context.index(x, row))) {
                return true;
            }
        }
        return false;
    }

    /** Counts the river tiles that lie within a tile of the valley column or the valley row. */
    private static int riverTilesInTheValley(GenerationContext context) {
        var inside = 0;
        for (var index = 0; index < context.size(); index++) {
            final var onColumn = Math.abs(context.xOf(index) - VALLEY) <= 1;
            final var onRow = Math.abs(context.yOf(index) - VALLEY) <= 1;
            inside += isRiver(context, index) && (onColumn || onRow) ? 1 : 0;
        }
        return inside;
    }

    /** Sets a column and a row of a field to one value and the rest to another, so a river has a valley either way. */
    private static void digValley(float[] field, GenerationContext context, float inside, float outside) {
        digValleyAt(field, context, VALLEY, inside, outside);
    }

    /** Sets the column and the row of a line to one value and the rest to another. */
    private static void digValleyAt(float[] field, GenerationContext context, int line, float inside,
                                    float outside) {
        for (var index = 0; index < field.length; index++) {
            final var inValley = context.xOf(index) == line || context.yOf(index) == line;
            field[index] = inValley ? inside : outside;
        }
    }

    /** Tells whether the rivers run from left to right on a map of this seed, which the seed decides. */
    private static boolean runsLeftToRight(long seed) {
        final var probe = grass(SIDE, seed);
        apply(probe, rivers(ONE, NONE, NONE, NO_TRIBUTARIES));
        return riverTouchesColumn(probe, 0);
    }

    /** Returns the smallest Chebyshev distance between a tile of one river and a tile of another river. */
    private static int distanceBetweenRivers(GenerationContext context) {
        final var labels = Regions.label(context.width(), context.height(), index -> isRiver(context, index));
        var closest = Integer.MAX_VALUE;
        for (var label = 1; label <= labels.count(); label++) {
            final var own = label;
            final var reach = DistanceTransform.chebyshev(context.width(), context.height(),
                    index -> labels.labelAt(index) == own);
            for (var index = 0; index < context.size(); index++) {
                final var other = labels.labelAt(index) != Regions.NO_LABEL && labels.labelAt(index) != own;
                closest = other ? Math.min(closest, reach[index]) : closest;
            }
        }
        return closest;
    }

    /** Returns the labels of the rivers with a tile at most one tile from a tile, the tile's own label included. */
    private static Set<Integer> riversBeside(GenerationContext context, int[] rivers, int tile) {
        final var beside = new HashSet<Integer>();
        for (var dy = -1; dy <= 1; dy++) {
            for (var dx = -1; dx <= 1; dx++) {
                final var x = context.xOf(tile) + dx;
                final var y = context.yOf(tile) + dy;
                if (isOnGrid(context, x, y) && rivers[context.index(x, y)] != Regions.NO_LABEL) {
                    beside.add(rivers[context.index(x, y)]);
                }
            }
        }
        return beside;
    }

    /**
     * Labels the 8-connected pieces of the tiles a mask selects, from 1 in reading order, with 0 for the others. A
     * stream touching another one at a corner is one piece, which the 4-connected {@link Regions} would split.
     */
    private static int[] labelEightConnected(GenerationContext context, IntPredicate mask) {
        final var labels = new int[context.size()];
        final var queue = new int[context.size()];
        var count = 0;
        for (var start = 0; start < labels.length; start++) {
            if (labels[start] == Regions.NO_LABEL && mask.test(start)) {
                count++;
                floodEightConnected(context, mask, labels, queue, start, count);
            }
        }
        return labels;
    }

    /** Gives a label to the start tile and to every selected tile joined to it along the eight directions. */
    private static void floodEightConnected(GenerationContext context, IntPredicate mask, int[] labels, int[] queue,
                                            int start, int label) {
        var head = 0;
        var tail = 1;
        labels[start] = label;
        queue[0] = start;
        while (head < tail) {
            final var tile = queue[head];
            head++;
            for (var around = 0; around < NEIGHBOURS; around++) {
                final var x = context.xOf(tile) + around % THREE - SQUARE_CENTRE;
                final var y = context.yOf(tile) + around / THREE - SQUARE_CENTRE;
                if (isOnGrid(context, x, y) && labels[context.index(x, y)] == Regions.NO_LABEL
                        && mask.test(context.index(x, y))) {
                    labels[context.index(x, y)] = label;
                    queue[tail] = context.index(x, y);
                    tail++;
                }
            }
        }
    }

    /** Tells whether a column and a row lie on the grid of the context. */
    private static boolean isOnGrid(GenerationContext context, int x, int y) {
        return x >= 0 && y >= 0 && x < context.width() && y < context.height();
    }

    /** Tells whether a tile of a river lies in the closed middle strip, with the bounds of the pass. */
    private static boolean inTheStrip(GenerationContext context, boolean leftToRight, int index) {
        final var along = leftToRight ? context.yOf(index) : context.xOf(index);
        final var span = leftToRight ? context.height() : context.width();
        final var share = along / (double) (span - 1);
        return share >= RiverCrossingPass.CORRIDOR_START && share <= RiverCrossingPass.CORRIDOR_END;
    }

    /** Returns the number of river tiles of each component, from the first component on. */
    private static int[] riverSizes(GenerationContext context) {
        return Regions.label(context.width(), context.height(), index -> isRiver(context, index)).sizes();
    }

    /** Grass wide enough for a tributary, whose height grows towards the far corner so the land has high places. */
    private static GenerationContext highEastGrass() {
        return highEastGrass(SEED);
    }

    /** The same grass for a seed, which decides the direction of the rivers and where they run. */
    private static GenerationContext highEastGrass(long seed) {
        final var context = grass(WIDE_SIDE, seed);
        final var heights = context.field(NoiseField.HEIGHT);
        for (var index = 0; index < heights.length; index++) {
            heights[index] = (context.xOf(index) + context.yOf(index)) / (float) (TWO * WIDE_SIDE);
        }
        return context;
    }

    @Test
    void aRiverRunsFromOneEdgeToTheOppositeOne() {
        final var context = grass(SMALL_SIDE, SEED);

        apply(context, rivers(ONE, NONE, NONE, NO_TRIBUTARIES));

        final var last = SMALL_SIDE - 1;
        assertEquals(ONE, riverComponents(context));
        final var topToBottom = riverTouchesRow(context, 0) && riverTouchesRow(context, last);
        final var leftToRight = riverTouchesColumn(context, 0) && riverTouchesColumn(context, last);
        assertTrue(topToBottom || leftToRight, "the river touches both edges it crosses between");
    }

    @Test
    void riversNeverTurnWaterIntoRiverAndGoAroundIt() {
        final var context = grass(SIDE, SEED);
        final var middle = SIDE / TWO;
        for (var y = middle - POND_HALF; y < middle + POND_HALF; y++) {
            for (var x = middle - POND_HALF; x < middle + POND_HALF; x++) {
                context.setGround(context.index(x, y), TileType.SHALLOW_WATER);
            }
        }

        apply(context, rivers(TWO, NONE, NONE, NO_TRIBUTARIES));

        for (var y = middle - POND_HALF; y < middle + POND_HALF; y++) {
            for (var x = middle - POND_HALF; x < middle + POND_HALF; x++) {
                assertEquals(TileType.SHALLOW_WATER, context.groundAt(context.index(x, y)));
            }
        }
        assertTrue(TestContexts.countGround(context, TileType.RIVER) > 0);
    }

    @Test
    void severalRiversStaySeparateAndKeepOutOfTheMiddleStrip() {
        final var context = grass(SIDE, SEED);

        apply(context, rivers(TWO, NONE, NONE, NO_TRIBUTARIES));

        assertEquals(TWO, riverComponents(context));
        final var leftToRight = riverTouchesColumn(context, 0);
        for (var index = 0; index < context.size(); index++) {
            if (isRiver(context, index)) {
                final var along = leftToRight ? context.yOf(index) : context.xOf(index);
                final var share = along / (double) (SIDE - 1);
                assertFalse(share > STRIP_START && share < STRIP_END, "a river tile at " + index + " is in the strip");
            }
        }
    }

    @Test
    void laterRiversNeverMergeWithEarlierOnesEvenWhenOneValleyCallsThemAll() {
        final var context = grass(SIDE, SEED);
        digValleyAt(context.field(NoiseField.HEIGHT), context, SHARED_VALLEY, LOW, HIGH);
        final var leftToRight = runsLeftToRight(SEED);

        apply(context, rivers(THREE, STRONG, NONE, NO_TRIBUTARIES));

        assertEquals(THREE, riverComponents(context));
        assertTrue(distanceBetweenRivers(context) >= KEEP_OUT, "the rivers keep their distance");
        var inTheValley = 0;
        for (var index = 0; index < context.size(); index++) {
            final var line = leftToRight ? context.yOf(index) : context.xOf(index);
            inTheValley += isRiver(context, index) && Math.abs(line - SHARED_VALLEY) <= 1 ? 1 : 0;
        }
        assertTrue(inTheValley >= MIN_VALLEY_TILES, "a river bends to the valley: " + inTheValley + " tiles");
    }

    @Test
    void aRiverThatCannotConnectItsLaneIsTriedAgainSoTheMapStillReachesTheMinimum() {
        final var leftToRight = runsLeftToRight(SEED);
        final var context = grass(SIDE, SEED);
        for (var index = 0; index < context.size(); index++) {
            final var along = leftToRight ? context.yOf(index) : context.xOf(index);
            if (along <= DEAD_LANE) {
                context.setGround(index, TileType.DEEP_WATER);
            }
        }

        apply(context, rivers(TWO, NONE, NONE, NO_TRIBUTARIES));

        assertEquals(TWO, riverComponents(context));
    }

    @Test
    void aMapTooSmallForARiverGetsNone() {
        final var tiny = grass(TOO_SMALL, SEED);
        final var smallest = grass(MIN_SPAN, SEED);

        apply(tiny, rivers(ONE, NONE, NONE, ONE));
        apply(smallest, rivers(ONE, NONE, NONE, ONE));

        assertEquals(0, TestContexts.countGround(tiny, TileType.RIVER));
        assertTrue(TestContexts.countGround(smallest, TileType.RIVER) > 0);
    }

    @Test
    void everyRiverGetsItsOwnTributariesAndTheyNeverCrossTheMiddleStrip() {
        final var without = highEastGrass();
        final var with = highEastGrass();

        apply(without, rivers(TWO, NONE, NONE, NO_TRIBUTARIES));
        apply(with, rivers(TWO, NONE, NONE, TWO));

        assertEquals(TWO, riverComponents(with));
        final var before = riverSizes(without);
        final var after = riverSizes(with);
        for (var river = 1; river <= TWO; river++) {
            assertTrue(after[river] > before[river], "river " + river + " grew from " + before[river]);
        }
        final var leftToRight = riverTouchesColumn(with, 0);
        for (var index = 0; index < with.size(); index++) {
            if (isRiver(with, index) && !isRiver(without, index)) {
                assertFalse(inTheStrip(with, leftToRight, index), "a tributary tile at " + index);
            }
        }
    }

    /** Tributaries are carved clear of every other river, so the rivers of a map keep their distance with them. */
    @Test
    void tributariesNeverComeWithinTheKeepOutBandOfAnotherRiver() {
        for (var seed = 1L; seed <= SEEDS; seed++) {
            final var context = highEastGrass(seed);

            apply(context, rivers(TWO, NONE, NONE, TRIBUTARIES));

            assertEquals(TWO, riverComponents(context), "seed " + seed);
            assertTrue(distanceBetweenRivers(context) >= KEEP_OUT,
                    "seed " + seed + ": rivers come within " + distanceBetweenRivers(context));
        }
    }

    /** Sets a rolling height field of waves, with a valley of height zero along one column and one row. */
    private static void rollWithValley(GenerationContext context, long seed) {
        final var random = new Random(seed * ROLLING_SEED_MIX + context.width());
        final var waveX = random.nextDouble() * WAVE_RANGE;
        final var waveY = random.nextDouble() * WAVE_RANGE;
        final var valley = random.nextInt(context.width());
        final var heights = context.field(NoiseField.HEIGHT);
        for (var index = 0; index < heights.length; index++) {
            final var x = context.xOf(index);
            final var y = context.yOf(index);
            final var inValley = Math.abs(x - valley) <= VALLEY_HALF_WIDTH || Math.abs(y - valley) <= VALLEY_HALF_WIDTH;
            final var wave = Math.sin(x * waveX / context.width() * WAVE_TURNS)
                    * Math.cos(y * waveY / context.height() * WAVE_TURNS);
            heights[index] = inValley ? LOW : (float) (WAVE_BASE + WAVE_AMPLITUDE * wave);
        }
    }

    /**
     * The tiles of a river's band that also lie within the band radius of a river carved later stay closed to the
     * first river's tributaries. The band belongs to the first river to reach it, so that claim does not imply any
     * clearance from the later river: two rivers a little more than the band radius apart share tiles between them.
     * Without the clearance check a tributary runs through those tiles and joins the two rivers into one.
     */
    @Test
    void aTributaryNeverRunsThroughTheBandSharedWithALaterRiver() {
        final var context = grass(WIDE_SIDE, ROLLING_SEED);
        rollWithValley(context, ROLLING_SEED);

        apply(context, rivers(ROLLING_RIVERS, STRONG, NONE, ROLLING_STREAMS));

        assertEquals(ROLLING_RIVERS, riverComponents(context), "no tributary joins two rivers");
    }

    /**
     * Returns the river of each tile of a small grid with the tiles of the rivers set: the first river at the first
     * tile, the second at the second one.
     */
    private static int[] gridWithRivers(int firstTile, int secondTile) {
        final var riverOf = new int[BAND_GRID * BAND_GRID];
        riverOf[firstTile] = FIRST_RIVER;
        riverOf[secondTile] = SECOND_RIVER;
        return riverOf;
    }

    /**
     * The land of the band of a river that lies within the band radius of another river stays closed to its
     * tributaries: a tile is near another river when a tile of it lies within the band radius in any direction, the
     * corners of the square included.
     */
    @Test
    void aTileWithinTheBandRadiusOfAnotherRiverIsNearItInEveryDirection() {
        final var tile = BAND_GRID * BAND_CENTRE + BAND_CENTRE;
        final var radius = RiverCrossingPass.BAND_RADIUS;
        final var farCorner = 0;

        for (var dy = -radius; dy <= radius; dy += radius) {
            for (var dx = -radius; dx <= radius; dx += radius) {
                final var other = tile + dy * BAND_GRID + dx;
                assertTrue(RiverCrossingPass.hasOtherRiverNear(gridWithRivers(farCorner, other), BAND_GRID, BAND_GRID,
                        tile, FIRST_RIVER), "the other river lies at " + dx + "," + dy);
            }
        }
    }

    /** The other river one tile beyond the band radius is not near, and the owner's own river does not count. */
    @Test
    void aTileBeyondTheBandRadiusOrBesideOnlyItsOwnRiverIsNotNearAnotherRiver() {
        final var tile = BAND_GRID * BAND_CENTRE + BAND_CENTRE;
        final var beyond = tile + RiverCrossingPass.BAND_RADIUS + 1;
        final var beside = tile + 1;
        final var farCorner = 0;

        assertFalse(RiverCrossingPass.hasOtherRiverNear(gridWithRivers(farCorner, beyond), BAND_GRID, BAND_GRID, tile,
                FIRST_RIVER), "the other river lies one tile beyond the radius");
        assertFalse(RiverCrossingPass.hasOtherRiverNear(gridWithRivers(beside, farCorner), BAND_GRID, BAND_GRID, tile,
                FIRST_RIVER), "the river beside the tile is the owner's own");
    }

    /** The scan stops at the edge of the map: a tile in the corner sees the other river inside the grid only. */
    @Test
    void theScanForAnotherRiverStopsAtTheEdgeOfTheMap() {
        final var corner = 0;
        final var lastTile = BAND_GRID * BAND_GRID - 1;
        final var inReach = RiverCrossingPass.BAND_RADIUS * BAND_GRID + RiverCrossingPass.BAND_RADIUS;

        assertFalse(RiverCrossingPass.hasOtherRiverNear(gridWithRivers(lastTile, lastTile - 1), BAND_GRID, BAND_GRID,
                corner, FIRST_RIVER), "the far corner of the grid is out of reach");
        assertTrue(RiverCrossingPass.hasOtherRiverNear(gridWithRivers(lastTile, inReach), BAND_GRID, BAND_GRID, corner,
                FIRST_RIVER), "the other river at the radius from the corner is near");
    }

    /**
     * Every tributary touches one river and only one, the one it belongs to, and its far end is at least the minimum
     * reach from the rivers and at most the maximum reach from that river.
     */
    @Test
    void everyTributaryJoinsOneRiverAndStartsBetweenTheMinimumAndTheMaximumReach() {
        for (var seed = 1L; seed <= SEEDS; seed++) {
            final var without = highEastGrass(seed);
            final var with = highEastGrass(seed);
            apply(without, rivers(TWO, NONE, NONE, NO_TRIBUTARIES));
            apply(with, rivers(TWO, NONE, NONE, TRIBUTARIES));
            final var rivers = labelEightConnected(without, index -> isRiver(without, index));
            final var reach = DistanceTransform.chebyshev(WIDE_SIDE, WIDE_SIDE, index -> isRiver(without, index));
            final var streams = labelEightConnected(with, index -> isRiver(with, index) && !isRiver(without, index));
            final var streamCount = Arrays.stream(streams).max().orElse(0);

            assertTrue(streamCount >= TWO, "seed " + seed + ": " + streamCount + " streams");
            for (var stream = 1; stream <= streamCount; stream++) {
                var farthest = 0;
                final var joined = new HashSet<Integer>();
                for (var index = 0; index < with.size(); index++) {
                    if (streams[index] == stream) {
                        farthest = Math.max(farthest, reach[index]);
                        joined.addAll(riversBeside(with, rivers, index));
                    }
                }
                final var label = "seed " + seed + " stream " + stream;
                assertEquals(1, joined.size(), label + " joins " + joined);
                assertTrue(farthest >= RiverCrossingPass.TRIBUTARY_MIN_REACH, label + " ends at " + farthest);
                assertTrue(farthest <= RiverCrossingPass.TRIBUTARY_MAX_REACH, label + " ends at " + farthest);
            }
        }
    }

    /**
     * The sources of the tributaries are checked again after each one is carved, so a source keeps the minimum reach
     * from every tributary carved before it. The free end of a stream, a tile with a single river tile beside it, is
     * where it starts, so no two free ends lie closer than the minimum reach. Without the new check the sources of the
     * highest land, which lie close together, start streams that begin beside each other and merge.
     */
    @Test
    void aTributarySourceIsCheckedAgainAfterEachCarveSoNoTwoStreamsStartBesideEachOther() {
        var withSeveralStreams = 0;
        for (var seed = 1L; seed <= SEEDS; seed++) {
            final var without = highEastGrass(seed);
            final var with = highEastGrass(seed);
            apply(without, rivers(ONE, NONE, NONE, NO_TRIBUTARIES));
            apply(with, rivers(ONE, NONE, NONE, TRIBUTARIES));

            final var ends = freeEnds(without, with);

            withSeveralStreams += ends.size() > 1 ? 1 : 0;
            for (var first = 0; first < ends.size(); first++) {
                for (var second = first + 1; second < ends.size(); second++) {
                    final var apart = Math.max(
                            Math.abs(with.xOf(ends.get(first)) - with.xOf(ends.get(second))),
                            Math.abs(with.yOf(ends.get(first)) - with.yOf(ends.get(second))));
                    assertTrue(apart >= RiverCrossingPass.TRIBUTARY_MIN_REACH,
                            "seed " + seed + ": two streams start " + apart + " apart");
                }
            }
        }
        assertTrue(withSeveralStreams > 0, "some seed gives a river more than one stream");
    }

    /**
     * Returns the free ends of the streams a pass added to a river: the new river tiles that have a single river tile
     * beside them along the four directions, which is the tile a stream starts from.
     */
    private static List<Integer> freeEnds(GenerationContext without, GenerationContext with) {
        final var ends = new ArrayList<Integer>();
        for (var index = 0; index < with.size(); index++) {
            if (isRiver(with, index) && !isRiver(without, index) && riverNeighbours(with, index) == 1) {
                ends.add(index);
            }
        }
        return ends;
    }

    /** Counts the river tiles beside a tile along the four directions. */
    private static int riverNeighbours(GenerationContext context, int tile) {
        var count = 0;
        for (var around = 0; around < STEP_X.length; around++) {
            final var x = context.xOf(tile) + STEP_X[around];
            final var y = context.yOf(tile) + STEP_Y[around];
            count += isOnGrid(context, x, y) && isRiver(context, context.index(x, y)) ? 1 : 0;
        }
        return count;
    }

    @Test
    void aRiverFollowsTheLowGroundWhenHeightMatters() {
        final var context = grass(SIDE, SEED);
        digValley(context.field(NoiseField.HEIGHT), context, LOW, HIGH);

        apply(context, rivers(ONE, STRONG, NONE, NO_TRIBUTARIES));

        final var total = TestContexts.countGround(context, TileType.RIVER);
        assertTrue(riverTilesInTheValley(context) > MOST * total,
                riverTilesInTheValley(context) + " of " + total + " river tiles lie in the valley");
    }

    @Test
    void aRiverBendsTowardsTheWetGroundWhenTheMeanderWeightIsHigh() {
        final var context = grass(SIDE, SEED);
        final var moisture = context.defineField(NoiseField.MOISTURE);
        digValley(moisture, context, HIGH, LOW);

        apply(context, rivers(ONE, NONE, STRONG, NO_TRIBUTARIES));

        final var total = TestContexts.countGround(context, TileType.RIVER);
        assertTrue(riverTilesInTheValley(context) > MOST * total,
                riverTilesInTheValley(context) + " of " + total + " river tiles lie in the wet ground");
    }

    @Test
    void tributariesJoinTheRiversFromHighLandAndAddRiverTilesWithoutNewComponents() {
        final var without = highEastGrass();
        final var with = highEastGrass();

        apply(without, rivers(ONE, NONE, NONE, NO_TRIBUTARIES));
        apply(with, rivers(ONE, NONE, NONE, TWO));

        assertEquals(ONE, riverComponents(without));
        assertEquals(ONE, riverComponents(with));
        assertTrue(TestContexts.countGround(with, TileType.RIVER) > TestContexts.countGround(without, TileType.RIVER));
    }

    @Test
    void aMapWithNoLandGetsNoRiverAndThrowsNothing() {
        final var context = grass(SMALL_SIDE, SEED);
        TestContexts.fillGround(context, TileType.DEEP_WATER);

        apply(context, rivers(TWO, NONE, NONE, ONE));

        assertEquals(context.size(), TestContexts.countGround(context, TileType.DEEP_WATER));
    }

    @Test
    void whenTheEdgesHoldNoLandNearTheLanesNoRiverIsCut() {
        final var context = grass(SIDE, SEED);
        for (var index = 0; index < context.size(); index++) {
            final var fromLeftOrTop = Math.min(context.xOf(index), context.yOf(index));
            final var fromRightOrBottom = Math.min(SIDE - 1 - context.xOf(index), SIDE - 1 - context.yOf(index));
            if (Math.min(fromLeftOrTop, fromRightOrBottom) < WATER_RIM) {
                context.setGround(index, TileType.DEEP_WATER);
            }
        }

        apply(context, rivers(TWO, NONE, NONE, ONE));

        assertEquals(0, TestContexts.countGround(context, TileType.RIVER));
    }

    @Test
    void whenWaterCutsTheLandInFourNoRiverCanCrossAndNoneIsCut() {
        final var context = grass(SIDE, SEED);
        for (var i = 0; i < SIDE; i++) {
            context.setGround(context.index(i, SIDE / TWO), TileType.DEEP_WATER);
            context.setGround(context.index(SIDE / TWO, i), TileType.DEEP_WATER);
        }

        apply(context, rivers(TWO, NONE, NONE, ONE));

        assertEquals(0, TestContexts.countGround(context, TileType.RIVER));
    }

    @Test
    void theSameSeedCutsTheSameRiversAndAnotherSeedCutsOthers() {
        final var first = grass(SIDE, SEED);
        final var second = grass(SIDE, SEED);
        final var other = grass(SIDE, OTHER_SEED);

        apply(first, rivers(TWO, NONE, NONE, ONE));
        apply(second, rivers(TWO, NONE, NONE, ONE));
        apply(other, rivers(TWO, NONE, NONE, ONE));

        assertArrayEquals(first.ground(), second.ground());
        assertNotEquals(Arrays.asList(first.ground()), Arrays.asList(other.ground()));
    }

    @Test
    void theNumberOfRiversScalesWithTheAreaWithinTheBounds() {
        final var pass = new RiverCrossingPass(new RiverCrossingParams(DENSITY, TWO, MAX_BOUND, NONE, NONE, 0));

        assertEquals(TWO, pass.riverCount(TILES_150));
        assertEquals(DENSITY, pass.riverCount(TILES_300));
        assertEquals(MAX_BOUND, pass.riverCount(TILES_HUGE));
    }

    @Test
    void theParametersRefuseNumbersThatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new RiverCrossingParams(-1, 1, 1, NONE, NONE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RiverCrossingParams(RiverCrossingParams.MAX_RIVERS_PER_100K + 1, 1, 1, NONE, NONE, 0));
        assertThrows(IllegalArgumentException.class, () -> new RiverCrossingParams(1, 0, 1, NONE, NONE, 0));
        assertThrows(IllegalArgumentException.class, () -> new RiverCrossingParams(1, TWO, ONE, NONE, NONE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RiverCrossingParams(1, 1, RiverCrossingParams.MAX_RIVERS + 1, NONE, NONE, 0));
        assertThrows(IllegalArgumentException.class, () -> new RiverCrossingParams(1, 1, 1, -1F, NONE, 0));
        assertThrows(IllegalArgumentException.class, () -> new RiverCrossingParams(1, 1, 1, NONE, Float.NaN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RiverCrossingParams(1, 1, 1, NONE, Float.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class, () -> new RiverCrossingParams(1, 1, 1, NONE, NONE, -1));
        assertThrows(IllegalArgumentException.class, () -> new RiverCrossingParams(1, 1, 1, NONE, NONE,
                RiverCrossingParams.MAX_TRIBUTARIES + 1));
        assertThrows(NullPointerException.class, () -> new RiverCrossingPass(null));
    }

    @Test
    void thePassIsNamed() {
        assertEquals("river-crossing", new RiverCrossingPass(rivers(ONE, NONE, NONE, 0)).name());
    }
}
