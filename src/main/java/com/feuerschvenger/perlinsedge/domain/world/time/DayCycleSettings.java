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
 * @param shadowSteps      how many distinct sun positions the shadows take across the daylight arc
 * @param minShadowLength  shadow length factor at noon, when the sun is highest
 * @param maxShadowLength  shadow length factor with the sun on the horizon
 */
public record DayCycleSettings(double dawnSeconds, double daySeconds, double duskSeconds, double nightSeconds,
                               double startSeconds, int shadowSteps, double minShadowLength,
                               double maxShadowLength) {

    /**
     * Validates the values.
     *
     * @throws IllegalArgumentException if a duration is not positive, the start is negative, there is no shadow step
     *                                  or the shadow length range is empty
     */
    public DayCycleSettings {
        if (dawnSeconds <= 0 || daySeconds <= 0 || duskSeconds <= 0 || nightSeconds <= 0) {
            throw new IllegalArgumentException("Every phase must last longer than zero seconds");
        }
        if (startSeconds < 0) {
            throw new IllegalArgumentException("The start cannot be negative");
        }
        if (shadowSteps < 1) {
            throw new IllegalArgumentException("There must be at least one shadow step");
        }
        if (minShadowLength <= 0 || maxShadowLength < minShadowLength) {
            throw new IllegalArgumentException("The shadow length range must be positive and ordered");
        }
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
