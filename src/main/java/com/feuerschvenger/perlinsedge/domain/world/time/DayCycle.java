package com.feuerschvenger.perlinsedge.domain.world.time;

import com.feuerschvenger.perlinsedge.domain.utils.Doubles;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * The clock of the sky: it moves the world through dawn, day, dusk and night and answers what the light looks like at
 * the current moment. Everything is a pure function of the seconds elapsed, so the cycle needs no framework and is
 * advanced by whoever owns it, which makes a paused game stand still.
 * <p>
 * A cycle starts at the beginning of the dawn. The sun rises part way through the dawn and sets at the end of the dusk,
 * and it crosses the sky from east to west between the two, highest at 12:00 on the clock of the cycle, so its arc is
 * symmetric around noon. Shadows are strongest in the day, fade out through the dusk, are absent at night and while the
 * sun is below the horizon, and come back through the dawn once it is up. The sky tint is neutral at midday, warm at
 * dawn and dusk and a deep moonlit blue at night. Every change between phases is eased, so nothing jumps.
 */
public final class DayCycle {

    /** The tint of the night: a deep blue, dark enough that the world is only just playable without a light. */
    private static final Rgba NIGHT_COLOUR = new Rgba(0.03, 0.05, 0.16, 1.0);

    /** How strongly the night tint is laid over the world, the most of any moment of the day. */
    private static final double NIGHT_STRENGTH = 0.72;

    /** The tint of the horizon at dawn and dusk: a warm orange. */
    private static final Rgba WARM_COLOUR = new Rgba(1.0, 0.48, 0.14, 1.0);

    /** How strongly the warm tint of the dawn and the dusk is laid over the world. */
    private static final double WARM_STRENGTH = 0.25;

    /** Share of the dawn the night takes to turn fully warm. */
    private static final double DAWN_WARM_SHARE = 0.30;
    /** Share of the dawn at which the warm light starts to fade to neutral, which it reaches as the dawn ends. */
    private static final double DAWN_HOLD_SHARE = 0.55;
    /** Share of the day the evening light takes to turn warm, 120 s of a 10 minute day. */
    private static final double EVENING_FADE_SHARE = 0.2;
    /** Share of the day before the dusk the evening light is already fully warm, 30 s of a 10 minute day. */
    private static final double EVENING_WARM_SHARE = 0.05;

    /** The tint of midday: no tint at all. */
    private static final Rgba NEUTRAL_COLOUR = new Rgba(1.0, 1.0, 1.0, 1.0);

    /** How strongly the neutral tint is laid over the world: not at all. */
    private static final double NEUTRAL_STRENGTH = 0.0;

    /**
     * The tint strength below which the sky cannot be told from no tint at all, so nothing is laid over the world for
     * it. It is the sky's own threshold, independent of the one the shadows use.
     */
    public static final double MIN_VISIBLE_TINT_STRENGTH = 0.004;
    /** Minutes in an hour of the 24 hour clock. */
    private static final int MINUTES_PER_HOUR = 60;
    /** Where in a step of the shadow sun the shadow sits: the middle of it. */
    private static final double STEP_MIDDLE = 0.5;
    /** The coefficients of the smooth step curve, 3x^2 - 2x^3. */
    private static final double EASE_QUADRATIC_WEIGHT = 3.0;
    private static final double EASE_CUBIC_WEIGHT = 2.0;

    /** The durations and the shadow rules the cycle runs on. */
    private final DayCycleSettings settings;
    /** Seconds since the world started, including the offset a new world begins at. */
    private double elapsed;
    /** The value of {@link #elapsed} the cached tint was worked out for, NaN while there is none. */
    private double tintAt = Double.NaN;
    /** The tint worked out for {@link #tintAt}, so the colour and the strength of a frame share one calculation. */
    private final SkyTint tint = new SkyTint();

    /**
     * Creates a cycle at the start of a new world.
     *
     * @param settings the durations and the shadow rules
     */
    public DayCycle(DayCycleSettings settings) {
        this.settings = settings;
        reset();
    }

    /** Puts the clock back to the start of a new world, at the clock hour the settings start it. */
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

    /**
     * Returns where the clock is inside the current cycle. Floating point rounding can leave the remainder a hair
     * outside the cycle, so it is clamped to keep every phase lookup in range.
     *
     * @return seconds into the current cycle, from zero at the beginning of the dawn up to but excluding the cycle
     *         length
     */
    public double secondsIntoCycle() {
        final double cycle = settings.cycleSeconds();
        final double seconds = elapsed - Math.floor(elapsed / cycle) * cycle;
        return Math.clamp(seconds, 0.0, Math.nextDown(cycle));
    }

