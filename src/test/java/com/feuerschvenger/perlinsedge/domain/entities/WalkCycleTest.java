package com.feuerschvenger.perlinsedge.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WalkCycleTest {

    @Test
    void aWalkPassesThroughTheStandingPoseBetweenSteps() {
        assertEquals(WalkCycle.WALK_A, WalkCycle.frameAt(0));
        assertEquals(WalkCycle.IDLE, WalkCycle.frameAt(1));
        assertEquals(WalkCycle.WALK_B, WalkCycle.frameAt(2));
        assertEquals(WalkCycle.IDLE, WalkCycle.frameAt(3));
    }

    @Test
    void theCycleRepeatsAndWrapsNegativeSteps() {
        assertEquals(WalkCycle.WALK_A, WalkCycle.frameAt(WalkCycle.length()));
        assertEquals(WalkCycle.WALK_B, WalkCycle.frameAt(-2));
    }
}
