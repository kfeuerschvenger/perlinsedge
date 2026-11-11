package com.feuerschvenger.perlinsedge.infra.fx.graphics;

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
}
