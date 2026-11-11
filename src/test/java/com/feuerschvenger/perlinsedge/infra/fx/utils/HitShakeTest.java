package com.feuerschvenger.perlinsedge.infra.fx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitShakeTest {

    @Test
    void thereIsNoShakeBeforeAHitOrAfterItEnds() {
        assertEquals(0.0, HitShake.offset(Double.POSITIVE_INFINITY));
        assertEquals(0.0, HitShake.offset(-0.1));
        assertEquals(0.0, HitShake.offset(HitShake.DURATION_SECONDS));
        assertEquals(0.0, HitShake.offset(5.0));
        assertEquals(0.0, HitShake.offset(0.0));
    }

    @Test
    void theShakeSwingsToBothSidesWithinTheAmplitude() {
        boolean left = false;
        boolean right = false;
        for (double s = 0; s < HitShake.DURATION_SECONDS; s += 0.001) {
            double offset = HitShake.offset(s);
            assertTrue(Math.abs(offset) <= HitShake.AMPLITUDE_ART_PIXELS + 1e-9);
            left |= offset < -0.3;
            right |= offset > 0.3;
        }
        assertTrue(left && right);
    }

    @Test
    void theShakeDecays() {
        double early = 0;
        double late = 0;
        for (double s = 0; s < HitShake.DURATION_SECONDS / 3; s += 0.0005) {
            early = Math.max(early, Math.abs(HitShake.offset(s)));
        }
        for (double s = HitShake.DURATION_SECONDS * 2 / 3; s < HitShake.DURATION_SECONDS; s += 0.0005) {
            late = Math.max(late, Math.abs(HitShake.offset(s)));
        }
        assertTrue(late < early);
    }

    @Test
    void theScreenOffsetIsWholePixelsAndScalesWithTheArt() {
        for (double s = 0; s < HitShake.DURATION_SECONDS; s += 0.003) {
            double offset = HitShake.screenOffset(s, 2.0);
            assertEquals(Math.rint(offset), offset);
            assertTrue(Math.abs(offset) <= 2.0);
        }
        assertEquals(0.0, HitShake.screenOffset(1.0, 4.0));
    }
}
