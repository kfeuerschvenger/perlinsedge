package com.feuerschvenger.perlinsedge.domain.world.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunScreenMappingTest {

    @Test
    void aMorningShadowPointsToTheLeftOfTheScreen() {
        assertEquals(-1.0, SunScreenMapping.shadowDirectionX(0.0), 1e-9);
        assertTrue(SunScreenMapping.shadowDirectionX(0.15) < -0.7);
    }

    @Test
    void aNoonShadowPointsStraightDownTowardTheViewer() {
        assertEquals(0.0, SunScreenMapping.shadowDirectionX(0.5), 1e-9);
        assertEquals(1.0, SunScreenMapping.shadowDirectionY(0.5), 1e-9);
    }

    @Test
    void anEveningShadowPointsToTheRightOfTheScreen() {
        assertEquals(1.0, SunScreenMapping.shadowDirectionX(1.0), 1e-9);
        assertTrue(SunScreenMapping.shadowDirectionX(0.85) > 0.7);
    }

    @Test
    void theShadowSwingsFromLeftThroughDownToRightWithoutEverPointingUp() {
        double previousX = -2;
        for (double p = 0; p <= 1.0; p += 0.01) {
            final double x = SunScreenMapping.shadowDirectionX(p);
            assertTrue(x >= previousX, "the shadow only turns one way, at " + p);
            assertTrue(SunScreenMapping.shadowDirectionY(p) >= -1e-12);
            assertEquals(1.0, Math.hypot(x, SunScreenMapping.shadowDirectionY(p)), 1e-9);
            previousX = x;
        }
    }

    @Test
    void aPositionOutsideTheArcIsClampedToTheNearestEnd() {
        assertEquals(SunScreenMapping.shadowAngle(0.0), SunScreenMapping.shadowAngle(-3.0));
        assertEquals(SunScreenMapping.shadowAngle(1.0), SunScreenMapping.shadowAngle(7.0));
    }
}
