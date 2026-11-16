package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;

import java.util.Arrays;

/**
 * The ground shadows of one frame. A shadow is either the silhouette of a sprite, standing on a contact point, or an
 * ellipse of a width for an object drawn from shapes. Each also carries how much of its opacity is left. The arrays are
 * kept between frames and only grow, so a steady frame allocates nothing here.
 */
public final class ShadowList {

    /** How many shadows the arrays hold before they first grow. */
    private static final int INITIAL_CAPACITY = 64;

    /** Screen X of the ground contact point of each shadow. */
    private double[] x = new double[INITIAL_CAPACITY];
    /** Screen Y of the ground contact point of each shadow. */
    private double[] y = new double[INITIAL_CAPACITY];
    /** Wanted width of each shadow in art pixels. */
    private double[] width = new double[INITIAL_CAPACITY];
    /** Share of the shadow opacity of each shadow, 0 to 1. */
    private double[] fade = new double[INITIAL_CAPACITY];
    /** The silhouette each shadow is cast from, or null for an ellipse. */
    private Silhouette[] silhouette = new Silhouette[INITIAL_CAPACITY];
    /** The row of the sprite of each shadow that touches the ground, zero for an ellipse. */
    private double[] groundRow = new double[INITIAL_CAPACITY];
    /** Width in art pixels the sprite of each shadow is drawn at, zero for an ellipse. */
    private double[] drawnWidth = new double[INITIAL_CAPACITY];
    /** Height in art pixels the sprite of each shadow is drawn at, zero for an ellipse. */
    private double[] drawnHeight = new double[INITIAL_CAPACITY];
    /** The number of shadows in the list. */
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
        append(centreX, centreY, widthArt, fade, null, 0.0, 0.0, 0.0);
    }

    /**
     * Adds the shadow of a sprite that is drawn at its own size, cast from its silhouette.
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
        addSilhouette(centreX, centreY, silhouette, groundRow, widthFactor, silhouette.width(), groundRow, fade);
    }

    /**
     * Adds the shadow of a sprite that is drawn squashed or stretched, cast from its silhouette. The shadow is anchored
     * on the base of the sprite as it is drawn and takes the height it is drawn at, while the width factor only narrows
     * the shadow. A shadow that is not squashed has the drawn size of its silhouette, as the shorter overload gives.
     *
     * @param centreX         screen X of the ground contact point, under the middle of the sprite
     * @param centreY         screen Y of the ground contact point
     * @param silhouette      the silhouette of the sprite
     * @param groundRow       the row of the sprite that touches the ground, counted from its top
     * @param widthFactor     how wide the shadow is relative to the silhouette, 1 for the whole width
     * @param drawnWidthArt   the width in art pixels the sprite is drawn at
     * @param drawnHeightArt  the height in art pixels the sprite is drawn at
     * @param fade            share of the shadow opacity, 0 to 1
     */
    public void addSilhouette(double centreX, double centreY, Silhouette silhouette, double groundRow,
                              double widthFactor, double drawnWidthArt, double drawnHeightArt, double fade) {
        append(centreX, centreY, silhouette.width() * widthFactor, fade, silhouette, groundRow, drawnWidthArt,
                drawnHeightArt);
    }

    /** Stores one shadow, growing the arrays when they are full. */
    private void append(double centreX, double centreY, double widthArt, double fade, Silhouette sprite,
                        double groundRow, double drawnWidthArt, double drawnHeightArt) {
        if (size == x.length) {
            final int capacity = size * 2;
            x = Arrays.copyOf(x, capacity);
            y = Arrays.copyOf(y, capacity);
            width = Arrays.copyOf(width, capacity);
            this.fade = Arrays.copyOf(this.fade, capacity);
            silhouette = Arrays.copyOf(silhouette, capacity);
            this.groundRow = Arrays.copyOf(this.groundRow, capacity);
            drawnWidth = Arrays.copyOf(drawnWidth, capacity);
            drawnHeight = Arrays.copyOf(drawnHeight, capacity);
        }
        x[size] = centreX;
        y[size] = centreY;
        width[size] = widthArt;
        this.fade[size] = fade;
        silhouette[size] = sprite;
        this.groundRow[size] = groundRow;
        drawnWidth[size] = drawnWidthArt;
        drawnHeight[size] = drawnHeightArt;
        size++;
    }

    /**
     * Copies the shadows of another list into this one, replacing what it held. The arrays only grow, so a copy of a
     * list no larger than before allocates nothing.
     *
     * @param other the list to copy
     */
    public void copyFrom(ShadowList other) {
        if (other.size > x.length) {
            final int capacity = other.size;
            x = new double[capacity];
            y = new double[capacity];
            width = new double[capacity];
            fade = new double[capacity];
            silhouette = new Silhouette[capacity];
            groundRow = new double[capacity];
            drawnWidth = new double[capacity];
            drawnHeight = new double[capacity];
        } else {
            Arrays.fill(silhouette, other.size, Math.max(size, other.size), null);
        }
        System.arraycopy(other.x, 0, x, 0, other.size);
        System.arraycopy(other.y, 0, y, 0, other.size);
        System.arraycopy(other.width, 0, width, 0, other.size);
        System.arraycopy(other.fade, 0, fade, 0, other.size);
        System.arraycopy(other.silhouette, 0, silhouette, 0, other.size);
        System.arraycopy(other.groundRow, 0, groundRow, 0, other.size);
        System.arraycopy(other.drawnWidth, 0, drawnWidth, 0, other.size);
        System.arraycopy(other.drawnHeight, 0, drawnHeight, 0, other.size);
        size = other.size;
    }

    /**
     * Tells whether another list holds exactly the same shadows in the same order, which means casting it gives the
     * same layer.
     *
     * @param other the list to compare with
     * @return true when every shadow matches in position, width, fade, silhouette, ground row and drawn size
     */
    public boolean sameShadowsAs(ShadowList other) {
        if (size != other.size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (x[i] != other.x[i] || y[i] != other.y[i] || width[i] != other.width[i] || fade[i] != other.fade[i]
                    || silhouette[i] != other.silhouette[i] || groundRow[i] != other.groundRow[i]
                    || drawnWidth[i] != other.drawnWidth[i] || drawnHeight[i] != other.drawnHeight[i]) {
                return false;
            }
        }
        return true;
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

    /** @return the width in art pixels the sprite of a shadow is drawn at, zero for an ellipse */
    public double drawnWidth(int index) {
        return drawnWidth[index];
    }

    /** @return the height in art pixels the sprite of a shadow is drawn at, zero for an ellipse */
    public double drawnHeight(int index) {
        return drawnHeight[index];
    }
}
