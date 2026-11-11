package com.feuerschvenger.perlinsedge.infra.fx.input;

import java.util.function.LongSupplier;

/**
 * Turns the stream of wheel deltas into discrete steps.
 * A notched wheel reports about 40 units per notch, while a touchpad or an inertial wheel reports many small events
 * per gesture, so acting on every event would cycle through several pages at once. The accumulator adds the deltas
 * up and yields one step each time their sum reaches the threshold. A large delta yields several steps at once and
 * the part of it that does not make a whole step stays in the sum. The sum starts over when the direction changes
 * and when the wheel has been idle for {@link #IDLE_RESET_NANOS}, so a half notch turned a while ago never
 * completes with the next, unrelated, one.
 */
public class WheelAccumulator {
    /** Wheel units that make one step, the size of a standard wheel notch. */
    public static final double STEP_THRESHOLD = 40.0;
    /** Time without wheel input after which a partial sum is forgotten, in nanoseconds (300 ms). */
    public static final long IDLE_RESET_NANOS = 300_000_000L;

    private final LongSupplier nanoClock;
    private double accumulated;
    private long lastInputNanos;

    /** Creates an accumulator that reads the system clock. */
    public WheelAccumulator() {
        this(System::nanoTime);
    }

    /**
     * Creates an accumulator with a given clock.
     *
     * @param nanoClock source of monotonic time in nanoseconds
     */
    WheelAccumulator(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    /**
     * Adds a wheel delta.
     *
     * @param deltaY the vertical wheel delta, positive when the wheel turns up or away
     * @return the number of whole steps reached: positive upwards, negative downwards, 0 when the sum is still short
     */
    public int add(double deltaY) {
        if (deltaY == 0) {
            return 0;
        }
        long now = nanoClock.getAsLong();
        boolean idle = now - lastInputNanos > IDLE_RESET_NANOS;
        lastInputNanos = now;
        if (accumulated != 0 && (idle || Math.signum(accumulated) != Math.signum(deltaY))) {
            accumulated = 0;
        }
        accumulated += deltaY;
        int steps = (int) (accumulated / STEP_THRESHOLD);
        accumulated -= steps * STEP_THRESHOLD;
        return steps;
    }

    /** Drops whatever has been accumulated, for when the wheel stops being used for steps. */
    public void reset() {
        accumulated = 0;
    }
}