    /**
     * Returns the time as a 24 hour clock. The cycle is one day of 24 hours, and the end of the dawn, when the full
     * daylight begins, is {@link DayCycleSettings#DAY_START_CLOCK_HOURS}, so the dawn begins before it and the clock
     * wraps at midnight.
     *
     * @return the hour of the day from 0 up to but excluding 24, with the minutes as the fraction
     */
    public double clockHours() {
        final double hours = unwrappedClockHours();
        final double wrapped = hours - Math.floor(hours / DayCycleSettings.HOURS_PER_DAY)
                * DayCycleSettings.HOURS_PER_DAY;
        return Math.clamp(wrapped, 0.0, Math.nextDown(DayCycleSettings.HOURS_PER_DAY));
    }

    /**
     * Returns the clock hour of the cycle without wrapping it at midnight, so the sunrise, the noon and the sunset
     * keep their order whatever the proportions of the cycle.
     */
    private double unwrappedClockHours() {
        return DayCycleSettings.DAY_START_CLOCK_HOURS
                + (secondsIntoCycle() - settings.dawnSeconds()) / settings.cycleSeconds()
                * DayCycleSettings.HOURS_PER_DAY;
    }

    /** @return the minutes since midnight on the 24 hour clock, 0 up to 1439, so 06:00 is 360 */
    public int clockMinuteOfDay() {
        // A hair above the exact minute keeps a time that lies on a minute from rounding down to the one before
        final int minutes = (int) Math.floor(clockHours() * MINUTES_PER_HOUR + 1e-6);
        return Math.min(minutes, (int) (DayCycleSettings.HOURS_PER_DAY * MINUTES_PER_HOUR) - 1);
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

    /**
     * Returns the number of the current day, one for the first. It goes up when a cycle begins, which is the beginning
     * of the dawn and not the sunrise or midnight: with the usual proportions that is 03:00 on the clock, three hours
     * before the daylight is full.
     *
     * @return the day number
     */
    public int dayNumber() {
        return 1 + (int) Math.floor(elapsed / settings.cycleSeconds());
    }

    /**
     * Returns where the sun is on its arc. The arc runs from the sunrise to the sunset of the settings, which are
     * mirror images around 12:00, so the sun is at its highest exactly at noon on the clock.
     *
     * @return 0 at sunrise, 0.5 at 12:00 and 1 at sunset, 0 before the sunrise and 1 after the sunset, when the sun
     *         cannot be seen
     */
    public double sunProgress() {
        final double sunrise = settings.sunriseClockHours();
        final double arc = settings.sunsetClockHours() - sunrise;
        if (arc <= 0.0) {
            return 1.0;
        }
        return Math.clamp((unwrappedClockHours() - sunrise) / arc, 0.0, 1.0);
    }

    /** @return true while the sun is above the horizon, from the sunrise up to but excluding the sunset */
    private boolean sunIsUp() {
        final double hours = unwrappedClockHours();
        return hours >= settings.sunriseClockHours() && hours < settings.sunsetClockHours();
    }

    /** @return how high the sun stands, 0 on the horizon and 1 at noon, and 0 at night */
    public double sunElevation() {
        return Math.sin(Math.PI * sunProgress());
    }

    /**
     * Returns the sun position the shadows are drawn from. The arc is cut into
     * {@link DayCycleSettings#shadowSteps()} equal steps and every position inside a step maps to the middle of it, so
     * the shadows move in crisp jumps instead of shimmering with every frame. The number of steps is odd, so the middle
     * step is centred on 12:00: its shadow points straight down and has the minimum length.
     *
     * @return the quantised position on the arc, strictly between 0 and 1
     */
    public double shadowSunProgress() {
        final int steps = settings.shadowSteps();
        final int step = Math.min(steps - 1, (int) Math.floor(sunProgress() * steps));
        return (step + STEP_MIDDLE) / steps;
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
     * @return 1 through the day, easing to 0 through the dusk, 0 at night, easing from 0 at the sunrise back to 1 at
     *         the end of the dawn, and 0 for as long as the sun is below the horizon, so the shadows never jump
     */
    public double shadowStrength() {
        if (!sunIsUp()) {
            return 0.0;
        }
        final double s = secondsIntoCycle();
        final double dawn = settings.dawnSeconds();
        final double dayEnd = dawn + settings.daySeconds();
        return switch (phase()) {
            case DAWN -> {
                final double sunrise = settings.sunriseClockHours();
                final double rise = DayCycleSettings.DAY_START_CLOCK_HOURS - sunrise;
                yield rise <= 0 ? 1.0 : ease((unwrappedClockHours() - sunrise) / rise);
            }
            case DAY -> 1.0;
            case DUSK -> 1.0 - ease((s - dayEnd) / settings.duskSeconds());
            case NIGHT -> 0.0;
        };
    }

    /**
     * Returns the tint of the moment: the colour of the ambient sky tint and how strongly it is laid over the world. It
     * is worked out once per position of the clock and in place, so reading it any number of times in a frame costs one
     * calculation and allocates nothing, including in the middle of a transition. The object stays the same for the
     * life of the cycle and holds the tint of the clock as it was last read, so it stays inside the cycle: callers
     * outside the package read the tint through {@link #skyTintRed()}, {@link #skyTintGreen()},
     * {@link #skyTintBlue()} and {@link #skyTintStrength()}.
     *
     * @return neutral with no strength at midday, warm orange at dawn and dusk and a cool moonlit blue at night
     */
    SkyTint tint() {
        if (!Doubles.areEqual(tintAt, elapsed)) {
            computeTint();
            tintAt = elapsed;
        }
        return tint;
    }

    /**
     * Returns the colour of the ambient sky tint. Its opacity is always 1, how much of it shows is
     * {@link #skyTintStrength()}. It makes a new object on every call, so the frame loop reads {@link #tint()} instead.
     *
     * @return neutral at midday, warm orange at dawn and dusk and a cool moonlit blue at night
     */
    public Rgba skyTint() {
        final SkyTint current = tint();
        return new Rgba(current.r(), current.g(), current.b(), 1.0);
    }

    /** @return the red channel of the sky tint, 0.0 to 1.0, which allocates nothing */
    public double skyTintRed() {
        return tint().r();
    }

    /** @return the green channel of the sky tint, 0.0 to 1.0, which allocates nothing */
    public double skyTintGreen() {
        return tint().g();
    }

    /** @return the blue channel of the sky tint, 0.0 to 1.0, which allocates nothing */
    public double skyTintBlue() {
        return tint().b();
    }

    /** @return how strongly the sky tint is laid over the world, 0 at midday and the most at night */
    public double skyTintStrength() {
        return tint().strength();
    }

    /**
     * Works out the tint of the moment from the keyframes into {@link #tint}. The night turns warm through the first part
     * of the dawn, the warm light holds and then fades to neutral, reaching it exactly as the dawn ends. Well before the
     * dusk the light turns warm again, holds through the first half of the dusk and then sinks into the night.
     */
    private void computeTint() {
        final double s = secondsIntoCycle();
        final double dawn = settings.dawnSeconds();
        final double day = settings.daySeconds();
        final double dayEnd = dawn + day;
        final double duskMid = dayEnd + settings.duskSeconds() / 2;
        final double warmUp = dawn * DAWN_WARM_SHARE;
        final double morningHoldEnd = dawn * DAWN_HOLD_SHARE;
        final double eveningFadeStart = dayEnd - day * (EVENING_FADE_SHARE + EVENING_WARM_SHARE);
        final double eveningWarmStart = dayEnd - day * EVENING_WARM_SHARE;
        if (s < warmUp) {
            blend(NIGHT_COLOUR, NIGHT_STRENGTH, WARM_COLOUR, WARM_STRENGTH, s / warmUp);
        } else if (s < morningHoldEnd) {
            hold(WARM_COLOUR, WARM_STRENGTH);
        } else if (s < dawn) {
            blend(WARM_COLOUR, WARM_STRENGTH, NEUTRAL_COLOUR, NEUTRAL_STRENGTH,
                    (s - morningHoldEnd) / (dawn - morningHoldEnd));
        } else if (s < eveningFadeStart) {
            hold(NEUTRAL_COLOUR, NEUTRAL_STRENGTH);
        } else if (s < eveningWarmStart) {
            blend(NEUTRAL_COLOUR, NEUTRAL_STRENGTH, WARM_COLOUR, WARM_STRENGTH,
                    (s - eveningFadeStart) / (eveningWarmStart - eveningFadeStart));
        } else if (s < duskMid) {
            hold(WARM_COLOUR, WARM_STRENGTH);
        } else if (s < settings.daylightSeconds()) {
            blend(WARM_COLOUR, WARM_STRENGTH, NIGHT_COLOUR, NIGHT_STRENGTH,
                    (s - duskMid) / (settings.daylightSeconds() - duskMid));
        } else {
            hold(NIGHT_COLOUR, NIGHT_STRENGTH);
        }
    }

    /** Sets the tint to a colour that a phase holds. */
    private void hold(Rgba colour, double strength) {
        tint.set(colour.r(), colour.g(), colour.b(), strength);
    }

    /** Sets the tint to a mix of two tints by an eased share. */
    private void blend(Rgba from, double fromStrength, Rgba to, double toStrength, double share) {
        final double t = ease(share);
        tint.set(channel(mix(from.r(), to.r(), t)), channel(mix(from.g(), to.g(), t)),
                channel(mix(from.b(), to.b(), t)), mix(fromStrength, toStrength, t));
    }

    /** Keeps a mixed colour channel inside 0 to 1, so rounding at the ends of a mix can never leave the range. */
    private static double channel(double value) {
        return Math.clamp(value, 0.0, 1.0);
    }

    /** Returns the point a share of the way from one value to another, {@code from} at zero and {@code to} at one. */
    private static double mix(double from, double to, double t) {
        return from + (to - from) * t;
    }

    /** A smooth step: zero at zero, one at one, with no jump in the slope at either end. */
    private static double ease(double t) {
        final double x = Math.clamp(t, 0.0, 1.0);
        return x * x * (EASE_QUADRATIC_WEIGHT - EASE_CUBIC_WEIGHT * x);
    }
}
