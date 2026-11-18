package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.infra.fx.utils.Silhouette;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * mapped to the along coordinate, scaled by the shadow length factor, and its width to the across coordinate, which
 * flips it onto the ground and shears it toward the sun. An object with no sprite casts an ellipse on the ground,
 * stretched along the shadow. The class holds plain arrays and no framework type, so it is tested without a window.
 * <p>
 * The cells a shadow covers depend only on the shape, the sun and where the anchor falls inside its cell, never on
 * which cell that is. An anchor is snapped to a sub-cell grid of half a screen pixel, since the art is drawn in whole
 * screen pixels and the middle of a sprite with an odd drawn width falls on the middle of a pixel, so the fraction is a
 * small whole number of sub-cells and equal positions always share one mask. The covered cells are worked out once per
 * shape, sun and sub-cell of the anchor, kept as a mask and stamped at the anchor of every later cast, so an object
 * that stands still is not rasterised again until the sun moves a step. The masks are kept in least recently used
 * order, and the one unused for the longest is dropped when the cache is full. Only the cells a mask covers are
 * written, and the area a frame touched is exactly their box, which is what is cleared for the next frame and handed
 * on for upload. The arrays are reused, so a frame whose shapes are all known allocates nothing.
 */
public final class ShadowLayer {

    /** How far an ellipse is stretched along the shadow, as a share of its radius for every unit of length factor. */
    public static final double ELLIPSE_REACH = 0.5;

    /** The most masks kept at once by default, after which the least recently used one is dropped. */
    private static final int MAX_MASKS = 2048;

    /** The number of cells across. */
    private int width;
    /** The number of cells down. */
    private int height;
    /** The cells as ARGB, row by row. */
    private int[] pixels = new int[0];

    /** Left column of the box of the cells a frame stamped. */
    private int stampedLeft;
    /** Top row of the box of the cells a frame stamped. */
    private int stampedTop;
    /** Column after the last of the box of the cells a frame stamped. */
    private int stampedRight;
    /** Row after the last of the box of the cells a frame stamped. */
    private int stampedBottom;

    /** Left column of the box of the cells the previous frame stamped, cleared but not yet shown cleared. */
    private int clearedLeft;
    /** Top row of the box of the cells the previous frame stamped, which were cleared and are not yet shown cleared. */
    private int clearedTop;
    /** Column after the last of the box of the cells the previous frame stamped, see {@link #clearedLeft}. */
    private int clearedRight;
    /** Row after the last of the box of the cells the previous frame stamped, see {@link #clearedLeft}. */
    private int clearedBottom;

    /** The area handed out by {@link #changedBox()}, reused by every call. */
    private final int[] changed = new int[4];

    /** Scratch for the box of a projected shape: left, top, right and bottom as screen offsets, reused each build. */
    private final double[] box = new double[4];

    /** Horizontal part of the direction of the shadow. */
    private double cosine = 0.0;
    /** Vertical part of the direction of the shadow. */
    private double sine = 1.0;
    /** How long a shadow is as a multiple of the height of what casts it. */
    private double length = 1.0;

    /**
     * The sub-cells per cell on each axis: two for every screen pixel, so an anchor can lie on the middle of a screen
     * pixel, which is where the middle of a sprite drawn an odd number of screen pixels wide falls.
     */
    private int subSteps = 2;

    /** The masks of the sun of the moment, least recently used first, which are dropped when the sun moves. */
    private final Map<MaskKey, Mask> masks;
    /** The key every lookup is made with, so a lookup that hits allocates nothing. */
    private final MaskKey probe = new MaskKey();
    /** How many masks were rasterised so far. */
    private int rasterised;

    /** Creates a layer that keeps up to the default number of masks. */
    public ShadowLayer() {
        this(MAX_MASKS);
    }

