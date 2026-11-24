package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntMinHeapTest {

    private static final int MANY = 1_000;
    private static final long SEED = 42L;
    private static final float LOW = 1F;
    private static final float MID = 2F;
    private static final float HIGH = 3F;
    private static final int NODE_A = 10;
    private static final int NODE_B = 20;
    private static final int NODE_C = 30;

    private final IntMinHeap heap = new IntMinHeap();

    @Test
    void aNewHeapIsEmpty() {
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test
    void entriesLeaveFromTheLowestKey() {
        heap.push(NODE_B, MID);
        heap.push(NODE_C, HIGH);
        heap.push(NODE_A, LOW);

        assertEquals(LOW, heap.peekKey());
        assertEquals(NODE_A, heap.pop());
        assertEquals(NODE_B, heap.pop());
        assertEquals(NODE_C, heap.pop());
        assertTrue(heap.isEmpty());
    }

    @Test
    void manyEntriesComeOutInKeyOrderAndKeepTheirNodes() {
        final var random = new SplittableRandom(SEED);
        final var keys = new float[MANY];
        for (var node = 0; node < MANY; node++) {
            keys[node] = random.nextFloat();
            heap.push(node, keys[node]);
        }
        final var sorted = keys.clone();
        Arrays.sort(sorted);

        assertEquals(MANY, heap.size());
        for (var position = 0; position < MANY; position++) {
            final var key = heap.peekKey();
            final var node = heap.pop();
            assertEquals(sorted[position], key);
            assertEquals(keys[node], key);
        }
        assertTrue(heap.isEmpty());
    }

    @Test
    void theSameNodeCanBePushedAgainWithABetterKey() {
        heap.push(NODE_A, HIGH);
        heap.push(NODE_A, LOW);

        assertEquals(2, heap.size());
        assertEquals(LOW, heap.peekKey());
        assertEquals(NODE_A, heap.pop());
        assertEquals(HIGH, heap.peekKey());
    }

    @Test
    void clearEmptiesTheHeapAndItIsUsableAgain() {
        heap.push(NODE_A, LOW);

        heap.clear();
        heap.push(NODE_B, MID);

        assertEquals(1, heap.size());
        assertEquals(NODE_B, heap.pop());
        assertTrue(heap.isEmpty());
    }

    @Test
    void anEmptyHeapRefusesToPopOrPeek() {
        assertThrows(NoSuchElementException.class, heap::pop);
        assertThrows(NoSuchElementException.class, heap::peekKey);
    }
}
