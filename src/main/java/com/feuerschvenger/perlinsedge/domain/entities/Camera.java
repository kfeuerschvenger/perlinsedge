package com.feuerschvenger.perlinsedge.domain.entities;

/**
 * Represents an abstract game camera that controls the viewport into the game world.
 * Manages position (x, y coordinates) and zoom level for rendering.
 */
public class Camera {
    // Configuration constants
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;
    private static final double DEFAULT_ZOOM = 1.0;

    // Instance state
    protected double x;
    protected double y;
    protected double zoom;

    /**
     * Creates a new camera instance at specified coordinates.
     *
     * @param x    Initial x-coordinate in world units
     * @param y    Initial y-coordinate in world units
     * @param zoom Initial zoom level (1.0 = normal, >1.0 = zoomed in, <1.0 = zoomed out)
     * @throws IllegalArgumentException if zoom is not within valid range
     */
    public Camera(double x, double y, double zoom) {
        this.x = x;
        this.y = y;
        setZoom(zoom);  // Use setter for validation
    }

    // ==================================================================
    //  Accessors
    // ==================================================================

    /** @return Current x-coordinate in world units */
    public double getX() {
        return x;
    }

    /** @return Current y-coordinate in world units */
    public double getY() {
        return y;
    }

    /** @return Current zoom level (1.0 = normal scale) */
    public double getZoom() {
        return zoom;
    }

    // ==================================================================
    //  Mutators
    // ==================================================================

    /**
     * Sets camera position to new coordinates.
     *
     * @param x New x-coordinate in world units
     * @param y New y-coordinate in world units
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the zoom level with validation.
     *
     * @param zoom New zoom level (clamped between MIN_ZOOM and MAX_ZOOM)
     * @throws IllegalArgumentException if zoom is not within valid range
     */
    public void setZoom(double zoom) {
        if (zoom < MIN_ZOOM || zoom > MAX_ZOOM) {
            throw new IllegalArgumentException(
                    "Zoom must be between " + MIN_ZOOM + " and " + MAX_ZOOM
            );
        }
        this.zoom = zoom;
    }

    // ==================================================================
    //  Movement Operations
    // ==================================================================

    /**
     * Moves the camera by specified offsets.
     *
     * @param dx Horizontal offset in world units
     * @param dy Vertical offset in world units
     */
    public void translate(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    /**
     * Zooms the camera in by specified factor.
     *
     * @param factor Multiplier for zoom increase (>1.0)
     * @throws IllegalArgumentException if factor is not greater than 1.0
     */
    public void zoomIn(double factor) {
        if (factor <= 1.0) {
            throw new IllegalArgumentException("Zoom factor must be greater than 1.0");
        }
        setZoom(zoom * factor);
    }

    /**
     * Zooms the camera out by specified factor.
     *
     * @param factor Divisor for zoom decrease (>1.0)
     * @throws IllegalArgumentException if factor is not greater than 1.0
     */
    public void zoomOut(double factor) {
        if (factor <= 1.0) {
            throw new IllegalArgumentException("Zoom factor must be greater than 1.0");
        }
        setZoom(zoom / factor);
    }

    // ==================================================================
    //  Utility Methods
    // ==================================================================

    /** Resets zoom level to default (1.0) */
    public void resetZoom() {
        setZoom(DEFAULT_ZOOM);
    }

}