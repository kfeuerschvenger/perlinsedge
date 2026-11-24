package com.feuerschvenger.perlinsedge.domain.world.generation;

import java.util.Arrays;
import java.util.Objects;

/**
 * A growable list of tile indices held in a plain {@code int[]}, so a pass that collects sites, such as the centres of
 * clearings, boxes nothing. It belongs to one generation and one thread.
 */
public final class SiteList {

    private static final int INITIAL_CAPACITY = 16;

    private int[] values = new int[INITIAL_CAPACITY];
    private int size;

    /**
     * Appends a tile index.
     *
     * @param index an index into the arrays of the context
     */
    public void add(int index) {
        if (size == values.length) {
            values = Arrays.copyOf(values, size * 2);
        }
        values[size] = index;
        size++;
    }

    /**
     * Returns the number of indices in the list.
     *
     * @return the size
     */
    public int size() {
        return size;
    }

    /**
     * Returns one index of the list.
     *
     * @param position where the index sits, from 0 to {@code size() - 1}
     * @return the tile index
     * @throws IndexOutOfBoundsException when the position is outside the list
     */
    public int get(int position) {
        Objects.checkIndex(position, size);
        return values[position];
    }

    /**
     * Returns a copy of the indices, in the order they were added.
     *
     * @return a new array of {@link #size()} elements
     */
    public int[] toArray() {
        return Arrays.copyOf(values, size);
    }
}
