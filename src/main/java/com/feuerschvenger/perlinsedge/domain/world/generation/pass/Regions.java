package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import java.util.Arrays;
import java.util.Objects;

/**
 * The 4-connected regions of the tiles a mask selects, found with one flood fill over an array of labels and an array
 * used as the queue, so it creates no object per tile. Regions are numbered from 1 in the order their first tile
 * appears when the map is read row by row, and the label 0 stands for a tile that belongs to no region.
 */
final class Regions {

    /** The label of a tile that is not selected. */
    static final int NO_LABEL = 0;

    private static final int INITIAL_SIZES = 16;

    private final int[] labels;
    private final int[] sizes;
    private final int largest;

    private Regions(int[] labels, int[] sizes, int largest) {
        this.labels = labels;
        this.sizes = sizes;
        this.largest = largest;
    }

    /**
     * Labels the regions of a grid. The mask is called once per tile for the scan and again for each neighbour a fill
     * examines that has no label yet.
     *
     * @param width  the number of columns
     * @param height the number of rows
     * @param mask   the tiles that belong to a region
     * @return the regions
     */
    static Regions label(int width, int height, Mask mask) {
        Objects.requireNonNull(mask, "mask");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("the grid needs at least one tile: " + width + "x" + height);
        }
        return new Fill(width, height, mask).run();
    }

    /**
     * Returns the number of regions.
     *
     * @return the count, so the labels run from 1 to this value
     */
    int count() {
        return sizes.length - 1;
    }

    /**
     * Returns the region of a tile.
     *
     * @param index an index into the grid
     * @return the label, or {@link #NO_LABEL} when the tile is not selected
     */
    int labelAt(int index) {
        return labels[index];
    }

    /**
     * Returns how many tiles a region holds.
     *
     * @param label a label from 1 to {@link #count()}
     * @return the number of tiles
     */
    int sizeOf(int label) {
        Objects.checkIndex(label, sizes.length);
        return sizes[label];
    }

    /**
     * Returns the largest region; the first of several of the same size wins, so the answer is stable.
     *
     * @return the label, or {@link #NO_LABEL} when no tile is selected
     */
    int largest() {
        return largest;
    }

    /** Returns a copy of the sizes, indexed by label, where the entry of {@link #NO_LABEL} is 0. */
    int[] sizes() {
        return sizes.clone();
    }

    /** The state of one labelling run. */
    private static final class Fill {
        private final int width;
        private final int height;
        private final Mask mask;
        private final int[] labels;
        private final int[] queue;
        private int[] sizes = new int[INITIAL_SIZES];
        private int count;
        private int largest = NO_LABEL;

        private Fill(int width, int height, Mask mask) {
            this.width = width;
            this.height = height;
            this.mask = mask;
            this.labels = new int[width * height];
            this.queue = new int[width * height];
        }

        private Regions run() {
            for (var index = 0; index < labels.length; index++) {
                if (labels[index] == NO_LABEL && mask.test(index)) {
                    open(index);
                }
            }
            return new Regions(labels, Arrays.copyOf(sizes, count + 1), largest);
        }

        /** Starts the next region at a tile, fills it and keeps the largest so far. */
        private void open(int start) {
            count++;
            if (count == sizes.length) {
                sizes = Arrays.copyOf(sizes, sizes.length * 2);
            }
            final var size = flood(start, count);
            sizes[count] = size;
            if (size > sizes[largest]) {
                largest = count;
            }
        }

        /** Labels every selected tile connected to the start along the four directions and returns how many. */
        private int flood(int start, int label) {
            var head = 0;
            var tail = 0;
            labels[start] = label;
            queue[tail] = start;
            tail++;
            while (head < tail) {
                final var index = queue[head];
                head++;
                // One division per tile: the column follows from the row and the index
                final var y = index / width;
                final var x = index - y * width;
                tail = visit(tail, x > 0, index - 1, label);
                tail = visit(tail, x < width - 1, index + 1, label);
                tail = visit(tail, y > 0, index - width, label);
                tail = visit(tail, y < height - 1, index + width, label);
            }
            return tail;
        }

        /** Queues a neighbour when it exists, is selected and has no label yet, and returns the new queue end. */
        private int visit(int tail, boolean exists, int neighbour, int label) {
            if (!exists || labels[neighbour] != NO_LABEL || !mask.test(neighbour)) {
                return tail;
            }
            labels[neighbour] = label;
            queue[tail] = neighbour;
            return tail + 1;
        }
    }
}
