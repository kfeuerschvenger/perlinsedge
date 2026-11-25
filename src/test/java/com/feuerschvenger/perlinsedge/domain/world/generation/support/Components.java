package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * The 4-connected components of the tiles of a map whose terrain a predicate selects, with the size and the bounding
 * box of each. Components are numbered from 1 in the order their first tile appears when the map is read row by row,
 * and the label 0 stands for a tile that is not selected.
 */
public final class Components {

    /** The label of a tile that is not selected. */
    public static final int NONE = 0;

    private static final int INITIAL_CAPACITY = 16;
    private static final int GROWTH_FACTOR = 2;

    private final int width;
    private final int height;
    private final int[] labels;
    private int count;
    private int[] sizes = new int[INITIAL_CAPACITY];
    private int[] minX = new int[INITIAL_CAPACITY];
    private int[] maxX = new int[INITIAL_CAPACITY];
    private int[] minY = new int[INITIAL_CAPACITY];
    private int[] maxY = new int[INITIAL_CAPACITY];

    private Components(TileMap map, Predicate<TileType> selected) {
        this.width = map.getWidth();
        this.height = map.getHeight();
        this.labels = new int[map.getWidth() * map.getHeight()];
        final var queue = new int[labels.length];
        for (var start = 0; start < labels.length; start++) {
            if (labels[start] == NONE && selected.test(map.getTile(start % width, start / width).getType())) {
                count++;
                grow();
                fill(map, selected, queue, start);
            }
        }
    }

    /**
     * Finds the components of a map.
     *
     * @param map      the map
     * @param selected tells which terrain belongs to a component
     * @return the components
     */
    public static Components of(TileMap map, Predicate<TileType> selected) {
        return new Components(map, selected);
    }

    /**
     * Returns the number of components.
     *
     * @return the count, so the labels run from 1 to this value
     */
    public int count() {
        return count;
    }

    /**
     * Counts the components of at least a size.
     *
     * @param minTiles the fewest tiles a component needs to count
     * @return the number of components with that many tiles or more
     */
    public int countAtLeast(int minTiles) {
        var found = 0;
        for (var label = 1; label <= count; label++) {
            found += sizes[label] >= minTiles ? 1 : 0;
        }
        return found;
    }

    /**
     * Counts the components whose size lies within a range.
     *
     * @param minTiles the fewest tiles
     * @param maxTiles the most tiles
     * @return the number of components with from {@code minTiles} to {@code maxTiles} tiles
     */
    public int countWithin(int minTiles, int maxTiles) {
        var found = 0;
        for (var label = 1; label <= count; label++) {
            found += sizes[label] >= minTiles && sizes[label] <= maxTiles ? 1 : 0;
        }
        return found;
    }

    /**
     * Returns the component of a tile.
     *
     * @param x the column
     * @param y the row
     * @return the label, or {@link #NONE} when the tile is not selected
     */
    public int labelAt(int x, int y) {
        return labels[y * width + x];
    }

    /**
     * Returns how many tiles a component holds.
     *
     * @param label a label from 1 to {@link #count()}
     * @return the number of tiles
     */
    public int sizeOf(int label) {
        return sizes[label];
    }

    /**
     * Returns the sizes of every component, in label order.
     *
     * @return a new array with one entry per component
     */
    public int[] sizes() {
        return Arrays.copyOfRange(sizes, 1, count + 1);
    }

    /**
     * Returns the number of columns a component spans.
     *
     * @param label a label from 1 to {@link #count()}
     * @return the width of its bounding box
     */
    public int spanX(int label) {
        return maxX[label] - minX[label] + 1;
    }

    /**
     * Returns the number of rows a component spans.
     *
     * @param label a label from 1 to {@link #count()}
     * @return the height of its bounding box
     */
    public int spanY(int label) {
        return maxY[label] - minY[label] + 1;
    }

    /**
     * Tells whether a component reaches the edge of the map.
     *
     * @param label a label from 1 to {@link #count()}
     * @return true when one of its tiles lies in the first or last row or column
     */
    public boolean touchesEdge(int label) {
        return minX[label] == 0 || minY[label] == 0 || maxX[label] == width - 1 || maxY[label] == height - 1;
    }

    /**
     * Counts the components that lie wholly inside the map, away from its edge, and have a number of tiles within a
     * range.
     *
     * @param minTiles the fewest tiles
     * @param maxTiles the most tiles
     * @return the number of components that touch no edge and hold from {@code minTiles} to {@code maxTiles} tiles
     */
    public int countInland(int minTiles, int maxTiles) {
        var found = 0;
        for (var label = 1; label <= count; label++) {
            found += !touchesEdge(label) && sizes[label] >= minTiles && sizes[label] <= maxTiles ? 1 : 0;
        }
        return found;
    }

    /**
     * Returns the size of the largest component.
     *
     * @return the number of tiles, 0 when nothing is selected
     */
    public int largest() {
        return Arrays.stream(sizes()).max().orElse(0);
    }

    private void grow() {
        if (count == sizes.length) {
            final var capacity = sizes.length * GROWTH_FACTOR;
            sizes = Arrays.copyOf(sizes, capacity);
            minX = Arrays.copyOf(minX, capacity);
            maxX = Arrays.copyOf(maxX, capacity);
            minY = Arrays.copyOf(minY, capacity);
            maxY = Arrays.copyOf(maxY, capacity);
        }
        minX[count] = Integer.MAX_VALUE;
        minY[count] = Integer.MAX_VALUE;
        maxX[count] = -1;
        maxY[count] = -1;
    }

    /** Labels the tiles connected to a start tile with the current label and records the size and bounding box. */
    private void fill(TileMap map, Predicate<TileType> selected, int[] queue, int start) {
        var head = 0;
        var tail = 0;
        labels[start] = count;
        queue[tail] = start;
        tail++;
        while (head < tail) {
            final var index = queue[head];
            head++;
            final var x = index % width;
            final var y = index / width;
            record(x, y);
            tail = visit(map, selected, queue, tail, x - 1, y);
            tail = visit(map, selected, queue, tail, x + 1, y);
            tail = visit(map, selected, queue, tail, x, y - 1);
            tail = visit(map, selected, queue, tail, x, y + 1);
        }
    }

    private void record(int x, int y) {
        sizes[count]++;
        minX[count] = Math.min(minX[count], x);
        maxX[count] = Math.max(maxX[count], x);
        minY[count] = Math.min(minY[count], y);
        maxY[count] = Math.max(maxY[count], y);
    }

    private int visit(TileMap map, Predicate<TileType> selected, int[] queue, int tail, int x, int y) {
        if (!map.isValidCoordinate(x, y)) {
            return tail;
        }
        final var index = y * width + x;
        if (labels[index] != NONE || !selected.test(map.getTile(x, y).getType())) {
            return tail;
        }
        labels[index] = count;
        queue[tail] = index;
        return tail + 1;
    }
}
