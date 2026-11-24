package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private static final Rgba ORANGE = new Rgba(1.0, 0.58, 0.25, 1.0);
    private static final Rgba YELLOW = new Rgba(1.0, 0.82, 0.45, 1.0);

    /** One golden row: the seed and moment of a flicker and the four factors it must give there. */
    private record Pinned(long seed, double time, double intensity, double radius, double colour, double shape) {
    }

    private static final Rgba DRIFT_RED = new Rgba(1, 0, 0, 1);
    private static final Rgba DRIFT_BLUE = new Rgba(0, 0, 1, 1);
    private static final double PINNED_TOLERANCE = 1e-12;
    private static final double SHAPE_AMOUNT = 0.2;
    private static final double SHAPE_SPEED = 0.5;
    private static final double DRIFT_SHARE = 0.4;
    private static final double SHAPE_TURN = 0.3;
    private static final long SEED_ZERO = 0;
    private static final long SEED_ONE = 1;
    private static final long SEED_NEGATIVE = -5;
    private static final long SEED_FORTY_TWO = 42;
    private static final double TIME_ORIGIN = 0.0;
    private static final double TIME_HALF_SECOND = 0.5;
    private static final double TIME_QUARTER_PAST_ONE = 1.25;
    private static final double TIME_LATE = 7.9;

    private static final List<Pinned> PINNED = List.of(
            new Pinned(SEED_ZERO, TIME_ORIGIN, 0.9823569111232505, 1.0135879630186317, 0.7127461053356473,
                    1.064748721560218),
            new Pinned(SEED_ZERO, TIME_LATE, 1.0578282938891777, 1.0061017845345113, 0.8886102640555026,
                    0.8768526428153425),
            new Pinned(SEED_ONE, TIME_HALF_SECOND, 1.0263712270761531, 1.0263367146762576, 0.4480675319837185,
                    1.0123333697125199),
            new Pinned(SEED_FORTY_TWO, TIME_QUARTER_PAST_ONE, 0.9547192708962329, 1.0163215672027863,
                    0.15649556661262748, 1.0564852225160033),
            new Pinned(SEED_NEGATIVE, TIME_ORIGIN, 1.0023595696962795, 0.9733009719107767, 0.6398103145459535,
                    1.016034387983858),
            new Pinned(SEED_NEGATIVE, TIME_LATE, 1.0722033128952002, 1.0156866989654614, 0.6208718631238823,
                    0.8423939852178733));

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
        final Flicker base = crackling(1);
        assertThrows(IllegalArgumentException.class, () -> base.withShape(0.5, 0.2));
        assertThrows(IllegalArgumentException.class, () -> base.withShape(0.1, 0.0));
        assertThrows(IllegalArgumentException.class, () -> base.withColourDrift(null, ORANGE, 0.7));
        assertThrows(IllegalArgumentException.class, () -> base.withColourDrift(ORANGE, YELLOW, 0.0));
    }

    private static Flicker flame(long seed) {
        return crackling(seed).withColourDrift(ORANGE, YELLOW, 0.7).withShape(0.10, 0.25);
    }

    @Test
    void aLightWithNoColourDriftAndNoShapeKeepsBoth() {
        final Flicker plain = crackling(3);

        assertFalse(plain.hasColourDrift());
        assertEquals(0.5, plain.colourShare(12.3));
        assertEquals(1.0, plain.shapeFactor(0.3, 12.3));
        assertTrue(Flicker.STEADY.isSteady());
        assertFalse(flame(3).isSteady());
    }

    @Test
    void theColourShareStaysBetweenTheTwoColoursAndDriftsSlowlyWithASmallJitter() {
        final Flicker flicker = flame(11);
        final double dt = 1.0 / 60.0;
        double lowest = 1.0;
        double highest = 0.0;
        double biggestStep = 0.0;
        for (double time = 0.0; time < 600.0; time += dt) {
            final double share = flicker.colourShare(time);
            assertTrue(share >= 0.0 && share <= 1.0, "" + share);
            lowest = Math.min(lowest, share);
            highest = Math.max(highest, share);
            biggestStep = Math.max(biggestStep, Math.abs(flicker.colourShare(time + dt) - share));
        }
        // Slower than the crackle: the share needs a good part of a second to cross most of its range
        assertTrue(highest - lowest > 0.6, "the colour uses most of the range: " + (highest - lowest));
        assertTrue(biggestStep < 0.1, "smooth from frame to frame: " + biggestStep);
        // The slow drift alone, a tenth of a second apart, moves it by less than the whole range
        for (double time = 0.0; time < 60.0; time += 0.5) {
            assertTrue(Math.abs(flicker.colourShare(time + 0.02) - flicker.colourShare(time)) < 0.3);
        }
        assertEquals(flicker.colourShare(5.5), flame(11).colourShare(5.5), "deterministic from the seed and the time");
    }

    @Test
    void theShapeStaysWithinItsAmplitudeAndTheEdgeIsNotARoundOne() {
        final Flicker flicker = flame(21);
        double lowest = Double.MAX_VALUE;
        double highest = -Double.MAX_VALUE;
        for (double time = 0.0; time < 200.0; time += 0.37) {
            for (double turn = 0.0; turn < 1.0; turn += 0.01) {
                final double factor = flicker.shapeFactor(turn, time);
                assertTrue(factor >= 0.9 - 1e-12 && factor <= 1.1 + 1e-12, "" + factor);
                lowest = Math.min(lowest, factor);
                highest = Math.max(highest, factor);
            }
        }
        assertTrue(highest - lowest > 0.12, "the lobes use most of the amplitude: " + (highest - lowest));
    }

    @Test
    void theShapeWrapsAroundTheSourceAndChangesSmoothlyBothAcrossAnglesAndOverTime() {
        final Flicker flicker = flame(33);

        for (double time = 0.0; time < 60.0; time += 1.3) {
            assertEquals(flicker.shapeFactor(0.0, time), flicker.shapeFactor(1.0, time), 1e-9, "a full turn wraps");
            for (double turn = 0.0; turn < 1.0; turn += 0.002) {
                assertTrue(Math.abs(flicker.shapeFactor(turn + 0.002, time) - flicker.shapeFactor(turn, time)) < 0.01,
                        "no jump between neighbouring angles");
            }
        }
        final double dt = 1.0 / 60.0;
        double biggest = 0.0;
        for (double time = 0.0; time < 120.0; time += 0.173) {
            for (double turn = 0.0; turn < 1.0; turn += 0.05) {
                biggest = Math.max(biggest,
                        Math.abs(flicker.shapeFactor(turn, time + dt) - flicker.shapeFactor(turn, time)));
            }
        }
        assertTrue(biggest < 0.003, "a frame moves the edge by a fraction of a percent: " + biggest);
        boolean changes = false;
        for (double turn = 0.0; turn < 1.0; turn += 0.1) {
            changes |= Math.abs(flicker.shapeFactor(turn, 0.0) - flicker.shapeFactor(turn, 10.0)) > 0.02;
        }
        assertTrue(changes, "the lobes change over time");
    }

    /** The shape and colour drift added to the pinned flickers, so every channel is covered. */
    private static Flicker pinnedFlicker(long seed) {
        return crackling(seed).withShape(SHAPE_AMOUNT, SHAPE_SPEED)
                .withColourDrift(DRIFT_RED, DRIFT_BLUE, DRIFT_SHARE);
    }

    @Test
    void outputsArePinnedForFixedSeedsAndTimes() {
        for (var row : PINNED) {
            final var flicker = pinnedFlicker(row.seed());
            assertEquals(row.intensity(), flicker.intensityFactor(row.time()), PINNED_TOLERANCE);
            assertEquals(row.radius(), flicker.radiusFactor(row.time()), PINNED_TOLERANCE);
            assertEquals(row.colour(), flicker.colourShare(row.time()), PINNED_TOLERANCE);
            assertEquals(row.shape(), flicker.shapeFactor(SHAPE_TURN, row.time()), PINNED_TOLERANCE);
        }
    }
}
