package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShadowListTest {

    @Test
    void keepsWhatWasAddedInOrder() {
        ShadowList list = new ShadowList();
        list.add(1, 2, 3, 0.5);
        list.add(4, 5, 6, 1.0);

        assertEquals(2, list.size());
        assertEquals(1, list.centreX(0));
        assertEquals(2, list.centreY(0));
        assertEquals(3, list.widthArt(0));
        assertEquals(0.5, list.fade(0));
        assertEquals(4, list.centreX(1));
        assertEquals(1.0, list.fade(1));
    }

    @Test
    void growsPastItsInitialCapacityWithoutLosingShadows() {
        ShadowList list = new ShadowList();
        for (int i = 0; i < 1000; i++) {
            list.add(i, i * 2.0, 10, 1.0);
        }

        assertEquals(1000, list.size());
        assertEquals(999, list.centreX(999));
        assertEquals(1998, list.centreY(999));
        assertEquals(500, list.centreX(500));
    }

    @Test
    void clearEmptiesTheListAndItIsReusable() {
        ShadowList list = new ShadowList();
        list.add(1, 1, 1, 1);
        list.clear();
        assertEquals(0, list.size());

        list.add(7, 8, 9, 0.25);
        assertEquals(1, list.size());
        assertEquals(7, list.centreX(0));
    }
}
