package com.feuerschvenger.perlinsedge.domain.world.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlickerTest {

    private static final double INTENSITY = 0.10;
    private static final double RADIUS = 0.05;
    private static final double HZ = 3.2;
    /** The steepest a mix of the three waves can climb, as a multiple of two pi times the frequency. */
    private static final double MAX_SLOPE_FACTOR = 0.5 * 1.0 + 0.3 * 1.73 + 0.2 * 2.91;

    private static Flicker crackling(long seed) {
        return Flicker.crackling(INTENSITY, RADIUS, HZ, seed);
    }

    @Test
    void aSteadyLightNeverChanges() {
        for (double time = 0.0; time < 100.0; time += 0.37) {
            assertEquals(1.0, Flicker.STEADY.intensityFactor(time));
            assertEquals(1.0, Flicker.STEADY.radiusFactor(time));
        }
        assertTrue(Flicker.STEADY.isSteady());
        assertFalse(crackling(1).isSteady());
    }

    @Test
    void theSameSeedAndMomentAlwaysGiveTheSameAnswer() {
        final Flicker first = crackling(42);
        final Flicker second = crackling(42);

        for (double time = 0.0; time < 30.0; time += 0.173) {
            assertEquals(first.intensityFactor(time), second.intensityFactor(time));
            assertEquals(first.radiusFactor(time), second.radiusFactor(time));
            assertEquals(first.intensityFactor(time), first.intensityFactor(time), "asking twice changes nothing");
        }
    }

    @Test
    void differentSeedsDoNotFlickerInStep() {
        final Flicker a = crackling(1);
        final Flicker b = crackling(2);

        int different = 0;
        for (double time = 0.0; time < 10.0; time += 0.1) {
            if (Math.abs(a.intensityFactor(time) - b.intensityFactor(time)) > 1e-3) {
                different++;
            }
        }
        assertTrue(different > 50, "different at " + different + " of 100 moments");
    }

    @Test
    void theFactorsStayInsideTheAmplitudeAndUseMostOfIt() {
        final Flicker flicker = crackling(7);
        double lowestIntensity = Double.MAX_VALUE;
        double highestIntensity = -Double.MAX_VALUE;
        double lowestRadius = Double.MAX_VALUE;
        double highestRadius = -Double.MAX_VALUE;

        for (double time = 0.0; time < 600.0; time += 0.01) {
            final double intensity = flicker.intensityFactor(time);
            final double radius = flicker.radiusFactor(time);
            assertTrue(intensity >= 1.0 - INTENSITY - 1e-12 && intensity <= 1.0 + INTENSITY + 1e-12, "" + intensity);
            assertTrue(radius >= 1.0 - RADIUS - 1e-12 && radius <= 1.0 + RADIUS + 1e-12, "" + radius);
            lowestIntensity = Math.min(lowestIntensity, intensity);
            highestIntensity = Math.max(highestIntensity, intensity);
            lowestRadius = Math.min(lowestRadius, radius);
            highestRadius = Math.max(highestRadius, radius);
        }

        assertTrue(highestIntensity - lowestIntensity > 1.2 * INTENSITY, "the intensity moves across most of its range");
        assertTrue(highestRadius - lowestRadius > 1.2 * RADIUS, "the radius moves across most of its range");
    }

    @Test
    void theWaveringIsSmoothSoALittleTimeMovesItByALittle() {
        final Flicker flicker = crackling(99);
        final double dt = 1.0 / 60.0;
        final double intensityStep = INTENSITY * 2 * Math.PI * HZ * MAX_SLOPE_FACTOR * dt;
        final double radiusStep = RADIUS * 2 * Math.PI * HZ * MAX_SLOPE_FACTOR * dt;

        for (double time = 0.0; time < 300.0; time += 0.0137) {
            assertTrue(Math.abs(flicker.intensityFactor(time + dt) - flicker.intensityFactor(time))
                    <= intensityStep + 1e-9, "intensity jumped at " + time);
            assertTrue(Math.abs(flicker.radiusFactor(time + dt) - flicker.radiusFactor(time))
                    <= radiusStep + 1e-9, "radius jumped at " + time);
        }
        // A frame of a sixtieth of a second never moves the intensity by more than a few hundredths
        assertTrue(intensityStep < 0.06, "" + intensityStep);
    }

    @Test
    void theIntensityAndTheRadiusMoveIndependently() {
        final Flicker flicker = crackling(5);

        int sameDirection = 0;
        int opposite = 0;
        for (double time = 0.0; time < 20.0; time += 0.05) {
            final double intensity = (flicker.intensityFactor(time) - 1.0) / INTENSITY;
            final double radius = (flicker.radiusFactor(time) - 1.0) / RADIUS;
            assertNotEquals(intensity, radius, 1e-9, "never the same wave at " + time);
            if (intensity * radius > 0) {
                sameDirection++;
            } else {
                opposite++;
            }
        }
        assertTrue(sameDirection > 30 && opposite > 30, sameDirection + " same, " + opposite + " opposite");
    }

    @Test
    void invalidValuesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Flicker.crackling(-0.1, 0.0, 3.0, 1));
        assertThrows(IllegalArgumentException.class, () -> Flicker.crackling(0.0, 1.0, 3.0, 1));
        assertThrows(IllegalArgumentException.class, () -> Flicker.crackling(0.1, 0.1, 0.0, 1));
        assertThrows(IllegalArgumentException.class, () -> Flicker.crackling(Double.NaN, 0.1, 3.0, 1));
    }
}
