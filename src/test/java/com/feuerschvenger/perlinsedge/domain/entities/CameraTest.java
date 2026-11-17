package com.feuerschvenger.perlinsedge.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CameraTest {

    @Test
    void aZoomOnTheStepGridIsKept() {
        assertEquals(1.5, new Camera(0, 0, 1.5).getZoom());
        assertEquals(2.0, new Camera(0, 0, 2.0).getZoom());
    }

    @Test
    void aZoomBetweenTwoStepsSnapsToTheNearestOne() {
        assertEquals(1.5, new Camera(0, 0, 1.4).getZoom());
        assertEquals(1.0, new Camera(0, 0, 1.2).getZoom());
        assertEquals(1.0, new Camera(0, 0, 1.0 + 1e-9).getZoom());
    }

    @Test
    void twiceTheZoomIsAlwaysAWholeNumber() {
        Camera camera = new Camera(0, 0, 1.0);
        for (double zoom = 0.1; zoom <= 10.0; zoom += 0.07) {
            camera.setZoom(zoom);
            assertEquals(Math.rint(camera.getZoom() * 2), camera.getZoom() * 2, 0.0, "zoom " + zoom);
        }
    }

    @Test
    void theSmallestZoomIsOneStepNotZero() {
        assertEquals(0.5, new Camera(0, 0, 0.1).getZoom());
    }

    @Test
    void aZoomOutsideTheRangeIsStillRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Camera(0, 0, 0.05));
        assertThrows(IllegalArgumentException.class, () -> new Camera(0, 0, 11));
    }

    @Test
    void zoomingByAFactorLandsOnTheGridToo() {
        Camera camera = new Camera(0, 0, 1.0);

        camera.zoomIn(1.2);

        assertEquals(1.0, camera.getZoom());
        camera.zoomIn(1.5);
        assertEquals(1.5, camera.getZoom());
    }
}
