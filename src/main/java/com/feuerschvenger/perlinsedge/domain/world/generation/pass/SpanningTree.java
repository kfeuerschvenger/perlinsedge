package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.SiteList;

import java.util.Arrays;

/**
 * The minimum spanning forest of a set of sites by Kruskal's algorithm over the edges no longer than a limit. The edges
 * that join two parts of the forest are the tree; the ones left over, each of which would close a cycle, are kept in
 * order of length, because a pass that wants a few extra connections beyond the tree, as a trail network does, takes
 * its pick from them. The build holds the sorted edge array of {@link SiteEdges} plus an array of the same length for
 * the leftover edges, so its memory is about twice the edge array, up to about 134 MB at the most sites the edges
 * accept, and a caller with that many sites should keep the limit short. When the limit leaves the sites in several
 * groups the result is one tree per group.
 */
final class SpanningTree {

    private final long[] tree;
    private final long[] cycleEdges;
    private final int components;

    private SpanningTree(long[] tree, long[] cycleEdges, int components) {
        this.tree = tree;
        this.cycleEdges = cycleEdges;
        this.components = components;
    }

    /**
     * Builds the forest.
     *
     * @param sites       the sites, as tile indices
     * @param width       the number of columns of the grid the indices refer to
     * @param maxDistance the longest edge in tiles; sites farther apart than this are not joined directly
     * @return the forest
     */
    static SpanningTree of(SiteList sites, int width, int maxDistance) {
        final var edges = SiteEdges.within(sites, width, maxDistance);
        final var groups = new UnionFind(sites.size());
        final var tree = new long[Math.max(0, sites.size() - 1)];
        final var rest = new long[edges.length];
        var treeSize = 0;
        var restSize = 0;
        for (final var edge : edges) {
            if (groups.union(SiteEdges.from(edge), SiteEdges.to(edge))) {
                tree[treeSize] = edge;
                treeSize++;
            } else {
                rest[restSize] = edge;
                restSize++;
            }
        }
        return new SpanningTree(Arrays.copyOf(tree, treeSize), Arrays.copyOf(rest, restSize), groups.sets());
    }

    /**
     * Returns the edges of the forest, shortest first, as packed by {@link SiteEdges}.
     *
     * @return a copy of the tree edges: the number of sites minus the number of components of them
     */
    long[] treeEdges() {
        return tree.clone();
    }

    /**
     * Returns the candidate edges that were not needed to connect the sites, shortest first.
     *
     * @return a copy of the edges that would each close a cycle
     */
    long[] cycleEdges() {
        return cycleEdges.clone();
    }

    /**
     * Returns the number of groups the sites fall into under the length limit.
     *
     * @return the count, 1 when every site can be reached from every other, 0 when there are no sites
     */
    int components() {
        return components;
    }
}
