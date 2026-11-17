package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PixelSnapTest {

    @Test
    void roundsToTheNearestWholePixelAndATieGoesUp() {
        assertEquals(3.0, PixelSnap.round(2.6));
        assertEquals(2.0, PixelSnap.round(2.4));
        assertEquals(3.0, PixelSnap.round(2.5));
        assertEquals(-2.0, PixelSnap.round(-2.5));
        assertEquals(-3.0, PixelSnap.round(-2.6));
    }

    @Test
    void theValueJustUnderAHalfRoundsDown() {
        // 0.49999999999999994 + 0.5 is 1.0 in floating point, so a floor of the sum would round it up
        assertEquals(0.0, PixelSnap.round(0.49999999999999994));
        assertEquals(2.0, PixelSnap.round(2.4999999999999996));
    }

    @Test
    void aWholePixelOffsetMovesTheResultByExactlyThatOffset() {
        for (double x : new double[]{0.25, 0.5, 0.75, 3.3, 7.5}) {
            for (int k = -4; k <= 4; k++) {
                assertEquals(PixelSnap.round(x) + k, PixelSnap.round(x + k), "x " + x + " k " + k);
            }
        }
    }
}
