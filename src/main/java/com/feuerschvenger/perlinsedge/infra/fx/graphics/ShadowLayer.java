package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;

import java.util.Arrays;

/**
 * The offscreen layer every shadow of a frame is cast into, at the resolution of the art: one cell is one art pixel.
 * Each cell keeps the strongest alpha any shadow gave it, as black, so two shadows that overlap make the same dark
 * cell as one and the layer is laid over the world once, at the shadow opacity of the moment. That is what keeps an
 * overlap from darkening twice.
 * <p>
 * A shadow is the ground projection of an object. The ground is the isometric plane, so a length on it appears half as
 * tall on the screen as it is wide. A ground point is described by two coordinates: how far it lies along the direction
 * of the shadow and how far across it. Along the shadow, one unit moves the point by {@code (cos, sin / 2)} on the
 * screen, and across it by {@code (sin, -cos / 2)}. A sprite is stood up on the ground at its anchor with its height
 * mapped to the along coordinate, scaled by the shadow length factor, and its width to the across coordinate, which flips
 * it onto the ground and shears it toward the sun. An object with no sprite casts an ellipse on the ground, stretched
 * along the shadow. The class holds plain arrays and no framework type, so it is tested without a window. Only the area
 * a frame touched is cleared and handed on, and the arrays are reused, so a steady frame allocates nothing.
 */
public final class ShadowLayer {

    /** How far an ellipse is stretched along the shadow, as a share of its radius for every unit of length factor. */
    public static final double ELLIPSE_REACH = 0.5;

    private int width;
    private int height;
    private int[] pixels = new int[0];

    /** The box of the cells a frame stamped, as left, top, right and bottom with the last two exclusive. */
    private int stampedLeft;
    private int stampedTop;
    private int stampedRight;
    private int stampedBottom;

    /** The box of the cells the previous frame stamped, which were cleared and are not yet shown cleared. */
    private int clearedLeft;
    private int clearedTop;
    private int clearedRight;
    private int clearedBottom;

    private final int[] changed = new int[4];

    private double cosine = 0.0;
    private double sine = 1.0;
    private double length = 1.0;

    /**
     * Gets the layer ready for a frame: sizes it, clears what the previous frame stamped and sets the sun.
     *
     * @param cellsWide number of cells across
     * @param cellsHigh number of cells down
     * @param angle     direction of the shadow on the screen in radians, zero to the right and pi / 2 down
     * @param length    how long a shadow is as a multiple of the height of what casts it
     */
    public void prepare(int cellsWide, int cellsHigh, double angle, double length) {
        if (cellsWide != width || cellsHigh != height) {
            width = cellsWide;
            height = cellsHigh;
            pixels = new int[cellsWide * cellsHigh];
            clearedLeft = 0;
            clearedTop = 0;
            clearedRight = 0;
            clearedBottom = 0;
        } else {
            clearStamped();
            if (hasShadows()) {
                if (clearedRight > clearedLeft && clearedBottom > clearedTop) {
                    clearedLeft = Math.min(clearedLeft, stampedLeft);
                    clearedTop = Math.min(clearedTop, stampedTop);
                    clearedRight = Math.max(clearedRight, stampedRight);
                    clearedBottom = Math.max(clearedBottom, stampedBottom);
                } else {
                    clearedLeft = stampedLeft;
                    clearedTop = stampedTop;
                    clearedRight = stampedRight;
                    clearedBottom = stampedBottom;
                }
            }
        }
        stampedLeft = 0;
        stampedTop = 0;
        stampedRight = 0;
        stampedBottom = 0;
        this.cosine = Math.cos(angle);
        this.sine = Math.sin(angle);
        this.length = length;
    }

    /** Zeroes the cells of the last frame's box. */
    private void clearStamped() {
        for (int y = stampedTop; y < stampedBottom; y++) {
            Arrays.fill(pixels, y * width + stampedLeft, y * width + stampedRight, 0);
        }
    }

