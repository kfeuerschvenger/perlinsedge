package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.Flicker;
import com.feuerschvenger.perlinsedge.domain.world.light.LightFalloff;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.LightSource;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.PixelSnap;

import java.util.Arrays;

/**
 * The overlay of the sky with the lights worked into it, at the resolution of the art: one cell is one art pixel and the
 * grid is aligned to the camera like the one of the shadows, so the gradient of a light is made of crisp blocks of whole
 * art pixels at every zoom.
 * <p>
 * Every cell follows one rule. A cell has an amount of light, from 0 to 1, and its overlay is the sky tint turned toward
 * the colour of the light by that amount: the colour moves from the tint to the colour of the light, and the opacity
 * moves from the strength of the sky to a modest warm strength. A lit patch is therefore a warm wash and never a hole in
 * the tint that shows the plain ground. The warm strength is scaled by how dark it is, so it is full in the deepest night,
 * a faint wash at dusk and nothing at midday, when the tint has no strength and there is nothing to turn.
 * <p>
 * A light covers the cells of the ground around it, and the ground is the isometric plane, so a circle of tiles around a
 * light is an ellipse twice as wide as it is tall on the screen. The distance of a cell from a light is worked out on the
 * ground, in tiles, and its square as a share of the radius gives the light of the cell through
 * {@link LightFalloff}, a continuous gradient that is exactly zero at the radius. The radius is not the same in every
 * direction: it varies with the angle around the light by the shape of its {@link Flicker}, read from a small table that
 * is worked out for each shaped light once per call and interpolated, indexed by a pseudo angle that needs no
 * trigonometry. Several lights combine their light as {@code 1 - (1 - a)(1 - b)} and their colours weighted by how much
 * each lights the cell.
 * <p>
 * Only the boxes around the lights are worked out and kept, and the world outside them is laid with the plain tint by the
 * caller. The boxes of lights that overlap are merged into one region, so a region is the smallest box that holds the
 * lights that can touch each other, and the regions never overlap: lights far apart cost the cells around each of them
 * and not the stretch of the world between them. The cells are worked out again only when something they depend on
 * changed since the last call, and no array is allocated while the canvas keeps its size and the lights and the regions
 * do not outgrow the arrays. The class holds plain arrays and no framework type, so it is tested without a window.
 */
public final class LightLayer {

    /**
     * Art pixels from the centre of a tile to its right vertex along the width, which is twice the art pixels along the
     * height. It follows from the tile geometry of the configuration and the art scale at unit zoom, so the grid of the
     * lights is the grid of the tiles at every zoom.
     */
    static final double TILE_ART_HALF_WIDTH = AppConfig.getInstance().graphics().getTileHalfWidth()
            / ZoomSteps.ART_SCALE_AT_UNIT_ZOOM;
    /** Art pixels from the top vertex of a tile to its centre along the height, where an entity stands. */
    static final double TILE_ART_HALF_HEIGHT = AppConfig.getInstance().graphics().getTileHalfHeight()
            / ZoomSteps.ART_SCALE_AT_UNIT_ZOOM;
    /** The numbers kept for each light: its centre cell, radius, intensity, colour, shape time and box. */
    private static final int STRIDE = 12;
    /** The part of the stride that holds the box as left, top, right and bottom. */
    private static final int BOX_AT = 8;
    /** The part of the stride that holds the time the shape was read at, 0 for a round light. */
    private static final int SHAPE_TIME_AT = 7;
    /** The numbers of the signature that are not about a light: the tint, the scale, the phase and the style. */
    private static final int SIGNATURE_EXTRA = 9;
    /** The number of sectors of the table of the radius by angle, each interpolated across. */
    private static final int SECTORS = 64;
    /** The lights, and so the regions, the arrays are sized for before they have to grow. */
    private static final int INITIAL_LIGHTS = 4;

