package com.feuerschvenger.perlinsedge.app;

/**
 * Pure fixed-timestep accumulator: turns frame timestamps into a number of logic steps and an interpolation factor.
 * It holds no JavaFX types, so the timing rules can be tested without a toolkit.
 * The remainder of the accumulator and the last timestamp are kept between frames, including across a stop and start.
 */
final class FixedStepAccumulator {

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final double stepSeconds;
    private final double maxFrameDelta;

    private long lastTimestamp;
    private double accumulator;
    private double lastDelta;

    /**
     * Creates an accumulator.
     *
     * @param stepSeconds   duration of one logic step in seconds, must be positive
     * @param maxFrameDelta largest frame time in seconds that is accumulated, which avoids a spiral of death
     */
    FixedStepAccumulator(double stepSeconds, double maxFrameDelta) {
        if (stepSeconds <= 0) {
            throw new IllegalArgumentException("stepSeconds must be positive");
        }
        this.stepSeconds = stepSeconds;
        this.maxFrameDelta = maxFrameDelta;
    }

    /**
     * Registers a new frame.
     * The first frame only records the timestamp and yields no steps.
     *
     * @param nowNanos timestamp of the frame in nanoseconds
     * @return number of logic steps to run for this frame
     */
    int advance(long nowNanos) {
        if (lastTimestamp == 0) {
            lastTimestamp = nowNanos;
            lastDelta = 0;
            return 0;
        }

        lastDelta = Math.min((nowNanos - lastTimestamp) / NANOS_PER_SECOND, maxFrameDelta);
        lastTimestamp = nowNanos;

        accumulator += lastDelta;
        int steps = 0;
        while (accumulator >= stepSeconds) {
            accumulator -= stepSeconds;
            steps++;
        }
        return steps;
    }

    /** @return true once the first frame has been registered */
    boolean hasStarted() {
        return lastTimestamp != 0;
    }

    /** @return fraction of a logic step left in the accumulator, from 0.0 up to but excluding 1.0 */
    double alpha() {
        return accumulator / stepSeconds;
    }

    /** @return clamped duration in seconds of the frame registered by the last {@link #advance(long)} */
    double lastDelta() {
        return lastDelta;
    }
}
