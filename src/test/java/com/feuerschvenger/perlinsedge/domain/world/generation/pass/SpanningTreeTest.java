package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.SiteList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SpanningTreeTest {

    private static final int WIDTH = 50;
    private static final int SITES = 40;
    private static final int REACH = 100;
    private static final long SEED = 5L;
    private static final int SQUARE = 2;
    private static final int SQUARE_REACH = 3;
    private static final int CLUSTER_REACH = 5;

    private static int at(int x, int y) {
        return y * WIDTH + x;
    }

    private static SiteList sites(int... tiles) {
        final var list = new SiteList();
        for (final var tile : tiles) {
            list.add(tile);
        }
        return list;
    }

    private static long totalLength(long[] edges) {
        var total = 0L;
        for (final var edge : edges) {
            total += SiteEdges.lengthSquared(edge);
        }
        return total;
    }

    /** The weight of a minimum spanning tree by Prim's algorithm, as the reference. */
    private static long primWeight(SiteList list) {
        final var count = list.size();
        final var best = new long[count];
        final var inTree = new boolean[count];
        Arrays.fill(best, Long.MAX_VALUE);
        best[0] = 0;
        var total = 0L;
        for (var round = 0; round < count; round++) {
            var node = -1;
            for (var candidate = 0; candidate < count; candidate++) {
                if (!inTree[candidate] && (node < 0 || best[candidate] < best[node])) {
                    node = candidate;
                }
            }
            inTree[node] = true;
            total += best[node];
            for (var other = 0; other < count; other++) {
                if (!inTree[other]) {
                    best[other] = Math.min(best[other], squared(list, node, other));
                }
            }
        }
        return total;
    }

    private static long squared(SiteList list, int first, int second) {
        final long dx = list.get(first) % WIDTH - list.get(second) % WIDTH;
        final long dy = list.get(first) / WIDTH - list.get(second) / WIDTH;
        return dx * dx + dy * dy;
    }

    @Test
    void theTreeOfSitesInALineJoinsNeighboursOnly() {
        final var list = sites(at(0, 0), at(5, 0), at(11, 0), at(18, 0));

        final var tree = SpanningTree.of(list, WIDTH, REACH);

        assertEquals(1, tree.components());
        assertEquals(3, tree.treeEdges().length);
        assertEquals(3, tree.cycleEdges().length);
        assertArrayEquals(new int[] {25, 36, 49},
                Arrays.stream(tree.treeEdges()).mapToInt(SiteEdges::lengthSquared).toArray());
    }

    @Test
    void theTotalLengthMatchesPrimOnRandomSites() {
        final var random = new SplittableRandom(SEED);
        final var chosen = new TreeSet<Integer>();
        while (chosen.size() < SITES) {
            chosen.add(random.nextInt(WIDTH * WIDTH));
        }
        final var list = new SiteList();
        chosen.forEach(list::add);

        final var tree = SpanningTree.of(list, WIDTH, REACH);

        assertEquals(1, tree.components());
        assertEquals(SITES - 1, tree.treeEdges().length);
        assertEquals(primWeight(list), totalLength(tree.treeEdges()));
        assertEquals(SITES * (SITES - 1) / 2 - (SITES - 1), tree.cycleEdges().length);
    }

    @Test
    void theEdgesLeftOverAreTheOnesThatCloseCyclesInLengthOrder() {
        final var list = sites(at(0, 0), at(SQUARE, 0), at(0, SQUARE), at(SQUARE, SQUARE));

        final var tree = SpanningTree.of(list, WIDTH, SQUARE_REACH);

        assertEquals(1, tree.components());
        assertArrayEquals(new int[] {4, 4, 4},
                Arrays.stream(tree.treeEdges()).mapToInt(SiteEdges::lengthSquared).toArray());
        assertArrayEquals(new int[] {4, 8, 8},
                Arrays.stream(tree.cycleEdges()).mapToInt(SiteEdges::lengthSquared).toArray());
    }

    @Test
    void sitesTooFarApartFormSeparateTrees() {
        final var list = sites(at(0, 0), at(1, 0), at(2, 0), at(40, 40), at(41, 40));

        final var tree = SpanningTree.of(list, WIDTH, CLUSTER_REACH);

        assertEquals(2, tree.components());
        assertEquals(list.size() - 2, tree.treeEdges().length);
    }

    @Test
    void noSiteGivesAnEmptyForest() {
        final var tree = SpanningTree.of(sites(), WIDTH, REACH);

        assertEquals(0, tree.components());
        assertEquals(0, tree.treeEdges().length);
        assertEquals(0, tree.cycleEdges().length);
    }

    @Test
    void aSingleSiteIsItsOwnTree() {
        final var tree = SpanningTree.of(sites(at(3, 3)), WIDTH, REACH);

        assertEquals(1, tree.components());
        assertEquals(0, tree.treeEdges().length);
    }

    @Test
    void theArraysReturnedAreCopies() {
        final var tree = SpanningTree.of(sites(at(0, 0), at(1, 0)), WIDTH, REACH);

        final var edges = tree.treeEdges();
        edges[0] = 0L;

        assertNotEquals(0L, tree.treeEdges()[0]);
    }
}
