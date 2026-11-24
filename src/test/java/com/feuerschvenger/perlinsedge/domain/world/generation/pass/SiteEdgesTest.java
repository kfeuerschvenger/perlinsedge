package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.generation.SiteList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteEdgesTest {

    private static final int WIDTH = 20;
    private static final int FAR = 10;

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

    @Test
    void edgesComeShortestFirstAndNameTheirSites() {
        final var list = sites(at(0, 0), at(3, 0), at(0, 4));

        final var edges = SiteEdges.within(list, WIDTH, FAR);

        assertEquals(3, edges.length);
        assertEquals(9, SiteEdges.lengthSquared(edges[0]));
        assertEquals(0, SiteEdges.from(edges[0]));
        assertEquals(1, SiteEdges.to(edges[0]));
        assertEquals(16, SiteEdges.lengthSquared(edges[1]));
        assertEquals(0, SiteEdges.from(edges[1]));
        assertEquals(2, SiteEdges.to(edges[1]));
        assertEquals(25, SiteEdges.lengthSquared(edges[2]));
        assertEquals(1, SiteEdges.from(edges[2]));
        assertEquals(2, SiteEdges.to(edges[2]));
    }

    @Test
    void edgesOfEqualLengthAreOrderedByTheirPositions() {
        final var list = sites(at(0, 0), at(2, 0), at(4, 0), at(6, 0));

        final var edges = SiteEdges.within(list, WIDTH, 2);

        assertEquals(3, edges.length);
        assertEquals(0, SiteEdges.from(edges[0]));
        assertEquals(1, SiteEdges.from(edges[1]));
        assertEquals(2, SiteEdges.from(edges[2]));
    }

    @Test
    void theLimitIsInclusiveAndLongerEdgesAreLeftOut() {
        final var list = sites(at(0, 0), at(3, 4));

        assertEquals(1, SiteEdges.within(list, WIDTH, 5).length);
        assertEquals(0, SiteEdges.within(list, WIDTH, 4).length);
    }

    @Test
    void fewerThanTwoSitesHaveNoEdges() {
        assertEquals(0, SiteEdges.within(sites(), WIDTH, FAR).length);
        assertEquals(0, SiteEdges.within(sites(at(1, 1)), WIDTH, FAR).length);
    }

    @Test
    void theLongestAllowedEdgeKeepsItsLength() {
        final var wide = SiteEdges.MAX_LENGTH + 1;
        final var list = sites(0, SiteEdges.MAX_LENGTH);

        final var edges = SiteEdges.within(list, wide, SiteEdges.MAX_LENGTH);

        assertEquals(1, edges.length);
        assertEquals(SiteEdges.MAX_LENGTH * SiteEdges.MAX_LENGTH, SiteEdges.lengthSquared(edges[0]));
        assertTrue(edges[0] > 0);
    }

    @Test
    void invalidArgumentsAreRefused() {
        final var list = sites(0, 1);
        final var tooMany = new SiteList();
        for (var position = 0; position <= SiteEdges.MAX_SITES; position++) {
            tooMany.add(position);
        }

        assertThrows(NullPointerException.class, () -> SiteEdges.within(null, WIDTH, FAR));
        assertThrows(IllegalArgumentException.class, () -> SiteEdges.within(list, 0, FAR));
        assertThrows(IllegalArgumentException.class, () -> SiteEdges.within(list, -WIDTH, FAR));
        assertThrows(IllegalArgumentException.class, () -> SiteEdges.within(list, WIDTH, -1));
        assertThrows(IllegalArgumentException.class, () -> SiteEdges.within(list, WIDTH, SiteEdges.MAX_LENGTH + 1));
        assertThrows(IllegalArgumentException.class, () -> SiteEdges.within(tooMany, WIDTH, FAR));
    }
}
