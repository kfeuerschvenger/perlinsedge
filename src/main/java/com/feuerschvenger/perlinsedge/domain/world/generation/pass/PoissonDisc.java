package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.SiteList;

import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Bridson Poisson-disc sampling over the tiles a mask selects: a set of sites no two of which are closer than the
 * spacing, spread so evenly that no host tile far from every site is left. An acceleration grid of {@code int} cells,
 * each holding at most one site, makes the test against the existing sites a lookup of a few cells.
 * <p>
 * From a site, each of up to {@value #ATTEMPTS} candidates lies at a random angle and at a distance between the spacing
 * and twice the spacing; a candidate on a host tile that is clear of every site becomes a site itself. When the sites
 * grown from a seed tile run out, the next seed is the first clear host tile met while sweeping the map once from a
 * random start, so host areas that the growth could not reach, such as a separate island, still get sites. The angles
 * use {@link StrictMath}, so one seed gives the same sites on every platform.
 */
final class PoissonDisc {

    /** The candidates tried around a site before it is retired. */
    static final int ATTEMPTS = 30;

    private static final int INITIAL_ACTIVE = 32;
    private static final double TURN = 2.0 * Math.PI;
    /** The side of a grid cell is the spacing over the square root of two, so a cell holds at most one site. */
    private static final double CELL_DIVISOR = Math.sqrt(2.0);
    private static final int NO_SITE = 0;

    private final int width;
    private final int height;
    private final Mask host;
    private final int spacing;
    private final long spacingSquared;
    private final RandomGenerator random;
    private final int cellSide;
    private final int cellColumns;
    private final int cellRows;
    private final int reach;
    private final int[] grid;
    private final SiteList sites = new SiteList();
    private int[] active = new int[INITIAL_ACTIVE];
    private int activeCount;

    private PoissonDisc(int width, int height, Mask host, int spacing, RandomGenerator random) {
        this.width = width;
        this.height = height;
        this.host = host;
        this.spacing = spacing;
        this.spacingSquared = (long) spacing * spacing;
        this.random = random;
        this.cellSide = Math.max(1, (int) (spacing / CELL_DIVISOR));
        this.cellColumns = (width + cellSide - 1) / cellSide;
        this.reach = (spacing + cellSide - 1) / cellSide;
        this.cellRows = (height + cellSide - 1) / cellSide;
        this.grid = new int[cellColumns * cellRows];
    }

    /**
     * Samples the sites.
     *
     * @param width   the number of columns of the grid
     * @param height  the number of rows of the grid
     * @param host    the tiles a site may stand on
     * @param spacing the least distance between two sites in tiles, at least 1
     * @param random  the seeded source of randomness
     * @return the tile indices of the sites, in the order they were found
     * @throws IllegalArgumentException when the grid is empty or the spacing is below 1
     */
    static SiteList sample(int width, int height, Mask host, int spacing, RandomGenerator random) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(random, "random");
        if (width <= 0 || height <= 0 || spacing < 1) {
            throw new IllegalArgumentException("the grid " + width + "x" + height + " and spacing " + spacing
                    + " must be positive");
        }
        return new PoissonDisc(width, height, host, spacing, random).run();
    }

    private SiteList run() {
        final var size = width * height;
        final var first = random.nextInt(size);
        for (var swept = 0; swept < size; swept++) {
            final var index = (first + swept) % size;
            if (isClear(index % width, index / width) && host.test(index)) {
                place(index);
                grow();
            }
        }
        return sites;
    }

    /** Grows sites from the active ones until none has room left for a neighbour. */
    private void grow() {
        while (activeCount > 0) {
            final var slot = random.nextInt(activeCount);
            if (!tryAround(active[slot])) {
                activeCount--;
                active[slot] = active[activeCount];
            }
        }
    }

    /** Tries the candidates around a site and places the first one that fits. */
    private boolean tryAround(int site) {
        final var siteX = site % width;
        final var siteY = site / width;
        for (var attempt = 0; attempt < ATTEMPTS; attempt++) {
            final var angle = random.nextDouble() * TURN;
            final var distance = spacing * (1.0 + random.nextDouble());
            final var x = siteX + (int) Math.round(StrictMath.cos(angle) * distance);
            final var y = siteY + (int) Math.round(StrictMath.sin(angle) * distance);
            if (x >= 0 && y >= 0 && x < width && y < height && host.test(y * width + x) && isClear(x, y)) {
                place(y * width + x);
                return true;
            }
        }
        return false;
    }

    /** Records a site in the result, the grid and the active list. */
    private void place(int index) {
        sites.add(index);
        grid[cellOf(index % width, index / width)] = index + 1;
        if (activeCount == active.length) {
            active = Arrays.copyOf(active, activeCount * 2);
        }
        active[activeCount] = index;
        activeCount++;
    }

    private int cellOf(int x, int y) {
        return (y / cellSide) * cellColumns + x / cellSide;
    }

    /** Tells whether no site lies within the spacing of a tile, looking only at the cells that can hold one. */
    private boolean isClear(int x, int y) {
        final var column = x / cellSide;
        final var row = y / cellSide;
        final var lastRow = Math.min(row + reach, cellRows - 1);
        final var lastColumn = Math.min(column + reach, cellColumns - 1);
        for (var cellRow = Math.max(0, row - reach); cellRow <= lastRow; cellRow++) {
            for (var cellColumn = Math.max(0, column - reach); cellColumn <= lastColumn; cellColumn++) {
                final var held = grid[cellRow * cellColumns + cellColumn];
                if (held != NO_SITE && isTooClose(held - 1, x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTooClose(int site, int x, int y) {
        final long dx = (long) site % width - x;
        final long dy = (long) site / width - y;
        return dx * dx + dy * dy < spacingSquared;
    }
}