    /** The number of cells across the grid. */
    private int cellsWide;
    /** The number of cells down the grid. */
    private int cellsHigh;
    /** How much of each cell is lit by all the lights, 0 to 255, for the box of a region of several lights. */
    private int[] levels = new int[0];
    /** The sum of the light of each light in each cell, 0 up, for the box of a region of several lights. */
    private int[] weights = new int[0];
    /** The sum of the light times the red of each light in each cell, for the box of a region of several lights. */
    private int[] reds = new int[0];
    /** The sum of the light times the green of each light in each cell, for the box of a region of several lights. */
    private int[] greens = new int[0];
    /** The sum of the light times the blue of each light in each cell, for the box of a region of several lights. */
    private int[] blues = new int[0];
    /** The left column of the region the sums are kept for, which the sums are indexed from. */
    private int sumLeft;
    /** The top row of the region the sums are kept for. */
    private int sumTop;
    /** The number of columns of the region the sums are kept for, which is the row length of the sums. */
    private int sumWide;
    /** The finished cells as premultiplied ARGB: the sky tint turned toward the light. */
    private int[] pixels = new int[0];
    /** The cell of the plain sky tint, which every cell no light reaches has. */
    private int plain;

    /** The numbers of the lights in view, {@link #STRIDE} for each. */
    private double[] lights = new double[STRIDE * INITIAL_LIGHTS];
    /** The flicker of each light in view, which gives its shape. */
    private Flicker[] flickers = new Flicker[INITIAL_LIGHTS];
    /** The region each light in view belongs to. */
    private int[] lightRegion = new int[INITIAL_LIGHTS];
    /** The box of each region as left, top, right and bottom, four numbers for each. */
    private int[] regionBoxes = new int[INITIAL_LIGHTS * 4];
    /** How many lights are in view. */
    private int count;
    /** How many regions the lights in view are merged into. */
    private int regionCount;

    /** The radius of the light being cast by angle: one over the square of the factor, with a closing entry. */
    private final double[] sectorInverse = new double[SECTORS + 1];
    /** The finished cell for each of the 256 steps of light, for the single light being cast. */
    private final int[] litCells = new int[256];
    /** The part of the squared distance that depends on the column, for the light being cast. */
    private double[] columnPart = new double[0];
    /** The ground distance along the width of each column from the light being cast, for its angle. */
    private double[] columnAcross = new double[0];

    /** What the cells of the last call were worked out from. */
    private double[] signature = new double[STRIDE * INITIAL_LIGHTS + SIGNATURE_EXTRA];
    /** The flicker of each light the cells of the last call were worked out from, which gives their shape. */
    private Flicker[] signatureFlickers = new Flicker[INITIAL_LIGHTS];
    /** What the cells of this call depend on, built here and then compared with the last. */
    private double[] scratch = new double[STRIDE * INITIAL_LIGHTS + SIGNATURE_EXTRA];
    /** How many numbers of {@link #signature} are valid. */
    private int signatureLength;

    /** The horizontal phase of the camera grid the cells were worked out at. */
    private double phaseX;
    /** The vertical phase of the camera grid the cells were worked out at. */
    private double phaseY;
    /** Whether the last call worked the cells out again, which the caller uploads. */
    private boolean changed;
    /** How many times the cells were worked out so far. */
    private int composes;

    /**
     * Works out the cells for the lights in view, unless nothing they depend on changed since the last call.
     *
     * @param registry     the lights of the world
     * @param style        how the lights are drawn
     * @param gameTime     the game time in seconds, which the flicker of the lights runs on
     * @param tint         the colour of the sky tint
     * @param tintStrength how strongly the sky tint is laid over the world, 0 to 1
     * @param artScale     the integer number of screen pixels per art pixel
     * @param originX      screen X of the camera origin, which the art grid is aligned to
     * @param originY      screen Y of the camera origin
     * @param wide         the number of cells across the canvas grid, with its border cells
     * @param high         the number of cells down the canvas grid, with its border cells
     * @return true when at least one light reaches into the canvas, false when the plain tint is all there is to draw
     */
    public boolean compose(LightRegistry registry, LightStyle style, double gameTime, Rgba tint, double tintStrength,
                           double artScale, double originX, double originY, int wide, int high) {
        changed = false;
        if (wide != cellsWide || high != cellsHigh) {
            cellsWide = wide;
            cellsHigh = high;
            pixels = new int[wide * high];
            signatureLength = 0;
        }
        final double snappedX = PixelSnap.round(originX);
        final double snappedY = PixelSnap.round(originY);
        phaseX = snappedX - Math.floor(snappedX / artScale) * artScale;
        phaseY = snappedY - Math.floor(snappedY / artScale) * artScale;
        plain = plainCellFor(tint, tintStrength);
        collect(registry, gameTime, artScale, snappedX, snappedY);
        if (count == 0) {
            return false;
        }
        group();
        if (!differsFromLast(style, tint, tintStrength, artScale)) {
            return true;
        }
        work(style, tint, tintStrength, gameTime);
        changed = true;
        composes++;
        return true;
    }

