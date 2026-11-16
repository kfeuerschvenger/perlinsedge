package com.feuerschvenger.perlinsedge.domain.entities.enemies;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The breath of a slime starts at a phase that follows from its spawn tile, so it does not change between runs. */
class SlimeBreathPhaseTest {

    @Test
    void theSamePhaseComesFromTheSameSpawnTile() {
        assertEquals(new Slime(12, 7).getBreathPhase(), new Slime(12, 7).getBreathPhase(), 0.0);
    }

    @Test
    void aPhaseIsAFractionOfABreath() {
        for (int x = -20; x < 20; x++) {
            for (int y = -20; y < 20; y++) {
                final double phase = new Slime(x, y).getBreathPhase();
                assertTrue(phase >= 0.0 && phase < 1.0, "phase " + phase + " at " + x + "," + y);
            }
        }
    }

    @Test
    void slimesOnNeighbouringTilesBreatheOutOfStep() {
        final Set<Double> phases = new HashSet<>();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                phases.add(new Slime(x, y).getBreathPhase());
            }
        }
        assertTrue(phases.size() > 8, "sixteen neighbouring tiles gave only " + phases.size() + " phases");
    }
}
