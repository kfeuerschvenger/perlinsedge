package com.feuerschvenger.perlinsedge.domain.world.time;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayCycleTest {

    private static final double DAWN = 150;
    private static final double DAY = 600;
    private static final double DUSK = 90;
    private static final double NIGHT = 360;
    private static final double CYCLE = DAWN + DAY + DUSK + NIGHT;
    private static final int STEPS = 17;

    /** The settings of a new world, which starts at 06:00. */
    private static DayCycleSettings settings() {
        return new DayCycleSettings(DAWN, DAY, DUSK, NIGHT, 0, STEPS, 0.6, 2.0).startingAtClockHour(6.0);
    }

    /** A cycle that starts at the first moment of the dawn, so tests can place the clock exactly. */
    private static DayCycle atCycleStart() {
        return new DayCycle(new DayCycleSettings(DAWN, DAY, DUSK, NIGHT, 0, STEPS, 0.6, 2.0));
    }

    private static DayCycle at(double secondsIntoCycle) {
        final DayCycle cycle = atCycleStart();
        cycle.advance(secondsIntoCycle);
        return cycle;
    }

    @Test
    void aCycleLastsTwentyMinutesWithMinecraftProportions() {
        assertEquals(1200, CYCLE);
        assertEquals(1200, settings().cycleSeconds());
        assertEquals(840, settings().daylightSeconds());
    }

    @Test
    void aNewWorldStartsAtSixInTheMorningWithNeutralLight() {
        final DayCycle cycle = new DayCycle(settings());

        assertEquals(DayPhase.DAY, cycle.phase());
        assertEquals(DAWN, cycle.secondsIntoCycle(), 1e-9);
        assertEquals(6.0, cycle.clockHours(), 1e-9);
        assertEquals(360, cycle.clockMinuteOfDay());
        assertEquals(0.0, cycle.skyTintStrength(), 0.0, "the light is exactly neutral at the start");
        assertEquals(1, cycle.dayNumber());
    }

    @Test
    void theClockIsAGameHourOfFiftySecondsAndTheEndOfTheDawnIsSixOClock() {
        assertEquals(6.0, at(DAWN).clockHours(), 1e-9);
        assertEquals(DayPhase.DAY, at(DAWN).phase());
        assertEquals(3.0, at(0).clockHours(), 1e-9, "the dawn begins at 03:00");
        assertEquals(DayPhase.DAWN, at(0).phase());
        assertEquals(18.0, at(DAWN + DAY).clockHours(), 1e-9, "the dusk begins at 18:00");
        assertEquals(DayPhase.DUSK, at(DAWN + DAY).phase());
        assertEquals(19.8, at(DAWN + DAY + DUSK).clockHours(), 1e-9, "the night begins at 19:48");
        assertEquals(19 * 60 + 48, at(DAWN + DAY + DUSK).clockMinuteOfDay());
        assertEquals(7.0, at(DAWN + 50).clockHours(), 1e-9, "one game hour is fifty seconds");
        assertEquals(4.5, at(75).clockHours(), 1e-9);
    }

    @Test
    void theClockWrapsAtMidnightAndNeverReachesTwentyFour() {
        // Midnight is 18 hours after the start of the day, that is 900 seconds into the day
        assertEquals(0.0, at(DAWN + 18 * 50).clockHours(), 1e-9);
        assertTrue(at(DAWN + 18 * 50 - 0.001).clockHours() > 23.99);
        assertEquals(1.0, at(DAWN + 19 * 50).clockHours(), 1e-9);
        for (double seconds : new double[]{0.0, CYCLE - 1e-9, CYCLE, CYCLE * 9 + 3.3, 1e12 + 0.7}) {
            final DayCycle cycle = at(seconds);
            assertTrue(cycle.clockHours() >= 0.0 && cycle.clockHours() < 24.0, "clock at " + seconds);
            assertTrue(cycle.clockMinuteOfDay() >= 0 && cycle.clockMinuteOfDay() < 1440);
        }
    }

    @Test
    void theStartHourIsDerivedFromTheSettingsAndWrapsIntoTheCycle() {
        final DayCycleSettings base = new DayCycleSettings(DAWN, DAY, DUSK, NIGHT, 0, STEPS, 0.6, 2.0);

        assertEquals(DAWN, base.startingAtClockHour(6.0).startSeconds(), 1e-9);
        assertEquals(0.0, base.startingAtClockHour(3.0).startSeconds(), 1e-9);
        assertEquals(DAWN + DAY, base.startingAtClockHour(18.0).startSeconds(), 1e-9);
        assertEquals(CYCLE - 150, base.startingAtClockHour(0.0).startSeconds(), 1e-9);
        assertEquals(base.startingAtClockHour(5.0).startSeconds(), base.startingAtClockHour(29.0).startSeconds(), 1e-9);
        assertEquals(0.0, new DayCycle(base.startingAtClockHour(0.0)).clockHours(), 1e-9);
    }

    @Test
    void phasesChangeExactlyOnTheirBoundaries() {
        assertEquals(DayPhase.DAWN, at(0).phase());
        assertEquals(DayPhase.DAWN, at(DAWN - 0.001).phase());
        assertEquals(DayPhase.DAY, at(DAWN).phase());
        assertEquals(DayPhase.DAY, at(DAWN + DAY - 0.001).phase());
        assertEquals(DayPhase.DUSK, at(DAWN + DAY).phase());
        assertEquals(DayPhase.DUSK, at(DAWN + DAY + DUSK - 0.001).phase());
        assertEquals(DayPhase.NIGHT, at(DAWN + DAY + DUSK).phase());
        assertEquals(DayPhase.NIGHT, at(CYCLE - 0.001).phase());
    }

    @Test
    void theClockWrapsAroundIntoTheNextDawnAndCountsDays() {
        final DayCycle cycle = at(CYCLE - 1);
        assertEquals(1, cycle.dayNumber());
        assertEquals(DayPhase.NIGHT, cycle.phase());

        cycle.advance(1);
        assertEquals(2, cycle.dayNumber());
        assertEquals(DayPhase.DAWN, cycle.phase());
        assertEquals(0.0, cycle.timeOfDay(), 1e-9);

        cycle.advance(CYCLE * 3);
        assertEquals(5, cycle.dayNumber());
    }

    @Test
    void timeOfDayStaysInsideZeroAndOne() {
        for (double s = 0; s < CYCLE * 2; s += 7.3) {
            final double t = at(s).timeOfDay();
            assertTrue(t >= 0.0 && t < 1.0, "time of day " + t + " at " + s);
        }
    }

    @Test
    void aNegativeAdvanceIsIgnored() {
        final DayCycle cycle = at(100);
        cycle.advance(-50);
        assertEquals(100, cycle.secondsIntoCycle(), 1e-9);
    }

    @Test
    void resetPutsTheClockBackToTheStartOfANewWorld() {
        final DayCycle cycle = new DayCycle(settings());
        cycle.advance(5000);

        cycle.reset();

        assertEquals(150, cycle.secondsIntoCycle(), 1e-9);
        assertEquals(1, cycle.dayNumber());
    }

    /** A cycle at an hour of the 24 hour clock, where the end of the dawn is 06:00. */
    private static DayCycle atHour(double hour) {
        return at((DAWN + (hour - 6.0) / 24.0 * CYCLE + CYCLE) % CYCLE);
    }

    @Test
    void theShadowsFadeInFromTheSunriseWithoutAJump() {
        final double sunrise = 24.0 - (6.0 + (DAY + DUSK) / CYCLE * 24.0);

        assertEquals(0.0, atHour(sunrise - 0.01).shadowStrength(), 1e-9, "no shadow just before the sunrise");
        assertTrue(atHour(sunrise + 0.01).shadowStrength() < 0.01, "barely any shadow just after the sunrise");
        assertTrue(atHour(5.0).shadowStrength() > 0.0 && atHour(5.0).shadowStrength() < 1.0, "fading in at 05:00");
        assertEquals(1.0, atHour(6.0).shadowStrength(), 1e-9, "full at the end of the dawn");
        double previous = 0.0;
        for (double hour = sunrise; hour <= 6.0; hour += 0.05) {
            final double strength = atHour(hour).shadowStrength();
            assertTrue(strength >= previous - 1e-9, "never darker then lighter on the way up");
            assertTrue(strength - previous < 0.1, "no jump between steps");
            previous = strength;
        }
    }

    @Test
    void aStartHourJustBeforeTheBeginningOfTheCycleWrapsToZeroAndNeverToTheWholeCycle() {
        final double[] hours = {Math.nextDown(3.0), 3.0, Math.nextUp(3.0), -21.0, Math.nextDown(-21.0), 27.0,
                Math.nextDown(27.0), 0.0, 24.0, Math.nextDown(24.0), 6.0, 123.456};
        for (double hour : hours) {
            final double start = settings().startingAtClockHour(hour).startSeconds();
            assertTrue(start >= 0.0 && start < CYCLE, "start " + start + " for hour " + hour);
        }
        assertEquals(0.0, settings().startingAtClockHour(Math.nextDown(3.0)).startSeconds(), 0.0);
    }

    @Test
    void theSettingsPlaceTheSunsetAtTheEndOfTheDuskAndTheSunriseMirroredAroundNoon() {
        assertEquals(19.8, settings().sunsetClockHours(), 1e-9);
        assertEquals(4.2, settings().sunriseClockHours(), 1e-9);
        assertEquals(12.0, (settings().sunriseClockHours() + settings().sunsetClockHours()) / 2, 1e-9);
    }

    @Test
    void theSunRisesAtFourTwelveIsHighestAtNoonAndSetsAtSevenFortyEight() {
        assertEquals(0.0, atHour(4.2).sunProgress(), 1e-9);
        assertEquals(0.5, atHour(12.0).sunProgress(), 1e-9);
        assertEquals(1.0, atHour(19.8).sunProgress(), 1e-9);
        assertEquals(0.0, atHour(3.5).sunProgress(), 1e-9, "before the sunrise the sun has not come up");
        assertEquals(1.0, atHour(23.0).sunProgress(), 1e-9, "after the sunset the sun is gone");
        assertEquals(1.0, at(CYCLE - 1).sunProgress(), 1e-9);

        assertEquals(0.0, atHour(4.2).sunElevation(), 1e-9);
        assertEquals(1.0, atHour(12.0).sunElevation(), 1e-9);
        assertEquals(0.0, atHour(19.8).sunElevation(), 1e-9);
        assertEquals(0.0, at(CYCLE - 1).sunElevation(), 1e-9);
        assertEquals(atHour(9.0).sunProgress() + atHour(15.0).sunProgress(), 1.0, 1e-9,
                "the arc is symmetric around noon");
    }

    @Test
    void atNoonTheShadowPointsStraightDownAtTheMinimumLength() {
        final DayCycle noon = atHour(12.0);

        assertEquals(0.5, noon.shadowSunProgress(), 1e-12);
        assertEquals(Math.PI / 2, SunScreenMapping.shadowAngle(noon.shadowSunProgress()), 1e-12);
        assertEquals(0.6, noon.shadowLengthFactor(), 1e-12);
        assertEquals(1.0, noon.shadowStrength(), 1e-12);
    }

    @Test
    void thereIsNoShadowWhileTheSunIsBelowTheHorizon() {
        assertEquals(0.0, atHour(3.5).shadowStrength(), 0.0, "03:30 is before the sunrise");
        assertEquals(0.0, atHour(4.1).shadowStrength(), 0.0);
        assertEquals(0.0, atHour(19.8).shadowStrength(), 1e-9);
        assertEquals(0.0, atHour(22.0).shadowStrength(), 0.0);
        assertEquals(0.0, atHour(1.0).shadowStrength(), 0.0);
        assertTrue(atHour(4.3).shadowStrength() > 0.0, "the shadows come back once the sun is up");
    }

    @Test
    void theShadowSunPositionJumpsInExactlyTheConfiguredNumberOfSteps() {
        final Set<Double> seen = new HashSet<>();
        double previous = -1;
        int jumps = 0;
        for (double s = 0; s <= settings().daylightSeconds(); s += 0.5) {
            final double position = at(s).shadowSunProgress();
            seen.add(position);
            assertTrue(position > 0.0 && position < 1.0);
            assertTrue(position >= previous, "the quantised sun never moves backwards inside the day");
            if (position != previous) {
                jumps++;
            }
            previous = position;
        }
        assertEquals(STEPS, seen.size());
        assertEquals(STEPS, jumps);
    }

    @Test
    void theShadowSunPositionHoldsStillInsideAStep() {
        final double sunrise = DAWN + (settings().sunriseClockHours() - 6.0) / 24.0 * CYCLE;
        final double stepSeconds = (settings().daylightSeconds() - sunrise) / STEPS;
        assertEquals(at(sunrise + stepSeconds * 3 + 0.1).shadowSunProgress(),
                at(sunrise + stepSeconds * 4 - 0.1).shadowSunProgress());
        assertTrue(at(sunrise + stepSeconds * 4 + 0.1).shadowSunProgress()
                > at(sunrise + stepSeconds * 4 - 0.1).shadowSunProgress());
    }

    @Test
    void shadowsAreShortestAtNoonAndLongerTowardBothHorizons() {
        final double noonSeconds = DAWN + (12.0 - 6.0) / 24.0 * CYCLE;
        double previous = Double.MAX_VALUE;
        for (double s = 0; s <= noonSeconds; s += 1) {
            final double length = at(s).shadowLengthFactor();
            assertTrue(length <= previous + 1e-12, "the shadow only shortens until noon, at " + s);
            previous = length;
        }
        for (double s = noonSeconds; s <= settings().daylightSeconds(); s += 1) {
            final double length = at(s).shadowLengthFactor();
            assertTrue(length >= previous - 1e-12, "the shadow only lengthens after noon, at " + s);
            previous = length;
        }
        final double noon = atHour(12.0).shadowLengthFactor();
        assertTrue(noon >= 0.6 && noon < 0.65, "noon is within a step of the minimum: " + noon);
        assertTrue(at(0).shadowLengthFactor() <= 2.0 && at(0).shadowLengthFactor() > 1.8);
    }

    @Test
    void shadowsAreFullByDayFadeOutThroughDuskAreAbsentAtNightAndFadeBackInThroughDawn() {
        assertEquals(0.0, at(0).shadowStrength(), 1e-9);
        assertEquals(1.0, at(DAWN).shadowStrength(), 1e-9);
        assertEquals(1.0, at(DAWN + DAY / 2).shadowStrength(), 1e-9);
        assertEquals(1.0, at(DAWN + DAY).shadowStrength(), 1e-9);
        assertEquals(0.0, at(DAWN + DAY + DUSK).shadowStrength(), 1e-9);
        assertEquals(0.0, at(DAWN + DAY + DUSK + NIGHT / 2).shadowStrength(), 1e-9);

        double previous = -1;
        for (double s = 0; s <= DAWN; s += 0.5) {
            final double strength = at(s).shadowStrength();
            assertTrue(strength >= previous, "the dawn only strengthens shadows, at " + s);
            previous = strength;
        }
        previous = 2;
        for (double s = DAWN + DAY; s <= DAWN + DAY + DUSK; s += 0.5) {
            final double strength = at(s).shadowStrength();
            assertTrue(strength <= previous, "the dusk only weakens shadows, at " + s);
            previous = strength;
        }
    }

    @Test
    void nothingJumpsAtAPhaseBoundary() {
        final double eps = 1e-6;
        for (double boundary : new double[]{DAWN, DAWN + DAY, DAWN + DAY + DUSK}) {
            final DayCycle before = at(boundary - eps);
            final DayCycle after = at(boundary + eps);
            assertEquals(before.shadowStrength(), after.shadowStrength(), 1e-4);
            assertEquals(before.skyTintStrength(), after.skyTintStrength(), 1e-4);
            assertEquals(before.skyTint().r(), after.skyTint().r(), 1e-4);
            assertEquals(before.skyTint().b(), after.skyTint().b(), 1e-4);
        }
        final DayCycle lastNight = at(CYCLE - eps);
        final DayCycle nextDawn = at(CYCLE + eps);
        assertEquals(lastNight.skyTintStrength(), nextDawn.skyTintStrength(), 1e-4);
    }

    @Test
    void theTintIsNeutralAtMiddayWarmAtDawnAndDuskAndBlueAtNight() {
        final DayCycle noon = at(settings().daylightSeconds() / 2);
        assertEquals(0.0, noon.skyTintStrength(), 1e-9);

        final Rgba dawnTint = at(DAWN / 2).skyTint();
        assertTrue(dawnTint.r() > dawnTint.g() && dawnTint.g() > dawnTint.b(), "warm: red over green over blue");
        assertTrue(at(DAWN / 2).skyTintStrength() > 0.0);

        final Rgba duskTint = at(DAWN + DAY + DUSK / 2).skyTint();
        assertTrue(duskTint.r() > duskTint.g() && duskTint.g() > duskTint.b());

        final DayCycle midnight = at(DAWN + DAY + DUSK + NIGHT / 2);
        final Rgba night = midnight.skyTint();
        assertTrue(night.b() > night.r() && night.b() > night.g(), "cool blue");
        assertTrue(midnight.skyTintStrength() > 0.55 && midnight.skyTintStrength() < 0.8,
                "dark, but short of black so the night stays barely playable");
    }

    @Test
    void theMorningLightIsWarmInsideTheDawnAndIsNeutralFromSixOClock() {
        final double dayEnd = DAWN + DAY;

        final DayCycle halfPastFour = at(75);
        assertEquals(4.5, halfPastFour.clockHours(), 1e-9);
        assertEquals(0.25, halfPastFour.skyTintStrength(), 1e-9, "fully warm at 04:30");
        assertTrue(halfPastFour.skyTint().r() > halfPastFour.skyTint().g()
                && halfPastFour.skyTint().g() > halfPastFour.skyTint().b(), "warm: red over green over blue");
        assertEquals(0.25, at(DAWN * 0.30).skyTintStrength(), 1e-9, "warm once the first 30% of the dawn is over");
        assertEquals(0.25, at(DAWN * 0.55 - 0.01).skyTintStrength(), 1e-3, "and held until 55% of the dawn");
        assertTrue(at(DAWN * 0.8).skyTintStrength() > 0.0 && at(DAWN * 0.8).skyTintStrength() < 0.25,
                "fading to neutral through the rest of the dawn");
        assertEquals(0.0, at(DAWN).skyTintStrength(), 0.0, "exactly neutral at 06:00");
        assertEquals(0.0, at(DAWN + 1).skyTintStrength(), 0.0, "and neutral after it");
        assertEquals(0.0, at(DAWN + 180).skyTintStrength(), 1e-9, "neutral through the morning");

        assertEquals(0.0, at(dayEnd - 160).skyTintStrength(), 1e-9, "neutral before the evening fade");
        assertTrue(at(dayEnd - 80).skyTintStrength() > 0.0, "turning warm more than a minute before the dusk");
        assertTrue(at(dayEnd - 20).skyTintStrength() > 0.2, "fully warm just before the dusk");
        final Rgba evening = at(dayEnd - 20).skyTint();
        assertTrue(evening.r() > evening.g() && evening.g() > evening.b(), "warm: red over green over blue");
    }

    @Test
    void theTintStrengthAlwaysStaysWithinTheValidRange() {
        for (double s = 0; s < CYCLE; s += 3.1) {
            final double strength = at(s).skyTintStrength();
            assertTrue(strength >= 0.0 && strength <= 1.0, "strength " + strength + " at " + s);
            assertEquals(1.0, at(s).skyTint().a());
        }
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(0, 1, 1, 1, 0, 4, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, -1, 4, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, 0, 0, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, 0, 4, 1.5, 1));
    }

    @Test
    void settingsRejectNotANumberAndInfiniteValues() {
        final double nan = Double.NaN;
        final double inf = Double.POSITIVE_INFINITY;
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(nan, 1, 1, 1, 0, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, nan, 1, 1, 0, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, nan, 1, 0, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, nan, 0, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, nan, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, 0, 3, nan, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, 0, 3, 0.5, nan));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(inf, 1, 1, 1, 0, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, inf, 0, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, inf, 3, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, 0, 3, 0.5, inf));
    }

    @Test
    void anEvenNumberOfShadowStepsIsRejectedBecauseNoStepWouldBeCentredOnNoon() {
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, 0, 16, 0.5, 1));
        assertThrows(IllegalArgumentException.class, () -> new DayCycleSettings(1, 1, 1, 1, 0, 2, 0.5, 1));
    }

    @Test
    void theClockAndTheSunStayInsideTheirRangeWhateverTheElapsedTime() {
        final double[] elapsed = {0.0, 1e-12, CYCLE - 1e-9, CYCLE, CYCLE * 3 + 0.5, 1e9, 1e12 + 0.3, 1e15, 123456789.987};
        for (double seconds : elapsed) {
            final DayCycle cycle = at(seconds);
            assertTrue(cycle.secondsIntoCycle() >= 0.0 && cycle.secondsIntoCycle() < CYCLE, "clock at " + seconds);
            assertTrue(cycle.sunProgress() >= 0.0 && cycle.sunProgress() <= 1.0, "sun at " + seconds);
            assertTrue(cycle.timeOfDay() >= 0.0 && cycle.timeOfDay() < 1.0, "time of day at " + seconds);
        }
    }

    @Test
    void oneLargeAdvanceAcrossSeveralDayBoundariesLandsOnTheRightDayAndPhase() {
        final DayCycle dusk = at(CYCLE * 7 + DAWN + DAY + 5);
        assertEquals(8, dusk.dayNumber());
        assertEquals(DayPhase.DUSK, dusk.phase());
        assertEquals(DAWN + DAY + 5, dusk.secondsIntoCycle(), 1e-6);

        final DayCycle night = at(CYCLE * 12 + DAWN + DAY + DUSK + 1);
        assertEquals(13, night.dayNumber());
        assertEquals(DayPhase.NIGHT, night.phase());

        final DayCycle dawn = at(CYCLE * 3);
        assertEquals(4, dawn.dayNumber());
        assertEquals(DayPhase.DAWN, dawn.phase());
        assertEquals(0.0, dawn.secondsIntoCycle(), 1e-6);
    }

    @Test
    void aVeryShortCycleStillWrapsAndKeepsItsPhases() {
        final double unit = 0.001;
        final DayCycle cycle = new DayCycle(new DayCycleSettings(unit, unit, unit, unit, 0, STEPS, 0.6, 2.0));

        assertEquals(DayPhase.DAWN, cycle.phase());
        cycle.advance(unit * 1.5);
        assertEquals(DayPhase.DAY, cycle.phase());
        cycle.advance(unit);
        assertEquals(DayPhase.DUSK, cycle.phase());
        cycle.advance(unit);
        assertEquals(DayPhase.NIGHT, cycle.phase());
        cycle.advance(unit * 1000);
        assertTrue(cycle.secondsIntoCycle() >= 0.0 && cycle.secondsIntoCycle() < unit * 4);
        assertTrue(cycle.dayNumber() > 200);
        assertTrue(cycle.shadowStrength() >= 0.0 && cycle.shadowStrength() <= 1.0);
        assertTrue(cycle.skyTintStrength() >= 0.0 && cycle.skyTintStrength() <= 1.0);
    }

    @Test
    void noonIsTheMiddleOfAStepSoShadowsPointStraightDownAtTheMinimumLength() {
        final DayCycle noon = atHour(12.0);

        assertEquals(0.5, noon.shadowSunProgress(), 1e-12);
        assertEquals(Math.PI / 2, SunScreenMapping.shadowAngle(noon.shadowSunProgress()), 1e-12);
        assertEquals(0.6, noon.shadowLengthFactor(), 1e-12);
        assertEquals(0.0, SunScreenMapping.shadowDirectionX(noon.shadowSunProgress()), 1e-12);
    }

    @Test
    void theFirstAndTheLastShadowStepAreMirrorImagesOfEachOther() {
        final DayCycle first = atHour(4.2);
        final DayCycle last = atHour(19.8);

        assertEquals(1.0, first.shadowSunProgress() + last.shadowSunProgress(), 1e-12);
        assertEquals(first.shadowLengthFactor(), last.shadowLengthFactor(), 1e-12);
        assertEquals(-SunScreenMapping.shadowDirectionX(first.shadowSunProgress()),
                SunScreenMapping.shadowDirectionX(last.shadowSunProgress()), 1e-12);
    }

    @Test
    void theTintOfAMomentIsOneObjectThatIsWorkedOutInPlace() {
        final DayCycle cycle = at(10);

        final SkyTint first = cycle.tint();
        assertSame(first, cycle.tint());
        final double strengthBefore = first.strength();
        cycle.advance(DAWN / 2.0);
        assertSame(first, cycle.tint(), "the same object holds the next moment");
        assertEquals(cycle.skyTintStrength(), cycle.tint().strength(), 0.0);
        assertEquals(cycle.skyTint().g(), cycle.tint().g(), 0.0);
        assertNotEquals(strengthBefore, cycle.tint().strength(), "and its values moved with the clock");
    }

    @Test
    void theColourAndTheStrengthOfOneMomentComeFromOneCalculationThatIsRepeatedForTheSameClock() {
        final DayCycle cycle = at(DAWN / 2.0);

        final double strength = cycle.tint().strength();
        final double red = cycle.tint().r();

        assertEquals(strength, cycle.skyTintStrength(), 0.0);
        assertEquals(red, cycle.skyTint().r(), 0.0);
    }

    @Test
    void aHoldPhaseHasTheSameColourEveryMoment() {
        final Rgba night = at(DAWN + DAY + DUSK + 100).skyTint();
        final Rgba laterNight = at(DAWN + DAY + DUSK + 200).skyTint();
        final Rgba noon = at(DAWN + 10).skyTint();
        final Rgba laterNoon = at(DAWN + 20).skyTint();

        assertEquals(night, laterNight);
        assertEquals(noon, laterNoon);
    }

    @Test
    void everyChannelOfTheTintStaysInRangeAcrossTheWholeCycle() {
        final DayCycle cycle = atCycleStart();
        for (int step = 0; step < 2000; step++) {
            final Rgba colour = cycle.skyTint();
            for (double channel : new double[]{colour.r(), colour.g(), colour.b()}) {
                assertTrue(channel >= 0.0 && channel <= 1.0);
            }
            assertTrue(cycle.skyTintStrength() >= 0.0 && cycle.skyTintStrength() <= 1.0);
            cycle.advance(CYCLE / 1000);
        }
    }

    @Test
    void theSkyHasItsOwnVisibilityThreshold() {
        assertTrue(DayCycle.MIN_VISIBLE_TINT_STRENGTH > 0.0 && DayCycle.MIN_VISIBLE_TINT_STRENGTH < 0.05);
        assertEquals(0.0, at(DAWN + 10).skyTintStrength(), 1e-12, "midday is below the threshold");
    }
}
