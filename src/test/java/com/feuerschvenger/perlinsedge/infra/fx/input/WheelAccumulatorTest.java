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
}
