package com.feuerschvenger.perlinsedge.infra.fx.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterShimmerTest {

    @Test
    void aCycleTakesAboutTwoSeconds() {
        assertEquals(1.0, WaterShimmer.cycles(2.0), 1e-12);
        assertEquals(0.0, WaterShimmer.cycles(0.0), 1e-12);
    }

    @Test
    void everyTileShowsTwoOrThreeLines() {
        boolean two = false;
        boolean three = false;
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 40; y++) {
                int count = WaterShimmer.lineCount(x, y);
                assertTrue(count == 2 || count == 3, "count " + count);
                two |= count == 2;
                three |= count == 3;
            }
        }
        assertTrue(two && three);
    }

    @Test
    void aLineThatTheTileLacksIsNeverVisible() {
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 40; y++) {
                if (WaterShimmer.lineCount(x, y) == 2) {
                    assertEquals(0.0, WaterShimmer.alpha(0.37, x, y, 2));
                }
            }
        }
    }

    @Test
    void theOpacityStaysInRangeAndReachesBothEnds() {
        double min = Double.MAX_VALUE;
        double max = 0.0;
        for (int step = 0; step < 400; step++) {
            double a = WaterShimmer.alpha(step * 0.01, 4, 9, 0);
            assertTrue(a >= 0.0 && a <= WaterShimmer.PEAK_ALPHA + 1e-12, "alpha " + a);
            min = Math.min(min, a);
            max = Math.max(max, a);
        }
        assertTrue(min < 0.01, "fades out, min " + min);
        assertTrue(max > WaterShimmer.PEAK_ALPHA - 0.01, "fades in, max " + max);
    }

    @Test
    void theLineRepeatsEveryCycle() {
        assertEquals(WaterShimmer.alpha(0.3, 6, 6, 1), WaterShimmer.alpha(1.3, 6, 6, 1), 1e-9);
    }

    @Test
    void neighbouringTilesAreOutOfStep() {
        int differing = 0;
        for (int x = 0; x < 30; x++) {
            double a = WaterShimmer.alpha(0.5, x, 10, 0);
            double b = WaterShimmer.alpha(0.5, x + 1, 10, 0);
            if (Math.abs(a - b) > 0.02) {
                differing++;
            }
        }
        assertTrue(differing > 15, "differing " + differing);
    }

    @Test
    void aTileAlwaysAnimatesTheSameWay() {
        assertEquals(WaterShimmer.alpha(3.7, 8, 2, 0), WaterShimmer.alpha(3.7, 8, 2, 0));
        assertEquals(WaterShimmer.offsetU(3.7, 8, 2, 0), WaterShimmer.offsetU(3.7, 8, 2, 0));
    }

    @Test
    void aLineHoldsItsSpotDuringOneFadeAndMovesInTheNext() {
        // A line is faded out exactly where its fade index changes, so it never jumps while visible.
        double start = 2.0 - WaterShimmer.tileOffset(5, 5);
        double justBefore = WaterShimmer.offsetU(start - 0.01, 5, 5, 0);
        double justAfter = WaterShimmer.offsetU(start + 0.01, 5, 5, 0);
        assertNotEquals(justBefore, justAfter);
        assertEquals(WaterShimmer.offsetU(start + 0.2, 5, 5, 0), justAfter);
        assertTrue(WaterShimmer.alpha(start - 0.0001, 5, 5, 0) < 0.001);
        assertTrue(WaterShimmer.alpha(start + 0.0001, 5, 5, 0) < 0.001);
    }

    @Test
    void offsetsStayInsideTheTile() {
        for (int x = 0; x < 30; x++) {
            for (int y = 0; y < 30; y++) {
                for (int line = 0; line < WaterShimmer.MAX_LINES; line++) {
                    double u = WaterShimmer.offsetU(x * 0.13, x, y, line);
                    double v = WaterShimmer.offsetV(x * 0.13, x, y, line);
                    assertTrue(Math.abs(u) <= WaterShimmer.MAX_OFFSET && Math.abs(v) <= WaterShimmer.MAX_OFFSET);
                }
            }
        }
    }
}