    /**
     * Creates a layer with a chosen mask capacity.
     *
     * @param maxMasks the most masks kept at once, above zero, past which the least recently used one is dropped
     */
    ShadowLayer(int maxMasks) {
        masks = new LinkedHashMap<>(64, 0.75f, true) {
            /**
             * Tells the map to drop its least recently used mask once it holds more than the capacity.
             *
             * @param eldest the least recently used entry
             * @return true when the entry is to be removed
             */
            @Override
            protected boolean removeEldestEntry(Map.Entry<MaskKey, Mask> eldest) {
                return size() > maxMasks;
            }
        };
    }

    /**
     * Gets the layer ready for a frame: sizes it, clears what the previous frame stamped and sets the sun.
     *
     * @param cellsWide number of cells across
     * @param cellsHigh number of cells down
     * @param angle     direction of the shadow on the screen in radians, zero to the right and pi / 2 down
     * @param length    how long a shadow is as a multiple of the height of what casts it
     * @param artScale  screen pixels per cell, above zero, which sets the half pixel grid an anchor is snapped to
     */
    public void prepare(int cellsWide, int cellsHigh, double angle, double length, int artScale) {
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
        final double newCosine = Math.cos(angle);
        final double newSine = Math.sin(angle);
        if (newCosine != cosine || newSine != sine || length != this.length) {
            masks.clear();
        }
        this.cosine = newCosine;
        this.sine = newSine;
        this.length = length;
        this.subSteps = Math.max(1, artScale) * 2;
    }

    /** Zeroes the cells of the last frame's box. */
    private void clearStamped() {
        for (int y = stampedTop; y < stampedBottom; y++) {
            Arrays.fill(pixels, y * width + stampedLeft, y * width + stampedRight, 0);
        }
    }

    /**
     * Casts the shadow of a sprite. The anchor is snapped to the sub-cell grid, and a shadow with a NaN or infinite
     * anchor is skipped.
     *
     * @param sprite      the silhouette of the sprite
     * @param anchorX     column of the point the sprite stands on, in cells, which may fall inside a cell
     * @param anchorY     row of the point the sprite stands on, in cells, which may fall inside a cell
     * @param groundRow   the row of the sprite that touches the ground, counted from its top, so the rows above it are
     *                    its height
     * @param widthFactor how wide the shadow is relative to the sprite, 1 for the whole width
     * @param alpha       opacity of the shadow cells, 0 to 255
     */
    public void castSprite(Silhouette sprite, double anchorX, double anchorY, double groundRow, double widthFactor,
                           int alpha) {
        castSprite(sprite, anchorX, anchorY, groundRow, widthFactor, 1.0, alpha);
    }

    /**
     * Casts the shadow of a sprite that is drawn squashed or stretched: the shadow is as tall as the sprite is drawn,
     * so a squashed sprite casts a shorter shadow of the same form. The anchor is snapped to the sub-cell grid, and a
     * shadow with a NaN or infinite anchor is skipped.
     *
     * @param sprite       the silhouette of the sprite
     * @param anchorX      column of the point the sprite stands on, in cells, which may fall inside a cell
     * @param anchorY      row of the point the sprite stands on, in cells, which may fall inside a cell
     * @param groundRow    the row of the sprite that touches the ground, counted from its top, so the rows above it are
     *                     its height
     * @param widthFactor  how wide the shadow is relative to the sprite, 1 for the whole width
     * @param heightFactor how tall the sprite is drawn relative to its silhouette, 1 for the whole height
     * @param alpha        opacity of the shadow cells, 0 to 255
     */
    public void castSprite(Silhouette sprite, double anchorX, double anchorY, double groundRow, double widthFactor,
                           double heightFactor, int alpha) {
        if (alpha <= 0 || widthFactor <= 0 || heightFactor <= 0 || groundRow <= 0 || !isFinite(anchorX, anchorY)) {
            return;
        }
        final long subX = Math.round(anchorX * subSteps);
        final long subY = Math.round(anchorY * subSteps);
        probe.set(sprite, widthFactor, heightFactor, groundRow, subSteps, (int) Math.floorMod(subX, (long) subSteps),
                (int) Math.floorMod(subY, (long) subSteps));
        stamp(maskFor(probe), (int) Math.floorDiv(subX, (long) subSteps), (int) Math.floorDiv(subY, (long) subSteps),
                alpha);
    }