    /**
     * Works out the centre cell, the effective radius, intensity and colour and the box of every light that reaches the
     * canvas.
     */
    private void collect(LightRegistry registry, double gameTime, double artScale, double snappedX, double snappedY) {
        count = 0;
        for (int i = 0, total = registry.size(); i < total; i++) {
            final LightSource source = registry.get(i);
            final Flicker flicker = source.flicker();
            // The light shines from where an entity at its position would stand, and is placed on a whole screen pixel
            final double screenX = PixelSnap.round((source.x() - source.y()) * TILE_ART_HALF_WIDTH * artScale + snappedX);
            final double screenY = PixelSnap.round(((source.x() + source.y()) * TILE_ART_HALF_HEIGHT
                    + TILE_ART_HALF_HEIGHT) * artScale + snappedY);
            final double centreX = (screenX - phaseX) / artScale + 1.0;
            final double centreY = (screenY - phaseY) / artScale + 1.0;
            // A light whose widest possible reach stays off the canvas is dropped before any flicker is evaluated
            if (!reachesCanvas(centreX, centreY, source.radius() * (1.0 + flicker.radiusAmplitude()), flicker)) {
                continue;
            }
            final double radius = source.radius() * flicker.radiusFactor(gameTime);
            final double intensity = Math.max(0.0, Math.min(1.0,
                    source.intensity() * flicker.intensityFactor(gameTime)));
            if (!(radius > 0.0) || intensity <= 0.0) {
                continue;
            }
            // A circle of the ground is an ellipse of the screen: sqrt(2) tiles along each diagonal of a tile, and the
            // shape of the light can reach out by its amplitude
            final double reach = radius * Math.sqrt(2.0) * (1.0 + flicker.shapeAmplitude());
            final double reachX = reach * TILE_ART_HALF_WIDTH;
            final double reachY = reach * TILE_ART_HALF_HEIGHT;
            final int boxLeft = Math.max(0, (int) Math.floor(centreX - reachX));
            final int boxTop = Math.max(0, (int) Math.floor(centreY - reachY));
            final int boxRight = Math.min(cellsWide, (int) Math.ceil(centreX + reachX));
            final int boxBottom = Math.min(cellsHigh, (int) Math.ceil(centreY + reachY));
            if (boxRight <= boxLeft || boxBottom <= boxTop) {
                continue;
            }
            if ((count + 1) * STRIDE > lights.length) {
                lights = Arrays.copyOf(lights, lights.length * 2);
                flickers = Arrays.copyOf(flickers, flickers.length * 2);
            }
            final int at = count * STRIDE;
            final Rgba colour = source.colour();
            double red = colour.r();
            double green = colour.g();
            double blue = colour.b();
            if (flicker.hasColourDrift()) {
                final double share = flicker.colourShare(gameTime);
                red = mix(flicker.colourLow().r(), flicker.colourHigh().r(), share);
                green = mix(flicker.colourLow().g(), flicker.colourHigh().g(), share);
                blue = mix(flicker.colourLow().b(), flicker.colourHigh().b(), share);
            }
            lights[at] = centreX;
            lights[at + 1] = centreY;
            lights[at + 2] = radius;
            lights[at + 3] = intensity;
            lights[at + 4] = red;
            lights[at + 5] = green;
            lights[at + 6] = blue;
            lights[at + SHAPE_TIME_AT] = flicker.shapeAmplitude() > 0.0 ? gameTime : 0.0;
            lights[at + BOX_AT] = boxLeft;
            lights[at + BOX_AT + 1] = boxTop;
            lights[at + BOX_AT + 2] = boxRight;
            lights[at + BOX_AT + 3] = boxBottom;
            flickers[count] = flicker;
            count++;
        }
    }

