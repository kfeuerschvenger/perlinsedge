package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FxCameraZoomTest {

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;

    private static AppConfig config;
    private static int lastColumn;
    private static int lastRow;

    @BeforeAll
    static void readTheConfiguration() {
        config = AppConfig.getInstance();
        lastColumn = config.world().getWidth() - 1;
        lastRow = config.world().getHeight() - 1;
    }

    /** The lowest horizontal offset the camera may take at a zoom: the viewport minus the right corner of the map. */
    private static double lowestX(double zoom) {
        return VIEW_WIDTH - lastColumn * config.graphics().getTileHalfWidth() * zoom;
    }

    /** The highest horizontal offset: the left corner of the map is the row axis, so it rests on the last row. */
    private static double highestX(double zoom) {
        return lastRow * config.graphics().getTileHalfWidth() * zoom;
    }

    /** The lowest vertical offset: the viewport minus the bottom corner of the map. */
    private static double lowestY(double zoom) {
        return VIEW_HEIGHT - (lastColumn + lastRow) * config.graphics().getTileHalfHeight() * zoom;
    }

    private static void assertInsideBounds(FxCamera camera) {
        double zoom = camera.getZoom();
        assertTrue(camera.getX() >= lowestX(zoom) - 0.5 && camera.getX() <= highestX(zoom) + 0.5,
                "x " + camera.getX() + " outside [" + lowestX(zoom) + ", " + highestX(zoom) + "]");
        assertTrue(camera.getY() >= lowestY(zoom) - 0.5 && camera.getY() <= 0.5,
                "y " + camera.getY() + " outside [" + lowestY(zoom) + ", 0]");
        assertEquals(Math.rint(camera.getX()), camera.getX());
        assertEquals(Math.rint(camera.getY()), camera.getY());
    }

    @Test
    void theTestMapIsLargerThanTheViewportAtEveryZoomItUses() {
        assertTrue(lowestX(1.0) < 0 && lowestY(1.0) < 0, "the bounds below assume a map larger than the viewport");
    }

    @Test
    void zoomingOutFromTheFarCornerStaysInsideTheMapBounds() {
        // At zoom 1.5 the camera rests on the lowest offsets the map allows
        FxCamera camera = new FxCamera(lowestX(1.5), lowestY(1.5), 1.5);

        camera.zoomStep(-1, 0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        // The lowest offsets at zoom 1: the viewport size minus the far corner of the map
        assertEquals(1.0, camera.getZoom());
        assertEquals(Math.rint(lowestX(1.0)), camera.getX());
        assertEquals(Math.rint(lowestY(1.0)), camera.getY());
        assertInsideBounds(camera);
    }

    @Test
    void zoomingInFromTheFarCornerStaysInsideTheMapBounds() {
        FxCamera camera = new FxCamera(lowestX(1.0), lowestY(1.0), 1.0);

        camera.zoomStep(1, 0, 0, VIEW_WIDTH, VIEW_HEIGHT);

        assertEquals(1.5, camera.getZoom());
        assertInsideBounds(camera);
        // The focus is the corner of the viewport, so the offset is scaled by the zoom ratio and already inside
        assertEquals(Math.rint(lowestX(1.0) * 1.5), camera.getX());
        assertEquals(Math.rint(lowestY(1.0) * 1.5), camera.getY());
    }

    @Test
    void zoomingOutFromTheOriginCornerStaysInsideTheMapBounds() {
        FxCamera camera = new FxCamera(highestX(1.5), 0, 1.5);

        camera.zoomStep(-1, VIEW_WIDTH, VIEW_HEIGHT, VIEW_WIDTH, VIEW_HEIGHT);

        assertEquals(1.0, camera.getZoom());
        assertInsideBounds(camera);
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
