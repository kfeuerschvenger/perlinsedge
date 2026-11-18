package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void aCopyHoldsTheSameShadowsAndIsRecognisedAsTheSame() {
        final Silhouette silhouette = new Silhouette(1, 1, new int[]{0xFFFFFFFF});
        final ShadowList list = new ShadowList();
        list.add(1, 2, 3, 0.5);
        list.addSilhouette(4, 5, silhouette, 1, 1.0, 1.0);
        for (int i = 0; i < 200; i++) {
            list.add(i, i, 5, 1.0);
        }

        final ShadowList copy = new ShadowList();
        copy.copyFrom(list);

        assertEquals(202, copy.size());
        assertSame(silhouette, copy.silhouette(1));
        assertEquals(4, copy.centreX(1));
        assertTrue(copy.sameShadowsAs(list));
        assertTrue(list.sameShadowsAs(copy));
    }

    @Test
    void anyDifferenceInAShadowMakesTheListsDiffer() {
        final Silhouette silhouette = new Silhouette(1, 1, new int[]{0xFFFFFFFF});
        final Silhouette other = new Silhouette(1, 1, new int[]{0xFFFFFFFF});
        final ShadowList base = new ShadowList();
        base.addSilhouette(4, 5, silhouette, 1, 1.0, 1.0);

        final ShadowList moved = new ShadowList();
        moved.addSilhouette(4.5, 5, silhouette, 1, 1.0, 1.0);
        final ShadowList fainter = new ShadowList();
        fainter.addSilhouette(4, 5, silhouette, 1, 1.0, 0.5);
        final ShadowList narrower = new ShadowList();
        narrower.addSilhouette(4, 5, silhouette, 1, 0.5, 1.0);
        final ShadowList otherShape = new ShadowList();
        otherShape.addSilhouette(4, 5, other, 1, 1.0, 1.0);
        final ShadowList longer = new ShadowList();
        longer.addSilhouette(4, 5, silhouette, 1, 1.0, 1.0);
        longer.add(1, 1, 1, 1);

        assertFalse(base.sameShadowsAs(moved));
        assertFalse(base.sameShadowsAs(fainter));
        assertFalse(base.sameShadowsAs(narrower));
        assertFalse(base.sameShadowsAs(otherShape));
        assertFalse(base.sameShadowsAs(longer));
        assertTrue(new ShadowList().sameShadowsAs(new ShadowList()));
    }

    @Test
    void copyingAShorterListDropsTheSpritesOfTheLongerOne() {
        final Silhouette silhouette = new Silhouette(1, 1, new int[]{0xFFFFFFFF});
        final ShadowList copy = new ShadowList();
        final ShadowList long3 = new ShadowList();
        long3.addSilhouette(1, 1, silhouette, 1, 1.0, 1.0);
        long3.addSilhouette(2, 2, silhouette, 1, 1.0, 1.0);
        long3.addSilhouette(3, 3, silhouette, 1, 1.0, 1.0);
        copy.copyFrom(long3);

        final ShadowList short1 = new ShadowList();
        short1.add(9, 9, 2, 1.0);
        copy.copyFrom(short1);

        assertEquals(1, copy.size());
        assertNull(copy.silhouette(0));
        assertTrue(copy.sameShadowsAs(short1));
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

    @Test
    void aSpriteDrawnAtItsOwnSizeHasTheDrawnSizeOfItsSilhouette() {
        final Silhouette sprite = new Silhouette(4, 6, new int[24]);
        final ShadowList list = new ShadowList();
        list.addSilhouette(10, 20, sprite, 5, 0.5, 1.0);

        assertEquals(4, list.drawnWidth(0));
        assertEquals(5, list.drawnHeight(0));
        assertEquals(2, list.widthArt(0));
    }

    @Test
    void theDrawnSizeIsKeptByCopiesAndTellsListsApart() {
        final Silhouette sprite = new Silhouette(4, 6, new int[24]);
        final ShadowList squashed = new ShadowList();
        squashed.addSilhouette(10, 20, sprite, 6, 1.0, new ShadowList.SpriteSize(4.5, 5.0), 1.0);
        final ShadowList plain = new ShadowList();
        plain.addSilhouette(10, 20, sprite, 6, 1.0, 1.0);

        assertFalse(squashed.sameShadowsAs(plain));
        final ShadowList copy = new ShadowList();
        copy.copyFrom(squashed);
        assertTrue(copy.sameShadowsAs(squashed));
        assertEquals(4.5, copy.drawnWidth(0));
        assertEquals(5.0, copy.drawnHeight(0));
    }
}