    /**
     * Casts the shadow of an object with no sprite: an ellipse on the ground under it, stretched along the shadow so it
     * leans toward where the shadow falls and starts at the object's base. The anchor is snapped to the sub-cell grid,
     * and a shadow with a NaN or infinite anchor is skipped.
     *
     * @param anchorX column of the point the object stands on, in cells, which may fall inside a cell
     * @param anchorY row of the point the object stands on, in cells, which may fall inside a cell
     * @param width   width of the ellipse at noon in art pixels
     * @param alpha   opacity of the shadow cells, 0 to 255
     */
    public void castEllipse(double anchorX, double anchorY, double width, int alpha) {
        if (alpha <= 0 || width <= 0 || !isFinite(anchorX, anchorY)) {
            return;
        }
        final long subX = Math.round(anchorX * subSteps);
        final long subY = Math.round(anchorY * subSteps);
        probe.set(null, width, 1.0, 0.0, subSteps, (int) Math.floorMod(subX, (long) subSteps),
                (int) Math.floorMod(subY, (long) subSteps));
        stamp(maskFor(probe), (int) Math.floorDiv(subX, (long) subSteps), (int) Math.floorDiv(subY, (long) subSteps),
                alpha);
    }

    /** Tells whether both coordinates of an anchor are finite numbers, which rules out NaN and the infinities. */
    private static boolean isFinite(double anchorX, double anchorY) {
        return Double.isFinite(anchorX) && Double.isFinite(anchorY);
    }

    /**
     * Returns the mask of a shape, rasterising it when this sun has not seen it yet. A lookup marks the mask as the
     * most recently used, and storing a new one drops the least recently used when the cache is full.
     */
    private Mask maskFor(MaskKey key) {
        Mask mask = masks.get(key);
        if (mask == null) {
            final double fractionX = (double) key.subX / key.subSteps;
            final double fractionY = (double) key.subY / key.subSteps;
            mask = key.sprite != null
                    ? rasteriseSprite(key.sprite, key.size, key.heightFactor, key.groundRow, fractionX, fractionY)
                    : rasteriseEllipse(key.size, fractionX, fractionY);
            masks.put(key.copy(), mask);
            rasterised++;
        }
        return mask;
    }

    /**
     * Works out the cells a sprite covers around an anchor that lies a fraction into its cell. Every cell is taken by
     * its centre, so the cells keep the crisp art pixel grid.
     */
    private Mask rasteriseSprite(Silhouette sprite, double widthFactor, double heightFactor, double groundRow,
                                 double fractionX, double fractionY) {
        final double halfWidth = sprite.width() / 2.0 * widthFactor;
        // A sprite drawn squashed is as tall as it is drawn, so its silhouette rows are spread over a shorter reach
        final double drawnLength = length * heightFactor;
        final double reach = drawnLength * groundRow;
        // The box of the four corners of the sprite after the projection
        resetBox();
        include(0, -halfWidth);
        include(0, halfWidth);
        include(reach, -halfWidth);
        include(reach, halfWidth);
        final int left = (int) Math.floor(fractionX + box[0]);
        final int top = (int) Math.floor(fractionY + box[1]);
        final int right = (int) Math.ceil(fractionX + box[2]);
        final int bottom = (int) Math.ceil(fractionY + box[3]);
        final boolean[] covered = new boolean[Math.max(0, right - left) * Math.max(0, bottom - top)];
        for (int y = top; y < bottom; y++) {
            final double dy = y + 0.5 - fractionY;
            for (int x = left; x < right; x++) {
                final double dx = x + 0.5 - fractionX;
                covered[(y - top) * (right - left) + (x - left)] = isShaded(sprite, widthFactor, drawnLength,
                        groundRow, dx, dy);
            }
        }
        return Mask.of(covered, left, top, right - left, bottom - top);
    }

