package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.support.TestContexts;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The clearings open in maps full of trees, so what each one cleared and reserved can be told from what is left. */
class ClearingPassTest {

    private static final int SIDE = 120;
    private static final int SPACING = 30;
    private static final int MIN_RADIUS = 3;
    private static final int MAX_RADIUS = 5;
    private static final int SPAWN_RADIUS = 7;
    private static final int CLEARANCE = 4;
    private static final int NO_REGIONS = 0;
    private static final int HALF = 2;
    private static final long SEED = 11L;
    private static final long OTHER_SEED = 12L;
    /** A strip of land 9 tiles wide between two walls of water 9 tiles wide, 40 rows high: 5 tiles from either wall. */
    private static final int STRIP_WIDTH = 9;
    private static final int STRIP_COUNT = 3;
    private static final int STRIP_ROWS = 40;
    private static final int STRIP_MIDDLE = 4;
    /** A band of land 24 columns wide, from column 48 to 71, in the middle of the square map of the test of spacing. */
    private static final int LAND_FIRST = 48;
    private static final int LAND_LAST = 71;
    private static final int MIN_SITES_ON_THE_STRIP = 2;
    /** A square map with a wall of water at column 30 and a strip of land 3 columns wide at columns 31 to 33. */
    private static final int WALL_SIDE = 60;
    private static final int WALL_COLUMN = 30;
    private static final int NARROW_FIRST = 31;
    private static final int NARROW_LAST = 33;
    private static final int NARROW_MIDDLE = 32;
    private static final int NARROW_TILES = WALL_SIDE * (NARROW_LAST - NARROW_FIRST + 1);
    private static final int MIN_REGION = 30;
    private static final int SIDES_PER_SPACING = 3;
    /** The box of tiles reserved before the pass around the middle of the map, 11 tiles across. */
    private static final int HELD_RADIUS = 5;
    /** A map of 32 by 32 tiles, whose spacing cannot be the 45 of a large map, and the spacing of a large map. */
    private static final int SMALL_SIDE = 32;
    private static final int LARGE_SPACING = 45;
    /** A map whose sites at the spacing of 1 would pass the most the trails can join. */
    private static final int CROWDED_SIDE = 200;

    private static ClearingParams params(int clearance, int minRegionTiles) {
        return new ClearingParams(SPACING, MIN_RADIUS, MAX_RADIUS, SPAWN_RADIUS, clearance, minRegionTiles);
    }

    /** Builds a map of grass of the given size with a tree on every tile. */
    private static GenerationContext forest(int width, int height, long seed) {
        final var context = TestContexts.of(MapType.DENSE_FOREST, seed, width, height, TestContexts.FLAT);
        TestContexts.fillGround(context, TileType.GRASS);
        for (var index = 0; index < context.size(); index++) {
            context.setResource(index, ResourceType.TREE);
        }
        return context;
    }

    private static void apply(GenerationContext context, ClearingParams params) {
        new ClearingPass(params).apply(context);
    }

    private static boolean isWithin(GenerationContext context, int tile, int centre, int radius) {
        final var dx = context.xOf(tile) - context.xOf(centre);
        final var dy = context.yOf(tile) - context.yOf(centre);
        return dx * dx + dy * dy <= radius * radius;
    }

    @Test
    void everySiteIsAClearedAndReservedDiscAndTheRestOfTheForestIsUntouched() {
        final var context = forest(SIDE, SIDE, SEED);

        apply(context, params(0, NO_REGIONS));

        assertTrue(context.sites().size() > 1, "the map holds several clearings");
        for (var tile = 0; tile < context.size(); tile++) {
            var inSomeDisc = false;
            for (var site = 0; site < context.sites().size(); site++) {
                final var radius = site == 0 ? SPAWN_RADIUS : MAX_RADIUS;
                inSomeDisc |= isWithin(context, tile, context.sites().get(site), radius);
            }
            if (context.isReserved(tile)) {
                assertTrue(inSomeDisc, "a reserved tile lies in a clearing: " + tile);
                assertNull(context.resourceAt(tile));
            } else {
                assertEquals(ResourceType.TREE, context.resourceAt(tile), "tile " + tile);
            }
        }
    }

