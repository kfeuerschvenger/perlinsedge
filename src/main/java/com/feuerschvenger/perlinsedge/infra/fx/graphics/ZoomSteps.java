package com.feuerschvenger.perlinsedge.infra.fx.graphics;

/**
 * Whole-step zoom levels for pixel art.
 * The art is drawn at an integer multiple of its logical size so every logical pixel stays a crisp square block. A zoom
 * of 1.0 is the base tile, which is the 32x16 art at 2x, so the zoom levels are multiples of one half and the art scale
 * is twice the zoom.
 */
public final class ZoomSteps {

    /** Art pixels per world unit at zoom 1.0: the 64x32 world tile is the 32x16 art drawn at 2x. */
    public static final double ART_SCALE_AT_UNIT_ZOOM = 2.0;

    private ZoomSteps() {}

    /**
     * Moves one step up or down from a zoom level, staying inside the range.
     *
     * @param current   the current zoom
     * @param direction positive to zoom in, negative to zoom out, zero to only snap the current zoom
     * @param min       the lowest zoom, also the origin of the step grid
     * @param max       the highest zoom
     * @param step      the distance between two levels
     * @return the zoom level after the move, always on the grid
     */
    public static double move(double current, int direction, double min, double max, double step) {
        int levels = (int) Math.round((max - min) / step);
        int index = (int) Math.round((current - min) / step) + Integer.signum(direction);
        index = Math.max(0, Math.min(levels, index));
        return min + index * step;
    }

    /**
     * Returns the art scale for a zoom, the number of screen pixels one logical art pixel covers.
     *
     * @param zoom the camera zoom
     * @return the art scale
     */
    public static double artScale(double zoom) {
        return zoom * ART_SCALE_AT_UNIT_ZOOM;
    }
}
