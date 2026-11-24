package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * A binary min-heap of int nodes ordered by a float key, held in two parallel arrays so a search pushes and pops
 * without boxing. The same node can be pushed again with a better key: the heap keeps both entries, and the caller
 * discards the stale one when it pops. Entries with equal keys leave in no defined order.
 */
final class IntMinHeap {

    private static final int INITIAL_CAPACITY = 64;
    private static final int GROWTH_FACTOR = 2;
    /** The number of children of a node. */
    private static final int ARITY = 2;

    private int[] nodes = new int[INITIAL_CAPACITY];
    private float[] keys = new float[INITIAL_CAPACITY];
    private int size;

    /**
     * Adds an entry.
     *
     * @param node the node
     * @param key  its priority, where a lower key leaves first
     */
    void push(int node, float key) {
        if (size == nodes.length) {
            nodes = Arrays.copyOf(nodes, size * GROWTH_FACTOR);
            keys = Arrays.copyOf(keys, size * GROWTH_FACTOR);
        }
        var hole = size;
        size++;
        while (hole > 0) {
            final var parent = (hole - 1) / ARITY;
            if (keys[parent] <= key) {
                break;
            }
            nodes[hole] = nodes[parent];
            keys[hole] = keys[parent];
            hole = parent;
        }
        nodes[hole] = node;
        keys[hole] = key;
    }

    /**
     * Returns the key of the entry that {@link #pop()} would remove.
     *
     * @return the lowest key
     * @throws NoSuchElementException when the heap is empty
     */
    float peekKey() {
        requireNotEmpty();
        return keys[0];
    }

    /**
     * Removes the entry with the lowest key.
     *
     * @return its node
     * @throws NoSuchElementException when the heap is empty
     */
    int pop() {
        requireNotEmpty();
        final var top = nodes[0];
        size--;
        if (size > 0) {
            siftDown(nodes[size], keys[size]);
        }
        return top;
    }

    /** Tells whether the heap holds no entry. */
    boolean isEmpty() {
        return size == 0;
    }

    /** Returns the number of entries, stale duplicates included. */
    int size() {
        return size;
    }

    /** Removes every entry and keeps the arrays for the next search. */
    void clear() {
        size = 0;
    }

    private void requireNotEmpty() {
        if (size == 0) {
            throw new NoSuchElementException("the heap is empty");
        }
    }

    /** Places the last entry, moved to the root, where the order is restored. */
    private void siftDown(int node, float key) {
        var hole = 0;
        var child = 1;
        while (child < size) {
            if (child + 1 < size && keys[child + 1] < keys[child]) {
                child++;
            }
            if (keys[child] >= key) {
                break;
            }
            nodes[hole] = nodes[child];
            keys[hole] = keys[child];
            hole = child;
            child = ARITY * hole + 1;
        }
        nodes[hole] = node;
        keys[hole] = key;
    }
}
