package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoomStepsTest {

    @Test
    void zoomingInMovesOneWholeStep() {
        assertEquals(1.5, ZoomSteps.move(1.0, 1, 1.0, 2.0, 0.5));
        assertEquals(2.0, ZoomSteps.move(1.5, 1, 1.0, 2.0, 0.5));
    }

    @Test
    void zoomingOutMovesOneWholeStep() {
        assertEquals(1.5, ZoomSteps.move(2.0, -1, 1.0, 2.0, 0.5));
        assertEquals(1.0, ZoomSteps.move(1.5, -1, 1.0, 2.0, 0.5));
    }

    @Test
    void theRangeEndsStopTheMove() {
        assertEquals(2.0, ZoomSteps.move(2.0, 1, 1.0, 2.0, 0.5));
        assertEquals(1.0, ZoomSteps.move(1.0, -1, 1.0, 2.0, 0.5));
    }

    @Test
    void aZoomOffTheGridSnapsToTheNearestLevel() {
        assertEquals(1.5, ZoomSteps.move(1.4, 0, 1.0, 2.0, 0.5));
        assertEquals(2.0, ZoomSteps.move(1.9, 1, 1.0, 2.0, 0.5));
    }

    @Test
    void theArtScaleIsAWholeNumberOnEveryLevel() {
        for (double zoom : new double[]{1.0, 1.5, 2.0}) {
            double scale = ZoomSteps.artScale(zoom);
            assertEquals(Math.rint(scale), scale);
        }
        assertEquals(2.0, ZoomSteps.artScale(1.0));
        assertEquals(4.0, ZoomSteps.artScale(2.0));
    }

    @Test
    void theIntegerArtScaleIsTheArtScaleOnTheGridAndAWholeNumberOffIt() {
        for (double zoom : new double[]{0.5, 1.0, 1.5, 2.0, 5.0}) {
            assertEquals(ZoomSteps.artScale(zoom), ZoomSteps.integerArtScale(zoom), "on the grid at " + zoom);
        }
        assertEquals(3.0, ZoomSteps.integerArtScale(1.4), "2.8 rounds to 3");
        assertEquals(2.0, ZoomSteps.integerArtScale(1.1), "2.2 rounds to 2");
        assertEquals(Math.rint(ZoomSteps.integerArtScale(1.3)), ZoomSteps.integerArtScale(1.3));
    }

    @Test
    void theIntegerArtScaleNeverFallsBelowOne() {
        assertEquals(1.0, ZoomSteps.integerArtScale(0.1));
        assertEquals(1.0, ZoomSteps.integerArtScale(0.0));
        assertEquals(1.0, ZoomSteps.integerArtScale(0.2));
    }

    @Test
    void aValidRangeIsAccepted() {
        assertDoesNotThrow(() -> ZoomSteps.requireValidRange(1.0, 2.0, 0.5));
        assertDoesNotThrow(() -> ZoomSteps.requireValidRange(1.0, 1.0, 0.5));
    }

    @Test
    void aLowestZoomThatIsNotPositiveIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(0.0, 2.0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(-1.0, 2.0, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> ZoomSteps.requireValidRange(Double.NEGATIVE_INFINITY, 2.0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(1.0, Double.NaN, 0.5));
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(1.0, 2.0, Double.NaN));
    }

    @Test
    void aBadRangeFailsWithAClearMessageInsteadOfLettingTheClampThrowLater() {
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(2.0, 1.0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(1.0, 2.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(1.0, 2.0, -0.5));
        assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(Double.NaN, 2.0, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> ZoomSteps.requireValidRange(1.0, Double.POSITIVE_INFINITY, 0.5));
        final var error = assertThrows(IllegalArgumentException.class, () -> ZoomSteps.requireValidRange(2.0, 1.0, 0.5));
        assertEquals("The lowest zoom 2.0 is above the highest zoom 1.0", error.getMessage());
    }
}
