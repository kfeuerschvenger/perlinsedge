package com.feuerschvenger.perlinsedge.domain.entities;

/**
 * Represents an abstract camera in the game world.
 * Contains position and zoom level, without specific rendering logic.
 */
public class Camera {
    protected double x;
    protected double y;
    protected double zoom;

    public Camera(double x, double y, double zoom) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public void move(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    public void zoomIn(double factor) {
        this.zoom *= factor;
    }

    public void zoomOut(double factor) {
        this.zoom /= factor;
    }

}