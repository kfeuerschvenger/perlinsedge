package com.feuerschvenger.perlinsedge.domain.strategies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlimeStrikeCycleTest {

    private static final double STEP = 0.1;

    @Test
    void nothingRunsWhileThePlayerIsOutOfReach() {
        SlimeStrikeCycle cycle = new SlimeStrikeCycle(0.6, 1.5);

        for (int i = 0; i < 100; i++) {
            assertFalse(cycle.update(STEP, false));
        }

        assertEquals(SlimeStrikeCycle.Phase.READY, cycle.getPhase());
        assertEquals(0.0, cycle.getWindupProgress());
    }

    @Test
    void thePlayerInReachStartsTheSquashAndTheStrikeLandsWhenItEnds() {
        SlimeStrikeCycle cycle = new SlimeStrikeCycle(0.6, 1.5);

        assertFalse(cycle.update(STEP, true));
        assertEquals(SlimeStrikeCycle.Phase.WINDUP, cycle.getPhase());

        int updatesToStrike = 1;
        while (!cycle.update(STEP, true)) {
            updatesToStrike++;
            assertTrue(updatesToStrike < 20, "the strike never landed");
        }

        // One update starts the squash, the next six fill it
        assertEquals(7, updatesToStrike + 1);
        assertEquals(SlimeStrikeCycle.Phase.WAIT, cycle.getPhase());
    }

    @Test
    void theSquashGrowsFromNothingToTheStrike() {
        SlimeStrikeCycle cycle = new SlimeStrikeCycle(1.0, 1.0);
        cycle.update(0.1, true);

        double previous = 0;
        for (int i = 0; i < 8; i++) {
            cycle.update(0.1, true);
            assertTrue(cycle.getWindupProgress() > previous, "the squash keeps growing");
            previous = cycle.getWindupProgress();
        }
        assertTrue(previous < 1.0);
    }

    @Test
    void afterAStrikeTheSlimeWaitsTheFullWaitBeforeTheNextSquash() {
        SlimeStrikeCycle cycle = new SlimeStrikeCycle(0.6, 1.5);
        strike(cycle);

        double waited = 0;
        while (cycle.getPhase() == SlimeStrikeCycle.Phase.WAIT) {
            assertFalse(cycle.update(STEP, true));
            assertEquals(0.0, cycle.getWindupProgress(), "no squash while it waits");
            waited += STEP;
        }

        assertEquals(1.5, waited, STEP);
        assertEquals(SlimeStrikeCycle.Phase.READY, cycle.getPhase());
    }

    @Test
    void aSlimeThatKeepsThePlayerInReachStrikesOnceEveryWindupPlusWait() {
        SlimeStrikeCycle cycle = new SlimeStrikeCycle(0.6, 1.5);

        int strikes = 0;
        for (int i = 0; i < 100; i++) {
            if (cycle.update(0.1, true)) {
                strikes++;
            }
        }

        // 10 seconds in 0.1 second updates: strikes on updates 7, 29, 51, 73 and 95, a squash and a wait and the
        // update that starts each squash apart
        assertEquals(5, strikes);
    }

    @Test
    void aSquashCutShortStartsOverFromNothing() {
        SlimeStrikeCycle cycle = new SlimeStrikeCycle(0.6, 1.5);
        cycle.update(STEP, true);
        cycle.update(STEP, true);
        cycle.update(STEP, true);

        cycle.update(STEP, false);

        assertEquals(SlimeStrikeCycle.Phase.READY, cycle.getPhase());
        assertEquals(0.0, cycle.getWindupProgress());
        cycle.update(STEP, true);
        assertEquals(0.0, cycle.getWindupProgress(), "the squash begins again from the start");
    }

    @Test
    void theWaitAfterAStrikeKeepsRunningWhenThePlayerLeavesTheReach() {
        SlimeStrikeCycle cycle = new SlimeStrikeCycle(0.6, 1.5);
        strike(cycle);

        for (int i = 0; i < 16; i++) {
            cycle.update(STEP, false);
        }

        assertEquals(SlimeStrikeCycle.Phase.READY, cycle.getPhase());
    }

    @Test
    void theTimesMustBeValid() {
        assertThrows(IllegalArgumentException.class, () -> new SlimeStrikeCycle(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new SlimeStrikeCycle(1, -1));
    }

    private static void strike(SlimeStrikeCycle cycle) {
        for (int i = 0; i < 100 && !cycle.update(STEP, true); i++) {
            // keep squashing until the strike lands
        }
    }
}
