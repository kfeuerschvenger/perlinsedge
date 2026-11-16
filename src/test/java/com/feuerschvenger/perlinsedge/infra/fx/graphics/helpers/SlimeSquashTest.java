package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlimeSquashTest {

    @Test
    void aSlimeThatIsNotBreathingAndNotWindingUpIsDrawnAtItsFullSize() {
        assertEquals(1.0, SlimeSquash.height(0, 0), 1e-12);
        assertEquals(1.0, SlimeSquash.width(0, 0), 1e-12);
    }

    @Test
    void breathingStretchesTallWhileItNarrowsAndTheOtherWayRound() {
        assertTrue(SlimeSquash.height(0, 1) > 1.0);
        assertTrue(SlimeSquash.width(0, 1) < 1.0);
        assertTrue(SlimeSquash.height(0, -1) < 1.0);
        assertTrue(SlimeSquash.width(0, -1) > 1.0);
    }

    @Test
    void aBreathStaysGentle() {
        assertEquals(1.0 + SlimeSquash.BREATH_AMPLITUDE, SlimeSquash.height(0, 1), 1e-12);
        assertTrue(SlimeSquash.BREATH_AMPLITUDE <= 0.08, "the breathing is a small movement");
    }

    @Test
    void theBreathRepeatsEveryPeriodAndDiffersBetweenOffsets() {
        assertEquals(SlimeSquash.breath(1.0, 0.0), SlimeSquash.breath(1.0 + SlimeSquash.BREATH_PERIOD_SECONDS, 0.0), 1e-9);
        assertTrue(Math.abs(SlimeSquash.breath(1.0, 0.0) - SlimeSquash.breath(1.0, 0.5)) > 0.1);
    }

    @Test
    void theSquashBeforeAStrikeGrowsFlatterAndWiderAndIgnoresTheBreath() {
        double early = SlimeSquash.height(0.2, 1);
        double late = SlimeSquash.height(0.9, 1);

        assertTrue(late < early);
        assertTrue(late < 1.0);
        assertTrue(SlimeSquash.width(0.9, 1) > SlimeSquash.width(0.2, 1));
        assertEquals(SlimeSquash.height(0.5, 1), SlimeSquash.height(0.5, -1), 1e-12);
    }

    @Test
    void theSquashNeverGoesBeyondItsLimit() {
        assertEquals(1.0 - SlimeSquash.WINDUP_SQUASH, SlimeSquash.height(5, 0), 1e-12);
        assertEquals(1.0 + SlimeSquash.WINDUP_SPREAD, SlimeSquash.width(5, 0), 1e-12);
    }
}
