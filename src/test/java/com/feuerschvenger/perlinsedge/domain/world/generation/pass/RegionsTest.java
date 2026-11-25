package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionsTest {

    private static final char SELECTED = '#';
    private static final int BIG_SIDE = 300;
    private static final int EVEN = 2;

    /** Labels a picture where {@code #} marks the selected tiles. */
    private static Regions regionsOf(String... rows) {
        final var width = rows[0].length();
        final var cells = String.join("", rows);
        return Regions.label(width, rows.length, index -> cells.charAt(index) == SELECTED);
    }

    @Test
    void tilesJoinAlongTheFourDirectionsOnly() {
        final var regions = regionsOf(
                "##.#",
                "#..#",
                "..#.");

        assertEquals(3, regions.count());
        assertArrayEquals(new int[] {0, 3, 2, 1}, regions.sizes());
        assertEquals(1, regions.labelAt(0));
        assertEquals(1, regions.labelAt(1));
        assertEquals(1, regions.labelAt(4));
        assertEquals(2, regions.labelAt(3));
        assertEquals(2, regions.labelAt(7));
        assertEquals(3, regions.labelAt(10));
        assertEquals(Regions.NO_LABEL, regions.labelAt(2));
    }

    /** Labelling again for another mask gives what a fresh labelling gives, with no label left from the first mask. */
    @Test
    void relabellingReusesTheArraysAndLeavesNothingOfTheFirstMask() {
        final var cells = String.join("", "##.#", "#..#", "..#.");
        final var first = regionsOf("##.#", "#..#", "..#.");
        final var fresh = regionsOf("..#.", ".##.", "##.#");

        final var second = first.relabel(index -> cells.charAt(index) != SELECTED);

        assertEquals(fresh.count(), second.count());
        assertArrayEquals(fresh.sizes(), second.sizes());
        assertEquals(Regions.NO_LABEL, second.labelAt(0));
        assertEquals(1, second.labelAt(2));
    }

    /** The regions a relabelling reused the arrays of refuse every question and a second relabelling. */
    @Test
    void relabelledRegionsAreSpentAndRefuseToAnswer() {
        final var first = regionsOf("##.#", "#..#", "..#.");
        final var second = first.relabel(index -> true);

        assertThrows(IllegalStateException.class, first::count);
        assertThrows(IllegalStateException.class, () -> first.labelAt(0));
        assertThrows(IllegalStateException.class, () -> first.sizeOf(1));
        assertThrows(IllegalStateException.class, first::largest);
        assertThrows(IllegalStateException.class, first::sizes);
        assertThrows(IllegalStateException.class, () -> first.relabel(index -> true));
        assertEquals(1, second.count());
        assertEquals(1, second.labelAt(0));
    }

    @Test
    void theLastTileOfARowIsNotJoinedToTheFirstTileOfTheNext() {
        final var regions = regionsOf(
                "..#",
                "#..");

        assertEquals(2, regions.count());
    }

    @Test
    void theLargestRegionIsReported() {
        final var regions = regionsOf(
                "#.##",
                "..##");

        assertEquals(2, regions.largest());
        assertEquals(4, regions.sizeOf(2));
        assertEquals(1, regions.sizeOf(1));
    }

    @Test
    void theFirstOfSeveralLargestRegionsWins() {
        final var regions = regionsOf("##.##");

        assertEquals(1, regions.largest());
    }

    @Test
    void aMapWithNoSelectedTileHasNoRegion() {
        final var regions = regionsOf(
                "...",
                "...");

        assertEquals(0, regions.count());
        assertEquals(Regions.NO_LABEL, regions.largest());
        assertArrayEquals(new int[] {0}, regions.sizes());
    }

    @Test
    void aWholeLargeMapIsOneRegionWithoutRecursion() {
        final var regions = Regions.label(BIG_SIDE, BIG_SIDE, index -> true);

        assertEquals(1, regions.count());
        assertEquals(BIG_SIDE * BIG_SIDE, regions.sizeOf(1));
    }

    @Test
    void aCheckerboardGivesOneRegionPerSelectedTileAndGrowsTheSizeTable() {
        final var regions = Regions.label(BIG_SIDE, BIG_SIDE,
                index -> (index % BIG_SIDE + index / BIG_SIDE) % EVEN == 0);

        assertEquals(BIG_SIDE * BIG_SIDE / EVEN, regions.count());
        assertEquals(1, regions.sizeOf(regions.count()));
    }

    @Test
    void theSizesAreACopy() {
        final var regions = regionsOf("##");

        regions.sizes()[1] = 0;

        assertEquals(EVEN, regions.sizeOf(1));
    }

    @Test
    void aLabelOutsideTheRangeIsRefused() {
        final var regions = regionsOf("#");

        assertThrows(IndexOutOfBoundsException.class, () -> regions.sizeOf(2));
    }

    @Test
    void invalidArgumentsAreRefused() {
        assertThrows(NullPointerException.class, () -> Regions.label(1, 1, null));
        assertThrows(IllegalArgumentException.class, () -> Regions.label(0, 1, index -> true));
        assertThrows(IllegalArgumentException.class, () -> Regions.label(1, 0, index -> true));
    }
}