    /**
     * Tells whether the centre of a cell, given by its offset from the anchor, lies in the shadow of a solid pixel of
     * the sprite.
     */
    private boolean isShaded(Silhouette sprite, double widthFactor, double drawnLength, double groundRow, double dx,
                             double dy) {
        final double along = cosine * dx + 2.0 * sine * dy;
        if (along < 0.0) {
            return false;
        }
        final int row = (int) Math.ceil(groundRow - along / drawnLength) - 1;
        final int column = (int) Math.floor((sine * dx - 2.0 * cosine * dy) / widthFactor + sprite.width() / 2.0);
        return row >= 0 && row < sprite.height() && column >= 0 && column < sprite.width()
                && sprite.isSolid(column, row);
    }

    /** Works out the cells an ellipse covers around an anchor that lies a fraction into its cell. */
    private Mask rasteriseEllipse(double ellipseWidth, double fractionX, double fractionY) {
        final double radius = ellipseWidth / 2.0;
        final double alongRadius = radius * (1.0 + ELLIPSE_REACH * length);
        final double alongCentre = radius * ELLIPSE_REACH * length;
        resetBox();
        include(alongCentre - alongRadius, -radius);
        include(alongCentre - alongRadius, radius);
        include(alongCentre + alongRadius, -radius);
        include(alongCentre + alongRadius, radius);
        final int left = (int) Math.floor(fractionX + box[0]);
        final int top = (int) Math.floor(fractionY + box[1]);
        final int right = (int) Math.ceil(fractionX + box[2]);
        final int bottom = (int) Math.ceil(fractionY + box[3]);
        final boolean[] covered = new boolean[Math.max(0, right - left) * Math.max(0, bottom - top)];
        for (int y = top; y < bottom; y++) {
            final double dy = y + 0.5 - fractionY;
            for (int x = left; x < right; x++) {
                final double dx = x + 0.5 - fractionX;
                final double along = (cosine * dx + 2.0 * sine * dy - alongCentre) / alongRadius;
                final double across = (sine * dx - 2.0 * cosine * dy) / radius;
                if (along * along + across * across <= 1.0) {
                    covered[(y - top) * (right - left) + (x - left)] = true;
                }
            }
        }
        return Mask.of(covered, left, top, right - left, bottom - top);
    }