    /**
     * Casts the shadow of a sprite.
     *
     * @param sprite      the silhouette of the sprite
     * @param anchorX     column of the point the sprite stands on, in cells
     * @param anchorY     row of the point the sprite stands on, in cells
     * @param groundRow   the row of the sprite that touches the ground, counted from its top, so the rows above it are
     *                    its height
     * @param widthFactor how wide the shadow is relative to the sprite, 1 for the whole width
     * @param alpha       opacity of the shadow cells, 0 to 255
     */
    public void castSprite(Silhouette sprite, double anchorX, double anchorY, double groundRow, double widthFactor,
                           int alpha) {
        if (alpha <= 0 || widthFactor <= 0 || groundRow <= 0) {
            return;
        }
        final double halfWidth = sprite.width() / 2.0 * widthFactor;
        final double reach = length * groundRow;
        // The box of the four corners of the sprite after the projection
        final double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        include(box, 0, -halfWidth);
        include(box, 0, halfWidth);
        include(box, reach, -halfWidth);
        include(box, reach, halfWidth);
        final int left = Math.max(0, (int) Math.floor(anchorX + box[0]));
        final int top = Math.max(0, (int) Math.floor(anchorY + box[1]));
        final int right = Math.min(width, (int) Math.ceil(anchorX + box[2]));
        final int bottom = Math.min(height, (int) Math.ceil(anchorY + box[3]));
        final int spriteWidth = sprite.width();
        final int spriteHeight = sprite.height();
        boolean touched = false;
        for (int y = top; y < bottom; y++) {
            final double dy = y + 0.5 - anchorY;
            for (int x = left; x < right; x++) {
                final double dx = x + 0.5 - anchorX;
                final double along = cosine * dx + 2.0 * sine * dy;
                if (along < 0.0) {
                    continue;
                }
                final int row = (int) Math.ceil(groundRow - along / length) - 1;
                final int column = (int) Math.floor((sine * dx - 2.0 * cosine * dy) / widthFactor + spriteWidth / 2.0);
                if (row < 0 || row >= spriteHeight || column < 0 || column >= spriteWidth
                        || !sprite.isSolid(column, row)) {
                    continue;
                }
                put(y * width + x, alpha);
                touched = true;
            }
        }
        if (touched) {
            extendStamped(left, top, right, bottom);
        }
    }

    /**
     * Casts the shadow of an object with no sprite: an ellipse on the ground under it, stretched along the shadow so it
     * leans toward where the shadow falls and starts at the object's base.
     *
     * @param anchorX column of the point the object stands on, in cells
     * @param anchorY row of the point the object stands on, in cells
     * @param width   width of the ellipse at noon in art pixels
     * @param alpha   opacity of the shadow cells, 0 to 255
     */
    public void castEllipse(double anchorX, double anchorY, double width, int alpha) {
        if (alpha <= 0 || width <= 0) {
            return;
        }
        final double radius = width / 2.0;
        final double alongRadius = radius * (1.0 + ELLIPSE_REACH * length);
        final double alongCentre = radius * ELLIPSE_REACH * length;
        final double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        include(box, alongCentre - alongRadius, -radius);
        include(box, alongCentre - alongRadius, radius);
        include(box, alongCentre + alongRadius, -radius);
        include(box, alongCentre + alongRadius, radius);
        final int left = Math.max(0, (int) Math.floor(anchorX + box[0]));
        final int top = Math.max(0, (int) Math.floor(anchorY + box[1]));
        final int right = Math.min(this.width, (int) Math.ceil(anchorX + box[2]));
        final int bottom = Math.min(height, (int) Math.ceil(anchorY + box[3]));
        boolean touched = false;
        for (int y = top; y < bottom; y++) {
            final double dy = y + 0.5 - anchorY;
            for (int x = left; x < right; x++) {
                final double dx = x + 0.5 - anchorX;
                final double along = (cosine * dx + 2.0 * sine * dy - alongCentre) / alongRadius;
                final double across = (sine * dx - 2.0 * cosine * dy) / radius;
                if (along * along + across * across <= 1.0) {
                    put(y * this.width + x, alpha);
                    touched = true;
                }
            }
        }
        if (touched) {
            extendStamped(left, top, right, bottom);
        }
    }

