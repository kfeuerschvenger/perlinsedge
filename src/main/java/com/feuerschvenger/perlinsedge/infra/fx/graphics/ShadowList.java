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

    /**
     * The size in art pixels a sprite is drawn at.
     *
     * @param widthArt  the width in art pixels
     * @param heightArt the height in art pixels
     */
    public record SpriteSize(double widthArt, double heightArt) {}

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
        append(centreX, centreY, widthArt, fade);
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
        final int index = append(centreX, centreY, silhouette.width() * widthFactor, fade);
        setSprite(index, silhouette, groundRow, silhouette.width(), groundRow);
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
     * @param drawnSize       the size in art pixels the sprite is drawn at
     * @param fade            share of the shadow opacity, 0 to 1
     */
    public void addSilhouette(double centreX, double centreY, Silhouette silhouette, double groundRow,
                              double widthFactor, SpriteSize drawnSize, double fade) {
        final int index = append(centreX, centreY, silhouette.width() * widthFactor, fade);
        setSprite(index, silhouette, groundRow, drawnSize.widthArt(), drawnSize.heightArt());
    }

    /** Makes a stored shadow the one of a sprite. */
    private void setSprite(int index, Silhouette sprite, double row, double drawnWidthArt, double drawnHeightArt) {
        silhouette[index] = sprite;
        groundRow[index] = row;
        drawnWidth[index] = drawnWidthArt;
        drawnHeight[index] = drawnHeightArt;
    }

    /**
     * Stores one ellipse shadow, growing the arrays when they are full.
     *
     * @return the index the shadow was stored at
     */
    private int append(double centreX, double centreY, double widthArt, double fade) {
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
        silhouette[size] = null;
        groundRow[size] = 0.0;
        drawnWidth[size] = 0.0;
        drawnHeight[size] = 0.0;
        return size++;
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
        for (var i = 0; i < size; i++) {
            if (!sameShadowAt(other, i)) {
                return false;
            }
        }
        return true;
    }

    /** Tells whether the shadow at an index matches the one at the same index of another list, in every field. */
    private boolean sameShadowAt(ShadowList other, int i) {
        return samePlacement(other, i) && sameShape(other, i) && silhouette[i] == other.silhouette[i];
    }

    /** Tells whether two shadows sit at the same point with the same opacity share. */
    private boolean samePlacement(ShadowList other, int i) {
        return same(x[i], other.x[i]) && same(y[i], other.y[i]) && same(fade[i], other.fade[i]);
    }

    /** Tells whether two shadows have the same width, ground row and drawn size. */
    private boolean sameShape(ShadowList other, int i) {
        return same(width[i], other.width[i]) && same(groundRow[i], other.groundRow[i])
                && same(drawnWidth[i], other.drawnWidth[i]) && same(drawnHeight[i], other.drawnHeight[i]);
    }

    /** Compares two stored values by their exact bits, which is what makes two casts give the same layer. */
    private static boolean same(double a, double b) {
        return Double.compare(a, b) == 0;
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
