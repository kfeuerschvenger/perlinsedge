package com.feuerschvenger.perlinsedge.domain.world.time;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * The clock of the sky: it moves the world through dawn, day, dusk and night and answers what the light looks like at the
 * current moment. Everything is a pure function of the seconds elapsed, so the cycle needs no framework and is advanced
 * by whoever owns it, which makes a paused game stand still.
 * <p>
 * A cycle starts at the beginning of the dawn. The sun is up from then until the end of the dusk and crosses the sky
 * from east to west across that stretch, highest in the middle. Shadows are strongest in the day, fade out through the
 * dusk, are absent at night and come back through the dawn. The sky tint is neutral at midday, warm at dawn and dusk and
 * a deep moonlit blue at night. Every change between phases is eased, so nothing jumps.
 */
public final class DayCycle {

    /** The tint of the night: a deep blue, dark enough that the world is only just playable without a light. */
    private static final Rgba NIGHT_COLOUR = new Rgba(0.03, 0.05, 0.16, 1.0);
    private static final double NIGHT_STRENGTH = 0.66;

    /** The tint of the horizon at dawn and dusk: a strong warm orange. */
    private static final Rgba WARM_COLOUR = new Rgba(1.0, 0.48, 0.14, 1.0);
    private static final double WARM_STRENGTH = 0.30;

    /** The tint of midday: no tint at all. */
    private static final Rgba NEUTRAL_COLOUR = new Rgba(1.0, 1.0, 1.0, 1.0);
    private static final double NEUTRAL_STRENGTH = 0.0;

    private final DayCycleSettings settings;
    /** Seconds since the world started, including the offset a new world begins at. */
    private double elapsed;

    /**
     * Creates a cycle at the start of a new world.
     *
     * @param settings the durations and the shadow rules
     */
    public DayCycle(DayCycleSettings settings) {
        this.settings = settings;
        reset();
    }

    /** Puts the clock back to the start of a new world, which is shortly after the dawn has ended. */
    public void reset() {
        elapsed = settings.startSeconds();
    }

    /**
     * Moves the clock forward.
     *
     * @param seconds game seconds to add, a negative value is ignored
     */
    public void advance(double seconds) {
        if (seconds > 0) {
            elapsed += seconds;
        }
    }

    /** @return the settings the cycle runs on */
    public DayCycleSettings settings() {
        return settings;
    }

    /** @return seconds into the current cycle, from zero at the beginning of the dawn */
    public double secondsIntoCycle() {
        final double cycle = settings.cycleSeconds();
        return elapsed - Math.floor(elapsed / cycle) * cycle;
    }

    /** @return the time of day from 0, the beginning of the dawn, up to but excluding 1, which is the next dawn */
    public double timeOfDay() {
        return secondsIntoCycle() / settings.cycleSeconds();
    }

    /** @return the phase the clock is in */
    public DayPhase phase() {
        final double s = secondsIntoCycle();
        if (s < settings.dawnSeconds()) {
            return DayPhase.DAWN;
        }
        if (s < settings.dawnSeconds() + settings.daySeconds()) {
            return DayPhase.DAY;
        }
        if (s < settings.daylightSeconds()) {
            return DayPhase.DUSK;
        }
        return DayPhase.NIGHT;
    }

    /** @return the number of the current day, one for the first, which goes up at every dawn */
    public int dayNumber() {
        return 1 + (int) Math.floor(elapsed / settings.cycleSeconds());
    }

    /**
     * Returns where the sun is on its arc.
     *
     * @return 0 at sunrise, 0.5 at noon and 1 at sunset, and 1 for the whole night, when the sun cannot be seen
     */
    public double sunProgress() {
        return Math.min(1.0, secondsIntoCycle() / settings.daylightSeconds());
    }

    /** @return how high the sun stands, 0 on the horizon and 1 at noon, and 0 at night */
    public double sunElevation() {
        return Math.sin(Math.PI * sunProgress());
    }

