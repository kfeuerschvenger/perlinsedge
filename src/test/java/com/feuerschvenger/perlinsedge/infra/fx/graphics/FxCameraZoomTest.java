package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FxCameraZoomTest {

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;

    @Test
    void zoomingOutFromTheFarCornerStaysInsideTheMapBounds() {
        // At zoom 1.5 the camera rests on the lowest offsets the 150 by 150 map allows
        FxCamera camera = new FxCamera(VIEW_WIDTH - 149 * 48, VIEW_HEIGHT - 298 * 24, 1.5);

        camera.zoomStep(-1, 0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        // The lowest offsets at zoom 1: the viewport size minus the far corner of the map
        assertEquals(1.0, camera.getZoom());
        assertEquals(VIEW_WIDTH - 149 * 32, camera.getX());
        assertEquals(VIEW_HEIGHT - 298 * 16, camera.getY());
    }

    @Test
    void zoomingInKeepsAnInsidePositionRoundedToWholePixels() {
        FxCamera camera = new FxCamera(100, 100, 1.0);

        camera.zoomStep(1, 401, 301, VIEW_WIDTH, VIEW_HEIGHT);

        assertEquals(1.5, camera.getZoom());
        assertTrue(camera.getX() == Math.rint(camera.getX()) && camera.getY() == Math.rint(camera.getY()));
    }

    @Test
    void aZoomAtTheLimitChangesNothing() {
        FxCamera camera = new FxCamera(100, 100, 2.0);

        camera.zoomStep(1, 10, 10, VIEW_WIDTH, VIEW_HEIGHT);

        assertEquals(2.0, camera.getZoom());
        assertEquals(100, camera.getX());
        assertEquals(100, camera.getY());
    }
}