    /**
     * Stamps a mask with its anchor on a cell: every covered cell takes the stronger of its alpha and the new one. The
     * stamped box grows by the covered cells that fall inside the layer, and by nothing else.
     */
    private void stamp(Mask mask, int cellX, int cellY, int alpha) {
        if (mask.width == 0) {
            return;
        }
        final int originX = cellX + mask.left;
        final int originY = cellY + mask.top;
        final int fromX = Math.max(0, originX);
        final int toX = Math.min(width, originX + mask.width);
        final int fromY = Math.max(0, originY);
        final int toY = Math.min(height, originY + mask.height);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int y = fromY; y < toY; y++) {
            final int maskRow = (y - originY) * mask.width - originX;
            final int layerRow = y * width;
            for (int x = fromX; x < toX; x++) {
                if (mask.covered[maskRow + x]) {
                    put(layerRow + x, alpha);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX >= minX) {
            extendStamped(minX, minY, maxX + 1, maxY + 1);
        }
    }

    /** Empties the scratch box, ready to take in points. */
    private void resetBox() {
        box[0] = Double.MAX_VALUE;
        box[1] = Double.MAX_VALUE;
        box[2] = -Double.MAX_VALUE;
        box[3] = -Double.MAX_VALUE;
    }

    /** Widens the scratch box to take in the screen offset of a ground point. */
    private void include(double along, double across) {
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

    /** @return how many masks are kept for the sun of the moment */
    int maskCount() {
        return masks.size();
    }

    /** @return how many masks were rasterised so far, which a cast that found its mask in the cache does not count */
    int rasterised() {
        return rasterised;
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

    /**
     * The cells a shape covers around an anchor, cropped to the covered cells. The box is relative to the cell the
     * anchor lies in, and a mask with no covered cell has no width.
     */
    private static final class Mask {
        /** First column of the box, relative to the cell of the anchor. */
        private final int left;
        /** First row of the box, relative to the cell of the anchor. */
        private final int top;
        /** Number of columns of the box, zero when nothing is covered. */
        private final int width;
        /** Number of rows of the box. */
        private final int height;
        /** Whether each cell of the box is covered, row by row. */
        private final boolean[] covered;

        /** Stores the cropped cells of a box. */
        private Mask(int left, int top, int width, int height, boolean[] covered) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.covered = covered;
        }

        /**
         * Crops a box of cells to the smallest box that holds every covered cell.
         *
         * @param cells  the covered cells of the box, row by row
         * @param left   first column of the box
         * @param top    first row of the box
         * @param wide   number of columns
         * @param high   number of rows
         * @return the cropped mask
         */
        private static Mask of(boolean[] cells, int left, int top, int wide, int high) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (int y = 0; y < high; y++) {
                for (int x = 0; x < wide; x++) {
                    if (cells[y * wide + x]) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < minX) {
                return new Mask(left, top, 0, 0, new boolean[0]);
            }
            final int croppedWide = maxX - minX + 1;
            final int croppedHigh = maxY - minY + 1;
            final boolean[] cropped = new boolean[croppedWide * croppedHigh];
            for (int y = 0; y < croppedHigh; y++) {
                System.arraycopy(cells, (y + minY) * wide + minX, cropped, y * croppedWide, croppedWide);
            }
            return new Mask(left + minX, top + minY, croppedWide, croppedHigh, cropped);
        }
    }

    /**
     * What makes two shapes cover the same cells for one sun: the silhouette or none for an ellipse, the width factor
     * or the ellipse width, the ground row, and which sub-cell of its cell the anchor lies in on each axis, counted in
     * whole half screen pixels. One instance is reused for every lookup and a copy is made when a mask is stored.
     */
    private static final class MaskKey {
        /** The silhouette of a sprite, compared by identity, or null for an ellipse. */
        private Silhouette sprite;
        /** The width factor of a sprite shadow, or the width in art pixels of an ellipse. */
        private double size;
        /** How tall a sprite is drawn relative to its silhouette, 1 for an ellipse. */
        private double heightFactor;
        /** The ground row of a sprite shadow, zero for an ellipse. */
        private double groundRow;
        /** The number of sub-cells per cell, which gives the sub-cell indices their meaning. */
        private int subSteps;
        /** The sub-cell the anchor lies in across, 0 up to but excluding the number of sub-cells. */
        private int subX;
        /** The sub-cell the anchor lies in down, 0 up to but excluding the number of sub-cells. */
        private int subY;

        /** Sets every part of the key. */
        private void set(Silhouette sprite, double size, double heightFactor, double groundRow, int subSteps, int subX,
                          int subY) {
            this.sprite = sprite;
            this.size = size;
            this.heightFactor = heightFactor;
            this.groundRow = groundRow;
            this.subSteps = subSteps;
            this.subX = subX;
            this.subY = subY;
        }

        /** Returns an independent key with the same parts, for storing. */
        private MaskKey copy() {
            final MaskKey key = new MaskKey();
            key.set(sprite, size, heightFactor, groundRow, subSteps, subX, subY);
            return key;
        }

        /** Tells whether another key stands for the same shape: same silhouette by identity and equal numbers. */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof MaskKey key)) {
                return false;
            }
            return sprite == key.sprite && size == key.size && heightFactor == key.heightFactor
                    && groundRow == key.groundRow && subSteps == key.subSteps && subX == key.subX && subY == key.subY;
        }

        /** Hashes every part the equality looks at, the silhouette by identity. */
        @Override
        public int hashCode() {
            int hash = System.identityHashCode(sprite);
            hash = 31 * hash + Double.hashCode(size);
            hash = 31 * hash + Double.hashCode(heightFactor);
            hash = 31 * hash + Double.hashCode(groundRow);
            hash = 31 * hash + subSteps;
            hash = 31 * hash + subX;
            hash = 31 * hash + subY;
            return hash;
        }
    }
}
