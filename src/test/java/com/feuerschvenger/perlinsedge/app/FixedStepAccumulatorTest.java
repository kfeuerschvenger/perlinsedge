package com.feuerschvenger.perlinsedge.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses binary-exact durations (multiples of 1/16 s) so the floating point arithmetic is deterministic.
 */
class FixedStepAccumulatorTest {

    private static final double STEP = 0.125;
    private static final double MAX_DELTA = 0.5;
    /** One sixteenth of a second in nanoseconds. */
    private static final long UNIT = 62_500_000L;
    private static final long START = 1_000_000_000L;

    private FixedStepAccumulator stepper;

    @BeforeEach
    void setUp() {
        stepper = new FixedStepAccumulator(STEP, MAX_DELTA);
        stepper.advance(START);
    }

    @Test
    void firstFrameOnlyRecordsTheTimestamp() {
        FixedStepAccumulator fresh = new FixedStepAccumulator(STEP, MAX_DELTA);

        assertFalse(fresh.hasStarted());
        assertEquals(0, fresh.advance(START));
        assertTrue(fresh.hasStarted());
        assertEquals(0.0, fresh.alpha());
        assertEquals(0.0, fresh.lastDelta());
    }

    @Test
    void frameShorterThanOneStepRunsNothingAndExposesAlpha() {
        assertEquals(0, stepper.advance(START + UNIT));

        assertEquals(0.5, stepper.alpha(), 1e-12);
        assertEquals(0.0625, stepper.lastDelta(), 1e-12);
    }

    @Test
    void longerFrameRunsSeveralStepsAndKeepsTheRemainder() {
        assertEquals(3, stepper.advance(START + 7 * UNIT));

        assertEquals(0.5, stepper.alpha(), 1e-12);
    }

    @Test
    void alphaStaysInsideTheUnitRange() {
        long now = START;
        for (int i = 0; i < 500; i++) {
            now += 3 * UNIT;
            stepper.advance(now);
            assertTrue(stepper.alpha() >= 0.0 && stepper.alpha() < 1.0, "alpha was " + stepper.alpha());
        }
    }

    @Test
    void hugeFrameIsClampedToTheMaximumDelta() {
        int steps = stepper.advance(START + 1_000_000 * UNIT);

        assertEquals(MAX_DELTA, stepper.lastDelta(), 1e-12);
        assertEquals(4, steps);
        assertEquals(0.0, stepper.alpha(), 1e-12);
    }

    @Test
    void remainderCarriesOverBetweenFrames() {
        assertEquals(0, stepper.advance(START + UNIT));
        assertEquals(1, stepper.advance(START + 3 * UNIT));

        assertEquals(0.5, stepper.alpha(), 1e-12);
    }

    @Test
    void remainderAndTimestampSurviveAGapLikeStopAndStart() {
        assertEquals(1, stepper.advance(START + 3 * UNIT));
        assertEquals(0.5, stepper.alpha(), 1e-12);

        int steps = stepper.advance(START + 3 * UNIT + 1_000_000 * UNIT);

        assertEquals(4, steps, "the clamped gap plus the carried remainder");
        assertEquals(0.5, stepper.alpha(), 1e-12);
    }

    @Test
    void nonPositiveStepIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FixedStepAccumulator(0, MAX_DELTA));
    }
}
