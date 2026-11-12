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
    void aLargeDeltaYieldsOneStepAndCarriesOnlyTheFractionOfAStep() {
        long[] now = {0};
        WheelAccumulator wheel = new WheelAccumulator(() -> now[0]);

        assertEquals(1, wheel.add(100), "an event never yields more than one step");
        assertEquals(0, wheel.add(10), "20 units are carried, with 10 more that is still short of a step");
        assertEquals(1, wheel.add(10), "the carried 20 and the 20 new ones make a step");
        assertEquals(0, wheel.add(10), "nothing is left over");
    }

    @Test
    void aLargeDeltaDownwardsYieldsOneNegativeStepAndCarriesTheFractionInTheSameDirection() {
        WheelAccumulator wheel = new WheelAccumulator(() -> 0L);

        assertEquals(-1, wheel.add(-125));
        assertEquals(0, wheel.add(-30), "5 units are carried, with 30 more that is 35, still short of a step");
        assertEquals(-1, wheel.add(-5), "the carried 35 and 5 more make a step");
    }

    @Test
    void anOversizedDeltaFollowedByTinyEventsYieldsOneStepAndNotAStream() {
        long[] now = {0};
        WheelAccumulator wheel = new WheelAccumulator(() -> now[0]);

        int steps = wheel.add(1000);
        for (int i = 0; i < 30; i++) {
            steps += wheel.add(1);
        }

        assertEquals(1, steps);
    }
}
