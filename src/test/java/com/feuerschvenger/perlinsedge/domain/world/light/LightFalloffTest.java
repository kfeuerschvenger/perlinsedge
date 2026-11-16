package com.feuerschvenger.perlinsedge.domain.world.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightFalloffTest {

    @Test
    void theLightIsFullAtTheSourceAndExactlyNothingAtAndBeyondTheRadius() {
        assertEquals(1.0, LightFalloff.level(0.0));
        assertEquals(0.0, LightFalloff.level(1.0));
        assertEquals(0.0, LightFalloff.level(3.0));
        assertEquals(0.0, LightFalloff.level(Double.NaN));
        assertEquals(1.0, LightFalloff.level(-0.5), "a distance below zero counts as the source");
    }

    @Test
    void theLightNeverRisesWithTheDistance() {
        double previous = 1.0;
        for (double square = 0.0; square <= 1.2; square += 0.0005) {
            final double level = LightFalloff.level(square);
            assertTrue(level <= previous + 1e-12, "rose at " + square);
            previous = level;
        }
    }

    @Test
    void theLightFadesWithoutAnyStep() {
        // A hundred cells to the radius: no cell differs from its neighbour by more than a few hundredths
        double previous = LightFalloff.level(0.0);
        double biggest = 0.0;
        for (int cell = 1; cell <= 100; cell++) {
            final double share = cell / 100.0;
            final double level = LightFalloff.level(share * share);
            biggest = Math.max(biggest, previous - level);
            previous = level;
        }
        assertTrue(biggest < 0.03, "the biggest step between neighbours is " + biggest);
        assertTrue(LightFalloff.level(0.0001) > 0.0 && LightFalloff.level(0.9999) < 0.001, "it reaches zero smoothly");
    }

    @Test
    void theGradientTakesManyDistinctLevelsInsteadOfARingOrTwo() {
        final java.util.Set<Long> levels = new java.util.HashSet<>();
        for (double d = 0.0; d < 1.0; d += 0.001) {
            levels.add(Math.round(LightFalloff.level(d * d) * 1_000_000.0));
        }
        assertTrue(levels.size() > 500, "" + levels.size());
    }
}