    /**
     * Tells whether a light can reach the canvas at all, from its position and the largest radius its flicker can give
     * it, so a light far outside costs a few multiplications and no flicker is evaluated for it.
     *
     * @param centreX   the column of the light on the grid
     * @param centreY   the row of the light on the grid
     * @param maxRadius the largest radius the flicker can give the light, in tiles
     * @param flicker   the flicker of the light, whose shape can stretch the radius
     * @return false when the box of the light is empty whatever the moment
     */
    private boolean reachesCanvas(double centreX, double centreY, double maxRadius, Flicker flicker) {
        final double reach = maxRadius * Math.sqrt(2.0) * (1.0 + flicker.shapeAmplitude());
        return centreX + reach * TILE_ART_HALF_WIDTH > 0.0 && centreX - reach * TILE_ART_HALF_WIDTH < cellsWide
                && centreY + reach * TILE_ART_HALF_HEIGHT > 0.0 && centreY - reach * TILE_ART_HALF_HEIGHT < cellsHigh;
    }

    /**
     * Merges the boxes of the lights that overlap into regions, until no two regions overlap. A light only changes the
     * cells of its own box, so the cells of a region depend on the lights of that region alone, and the regions can be
     * worked out and uploaded one by one.
     */
    private void group() {
        if (lightRegion.length < flickers.length) {
            lightRegion = new int[flickers.length];
            regionBoxes = new int[flickers.length * 4];
        }
        for (int i = 0; i < count; i++) {
            lightRegion[i] = i;
            for (int side = 0; side < 4; side++) {
                regionBoxes[i * 4 + side] = (int) lights[i * STRIDE + BOX_AT + side];
            }
        }
        boolean merged = count > 1;
        while (merged) {
            merged = false;
            for (int a = 0; a < count; a++) {
                if (isEmptyRegion(a)) {
                    continue;
                }
                for (int b = a + 1; b < count; b++) {
                    if (!isEmptyRegion(b) && regionsOverlap(a, b)) {
                        mergeRegion(a, b);
                        merged = true;
                    }
                }
            }
        }
        // The regions that are left take the first numbers
        int next = 0;
        for (int a = 0; a < count; a++) {
            if (isEmptyRegion(a)) {
                continue;
            }
            if (next != a) {
                System.arraycopy(regionBoxes, a * 4, regionBoxes, next * 4, 4);
                for (int i = 0; i < count; i++) {
                    if (lightRegion[i] == a) {
                        lightRegion[i] = next;
                    }
                }
            }
            next++;
        }
        regionCount = next;
    }

    /** Tells whether a region was merged into another, which leaves it with no box. */
    private boolean isEmptyRegion(int region) {
        return regionBoxes[region * 4 + 2] <= regionBoxes[region * 4];
    }

    /** Tells whether the boxes of two regions share at least one cell. */
    private boolean regionsOverlap(int a, int b) {
        final int[] boxes = regionBoxes;
        return boxes[a * 4] < boxes[b * 4 + 2] && boxes[b * 4] < boxes[a * 4 + 2]
                && boxes[a * 4 + 1] < boxes[b * 4 + 3] && boxes[b * 4 + 1] < boxes[a * 4 + 3];
    }

    /** Merges a region into another: the box that holds both, and the lights of the one are the lights of the other. */
    private void mergeRegion(int into, int from) {
        regionBoxes[into * 4] = Math.min(regionBoxes[into * 4], regionBoxes[from * 4]);
        regionBoxes[into * 4 + 1] = Math.min(regionBoxes[into * 4 + 1], regionBoxes[from * 4 + 1]);
        regionBoxes[into * 4 + 2] = Math.max(regionBoxes[into * 4 + 2], regionBoxes[from * 4 + 2]);
        regionBoxes[into * 4 + 3] = Math.max(regionBoxes[into * 4 + 3], regionBoxes[from * 4 + 3]);
        regionBoxes[from * 4 + 2] = regionBoxes[from * 4];
        for (int i = 0; i < count; i++) {
            if (lightRegion[i] == from) {
                lightRegion[i] = into;
            }
        }
    }

    /** Returns the point a share of the way from one value to another. */
    private static double mix(double from, double to, double share) {
        return from + (to - from) * share;
    }

