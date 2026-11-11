package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import java.util.Arrays;

/**
 * The ground shadows of one frame: where each one is centred, how wide it is and how much of its opacity is left.
 * The arrays are kept between frames and only grow, so a steady frame allocates nothing here.
 */
public final class ShadowList {

    private static final int INITIAL_CAPACITY = 64;

    private double[] x = new double[INITIAL_CAPACITY];
    private double[] y = new double[INITIAL_CAPACITY];
    private double[] width = new double[INITIAL_CAPACITY];
    private double[] fade = new double[INITIAL_CAPACITY];
    private int size;

    /** Empties the list for the next frame, keeping the arrays. */
    public void clear() {
        size = 0;
    }

    /**
     * Adds a shadow.
     *
     * @param centreX  screen X of the ground contact point
     * @param centreY  screen Y of the ground contact point
     * @param widthArt wanted width in art pixels
     * @param fade     share of the baked shadow opacity, 0 to 1
     */
    public void add(double centreX, double centreY, double widthArt, double fade) {
        if (size == x.length) {
            final int capacity = size * 2;
            x = Arrays.copyOf(x, capacity);
            y = Arrays.copyOf(y, capacity);
            width = Arrays.copyOf(width, capacity);
            this.fade = Arrays.copyOf(this.fade, capacity);
        }
        x[size] = centreX;
        y[size] = centreY;
        width[size] = widthArt;
        this.fade[size] = fade;
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

    /** @return the wanted width of a shadow in art pixels */
    public double widthArt(int index) {
        return width[index];
    }

    /** @return the share of the baked opacity of a shadow */
    public double fade(int index) {
        return fade[index];
    }
}
