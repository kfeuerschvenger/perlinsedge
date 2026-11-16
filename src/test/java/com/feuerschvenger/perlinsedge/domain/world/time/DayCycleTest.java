package com.feuerschvenger.perlinsedge.domain.world.time;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayCycleTest {

    private static final double DAWN = 90;
    private static final double DAY = 600;
    private static final double DUSK = 90;
    private static final double NIGHT = 420;
    private static final double CYCLE = DAWN + DAY + DUSK + NIGHT;
    private static final int STEPS = 16;

    private static DayCycleSettings settings() {
        return new DayCycleSettings(DAWN, DAY, DUSK, NIGHT, 120, STEPS, 0.6, 2.0);
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
        assertEquals(780, settings().daylightSeconds());
    }

    @Test
    void aNewWorldStartsShortlyAfterTheDawnHasEnded() {
        final DayCycle cycle = new DayCycle(settings());

        assertEquals(DayPhase.DAY, cycle.phase());
        assertTrue(cycle.secondsIntoCycle() > DAWN && cycle.secondsIntoCycle() < DAWN + 60);
        assertEquals(1, cycle.dayNumber());
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

        assertEquals(120, cycle.secondsIntoCycle(), 1e-9);
        assertEquals(1, cycle.dayNumber());
    }

    @Test
    void theSunClimbsToNoonInTheMiddleOfItsArcAndSetsAtTheEndOfTheDusk() {
        assertEquals(0.0, at(0).sunProgress(), 1e-9);
        assertEquals(0.5, at(settings().daylightSeconds() / 2).sunProgress(), 1e-9);
        assertEquals(1.0, at(DAWN + DAY + DUSK).sunProgress(), 1e-9);
        assertEquals(1.0, at(CYCLE - 1).sunProgress(), 1e-9);

        assertEquals(0.0, at(0).sunElevation(), 1e-9);
        assertEquals(1.0, at(settings().daylightSeconds() / 2).sunElevation(), 1e-9);
        assertEquals(0.0, at(CYCLE - 1).sunElevation(), 1e-9);
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
        final double stepSeconds = settings().daylightSeconds() / STEPS;
        assertEquals(at(stepSeconds * 3 + 0.1).shadowSunProgress(), at(stepSeconds * 4 - 0.1).shadowSunProgress());
        assertTrue(at(stepSeconds * 4 + 0.1).shadowSunProgress() > at(stepSeconds * 4 - 0.1).shadowSunProgress());
    }

    @Test
    void shadowsAreShortestAtNoonAndLongerTowardBothHorizons() {
        double previous = Double.MAX_VALUE;
        for (double s = 0; s <= settings().daylightSeconds() / 2; s += 1) {
            final double length = at(s).shadowLengthFactor();
            assertTrue(length <= previous + 1e-12, "the shadow only shortens until noon, at " + s);
            previous = length;
        }
        for (double s = settings().daylightSeconds() / 2; s <= settings().daylightSeconds(); s += 1) {
            final double length = at(s).shadowLengthFactor();
            assertTrue(length >= previous - 1e-12, "the shadow only lengthens after noon, at " + s);
            previous = length;
        }
        final double noon = at(settings().daylightSeconds() / 2).shadowLengthFactor();
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
    void theWarmLightReachesIntoTheMorningAndStartsWellBeforeTheDusk() {
        final double dayEnd = DAWN + DAY;

        assertTrue(at(DAWN + 30).skyTintStrength() > 0.2, "still fully warm half a minute after the dawn");
        assertTrue(at(DAWN + 90).skyTintStrength() > 0.0, "still fading a minute and a half after the dawn");
        assertEquals(0.0, at(DAWN + 180).skyTintStrength(), 1e-9, "neutral three minutes into the day");

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
}