    /** Tells whether what the cells depend on differs from the last call, and remembers it. */
    private boolean differsFromLast(LightStyle style, Rgba tint, double tintStrength, double artScale) {
        final int length = count * STRIDE + SIGNATURE_EXTRA;
        // Both arrays are as large as the lights are, so a call that has more lights grows each of them once
        final int capacity = lights.length + SIGNATURE_EXTRA;
        if (scratch.length < length) {
            scratch = new double[capacity];
        }
        if (signature.length < length) {
            signature = Arrays.copyOf(signature, capacity);
        }
        if (signatureFlickers.length < flickers.length) {
            signatureFlickers = Arrays.copyOf(signatureFlickers, flickers.length);
        }
        System.arraycopy(lights, 0, scratch, 0, count * STRIDE);
        int at = count * STRIDE;
        scratch[at++] = tint.r();
        scratch[at++] = tint.g();
        scratch[at++] = tint.b();
        scratch[at++] = tintStrength;
        scratch[at++] = artScale;
        scratch[at++] = phaseX;
        scratch[at++] = phaseY;
        scratch[at++] = style.warmStrength();
        scratch[at] = style.referenceDarkness();
        boolean same = signatureLength == length;
        for (int i = 0; same && i < length; i++) {
            same = scratch[i] == signature[i];
        }
        // The shape of a light is read from its flicker and the numbers above only say when it was read, so the flicker
        // itself is part of what the cells depend on
        for (int i = 0; same && i < count; i++) {
            same = flickers[i] == signatureFlickers[i];
        }
        if (same) {
            return false;
        }
        final double[] swap = signature;
        signature = scratch;
        scratch = swap;
        System.arraycopy(flickers, 0, signatureFlickers, 0, count);
        signatureLength = length;
        return true;
    }

    /** Works out every cell of the box of every region. */
    private void work(LightStyle style, Rgba tint, double tintStrength, double gameTime) {
        final double darkness = Math.min(1.0, tintStrength / style.referenceDarkness());
        final double warm = style.warmStrength() * darkness;
        for (int region = 0; region < regionCount; region++) {
            workRegion(region, gameTime, tint, tintStrength, warm);
        }
    }

    /** Works out every cell of the box of one region. */
    private void workRegion(int region, double gameTime, Rgba tint, double tintStrength, double warm) {
        final int regionLeft = regionBoxes[region * 4];
        final int regionTop = regionBoxes[region * 4 + 1];
        final int regionRight = regionBoxes[region * 4 + 2];
        final int regionBottom = regionBoxes[region * 4 + 3];
        for (int y = regionTop; y < regionBottom; y++) {
            Arrays.fill(pixels, y * cellsWide + regionLeft, y * cellsWide + regionRight, plain);
        }
        int first = -1;
        int members = 0;
        for (int light = 0; light < count; light++) {
            if (lightRegion[light] == region) {
                first = first < 0 ? light : first;
                members++;
            }
        }
        if (members == 1) {
            // One light needs nothing remembered between cells: every cell is worked out straight to its colour
            castLight(first, gameTime, tint, tintStrength, warm, false);
            return;
        }
        sumLeft = regionLeft;
        sumTop = regionTop;
        sumWide = regionRight - regionLeft;
        final int cells = sumWide * (regionBottom - regionTop);
        if (levels.length < cells) {
            levels = new int[cells];
            weights = new int[cells];
            reds = new int[cells];
            greens = new int[cells];
            blues = new int[cells];
        } else {
            Arrays.fill(levels, 0, cells, 0);
            Arrays.fill(weights, 0, cells, 0);
            Arrays.fill(reds, 0, cells, 0);
            Arrays.fill(greens, 0, cells, 0);
            Arrays.fill(blues, 0, cells, 0);
        }
        for (int light = 0; light < count; light++) {
            if (lightRegion[light] == region) {
                castLight(light, gameTime, tint, tintStrength, warm, true);
            }
        }
        for (int y = regionTop; y < regionBottom; y++) {
            for (int x = regionLeft; x < regionRight; x++) {
                final int sum = (y - sumTop) * sumWide + (x - sumLeft);
                if (weights[sum] > 0) {
                    final double w = weights[sum] * 255.0;
                    pixels[y * cellsWide + x] = cell(levels[sum] / 255.0, reds[sum] / w, greens[sum] / w,
                            blues[sum] / w, tint, tintStrength, warm);
                }
            }
        }
    }