    /** Widens a box to take in the screen offset of a ground point. */
    private void include(double[] box, double along, double across) {
        final double x = along * cosine + across * sine;
        final double y = (along * sine - across * cosine) / 2.0;
        box[0] = Math.min(box[0], x);
        box[1] = Math.min(box[1], y);
        box[2] = Math.max(box[2], x);
        box[3] = Math.max(box[3], y);
    }

    /** Gives a cell the stronger of its alpha and the new one. */
    private void put(int index, int alpha) {
        if ((pixels[index] >>> 24) < alpha) {
            pixels[index] = alpha << 24;
        }
    }

    /** Grows the box of stamped cells to take in another box. */
    private void extendStamped(int left, int top, int right, int bottom) {
        if (stampedRight <= stampedLeft || stampedBottom <= stampedTop) {
            stampedLeft = left;
            stampedTop = top;
            stampedRight = right;
            stampedBottom = bottom;
            return;
        }
        stampedLeft = Math.min(stampedLeft, left);
        stampedTop = Math.min(stampedTop, top);
        stampedRight = Math.max(stampedRight, right);
        stampedBottom = Math.max(stampedBottom, bottom);
    }

    /** @return the number of cells across */
    public int width() {
        return width;
    }

    /** @return the number of cells down */
    public int height() {
        return height;
    }

    /** @return the cells as ARGB, black with the alpha of the shadow, row by row, which the caller must not change */
    public int[] pixels() {
        return pixels;
    }

    /**
     * Returns the alpha of a cell.
     *
     * @param x column
     * @param y row
     * @return 0 to 255
     */
    public int alphaAt(int x, int y) {
        return pixels[y * width + x] >>> 24;
    }

    /** @return true when this frame has stamped at least one cell, so there is something to lay over the world */
    public boolean hasShadows() {
        return stampedRight > stampedLeft && stampedBottom > stampedTop;
    }

    /** @return the left column of the box of the stamped cells */
    public int stampedLeft() {
        return stampedLeft;
    }

    /** @return the top row of the box of the stamped cells */
    public int stampedTop() {
        return stampedTop;
    }

    /** @return the column after the last of the box of the stamped cells */
    public int stampedRight() {
        return stampedRight;
    }

    /** @return the row after the last of the box of the stamped cells */
    public int stampedBottom() {
        return stampedBottom;
    }

    /** Tells the layer that the changed area has been handed on, so the cleared cells need not be reported again. */
    public void handedOn() {
        clearedLeft = 0;
        clearedTop = 0;
        clearedRight = 0;
        clearedBottom = 0;
    }

    /**
     * Returns the area whose cells differ from what was last handed on: the cells this frame stamped together with the
     * cells the previous frame stamped and this frame cleared. A caller that mirrors the layer into an image uploads
     * exactly this area.
     *
     * @return left, top, right and bottom with the last two exclusive, an empty area when right is not past left; the
     *         array is reused by the next call
     */
    public int[] changedBox() {
        final boolean cleared = clearedRight > clearedLeft && clearedBottom > clearedTop;
        final boolean stamped = hasShadows();
        if (cleared && stamped) {
            changed[0] = Math.min(clearedLeft, stampedLeft);
            changed[1] = Math.min(clearedTop, stampedTop);
            changed[2] = Math.max(clearedRight, stampedRight);
            changed[3] = Math.max(clearedBottom, stampedBottom);
        } else if (cleared) {
            changed[0] = clearedLeft;
            changed[1] = clearedTop;
            changed[2] = clearedRight;
            changed[3] = clearedBottom;
        } else {
            changed[0] = stampedLeft;
            changed[1] = stampedTop;
            changed[2] = stamped ? stampedRight : stampedLeft;
            changed[3] = stamped ? stampedBottom : stampedTop;
        }
        return changed;
    }
}
