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

    /** Hours on the clock of a cycle, which is one day. */
    public static final double HOURS_PER_DAY = 24.0;

    /** The clock hour at which the dawn ends and the full daylight begins. */
    public static final double DAY_START_CLOCK_HOURS = 6.0;

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
        if (shadowSteps < 1 || (shadowSteps & 1) == 0) {
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

    /**
     * Returns the same settings with a new world starting at a clock hour.
     *
     * @param clockHour the hour of the 24 hour clock a new world starts at, where the end of the dawn is
     *                  {@link #DAY_START_CLOCK_HOURS}
     * @return settings whose start is the seconds into the cycle that hour falls at, from zero up to but excluding the
     *         length of the cycle
     */
    public DayCycleSettings startingAtClockHour(double clockHour) {
        final double cycle = cycleSeconds();
        final double seconds = dawnSeconds + (clockHour - DAY_START_CLOCK_HOURS) / HOURS_PER_DAY * cycle;
        final double remainder = Math.max(0.0, seconds - Math.floor(seconds / cycle) * cycle);
        // A moment a hair before the start of the cycle rounds to the whole cycle, which is the same moment as zero
        final double wrapped = remainder >= cycle ? 0.0 : remainder;
        return new DayCycleSettings(dawnSeconds, daySeconds, duskSeconds, nightSeconds, wrapped, shadowSteps,
                minShadowLength, maxShadowLength);
    }

    /**
     * Returns the clock hour of the sunset, which is the end of the dusk. It is counted on the clock of a cycle, where
     * the end of the dawn is {@link #DAY_START_CLOCK_HOURS}, and it can pass {@link #HOURS_PER_DAY} for a cycle whose
     * daylight is longer than the clock allows after the end of the dawn.
     *
     * @return the hour of the sunset
     */
    public double sunsetClockHours() {
        return DAY_START_CLOCK_HOURS + (daySeconds + duskSeconds) / cycleSeconds() * HOURS_PER_DAY;
    }

    /**
     * Returns the clock hour of the sunrise. The sun's arc is symmetric around 12:00, so the sunrise is as many hours
     * before noon as the sunset is after it.
     *
     * @return the hour of the sunrise, a mirror image of {@link #sunsetClockHours()} around 12:00
     */
    public double sunriseClockHours() {
        return HOURS_PER_DAY - sunsetClockHours();
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
