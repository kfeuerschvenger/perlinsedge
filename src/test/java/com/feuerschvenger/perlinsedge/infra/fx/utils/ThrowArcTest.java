package com.feuerschvenger.perlinsedge.infra.fx.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowArcTest {

    @Test
    void theItemIsOnTheGroundAtTheThrowAndAtTheLanding() {
        assertEquals(0.0, ThrowArc.heightFraction(0.0), 1e-12);
        assertEquals(0.0, ThrowArc.heightFraction(1.0), 1e-12);
    }

    @Test
    void theItemIsAtTheTopHalfway() {
        assertEquals(1.0, ThrowArc.heightFraction(0.5), 1e-12);
    }

    @Test
    void theArcIsSymmetric() {
        assertEquals(ThrowArc.heightFraction(0.2), ThrowArc.heightFraction(0.8), 1e-12);
    }

    @Test
    void theProgressIsClampedSoAnItemNeverSinksBelowTheGround() {
        assertEquals(0.0, ThrowArc.heightFraction(-0.5), 1e-12);
        assertEquals(0.0, ThrowArc.heightFraction(3.0), 1e-12);
    }

    @Test
    void theLiftScalesWithTheZoom() {
        assertEquals(ThrowArc.PEAK_PIXELS, ThrowArc.liftPixels(0.5, 1.0), 1e-12);
        assertEquals(ThrowArc.PEAK_PIXELS * 2, ThrowArc.liftPixels(0.5, 2.0), 1e-12);
        assertTrue(ThrowArc.liftPixels(0.25, 1.0) < ThrowArc.PEAK_PIXELS);
    }
}
