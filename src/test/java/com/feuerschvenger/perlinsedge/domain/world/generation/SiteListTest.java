package com.feuerschvenger.perlinsedge.domain.world.generation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SiteListTest {

    private static final int FIRST = 7;
    private static final int SECOND = 3;
    private static final int MANY = 1_000;
    private static final int STRIDE = 3;

    private final SiteList sites = new SiteList();

    @Test
    void aNewListIsEmpty() {
        assertEquals(0, sites.size());
        assertEquals(0, sites.toArray().length);
    }

    @Test
    void indicesKeepTheOrderTheyWereAddedIn() {
        sites.add(FIRST);
        sites.add(SECOND);

        assertEquals(2, sites.size());
        assertEquals(FIRST, sites.get(0));
        assertEquals(SECOND, sites.get(1));
        assertArrayEquals(new int[] {FIRST, SECOND}, sites.toArray());
    }

    @Test
    void theListGrowsPastItsInitialCapacityWithoutLosingAnything() {
        for (var value = 0; value < MANY; value++) {
            sites.add(value * STRIDE);
        }

        assertEquals(MANY, sites.size());
        for (var position = 0; position < MANY; position++) {
            assertEquals(position * STRIDE, sites.get(position));
        }
    }

    @Test
    void toArrayReturnsACopy() {
        sites.add(FIRST);

        final var copy = sites.toArray();
        copy[0] = SECOND;

        assertEquals(FIRST, sites.get(0));
    }

    @Test
    void getRefusesAPositionOutsideTheList() {
        sites.add(FIRST);

        assertThrows(IndexOutOfBoundsException.class, () -> sites.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> sites.get(-1));
    }
}