    /**
     * Casts one light over the cells of its box, either straight to the pixels or into the sums that several lights
     * share.
     */
    private void castLight(int light, double gameTime, Rgba tint, double tintStrength, double warm, boolean sums) {
        final int at = light * STRIDE;
        final double centreX = lights[at];
        final double centreY = lights[at + 1];
        final double radius = lights[at + 2];
        final double intensity = lights[at + 3];
        final double red = lights[at + 4];
        final double green = lights[at + 5];
        final double blue = lights[at + 6];
        final int boxLeft = (int) lights[at + BOX_AT];
        final int boxTop = (int) lights[at + BOX_AT + 1];
        final int boxRight = (int) lights[at + BOX_AT + 2];
        final int boxBottom = (int) lights[at + BOX_AT + 3];
        final boolean shaped = flickers[light].shapeAmplitude() > 0.0;
        if (shaped) {
            // A round light has a factor of one in every direction and needs no table
            prepareShape(flickers[light], lights[at + SHAPE_TIME_AT]);
        }
        if (!sums) {
            // One light has one colour, so a cell is looked up by its light in 256 steps instead of being mixed
            for (int level = 0; level <= 255; level++) {
                litCells[level] = cell(level / 255.0, red, green, blue, tint, tintStrength, warm);
            }
        }
        final int red255 = (int) Math.round(red * 255.0);
        final int green255 = (int) Math.round(green * 255.0);
        final int blue255 = (int) Math.round(blue * 255.0);
        // A distance on the ground in tiles, from two distances in art pixels: tiles are 32 across and 16 down, and the
        // square of a step along a tile axis is half the sum of the squares of its two diagonal parts
        final double scale = 0.5 / (radius * radius);
        final int boxWide = boxRight - boxLeft;
        if (columnPart.length < boxWide) {
            columnPart = new double[boxWide];
            columnAcross = new double[boxWide];
        }
        for (int x = 0; x < boxWide; x++) {
            final double across = (boxLeft + x + 0.5 - centreX) / TILE_ART_HALF_WIDTH;
            columnAcross[x] = across;
            columnPart[x] = across * across;
        }
        for (int y = boxTop; y < boxBottom; y++) {
            final double down = (y + 0.5 - centreY) / TILE_ART_HALF_HEIGHT;
            final double downSquare = down * down;
            final int row = y * cellsWide + boxLeft;
            final int sumRow = (y - sumTop) * sumWide + (boxLeft - sumLeft);
            for (int x = 0; x < boxWide; x++) {
                double share = (downSquare + columnPart[x]) * scale;
                if (shaped) {
                    share *= inverseAt(columnAcross[x], down);
                }
                if (!(share < 1.0)) {
                    continue;
                }
                final double lit = Math.min(1.0, LightFalloff.level(share) * intensity);
                final int level = (int) (lit * 255.0 + 0.5);
                if (!sums) {
                    pixels[row + x] = litCells[level];
                    continue;
                }
                final int i = sumRow + x;
                levels[i] = levels[i] == 0 ? level : 255 - ((255 - levels[i]) * (255 - level) + 127) / 255;
                weights[i] += level;
                reds[i] += level * red255;
                greens[i] += level * green255;
                blues[i] += level * blue255;
            }
        }
    }

    /**
     * Fills the table of the radius by angle of a light: for each sector the inverse of the square of the factor that
     * the shape of its flicker gives the radius in that direction at the moment.
     */
    private void prepareShape(Flicker flicker, double time) {
        for (int k = 0; k <= SECTORS; k++) {
            final double factor = flicker.shapeFactor((double) k / SECTORS, time);
            sectorInverse[k] = 1.0 / (factor * factor);
        }
    }

    /**
     * Returns the inverse square of the radius factor in the direction of a ground offset, interpolated between the
     * sectors of the table. The direction is a pseudo angle, from 0 to 4 around the source, which is a continuous and
     * monotone stand-in for the angle that is found with one division.
     */
    private double inverseAt(double x, double y) {
        final double turn;
        if (y >= 0.0) {
            turn = x >= 0.0 ? y / (x + y + 1e-300) : 1.0 - x / (y - x);
        } else {
            turn = x < 0.0 ? 2.0 - y / (-x - y) : 3.0 + x / (x - y);
        }
        final double position = turn * (SECTORS / 4.0);
        final int sector = Math.min(SECTORS - 1, (int) position);
        final double fraction = position - sector;
        return sectorInverse[sector] + (sectorInverse[sector + 1] - sectorInverse[sector]) * fraction;
    }