    /**
     * Returns the sun position the shadows are drawn from. The arc is cut into
     * {@link DayCycleSettings#shadowSteps()} equal steps and every position inside a step maps to the middle of it, so
     * the shadows move in crisp jumps instead of shimmering with every frame.
     *
     * @return the quantised position on the arc, strictly between 0 and 1
     */
    public double shadowSunProgress() {
        final int steps = settings.shadowSteps();
        final int step = Math.min(steps - 1, (int) Math.floor(sunProgress() * steps));
        return (step + 0.5) / steps;
    }

    /**
     * Returns how long shadows are, as a multiple of the height of what casts them.
     * It is shortest at noon and longest with the sun on the horizon, and follows the quantised sun position.
     *
     * @return the factor, between the minimum and the maximum of the settings
     */
    public double shadowLengthFactor() {
        final double low = Math.sin(Math.PI * shadowSunProgress());
        return settings.maxShadowLength() - (settings.maxShadowLength() - settings.minShadowLength()) * low;
    }

    /**
     * Returns how dark the shadows are as a share of their full strength.
     *
     * @return 1 through the day, easing to 0 through the dusk, 0 at night and easing back to 1 through the dawn
     */
    public double shadowStrength() {
        final double s = secondsIntoCycle();
        final double dawn = settings.dawnSeconds();
        final double dayEnd = dawn + settings.daySeconds();
        return switch (phase()) {
            case DAWN -> ease(s / dawn);
            case DAY -> 1.0;
            case DUSK -> 1.0 - ease((s - dayEnd) / settings.duskSeconds());
            case NIGHT -> 0.0;
        };
    }

    /**
     * Returns the colour of the ambient sky tint. Its opacity is always 1, how much of it shows is
     * {@link #skyTintStrength()}.
     *
     * @return neutral at midday, warm orange at dawn and dusk and a cool moonlit blue at night
     */
    public Rgba skyTint() {
        return sampleTint().colour();
    }

    /** @return how strongly the sky tint is laid over the world, 0 at midday and the most at night */
    public double skyTintStrength() {
        return sampleTint().strength();
    }

    /** Works out the tint of the moment from the keyframes: night, warm, neutral, neutral, warm and night again. */
    private Tint sampleTint() {
        final double s = secondsIntoCycle();
        final double dawn = settings.dawnSeconds();
        final double dayEnd = dawn + settings.daySeconds();
        final double duskMid = dayEnd + settings.duskSeconds() / 2;
        if (s < dawn / 2) {
            return blend(NIGHT_COLOUR, NIGHT_STRENGTH, WARM_COLOUR, WARM_STRENGTH, s / (dawn / 2));
        }
        if (s < dawn) {
            return blend(WARM_COLOUR, WARM_STRENGTH, NEUTRAL_COLOUR, NEUTRAL_STRENGTH, (s - dawn / 2) / (dawn / 2));
        }
        if (s < dayEnd) {
            return new Tint(NEUTRAL_COLOUR, NEUTRAL_STRENGTH);
        }
        if (s < duskMid) {
            return blend(NEUTRAL_COLOUR, NEUTRAL_STRENGTH, WARM_COLOUR, WARM_STRENGTH,
                    (s - dayEnd) / (duskMid - dayEnd));
        }
        if (s < settings.daylightSeconds()) {
            return blend(WARM_COLOUR, WARM_STRENGTH, NIGHT_COLOUR, NIGHT_STRENGTH,
                    (s - duskMid) / (settings.daylightSeconds() - duskMid));
        }
        return new Tint(NIGHT_COLOUR, NIGHT_STRENGTH);
    }

    /** Mixes two tints by an eased share. */
    private static Tint blend(Rgba from, double fromStrength, Rgba to, double toStrength, double share) {
        final double t = ease(share);
        return new Tint(new Rgba(
                mix(from.r(), to.r(), t), mix(from.g(), to.g(), t), mix(from.b(), to.b(), t), 1.0),
                mix(fromStrength, toStrength, t));
    }

    private static double mix(double from, double to, double t) {
        return from + (to - from) * t;
    }

    /** A smooth step: zero at zero, one at one, with no jump in the slope at either end. */
    private static double ease(double t) {
        final double x = Math.max(0.0, Math.min(1.0, t));
        return x * x * (3.0 - 2.0 * x);
    }

    /** A sky tint: the colour and how strongly it is laid over the world. */
    private record Tint(Rgba colour, double strength) {}
}
