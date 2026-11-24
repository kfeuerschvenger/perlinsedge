package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.SiteList;

import java.util.Arrays;
import java.util.Objects;

/**
 * The candidate edges between sites, each packed into one {@code long} so a plain sort orders them: the squared length
 * in the high 32 bits, then the position of the first site and the position of the second in the 16 bits below. Sorting
 * the array gives the edges from the shortest, and edges of equal length by their positions, so the order is
 * deterministic. A position is the place of a site in its {@link SiteList}, not its tile index.
 */
final class SiteEdges {

    /**
     * The most sites the build accepts. It tests every pair of sites twice, once to count the edges and once to fill
     * them, so the work and the size of the edge array grow with the square of this number. A position takes 16 bits,
     * which would allow far more sites than the pair build can hold.
     */
    public static final int MAX_SITES = 4096;
    /** The longest edge the packing holds, because the squared length takes 31 bits. */
    public static final int MAX_LENGTH = 46_340;

    private static final int POSITION_BITS = 16;
    private static final long POSITION_MASK = 0xFFFFL;
    private static final int LENGTH_SHIFT = 32;

    private SiteEdges() {
    }

    /**
     * Lists the edges between every pair of sites no farther apart than a limit, shortest first.
     *
     * @param sites       the sites, as tile indices
     * @param width       the number of columns of the grid the indices refer to
     * @param maxDistance the longest edge in tiles, measured between the centres of the tiles
     * @return a new array of packed edges in ascending order
     * @throws IllegalArgumentException when the width is not positive, there are more than {@value #MAX_SITES} sites
     *                                  or the limit is outside 0 to {@value #MAX_LENGTH}
     */
    static long[] within(SiteList sites, int width, int maxDistance) {
        Objects.requireNonNull(sites, "sites");
        if (width <= 0) {
            throw new IllegalArgumentException("the grid needs at least one column: " + width);
        }
        if (sites.size() > MAX_SITES) {
            throw new IllegalArgumentException("too many sites for an edge to name them: " + sites.size());
        }
        if (maxDistance < 0 || maxDistance > MAX_LENGTH) {
            throw new IllegalArgumentException("the longest edge must be from 0 to " + MAX_LENGTH + ": " + maxDistance);
        }
        final var limit = (long) maxDistance * maxDistance;
        final var edges = new long[Math.toIntExact(count(sites, width, limit))];
        var filled = 0;
        for (var first = 0; first < sites.size(); first++) {
            for (var second = first + 1; second < sites.size(); second++) {
                final var squared = squaredDistance(sites, width, first, second);
                if (squared <= limit) {
                    edges[filled] = pack(squared, first, second);
                    filled++;
                }
            }
        }
        Arrays.sort(edges);
        return edges;
    }

    /**
     * Returns the position of the first site of an edge.
     *
     * @param edge a packed edge
     * @return the position in the site list
     */
    static int from(long edge) {
        return (int) ((edge >>> POSITION_BITS) & POSITION_MASK);
    }

    /**
     * Returns the position of the second site of an edge.
     *
     * @param edge a packed edge
     * @return the position in the site list
     */
    static int to(long edge) {
        return (int) (edge & POSITION_MASK);
    }

    /**
     * Returns the squared length of an edge.
     *
     * @param edge a packed edge
     * @return the squared distance in tiles
     */
    static int lengthSquared(long edge) {
        return (int) (edge >>> LENGTH_SHIFT);
    }

    private static long pack(long squared, int first, int second) {
        return (squared << LENGTH_SHIFT) | ((long) first << POSITION_BITS) | second;
    }

    private static long count(SiteList sites, int width, long limit) {
        var count = 0L;
        for (var first = 0; first < sites.size(); first++) {
            for (var second = first + 1; second < sites.size(); second++) {
                if (squaredDistance(sites, width, first, second) <= limit) {
                    count++;
                }
            }
        }
        return count;
    }

    private static long squaredDistance(SiteList sites, int width, int first, int second) {
        final var a = sites.get(first);
        final var b = sites.get(second);
        final long dx = (long) a % width - b % width;
        final long dy = (long) a / width - b / width;
        return dx * dx + dy * dy;
    }
}