    @Test
    void theCentreOfEveryDiscIsClearedWithTheRadiusOfItsKind() {
        final var context = forest(SIDE, SIDE, SEED);

        apply(context, params(0, NO_REGIONS));

        final var spawn = context.sites().get(0);
        for (var tile = 0; tile < context.size(); tile++) {
            if (isWithin(context, tile, spawn, SPAWN_RADIUS)) {
                assertTrue(context.isReserved(tile), "the spawn clearing is whole at " + tile);
            }
        }
        for (var site = 1; site < context.sites().size(); site++) {
            final var centre = context.sites().get(site);
            for (var tile = 0; tile < context.size(); tile++) {
                if (isWithin(context, tile, centre, MIN_RADIUS)) {
                    assertTrue(context.isReserved(tile), "an ordinary clearing is whole at " + tile);
                }
            }
        }
    }

    @Test
    void theSpawnClearingIsCentredOnTheMiddleOfAnOpenMapAndSetsTheSpawnHint() {
        final var context = forest(SIDE, SIDE, SEED);

        apply(context, params(CLEARANCE, NO_REGIONS));

        final var middle = context.index(SIDE / HALF, SIDE / HALF);
        assertEquals(middle, context.sites().get(0));
        assertEquals(middle, context.spawnHint());
    }

    @Test
    void sitesAreSpacedApartAndKeepTheirClearanceFromWater() {
        final var context = forest(SIDE, SIDE, SEED);
        for (var y = 0; y < SIDE; y++) {
            for (var x = 0; x < SIDE; x++) {
                if (x < LAND_FIRST || x > LAND_LAST) {
                    context.setGround(context.index(x, y), TileType.SHALLOW_WATER);
                }
            }
        }

        apply(context, params(CLEARANCE, NO_REGIONS));

        final var sites = context.sites();
        assertTrue(sites.size() > MIN_SITES_ON_THE_STRIP, "the strip holds several sites: " + sites.size());
        for (var first = 0; first < sites.size(); first++) {
            final var x = context.xOf(sites.get(first));
            assertTrue(x >= LAND_FIRST + CLEARANCE && x <= LAND_LAST - CLEARANCE, "clear of the water: site " + first);
        }
        assertSpacedApart(context, SPACING);
    }

    /**
     * Asserts every pair of ordinary sites, which are all but the first, which is the spawn clearing, is at least the
     * spacing apart, and that the spawn clearing is at least half the spacing from each.
     */
    private static void assertSpacedApart(GenerationContext context, int spacing) {
        final var sites = context.sites();
        for (var first = 0; first < sites.size(); first++) {
            for (var second = first + 1; second < sites.size(); second++) {
                final var dx = context.xOf(sites.get(first)) - context.xOf(sites.get(second));
                final var dy = context.yOf(sites.get(first)) - context.yOf(sites.get(second));
                final var least = first == 0 ? spacing / HALF : spacing;
                assertTrue(dx * dx + dy * dy >= least * least, "spaced apart: " + first + " and " + second);
            }
        }
    }

    @Test
    void waterTilesAreNeverReservedOrChanged() {
        final var context = forest(SIDE, SIDE, SEED);
        final var middle = context.index(SIDE / HALF, SIDE / HALF);
        context.setGround(middle, TileType.SHALLOW_WATER);
        context.setResource(middle, null);

        apply(context, params(0, NO_REGIONS));

        assertFalse(context.isReserved(middle));
        assertEquals(TileType.SHALLOW_WATER, context.groundAt(middle));
        assertNotEquals(middle, context.spawnHint());
    }

    @Test
    void theSpawnRoomFallsBackToWhatANarrowLandHolds() {
        final var context = TestContexts.of(MapType.DENSE_FOREST, SEED, STRIP_WIDTH * STRIP_COUNT, STRIP_ROWS,
                TestContexts.FLAT);
        TestContexts.fillGround(context, TileType.SHALLOW_WATER);
        for (var y = 0; y < STRIP_ROWS; y++) {
            for (var x = STRIP_WIDTH; x < STRIP_WIDTH * HALF; x++) {
                context.setGround(context.index(x, y), TileType.GRASS);
            }
        }

        apply(context, params(2, NO_REGIONS));

        assertEquals(STRIP_WIDTH + STRIP_MIDDLE, context.xOf(context.spawnHint()));
        assertEquals(STRIP_ROWS / HALF, context.yOf(context.spawnHint()));
    }

    @Test
    void aSeparateValleyTooNarrowForASiteGetsAClearingWhenItIsLargeEnough() {
        final var context = valleys();

        apply(context, params(CLEARANCE, MIN_REGION));

        assertTrue(hasSiteOnTheStrip(context), "the narrow valley got a clearing");
        assertTrue(context.isReserved(context.index(NARROW_MIDDLE, 0)));
    }

