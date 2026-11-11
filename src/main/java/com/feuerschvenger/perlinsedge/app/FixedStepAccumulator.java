package com.feuerschvenger.perlinsedge.app;

/**
 * Pure fixed-timestep accumulator: turns frame timestamps into a number of logic steps and an interpolation factor.
 * It holds no JavaFX types, so the timing rules can be tested without a toolkit.
 * The remainder of the accumulator and the last timestamp are kept between frames until {@link #reset()} is called.
 * A timestamp that goes backwards counts as a zero-length frame.
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
     * @param maxFrameDelta largest frame time in seconds that is accumulated, which avoids a spiral of death; must be positive and finite
     */
    FixedStepAccumulator(double stepSeconds, double maxFrameDelta) {
        if (!(stepSeconds > 0) || Double.isInfinite(stepSeconds)) {
            throw new IllegalArgumentException("stepSeconds must be positive and finite");
        }
        if (!(maxFrameDelta > 0) || Double.isInfinite(maxFrameDelta)) {
            throw new IllegalArgumentException("maxFrameDelta must be positive and finite");
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

        double elapsed = (nowNanos - lastTimestamp) / NANOS_PER_SECOND;
        lastDelta = Math.max(0.0, Math.min(elapsed, maxFrameDelta));
        lastTimestamp = nowNanos;

        accumulator += lastDelta;
        int steps = 0;
        while (accumulator >= stepSeconds) {
            accumulator -= stepSeconds;
            steps++;
        }
        return steps;
    }

    /**
     * Forgets the last timestamp and the carried remainder, so the next frame is treated as the first one.
     * Called when the loop resumes after a pause, which would otherwise be replayed as one clamped burst of steps.
     */
    void reset() {
        lastTimestamp = 0;
        accumulator = 0;
        lastDelta = 0;
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
