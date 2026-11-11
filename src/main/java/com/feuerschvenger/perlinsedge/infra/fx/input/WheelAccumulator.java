package com.feuerschvenger.perlinsedge.infra.fx.input;

/**
 * Turns the stream of wheel deltas into discrete steps.
 * A notched wheel reports about 40 units per notch, while a touchpad or an inertial wheel reports many small events
 * per gesture, so acting on every event would cycle through several pages at once. The accumulator adds the deltas
 * up and yields one step each time their sum reaches the threshold, and it starts over when the direction changes.
 */
public class WheelAccumulator {
    /** Wheel units that make one step, the size of a standard wheel notch. */
    public static final double STEP_THRESHOLD = 40.0;

    private double accumulated;

    /**
     * Adds a wheel delta.
     *
     * @param deltaY the vertical wheel delta, positive when the wheel turns up or away
     * @return 1 when the sum reached the threshold upwards, -1 when it reached it downwards, 0 otherwise
     */
    public int add(double deltaY) {
        if (deltaY == 0) {
            return 0;
        }
        if (accumulated != 0 && Math.signum(accumulated) != Math.signum(deltaY)) {
            accumulated = 0;
        }
        accumulated += deltaY;
        if (Math.abs(accumulated) >= STEP_THRESHOLD) {
            int step = accumulated > 0 ? 1 : -1;
            accumulated = 0;
            return step;
        }
        return 0;
    }

    /** Drops whatever has been accumulated, for when the wheel stops being used for steps. */
    public void reset() {
        accumulated = 0;
    }
}