    @Test
    void aValleyBelowTheMinimumOrWithTheRuleOffGetsNone() {
        final var small = valleys();
        final var off = valleys();

        apply(small, params(CLEARANCE, NARROW_TILES + 1));
        apply(off, params(CLEARANCE, NO_REGIONS));

        assertFalse(hasSiteOnTheStrip(small));
        assertFalse(hasSiteOnTheStrip(off));
    }

    @Test
    void aMapWithNoWalkableGroundHasNoSiteAndNoHint() {
        final var context = forest(SIDE, SIDE, SEED);
        TestContexts.fillGround(context, TileType.DEEP_WATER);

        apply(context, params(0, NO_REGIONS));

        assertEquals(0, context.sites().size());
        assertEquals(GenerationContext.NONE, context.spawnHint());
    }

    @Test
    void aMapSmallerThanTheSpacingStillGetsItsSpawnClearing() {
        final var context = forest(STRIP_WIDTH * 2, STRIP_WIDTH * 2, SEED);

        apply(context, params(0, NO_REGIONS));

        assertTrue(context.sites().size() >= 1);
        assertNotEquals(GenerationContext.NONE, context.spawnHint());
    }

    @Test
    void theSameSeedOpensTheSameClearingsAndAnotherSeedOpensOthers() {
        final var first = forest(SIDE, SIDE, SEED);
        final var again = forest(SIDE, SIDE, SEED);
        final var other = forest(SIDE, SIDE, OTHER_SEED);

        apply(first, params(0, NO_REGIONS));
        apply(again, params(0, NO_REGIONS));
        apply(other, params(0, NO_REGIONS));

        assertArrayEquals(first.sites().toArray(), again.sites().toArray());
        assertFalse(Arrays.equals(first.sites().toArray(), other.sites().toArray()));
    }

