package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnionFindTest {

    private static final int ELEMENTS = 6;
    private static final int LONG_CHAIN = 10_000;

    private final UnionFind groups = new UnionFind(ELEMENTS);

    @Test
    void everyElementStartsInItsOwnSet() {
        assertEquals(ELEMENTS, groups.sets());
        for (var element = 0; element < ELEMENTS; element++) {
            assertEquals(element, groups.find(element));
        }
    }

    @Test
    void unionMergesTwoSetsOnce() {
        assertTrue(groups.union(0, 1));
        assertFalse(groups.union(1, 0));

        assertEquals(groups.find(0), groups.find(1));
        assertEquals(ELEMENTS - 1, groups.sets());
    }

    @Test
    void mergingIsTransitive() {
        groups.union(0, 1);
        groups.union(2, 3);
        groups.union(1, 2);

        assertEquals(groups.find(0), groups.find(3));
        assertNotEquals(groups.find(0), groups.find(4));
        assertEquals(ELEMENTS - 3, groups.sets());
    }

    @Test
    void aLongChainOfMergesStaysUsable() {
        final var chain = new UnionFind(LONG_CHAIN);

        for (var element = 1; element < LONG_CHAIN; element++) {
            assertTrue(chain.union(element - 1, element));
        }

        assertEquals(1, chain.sets());
        assertEquals(chain.find(0), chain.find(LONG_CHAIN - 1));
    }

    @Test
    void anEmptyStructureIsAllowed() {
        assertEquals(0, new UnionFind(0).sets());
    }

    @Test
    void invalidArgumentsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new UnionFind(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> groups.find(ELEMENTS));
        assertThrows(IndexOutOfBoundsException.class, () -> groups.union(0, ELEMENTS));
    }
}
