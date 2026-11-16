package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import com.feuerschvenger.perlinsedge.domain.world.time.DayCycle;
import com.feuerschvenger.perlinsedge.domain.world.time.DayCycleSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FxDebugDrawingHelperTest {

    private static DayCycle cycleAt(double seconds) {
        final DayCycle cycle = new DayCycle(new DayCycleSettings(150, 600, 90, 360, 0, 17, 0.6, 2.0));
        cycle.advance(seconds);
        return cycle;
    }

    @Test
    void theSkyLineShowsTheDayThePhaseAndTheTimeOnThe24HourClock() {
        assertEquals("Sky: Day 1 DAY 06:00", FxDebugDrawingHelper.describeSky(cycleAt(150), false));
        assertEquals("Sky: Day 1 DAY 06:30", FxDebugDrawingHelper.describeSky(cycleAt(175), false));
        assertEquals("Sky: Day 2 DAWN 04:30", FxDebugDrawingHelper.describeSky(cycleAt(1275), false));
    }

    @Test
    void theSkyLineMarksTheFastTime() {
        assertEquals("Sky: Day 1 NIGHT 21:00 (fast)", FxDebugDrawingHelper.describeSky(cycleAt(900), true));
    }
}
