package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycleSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FxDebugDrawingHelperTest {

    private static DayCycle cycleAt(double seconds) {
        final DayCycle cycle = new DayCycle(new DayCycleSettings(90, 600, 90, 420, 0, 16, 0.6, 2.0));
        cycle.advance(seconds);
        return cycle;
    }

    @Test
    void theSkyLineShowsTheDayThePhaseAndTheTimeIntoTheCycle() {
        assertEquals("Sky: Day 1 DAY 02:05", FxDebugDrawingHelper.describeSky(cycleAt(125), false));
        assertEquals("Sky: Day 2 DAWN 00:10", FxDebugDrawingHelper.describeSky(cycleAt(1210), false));
    }

    @Test
    void theSkyLineMarksTheFastTime() {
        assertEquals("Sky: Day 1 NIGHT 15:00 (fast)", FxDebugDrawingHelper.describeSky(cycleAt(900), true));
    }
}
