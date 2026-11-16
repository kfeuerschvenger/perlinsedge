package com.feuerschvenger.perlinsedge.domain.world.light;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightFalloffTest {

    @Test
    void theInnermostRingIsFullAndNothingLiesOutsideTheRadius() {
        assertEquals(1.0, LightFalloff.level(0.0, 5));
        assertEquals(0.0, LightFalloff.level(1.0, 5));
        assertEquals(0.0, LightFalloff.level(3.0, 5));
        assertEquals(0.0, LightFalloff.level(Double.NaN, 5));
        assertEquals(1.0, LightFalloff.level(-0.5, 5), "a distance below zero counts as the centre");
    }

    @Test
    void everyRingHasOneFlatLevelOneStepBelowTheRingInsideIt() {
        assertEquals(1.0, LightFalloff.level(0.19, 5), 1e-12);
        assertEquals(0.8, LightFalloff.level(0.2, 5), 1e-12);
        assertEquals(0.8, LightFalloff.level(0.39, 5), 1e-12);
        assertEquals(0.6, LightFalloff.level(0.4, 5), 1e-12);
        assertEquals(0.4, LightFalloff.level(0.6, 5), 1e-12);
        assertEquals(0.2, LightFalloff.level(0.8, 5), 1e-12);
        assertEquals(0.2, LightFalloff.level(0.999, 5), 1e-12);
    }

    @Test
    void theLevelNeverRisesWithTheDistance() {
        double previous = 1.0;
        for (double d = 0.0; d <= 1.2; d += 0.001) {
            final double level = LightFalloff.level(d, 5);
            assertTrue(level <= previous + 1e-12, "rose at " + d);
            previous = level;
        }
    }

    @Test
    void aFadeOfNRingsTakesAtMostNPlusOneDistinctLevels() {
        for (int rings = 1; rings <= 8; rings++) {
            final Set<Long> levels = new TreeSet<>();
            for (double d = -0.1; d <= 1.3; d += 0.0007) {
                levels.add(Math.round(LightFalloff.level(d, rings) * 1_000_000.0));
            }
            assertEquals(rings + 1, levels.size(), "rings " + rings);
        }
    }
}