    /**
     * Returns a finished cell: the sky tint turned toward the light. The colour moves from the tint to the colour of the
     * light and the opacity from the strength of the sky to the warm strength, both by the amount of light.
     */
    private static int cell(double lit, double red, double green, double blue, Rgba tint, double tintStrength,
                            double warm) {
        final double alpha = mix(tintStrength, warm, lit);
        return pack(alpha * 255.0, mix(tint.r(), red, lit) * alpha * 255.0, mix(tint.g(), green, lit) * alpha * 255.0,
                mix(tint.b(), blue, lit) * alpha * 255.0);
    }

    /**
     * Returns the cell of the plain sky tint as premultiplied ARGB, rounded to 8 bits per channel. It is the one place
     * the plain tint is quantised, so the cells no light reaches and a fill laid where there are no lights are the same
     * colour to the level.
     *
     * @param tint         the colour of the sky tint
     * @param tintStrength how strongly the sky tint is laid over the world, 0 to 1
     * @return the plain cell
     */
    public static int plainCellFor(Rgba tint, double tintStrength) {
        return pack(tintStrength * 255.0, tint.r() * tintStrength * 255.0, tint.g() * tintStrength * 255.0,
                tint.b() * tintStrength * 255.0);
    }

    /** Packs four channels, each rounded and held to 0 to 255, with the colour not above the alpha. */
    private static int pack(double alpha, double red, double green, double blue) {
        final int a = clamp(alpha);
        return (a << 24) | (Math.min(a, clamp(red)) << 16) | (Math.min(a, clamp(green)) << 8) | Math.min(a, clamp(blue));
    }

    /** Rounds a channel and holds it to 0 to 255. */
    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    /** @return true when at least one light was in view at the last call */
    public boolean hasLights() {
        return count > 0;
    }

    /** @return true when the last call worked the cells out again, so the caller uploads the regions */
    public boolean changed() {
        return changed;
    }

    /** @return the number of cells across the grid */
    public int width() {
        return cellsWide;
    }

    /** @return the number of cells down the grid */
    public int height() {
        return cellsHigh;
    }

    /** @return how many regions the lights are merged into: the boxes the cells are worked out and kept in */
    public int regionCount() {
        return regionCount;
    }

    /**
     * Returns the left column of a region.
     *
     * @param region the number of the region, from 0 up to but excluding {@link #regionCount()}
     * @return the left column
     */
    public int regionLeft(int region) {
        return regionBoxes[region * 4];
    }

    /**
     * Returns the top row of a region.
     *
     * @param region the number of the region
     * @return the top row
     */
    public int regionTop(int region) {
        return regionBoxes[region * 4 + 1];
    }

    /**
     * Returns the column after the last of a region.
     *
     * @param region the number of the region
     * @return the column after the last
     */
    public int regionRight(int region) {
        return regionBoxes[region * 4 + 2];
    }

    /**
     * Returns the row after the last of a region.
     *
     * @param region the number of the region
     * @return the row after the last
     */
    public int regionBottom(int region) {
        return regionBoxes[region * 4 + 3];
    }

    /** @return the horizontal phase of the camera grid of the last call */
    public double phaseX() {
        return phaseX;
    }

    /** @return the vertical phase of the camera grid of the last call */
    public double phaseY() {
        return phaseY;
    }

    /**
     * Returns the cell of the plain sky tint as premultiplied ARGB: what every cell of the layer that no light reaches
     * holds, and so what the world outside the regions must be laid with for the two to meet without a seam.
     *
     * @return the plain cell of the last call
     */
    public int plainCell() {
        return plain;
    }

    /** @return the cells as premultiplied ARGB, row by row, valid inside the regions, which the caller must not change */
    public int[] pixels() {
        return pixels;
    }

    /**
     * Returns one cell of a region.
     *
     * @param x column, inside a region
     * @param y row, inside a region
     * @return the cell as premultiplied ARGB
     */
    public int pixelAt(int x, int y) {
        return pixels[y * cellsWide + x];
    }

    /** @return how many lights were in view at the last call */
    public int lightCount() {
        return count;
    }

    /** @return how many times the cells were worked out so far, which a call that found nothing changed does not count */
    public int composes() {
        return composes;
    }
}
