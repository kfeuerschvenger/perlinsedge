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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rivers run over small grass maps, with the height and the moisture drawn by hand where a test needs them. */
class RiverCrossingPassTest {

    private static final int SIDE = 60;
    private static final int SMALL_SIDE = 40;
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
    /** The middle strip across the rivers, in which no river runs. */
    private static final double STRIP_START = 0.35;
    private static final double STRIP_END = 0.65;
    private static final int TILES_150 = 150 * 150;
    private static final int TILES_300 = 300 * 300;
    private static final int TILES_HUGE = 1_000 * 1_000;
    private static final int DENSITY = 3;
    private static final int MAX_BOUND = 4;

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
        for (var index = 0; index < field.length; index++) {
            final var inValley = context.xOf(index) == VALLEY || context.yOf(index) == VALLEY;
            field[index] = inValley ? inside : outside;
        }
    }

    /** Grass whose height grows towards the far corner, so the highest land lies far from the rivers of the lanes. */
    private static GenerationContext highEastGrass() {
        final var context = grass(SIDE, SEED);
        final var heights = context.field(NoiseField.HEIGHT);
        for (var index = 0; index < heights.length; index++) {
            heights[index] = (context.xOf(index) + context.yOf(index)) / (float) (TWO * SIDE);
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