    @Test
    void theParametersRefuseSizesThatCannotBuildAClearing() {
        assertThrows(IllegalArgumentException.class, () -> new ClearingParams(0, 1, 1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClearingParams(1, 0, 1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClearingParams(1, 2, 1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClearingParams(1, 1, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClearingParams(1, 1, 1, 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClearingParams(1, 1, 1, 1, 0, -1));
        assertThrows(NullPointerException.class, () -> new ClearingPass(null));
    }

    @Test
    void tilesReservedBeforeThePassHostNeitherASiteNorTheSpawnClearing() {
        final var context = forest(SIDE, SIDE, SEED);
        final var held = new boolean[context.size()];
        for (var y = SIDE / HALF - HELD_RADIUS; y <= SIDE / HALF + HELD_RADIUS; y++) {
            for (var x = SIDE / HALF - HELD_RADIUS; x <= SIDE / HALF + HELD_RADIUS; x++) {
                context.reserve(context.index(x, y));
                held[context.index(x, y)] = true;
            }
        }

        apply(context, params(0, NO_REGIONS));

        assertTrue(context.sites().size() > 1, "the map holds several clearings");
        for (var site = 0; site < context.sites().size(); site++) {
            assertFalse(held[context.sites().get(site)], "a site stands on a reserved tile: " + site);
        }
        assertFalse(held[context.spawnHint()], "the spawn clearing is not on a reserved tile");
    }

    @Test
    void aValleyClearingStandsOnATileThatNothingReservedBefore() {
        final var context = valleys();
        for (var y = 0; y < WALL_SIDE; y++) {
            context.reserve(context.index(NARROW_MIDDLE, y));
        }

        apply(context, params(CLEARANCE, MIN_REGION));

        assertTrue(hasSiteOnTheStrip(context), "the narrow valley got a clearing");
        for (var site = 0; site < context.sites().size(); site++) {
            assertNotEquals(NARROW_MIDDLE, context.xOf(context.sites().get(site)), "a site on the reserved column");
        }
    }

    @ParameterizedTest
    @CsvSource({"200,60", "60,200"})
    void aMapThatIsNotSquareHasItsSpawnInTheMiddleAndItsSitesSpacedByItsShorterSide(int width, int height) {
        final var context = forest(width, height, SEED);

        apply(context, params(CLEARANCE, NO_REGIONS));

        assertEquals(context.index(width / HALF, height / HALF), context.sites().get(0));
        assertEquals(context.sites().get(0), context.spawnHint());
        assertTrue(context.sites().size() > MIN_SITES_ON_THE_STRIP, "several sites: " + context.sites().size());
        assertSpacedApart(context, Math.min(SPACING, Math.min(width, height) / SIDES_PER_SPACING));
    }

    @Test
    void aSmallMapShrinksItsClearingsSoMostOfItsLandStaysUnderTrees() {
        final var shrunk = forest(SMALL_SIDE, SMALL_SIDE, SEED);
        final var unshrunk = forest(SMALL_SIDE, SMALL_SIDE, SEED);

        apply(shrunk, new ClearingParams(LARGE_SPACING, MIN_RADIUS, MAX_RADIUS, SPAWN_RADIUS, 0, NO_REGIONS));
        apply(unshrunk, new ClearingParams(SMALL_SIDE / SIDES_PER_SPACING, MIN_RADIUS, MAX_RADIUS, SPAWN_RADIUS, 0,
                NO_REGIONS));

        assertTrue(reservedCount(shrunk) < reservedCount(unshrunk), "fewer tiles cleared than without shrinking");
        assertTrue(reservedCount(shrunk) < shrunk.size() / HALF, "most of the land stays under trees");
        final var spawn = shrunk.sites().get(0);
        for (var tile = 0; tile < shrunk.size(); tile++) {
            if (isWithin(shrunk, tile, spawn, ClearingPass.MIN_SPAWN_RADIUS)) {
                assertTrue(shrunk.isReserved(tile), "the spawn clearing keeps its smallest radius at " + tile);
            }
        }
    }

    @Test
    void theRadiiOfAMapThatNeedsNoShrinkingAreTheOnesOfTheParameters() {
        final var context = forest(SIDE, SIDE, SEED);

        apply(context, params(0, NO_REGIONS));

        final var spawn = context.sites().get(0);
        assertTrue(context.isReserved(spawn + SPAWN_RADIUS), "the spawn clearing has the radius of the parameters");
    }

    @Test
    void aMapTooBigForTheTrailsHoldsNoMoreSitesThanTheyCanJoin() {
        final var context = forest(CROWDED_SIDE, CROWDED_SIDE, SEED);

        apply(context, new ClearingParams(1, 1, 1, 1, 0, NO_REGIONS));

        assertTrue(context.sites().size() > 1, "the map holds several clearings");
        assertTrue(context.sites().size() <= SiteEdges.MAX_SITES, "sites: " + context.sites().size());
    }

    @Test
    void noClearingOpensOnceTheSitesAreAsManyAsTheTrailsCanJoin() {
        final var context = forest(SIDE, SIDE, SEED);
        for (var site = 0; site < SiteEdges.MAX_SITES; site++) {
            context.sites().add(0);
        }

        apply(context, params(0, NO_REGIONS));

        assertEquals(SiteEdges.MAX_SITES, context.sites().size());
        assertEquals(0, reservedCount(context));
    }

    @Test
    void thePassIsNamedForTheTimings() {
        assertEquals("clearings", new ClearingPass(params(0, NO_REGIONS)).name());
    }

    private static int reservedCount(GenerationContext context) {
        var count = 0;
        for (var index = 0; index < context.size(); index++) {
            count += context.isReserved(index) ? 1 : 0;
        }
        return count;
    }

    /**
     * Builds a wide valley on the left, a wall of water, a narrow strip of grass on the right, and water beyond it:
     * the strip has 180 tiles but no tile has the room a clearing asks for.
     */
    private static GenerationContext valleys() {
        final var context = forest(WALL_SIDE, WALL_SIDE, SEED);
        for (var y = 0; y < WALL_SIDE; y++) {
            for (var x = WALL_COLUMN; x < WALL_SIDE; x++) {
                final var isStrip = x >= NARROW_FIRST && x <= NARROW_LAST;
                if (!isStrip) {
                    context.setGround(context.index(x, y), TileType.SHALLOW_WATER);
                    context.setResource(context.index(x, y), null);
                }
            }
        }
        return context;
    }

    private static boolean hasSiteOnTheStrip(GenerationContext context) {
        for (var site = 0; site < context.sites().size(); site++) {
            final var x = context.xOf(context.sites().get(site));
            if (x >= NARROW_FIRST && x <= NARROW_LAST) {
                return true;
            }
        }
        return false;
    }
}
