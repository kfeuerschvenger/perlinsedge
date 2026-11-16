package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;

import java.util.Arrays;

/**
 * The ground shadows of one frame. A shadow is either the silhouette of a sprite, standing on a contact point, or an
 * ellipse of a width for an object drawn from shapes. Each also carries how much of its opacity is left. The arrays are
 * kept between frames and only grow, so a steady frame allocates nothing here.
 */
public final class ShadowList {

    private static final int INITIAL_CAPACITY = 64;

    private double[] x = new double[INITIAL_CAPACITY];
    private double[] y = new double[INITIAL_CAPACITY];
    private double[] width = new double[INITIAL_CAPACITY];
    private double[] fade = new double[INITIAL_CAPACITY];
    private Silhouette[] silhouette = new Silhouette[INITIAL_CAPACITY];
    private double[] groundRow = new double[INITIAL_CAPACITY];
    private int size;

    /** Empties the list for the next frame, keeping the arrays. */
    public void clear() {
        Arrays.fill(silhouette, 0, size, null);
        size = 0;
    }

    /**
     * Adds the ellipse shadow of an object drawn from shapes.
     *
     * @param centreX  screen X of the ground contact point
     * @param centreY  screen Y of the ground contact point
     * @param widthArt wanted width in art pixels
     * @param fade     share of the shadow opacity, 0 to 1
     */
    public void add(double centreX, double centreY, double widthArt, double fade) {
        append(centreX, centreY, widthArt, fade, null, 0.0);
    }

    /**
     * Adds the shadow of a sprite, cast from its silhouette.
     *
     * @param centreX     screen X of the ground contact point, under the middle of the sprite
     * @param centreY     screen Y of the ground contact point
     * @param silhouette  the silhouette of the sprite
     * @param groundRow   the row of the sprite that touches the ground, counted from its top
     * @param widthFactor how wide the shadow is relative to the sprite, 1 for the whole width
     * @param fade        share of the shadow opacity, 0 to 1
     */
    public void addSilhouette(double centreX, double centreY, Silhouette silhouette, double groundRow,
                              double widthFactor, double fade) {
        append(centreX, centreY, silhouette.width() * widthFactor, fade, silhouette, groundRow);
    }

    /** Stores one shadow, growing the arrays when they are full. */
    private void append(double centreX, double centreY, double widthArt, double fade, Silhouette sprite,
                        double groundRow) {
        if (size == x.length) {
            final int capacity = size * 2;
            x = Arrays.copyOf(x, capacity);
            y = Arrays.copyOf(y, capacity);
            width = Arrays.copyOf(width, capacity);
            this.fade = Arrays.copyOf(this.fade, capacity);
            silhouette = Arrays.copyOf(silhouette, capacity);
            this.groundRow = Arrays.copyOf(this.groundRow, capacity);
        }
        x[size] = centreX;
        y[size] = centreY;
        width[size] = widthArt;
        this.fade[size] = fade;
        silhouette[size] = sprite;
        this.groundRow[size] = groundRow;
        size++;
    }

    /** @return the number of shadows in the list */
    public int size() {
        return size;
    }

    /** @return the screen X of a shadow */
    public double centreX(int index) {
        return x[index];
    }

    /** @return the screen Y of a shadow */
    public double centreY(int index) {
        return y[index];
    }

    /** @return the wanted width of a shadow in art pixels, for a sprite the width of its silhouette times the factor */
    public double widthArt(int index) {
        return width[index];
    }

    /** @return the share of the opacity of a shadow */
    public double fade(int index) {
        return fade[index];
    }

    /** @return the silhouette a shadow is cast from, or null for an ellipse */
    public Silhouette silhouette(int index) {
        return silhouette[index];
    }

    /** @return the row of the sprite of a shadow that touches the ground, zero for an ellipse */
    public double groundRow(int index) {
        return groundRow[index];
    }
}
