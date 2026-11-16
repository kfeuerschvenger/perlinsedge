package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;
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
    void anEllipseShadowHasNoSilhouette() {
        ShadowList list = new ShadowList();
        list.add(1, 2, 3, 0.5);

        assertNull(list.silhouette(0));
        assertEquals(0.0, list.groundRow(0));
    }

    @Test
    void aSpriteShadowKeepsItsSilhouetteGroundRowAndWidth() {
        ShadowList list = new ShadowList();
        Silhouette silhouette = new Silhouette(10, 12, new int[120]);
        list.addSilhouette(5, 6, silhouette, 11, 0.5, 0.8);

        assertSame(silhouette, list.silhouette(0));
        assertEquals(11, list.groundRow(0));
        assertEquals(5.0, list.widthArt(0), "the width is the sprite width times the factor");
        assertEquals(0.8, list.fade(0));
        assertEquals(5, list.centreX(0));
        assertEquals(6, list.centreY(0));
    }

    @Test
    void silhouettesSurviveGrowthAndAreDroppedByClear() {
        ShadowList list = new ShadowList();
        Silhouette silhouette = new Silhouette(2, 2, new int[4]);
        for (int i = 0; i < 300; i++) {
            list.addSilhouette(i, i, silhouette, 2, 1.0, 1.0);
        }
        assertSame(silhouette, list.silhouette(299));

        list.clear();
        list.add(1, 1, 4, 1.0);

        assertNull(list.silhouette(0), "a reused slot must not keep the sprite of an earlier frame");
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
