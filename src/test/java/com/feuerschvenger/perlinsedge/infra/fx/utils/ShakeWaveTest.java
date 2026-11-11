package com.feuerschvenger.perlinsedge.infra.fx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShakeWaveTest {

    @Test
    void theWaveIsFlatBeforeTheStartAndAfterTheEnd() {
        assertEquals(0.0, ShakeWave.offset(-0.1, 0.2, 1.0, 3.0));
        assertEquals(0.0, ShakeWave.offset(0.2, 0.2, 1.0, 3.0));
        assertEquals(0.0, ShakeWave.offset(Double.POSITIVE_INFINITY, 0.2, 1.0, 3.0));
    }

    @Test
    void theSwingsNeverExceedTheAmplitudeAndFadeOut() {
        double early = 0.0;
        double late = 0.0;
        for (int i = 0; i < 200; i++) {
            double t = i * 0.001;
            double value = Math.abs(ShakeWave.offset(t, 0.2, 2.0, 3.0));
            assertTrue(value <= 2.0);
            if (t < 0.1) {
                early = Math.max(early, value);
            } else {
                late = Math.max(late, value);
            }
        }
        assertTrue(early > late);
    }

    @Test
    void theHitAndHurtShakesUseTheSameCurve() {
        assertEquals(ShakeWave.offset(0.05, HitShake.DURATION_SECONDS, HitShake.AMPLITUDE_ART_PIXELS, HitShake.SWINGS),
                HitShake.offset(0.05));
        assertEquals(ShakeWave.offset(0.05, HurtReaction.FLASH_SECONDS, HurtReaction.SHAKE_AMPLITUDE_ART_PIXELS,
                HurtReaction.SWINGS), HurtReaction.shakeOffset(0.05));
    }

    @Test
    void theScreenOffsetSnapsToWholePixels() {
        assertEquals(0.0, ShakeWave.toScreen(0.0, 2.0));
        assertEquals(3.0, ShakeWave.toScreen(0.7, 4.0));
    }
}
