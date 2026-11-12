package com.feuerschvenger.perlinsedge.infra.fx.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WheelAccumulatorTest {

    @Test
    void smallDeltasAddUpToOneStep() {
        WheelAccumulator wheel = new WheelAccumulator();

        assertEquals(0, wheel.add(15));
        assertEquals(0, wheel.add(15));
        assertEquals(1, wheel.add(15));
        assertEquals(0, wheel.add(15));
    }

    @Test
    void aLongGestureOfTinyEventsYieldsFewSteps() {
        WheelAccumulator wheel = new WheelAccumulator();
        int steps = 0;
        for (int i = 0; i < 100; i++) {
            steps += Math.abs(wheel.add(-2));
        }

        assertEquals(5, steps);
    }

    @Test
    void aNotchIsOneStepInItsOwnDirection() {
        WheelAccumulator wheel = new WheelAccumulator();

        assertEquals(1, wheel.add(40));
        assertEquals(-1, wheel.add(-40));
    }

    @Test
    void aDirectionChangeDropsTheAccumulatedAmount() {
        WheelAccumulator wheel = new WheelAccumulator();
        wheel.add(30);

        assertEquals(0, wheel.add(-30));
        assertEquals(-1, wheel.add(-10));
    }

    @Test
    void resetDropsTheAccumulatedAmount() {
        WheelAccumulator wheel = new WheelAccumulator();
        wheel.add(30);
        wheel.reset();

        assertEquals(0, wheel.add(30));
    }

    @Test
    void aZeroDeltaDoesNothing() {
        WheelAccumulator wheel = new WheelAccumulator();
        wheel.add(30);

        assertEquals(0, wheel.add(0));
        assertEquals(1, wheel.add(10));
    }

    @Test
    void aPartialSumIsForgottenAfterTheWheelWasIdle() {
        long[] now = {0};
        WheelAccumulator wheel = new WheelAccumulator(() -> now[0]);

        assertEquals(0, wheel.add(30));
        now[0] += WheelAccumulator.IDLE_RESET_NANOS + 1;

        assertEquals(0, wheel.add(30), "the stale 30 must not complete this notch");
        assertEquals(1, wheel.add(10), "the fresh sum still counts");
    }

    @Test
    void aPartialSumSurvivesInputThatKeepsComingWithinTheIdleWindow() {
        long[] now = {0};
        WheelAccumulator wheel = new WheelAccumulator(() -> now[0]);

        assertEquals(0, wheel.add(15));
        now[0] += WheelAccumulator.IDLE_RESET_NANOS;
        assertEquals(0, wheel.add(15));
        now[0] += WheelAccumulator.IDLE_RESET_NANOS;

        assertEquals(1, wheel.add(15), "each event came within the window, so the sum is kept");
    }

    @Test
    void aLargeDeltaYieldsOneStepAndCarriesTheRemainder() {
        long[] now = {0};
        WheelAccumulator wheel = new WheelAccumulator(() -> now[0]);

        assertEquals(1, wheel.add(100), "an event never yields more than one step");
        assertEquals(1, wheel.add(20), "the 60 units left over and the 20 new ones make the next step");
        assertEquals(1, wheel.add(10), "40 units were still left over");
        assertEquals(0, wheel.add(10), "only 10 units are left now, with 10 more that is still short of a step");
    }

    @Test
    void aLargeDeltaDownwardsYieldsOneNegativeStep() {
        WheelAccumulator wheel = new WheelAccumulator(() -> 0L);

        assertEquals(-1, wheel.add(-125));
        assertEquals(-1, wheel.add(-35), "the 85 units left over are carried in the same direction");
        assertEquals(-1, wheel.add(-1), "80 units were still left over");
    }
}
