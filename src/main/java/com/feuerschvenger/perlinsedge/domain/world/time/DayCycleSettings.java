package com.feuerschvenger.perlinsedge.domain.world.time;

/**
 * The numbers that shape a {@link DayCycle}: how long each phase lasts, where a new world starts and how the sun
 * shadows are quantised. All durations are game seconds.
 *
 * @param dawnSeconds      length of the dawn
 * @param daySeconds       length of the full daylight
 * @param duskSeconds      length of the dusk
 * @param nightSeconds     length of the night
 * @param startSeconds     seconds into the cycle at which a new world starts, counted from the beginning of the dawn
 * @param shadowSteps      how many distinct sun positions the shadows take across the daylight arc, an odd number so the
 *                         middle step is centred on noon and its shadow points straight down
 * @param minShadowLength  shadow length factor at noon, when the sun is highest
 * @param maxShadowLength  shadow length factor with the sun on the horizon
 */
public record DayCycleSettings(double dawnSeconds, double daySeconds, double duskSeconds, double nightSeconds,
                               double startSeconds, int shadowSteps, double minShadowLength,
                               double maxShadowLength) {

    /**
     * Validates the values.
     *
     * @throws IllegalArgumentException if a value is not a finite number, a duration is not positive, the start is
     *                                  negative, the number of shadow steps is not odd or the shadow length range is
     *                                  empty
     */
    public DayCycleSettings {
        if (!isPositive(dawnSeconds) || !isPositive(daySeconds) || !isPositive(duskSeconds)
                || !isPositive(nightSeconds)) {
            throw new IllegalArgumentException("Every phase must last longer than zero seconds");
        }
        if (!Double.isFinite(startSeconds) || startSeconds < 0) {
            throw new IllegalArgumentException("The start must be a finite number that is not negative");
        }
        if (shadowSteps < 1 || shadowSteps % 2 == 0) {
            throw new IllegalArgumentException("The shadow steps must be an odd number so one is centred on noon");
        }
        if (!isPositive(minShadowLength) || !Double.isFinite(maxShadowLength) || maxShadowLength < minShadowLength) {
            throw new IllegalArgumentException("The shadow length range must be positive and ordered");
        }
    }

    /** Tells whether a value is a finite number above zero, which rules out NaN and the infinities. */
    private static boolean isPositive(double value) {
        return Double.isFinite(value) && value > 0;
    }

    /** @return the length of a whole cycle in seconds */
    public double cycleSeconds() {
        return dawnSeconds + daySeconds + duskSeconds + nightSeconds;
    }

    /** @return the seconds from the beginning of the dawn to the end of the dusk, which is when the sun is up */
    public double daylightSeconds() {
        return dawnSeconds + daySeconds + duskSeconds;
    }
}
