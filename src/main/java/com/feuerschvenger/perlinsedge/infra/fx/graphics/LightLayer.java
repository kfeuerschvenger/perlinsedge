package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.utils.ColourChannels;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.Flicker;
import com.feuerschvenger.perlinsedge.domain.world.light.LightFalloff;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.LightSource;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.PixelSnap;

import java.util.Arrays;

/**
 * The overlay of the sky with the lights worked into it, at the resolution of the art: one cell is one art pixel and
 * the grid is aligned to the camera like the one of the shadows, so the gradient of a light is made of crisp blocks of
 * whole art pixels at every zoom.
 * <p>
 * Every cell follows one rule. A cell has an amount of light, from 0 to 1, and its overlay is the sky tint turned
 * toward the colour of the light by that amount: the colour moves from the tint to the colour of the light, and the
 * opacity moves from the strength of the sky to a modest warm strength. A lit patch is therefore a warm wash and never
 * a hole in the tint that shows the plain ground. The warm strength is scaled by how dark it is, so it is full in the
 * deepest night, a faint wash at dusk and nothing at midday, when the tint has no strength and there is nothing to
 * turn.
 * <p>
 * A light covers the cells of the ground around it, and the ground is the isometric plane, so a circle of tiles around
 * a light is an ellipse twice as wide as it is tall on the screen. The distance of a cell from a light is worked out
 * on the ground, in tiles, and its square as a share of the radius gives the light of the cell through
 * {@link LightFalloff}, a continuous gradient that is exactly zero at the radius. The radius is not the same in every
 * direction: it varies with the angle around the light by the shape of its {@link Flicker}, read from a small table
 * that is worked out for each shaped light once per call and interpolated, indexed by a pseudo angle that needs no
 * trigonometry. Several lights combine their light as {@code 1 - (1 - a)(1 - b)} and their colours weighted by how
 * much each lights the cell.
 * <p>
 * Only the boxes around the lights are worked out and kept, and the world outside them is laid with the plain tint by
 * the caller. The boxes of lights that overlap are merged into one region, so a region is the smallest box that holds
 * the lights that can touch each other, and the regions never overlap: lights far apart cost the cells around each of
 * them and not the stretch of the world between them. The cells are worked out again only when something they depend
 * on changed since the last call, and no array is allocated while the canvas keeps its size and the lights and the
 * regions do not outgrow the arrays. The class holds plain arrays and no framework type, so it is tested without a
 * window.
 */
public final class LightLayer {

    /**
     * Art pixels from the centre of a tile to its right vertex along the width, which is twice the art pixels along the
     * height. It follows from the tile geometry of the configuration and the art scale at unit zoom, so the grid of the
     * lights is the grid of the tiles at every zoom.
     */
    public static final double TILE_ART_HALF_WIDTH = AppConfig.getInstance().graphics().getTileHalfWidth()
            / ZoomSteps.ART_SCALE_AT_UNIT_ZOOM;
    /** Art pixels from the top vertex of a tile to its centre along the height, where an entity stands. */
    public static final double TILE_ART_HALF_HEIGHT = AppConfig.getInstance().graphics().getTileHalfHeight()
            / ZoomSteps.ART_SCALE_AT_UNIT_ZOOM;
    /**
     * The art grid of the canvas the cells are laid on.
     *
     * @param artScale the integer number of screen pixels per art pixel
     * @param originX  screen X of the camera origin, which the art grid is aligned to
     * @param originY  screen Y of the camera origin
     * @param wide     the number of cells across the canvas grid, with its border cells
     * @param high     the number of cells down the canvas grid, with its border cells
     */
    public record GridView(double artScale, double originX, double originY, int wide, int high) {}

    /** The numbers kept for each light: its centre cell, radius, intensity, colour, shape time and box. */
    private static final int STRIDE = 12;
    /** The part of the stride that holds the column of the centre cell. */
    private static final int CENTRE_X_AT = 0;
    /** The part of the stride that holds the row of the centre cell. */
    private static final int CENTRE_Y_AT = 1;
    /** The part of the stride that holds the radius. */
    private static final int RADIUS_AT = 2;
    /** The part of the stride that holds the intensity. */
    private static final int INTENSITY_AT = 3;
    /** The part of the stride that holds the red of the colour. */
    private static final int RED_AT = 4;
    /** The part of the stride that holds the green of the colour. */
    private static final int GREEN_AT = 5;
    /** The part of the stride that holds the blue of the colour. */
    private static final int BLUE_AT = 6;
    /** The numbers a box of cells is given by: left, top, right and bottom. */
    private static final int BOX_VALUES = 4;
    /** Position of the left side in a box. */
    private static final int BOX_LEFT = 0;
    /** Position of the top side in a box. */
    private static final int BOX_TOP = 1;
    /** Position of the right side in a box. */
    private static final int BOX_RIGHT = 2;
    /** Position of the bottom side in a box. */
    private static final int BOX_BOTTOM = 3;
    /** The part of the signature, past the lights, that holds the red of the tint. */
    private static final int SIGNATURE_TINT_RED = 0;
    /** The part of the signature, past the lights, that holds the green of the tint. */
    private static final int SIGNATURE_TINT_GREEN = 1;
    /** The part of the signature, past the lights, that holds the blue of the tint. */
    private static final int SIGNATURE_TINT_BLUE = 2;
    /** The part of the signature, past the lights, that holds the strength of the tint. */
    private static final int SIGNATURE_STRENGTH = 3;
    /** The part of the signature, past the lights, that holds the art scale. */
    private static final int SIGNATURE_SCALE = 4;
    /** The part of the signature, past the lights, that holds the horizontal phase. */
    private static final int SIGNATURE_PHASE_X = 5;
    /** The part of the signature, past the lights, that holds the vertical phase. */
    private static final int SIGNATURE_PHASE_Y = 6;
    /** The part of the signature, past the lights, that holds the warm strength of the style. */
    private static final int SIGNATURE_WARM = 7;
    /** The part of the signature, past the lights, that holds the reference darkness of the style. */
    private static final int SIGNATURE_DARKNESS = 8;
    /** How many times an array of lights grows when it is full. */
    private static final int GROWTH_FACTOR = 2;
    /** The largest step of light, and the largest value of an 8 bit channel. */
    private static final int MAX_LEVEL = 255;
    /** The largest step of light as a double, the factor between a share and a step. */
    private static final double LEVEL_SCALE = MAX_LEVEL;
    /** Added before a division by the largest step so the result is rounded to the nearest step. */
    private static final int LEVEL_ROUNDING = 127;
    /** Bits the alpha is shifted up in a packed cell. */
    private static final int ALPHA_SHIFT = 24;
    /** Bits the red is shifted up in a packed cell. */
    private static final int RED_SHIFT = 16;
    /** Bits the green is shifted up in a packed cell. */
    private static final int GREEN_SHIFT = 8;
    /** The pseudo angle of the negative X axis, the half of the turn that goes from 0 to 4 around the source. */
    private static final double HALF_TURN = 2.0;
    /** The pseudo angle of the negative Y axis, three quarters of the turn around the source. */
    private static final double THREE_QUARTER_TURN = 3.0;
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
    private int[] regionBoxes = new int[INITIAL_LIGHTS * BOX_VALUES];
    /** How many lights are in view. */
    private int count;
    /** How many regions the lights in view are merged into. */
    private int regionCount;

    /** The radius of the light being cast by angle: one over the square of the factor, with a closing entry. */
    private final double[] sectorInverse = new double[SECTORS + 1];
    /** The finished cell for each of the 256 steps of light, for the single light being cast. */
    private final int[] litCells = new int[MAX_LEVEL + 1];
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
    /** How many times the boxes were merged into regions so far. */
    private int groupRuns;

    /**
     * Works out the cells for the lights in view, unless nothing they depend on changed since the last call.
     *
     * @param registry     the lights of the world
     * @param style        how the lights are drawn
     * @param gameTime     the game time in seconds, which the flicker of the lights runs on
     * @param tint         the colour of the sky tint
     * @param tintStrength how strongly the sky tint is laid over the world, 0 to 1
     * @param grid         the art grid of the canvas the cells are laid on
     * @return true when at least one light reaches into the canvas, false when the plain tint is all there is to draw
     */
    public boolean compose(LightRegistry registry, LightStyle style, double gameTime, ColourChannels tint, double tintStrength,
                           GridView grid) {
        changed = false;
        final double artScale = grid.artScale();
        if (grid.wide() != cellsWide || grid.high() != cellsHigh) {
            cellsWide = grid.wide();
            cellsHigh = grid.high();
            pixels = new int[cellsWide * cellsHigh];
            signatureLength = 0;
        }
        final double snappedX = PixelSnap.round(grid.originX());
        final double snappedY = PixelSnap.round(grid.originY());
        phaseX = snappedX - Math.floor(snappedX / artScale) * artScale;
        phaseY = snappedY - Math.floor(snappedY / artScale) * artScale;
        plain = plainCellFor(tint, tintStrength);
        collect(registry, gameTime, artScale, snappedX, snappedY);
        if (count == 0) {
            return false;
        }
        if (!differsFromLast(style, tint, tintStrength, artScale)) {
            return true;
        }
        // The regions follow from the boxes of the lights, which the signature holds, so a call that found no change
        // keeps the regions of the last one and runs no merge
        group();
        groupRuns++;
        work(style, tint, tintStrength);
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
            addLight(registry.get(i), gameTime, artScale, snappedX, snappedY);
        }
    }

    /** Adds a light to the lights in view when its box has a cell on the canvas. */
    private void addLight(LightSource source, double gameTime, double artScale, double snappedX, double snappedY) {
        final var flicker = source.flicker();
        // The light shines from where an entity at its position would stand, and is placed on a whole screen pixel
        final double screenX = PixelSnap.round((source.x() - source.y()) * TILE_ART_HALF_WIDTH * artScale
                + snappedX);
        final double screenY = PixelSnap.round(((source.x() + source.y()) * TILE_ART_HALF_HEIGHT
                + TILE_ART_HALF_HEIGHT) * artScale + snappedY);
        final double centreX = (screenX - phaseX) / artScale + 1.0;
        final double centreY = (screenY - phaseY) / artScale + 1.0;
        // A light whose widest possible reach stays off the canvas is dropped before any flicker is evaluated
        if (!reachesCanvas(centreX, centreY, source.radius() * (1.0 + flicker.radiusAmplitude()), flicker)) {
            return;
        }
        final double radius = source.radius() * flicker.radiusFactor(gameTime);
        final double intensity = Math.clamp(source.intensity() * flicker.intensityFactor(gameTime), 0.0, 1.0);
        if (radius <= 0.0 || intensity <= 0.0) {
            return;
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
            return;
        }
        if ((count + 1) * STRIDE > lights.length) {
            lights = Arrays.copyOf(lights, lights.length * GROWTH_FACTOR);
            flickers = Arrays.copyOf(flickers, flickers.length * GROWTH_FACTOR);
        }
        final int at = count * STRIDE;
        lights[at + CENTRE_X_AT] = centreX;
        lights[at + CENTRE_Y_AT] = centreY;
        lights[at + RADIUS_AT] = radius;
        lights[at + INTENSITY_AT] = intensity;
        storeColour(at, source.colour(), flicker, gameTime);
        lights[at + SHAPE_TIME_AT] = flicker.shapeAmplitude() > 0.0 ? gameTime : 0.0;
        lights[at + BOX_AT + BOX_LEFT] = boxLeft;
        lights[at + BOX_AT + BOX_TOP] = boxTop;
        lights[at + BOX_AT + BOX_RIGHT] = boxRight;
        lights[at + BOX_AT + BOX_BOTTOM] = boxBottom;
        flickers[count] = flicker;
        count++;
    }

    /** Stores the colour of a light at the place of the light, drifted between the two ends of its flicker when it drifts. */
    private void storeColour(int at, Rgba colour, Flicker flicker, double gameTime) {
        if (flicker.hasColourDrift()) {
            final double share = flicker.colourShare(gameTime);
            lights[at + RED_AT] = mix(flicker.colourLow().r(), flicker.colourHigh().r(), share);
            lights[at + GREEN_AT] = mix(flicker.colourLow().g(), flicker.colourHigh().g(), share);
            lights[at + BLUE_AT] = mix(flicker.colourLow().b(), flicker.colourHigh().b(), share);
        } else {
            lights[at + RED_AT] = colour.r();
            lights[at + GREEN_AT] = colour.g();
            lights[at + BLUE_AT] = colour.b();
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
            regionBoxes = new int[flickers.length * BOX_VALUES];
        }
        for (var i = 0; i < count; i++) {
            lightRegion[i] = i;
            for (var side = 0; side < BOX_VALUES; side++) {
                regionBoxes[i * BOX_VALUES + side] = (int) lights[i * STRIDE + BOX_AT + side];
            }
        }
        boolean merged = count > 1;
        while (merged) {
            merged = mergePass();
        }
        regionCount = compactRegions();
    }

    /**
     * Merges every region that overlaps another once.
     *
     * @return whether any two regions were merged
     */
    private boolean mergePass() {
        var merged = false;
        for (var a = 0; a < count; a++) {
            if (!isEmptyRegion(a) && absorbOverlapping(a)) {
                merged = true;
            }
        }
        return merged;
    }

    /**
     * Merges into a region every later region that overlaps it.
     *
     * @return whether any region was merged
     */
    private boolean absorbOverlapping(int a) {
        var merged = false;
        for (var b = a + 1; b < count; b++) {
            if (!isEmptyRegion(b) && regionsOverlap(a, b)) {
                mergeRegion(a, b);
                merged = true;
            }
        }
        return merged;
    }

    /**
     * Gives the regions that are left the first numbers.
     *
     * @return how many regions are left
     */
    private int compactRegions() {
        var next = 0;
        for (var a = 0; a < count; a++) {
            if (!isEmptyRegion(a)) {
                if (next != a) {
                    renumberRegion(a, next);
                }
                next++;
            }
        }
        return next;
    }

    /** Moves the box of a region to another number, and the lights of the region with it. */
    private void renumberRegion(int from, int to) {
        System.arraycopy(regionBoxes, from * BOX_VALUES, regionBoxes, to * BOX_VALUES, BOX_VALUES);
        for (var i = 0; i < count; i++) {
            if (lightRegion[i] == from) {
                lightRegion[i] = to;
            }
        }
    }

    /** Tells whether a region was merged into another, which leaves it with no box. */
    private boolean isEmptyRegion(int region) {
        return regionBoxes[region * BOX_VALUES + BOX_RIGHT] <= regionBoxes[region * BOX_VALUES + BOX_LEFT];
    }

    /** Tells whether the boxes of two regions share at least one cell. */
    private boolean regionsOverlap(int a, int b) {
        final int[] boxes = regionBoxes;
        final int aBase = a * BOX_VALUES;
        final int bBase = b * BOX_VALUES;
        return boxes[aBase + BOX_LEFT] < boxes[bBase + BOX_RIGHT] && boxes[bBase + BOX_LEFT] < boxes[aBase + BOX_RIGHT]
                && boxes[aBase + BOX_TOP] < boxes[bBase + BOX_BOTTOM]
                && boxes[bBase + BOX_TOP] < boxes[aBase + BOX_BOTTOM];
    }

    /** Merges a region into another: the box that holds both, and the lights of the one are the lights of the other. */
    private void mergeRegion(int into, int from) {
        final int intoBase = into * BOX_VALUES;
        final int fromBase = from * BOX_VALUES;
        regionBoxes[intoBase + BOX_LEFT] = Math.min(regionBoxes[intoBase + BOX_LEFT], regionBoxes[fromBase + BOX_LEFT]);
        regionBoxes[intoBase + BOX_TOP] = Math.min(regionBoxes[intoBase + BOX_TOP], regionBoxes[fromBase + BOX_TOP]);
        regionBoxes[intoBase + BOX_RIGHT] = Math.max(regionBoxes[intoBase + BOX_RIGHT],
                regionBoxes[fromBase + BOX_RIGHT]);
        regionBoxes[intoBase + BOX_BOTTOM] = Math.max(regionBoxes[intoBase + BOX_BOTTOM],
                regionBoxes[fromBase + BOX_BOTTOM]);
        regionBoxes[fromBase + BOX_RIGHT] = regionBoxes[fromBase + BOX_LEFT];
        for (var i = 0; i < count; i++) {
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
    private boolean differsFromLast(LightStyle style, ColourChannels tint, double tintStrength, double artScale) {
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
        final int at = count * STRIDE;
        scratch[at + SIGNATURE_TINT_RED] = tint.r();
        scratch[at + SIGNATURE_TINT_GREEN] = tint.g();
        scratch[at + SIGNATURE_TINT_BLUE] = tint.b();
        scratch[at + SIGNATURE_STRENGTH] = tintStrength;
        scratch[at + SIGNATURE_SCALE] = artScale;
        scratch[at + SIGNATURE_PHASE_X] = phaseX;
        scratch[at + SIGNATURE_PHASE_Y] = phaseY;
        scratch[at + SIGNATURE_WARM] = style.warmStrength();
        scratch[at + SIGNATURE_DARKNESS] = style.referenceDarkness();
        var same = signatureLength == length;
        for (var i = 0; same && i < length; i++) {
            same = Double.compare(scratch[i], signature[i]) == 0;
        }
        // The shape of a light is read from its flicker and the numbers above only say when it was read, so the flicker
        // itself is part of what the cells depend on
        for (var i = 0; same && i < count; i++) {
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
    private void work(LightStyle style, ColourChannels tint, double tintStrength) {
        final double darkness = Math.min(1.0, tintStrength / style.referenceDarkness());
        final double warm = style.warmStrength() * darkness;
        for (var region = 0; region < regionCount; region++) {
            workRegion(region, tint, tintStrength, warm);
        }
    }

    /** Works out every cell of the box of one region. */
    private void workRegion(int region, ColourChannels tint, double tintStrength, double warm) {
        final int regionLeft = regionBoxes[region * BOX_VALUES + BOX_LEFT];
        final int regionTop = regionBoxes[region * BOX_VALUES + BOX_TOP];
        final int regionRight = regionBoxes[region * BOX_VALUES + BOX_RIGHT];
        final int regionBottom = regionBoxes[region * BOX_VALUES + BOX_BOTTOM];
        for (int y = regionTop; y < regionBottom; y++) {
            Arrays.fill(pixels, y * cellsWide + regionLeft, y * cellsWide + regionRight, plain);
        }
        final int first = firstLightOf(region);
        if (first >= 0 && membersOf(region) == 1) {
            // One light needs nothing remembered between cells: every cell is worked out straight to its colour
            castLight(first, tint, tintStrength, warm, false);
            return;
        }
        sumLeft = regionLeft;
        sumTop = regionTop;
        sumWide = regionRight - regionLeft;
        clearSums(sumWide * (regionBottom - regionTop));
        for (var light = 0; light < count; light++) {
            if (lightRegion[light] == region) {
                castLight(light, tint, tintStrength, warm, true);
            }
        }
        resolveSums(regionLeft, regionTop, regionRight, regionBottom, tint, tintStrength, warm);
    }

    /** Returns the first light of a region, or -1 when it has none. */
    private int firstLightOf(int region) {
        for (var light = 0; light < count; light++) {
            if (lightRegion[light] == region) {
                return light;
            }
        }
        return -1;
    }

    /** Returns how many lights a region holds. */
    private int membersOf(int region) {
        var members = 0;
        for (var light = 0; light < count; light++) {
            if (lightRegion[light] == region) {
                members++;
            }
        }
        return members;
    }

    /** Makes the sums hold room for a number of cells, every one of them zero. */
    private void clearSums(int cells) {
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
    }

    /** Turns the sums of the lights of a region into the finished cells of its box. */
    private void resolveSums(int regionLeft, int regionTop, int regionRight, int regionBottom, ColourChannels tint,
                             double tintStrength, double warm) {
        for (int y = regionTop; y < regionBottom; y++) {
            for (int x = regionLeft; x < regionRight; x++) {
                final int sum = (y - sumTop) * sumWide + (x - sumLeft);
                if (weights[sum] > 0) {
                    final double w = weights[sum] * LEVEL_SCALE;
                    pixels[y * cellsWide + x] = cell(levels[sum] / LEVEL_SCALE, reds[sum] / w, greens[sum] / w,
                            blues[sum] / w, tint, tintStrength, warm);
                }
            }
        }
    }

    /**
     * Casts one light over the cells of its box, either straight to the pixels or into the sums that several lights
     * share.
     */
    private void castLight(int light, ColourChannels tint, double tintStrength, double warm, boolean sums) {
        final int at = light * STRIDE;
        final double centreX = lights[at + CENTRE_X_AT];
        final double centreY = lights[at + CENTRE_Y_AT];
        final double radius = lights[at + RADIUS_AT];
        final double intensity = lights[at + INTENSITY_AT];
        final int boxLeft = (int) lights[at + BOX_AT + BOX_LEFT];
        final int boxTop = (int) lights[at + BOX_AT + BOX_TOP];
        final int boxRight = (int) lights[at + BOX_AT + BOX_RIGHT];
        final int boxBottom = (int) lights[at + BOX_AT + BOX_BOTTOM];
        final boolean shaped = flickers[light].shapeAmplitude() > 0.0;
        if (shaped) {
            // A round light has a factor of one in every direction and needs no table
            prepareShape(flickers[light], lights[at + SHAPE_TIME_AT]);
        }
        if (!sums) {
            fillLitCells(lights[at + RED_AT], lights[at + GREEN_AT], lights[at + BLUE_AT], tint, tintStrength, warm);
        }
        final int red255 = (int) Math.round(lights[at + RED_AT] * LEVEL_SCALE);
        final int green255 = (int) Math.round(lights[at + GREEN_AT] * LEVEL_SCALE);
        final int blue255 = (int) Math.round(lights[at + BLUE_AT] * LEVEL_SCALE);
        // A distance on the ground in tiles, from two distances in art pixels: tiles are 32 across and 16 down, and the
        // square of a step along a tile axis is half the sum of the squares of its two diagonal parts
        final double scale = 0.5 / (radius * radius);
        final int boxWide = boxRight - boxLeft;
        prepareColumns(boxLeft, boxWide, centreX);
        for (int y = boxTop; y < boxBottom; y++) {
            final double down = (y + 0.5 - centreY) / TILE_ART_HALF_HEIGHT;
            final double downSquare = down * down;
            final int row = y * cellsWide + boxLeft;
            final int sumRow = (y - sumTop) * sumWide + (boxLeft - sumLeft);
            for (var x = 0; x < boxWide; x++) {
                final double share = shareAt(x, down, downSquare, scale, shaped);
                if (share < 1.0) {
                    final int level = (int) (Math.min(1.0, LightFalloff.level(share) * intensity) * LEVEL_SCALE + 0.5);
                    if (sums) {
                        addToSums(sumRow + x, level, red255, green255, blue255);
                    } else {
                        pixels[row + x] = litCells[level];
                    }
                }
            }
        }
    }

    /** Works out the finished cell of each of the 256 steps of light of one colour, which a single light looks up. */
    private void fillLitCells(double red, double green, double blue, ColourChannels tint, double tintStrength,
                              double warm) {
        // One light has one colour, so a cell is looked up by its light in 256 steps instead of being mixed
        for (var level = 0; level <= MAX_LEVEL; level++) {
            litCells[level] = cell(level / LEVEL_SCALE, red, green, blue, tint, tintStrength, warm);
        }
    }

    /** Works out, for each column of the box of a light, its distance across the ground from the light. */
    private void prepareColumns(int boxLeft, int boxWide, double centreX) {
        if (columnPart.length < boxWide) {
            columnPart = new double[boxWide];
            columnAcross = new double[boxWide];
        }
        for (var x = 0; x < boxWide; x++) {
            final double across = (boxLeft + x + 0.5 - centreX) / TILE_ART_HALF_WIDTH;
            columnAcross[x] = across;
            columnPart[x] = across * across;
        }
    }

    /** Returns the square of the distance of a cell from the light as a share of its radius, which is 1 at its edge. */
    private double shareAt(int column, double down, double downSquare, double scale, boolean shaped) {
        final double share = (downSquare + columnPart[column]) * scale;
        return shaped ? share * inverseAt(columnAcross[column], down) : share;
    }

    /** Adds the light of one light to the sums of a cell. */
    private void addToSums(int i, int level, int red255, int green255, int blue255) {
        levels[i] = levels[i] == 0 ? level
                : MAX_LEVEL - ((MAX_LEVEL - levels[i]) * (MAX_LEVEL - level) + LEVEL_ROUNDING) / MAX_LEVEL;
        weights[i] += level;
        reds[i] += level * red255;
        greens[i] += level * green255;
        blues[i] += level * blue255;
    }

    /**
     * Fills the table of the radius by angle of a light: for each sector the inverse of the square of the factor that
     * the shape of its flicker gives the radius in that direction at the moment.
     */
    private void prepareShape(Flicker flicker, double time) {
        for (var k = 0; k <= SECTORS; k++) {
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
            turn = x < 0.0 ? HALF_TURN - y / (-x - y) : THREE_QUARTER_TURN + x / (x - y);
        }
        final double position = turn * (SECTORS / 4.0);
        final int sector = Math.min(SECTORS - 1, (int) position);
        final double fraction = position - sector;
        return sectorInverse[sector] + (sectorInverse[sector + 1] - sectorInverse[sector]) * fraction;
    }

    /**
     * Returns a finished cell: the sky tint turned toward the light. The colour moves from the tint to the colour of
     * the light and the opacity from the strength of the sky to the warm strength, both by the amount of light.
     */
    private static int cell(double lit, double red, double green, double blue, ColourChannels tint, double tintStrength,
                            double warm) {
        final double alpha = mix(tintStrength, warm, lit);
        return pack(alpha * LEVEL_SCALE, mix(tint.r(), red, lit) * alpha * LEVEL_SCALE,
                mix(tint.g(), green, lit) * alpha * LEVEL_SCALE, mix(tint.b(), blue, lit) * alpha * LEVEL_SCALE);
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
    public static int plainCellFor(ColourChannels tint, double tintStrength) {
        return pack(tintStrength * LEVEL_SCALE, tint.r() * tintStrength * LEVEL_SCALE,
                tint.g() * tintStrength * LEVEL_SCALE, tint.b() * tintStrength * LEVEL_SCALE);
    }

    /** Packs four channels, each rounded and held to 0 to 255, with the colour not above the alpha. */
    private static int pack(double alpha, double red, double green, double blue) {
        final int a = clamp(alpha);
        return (a << ALPHA_SHIFT) | (Math.min(a, clamp(red)) << RED_SHIFT) | (Math.min(a, clamp(green)) << GREEN_SHIFT)
                | Math.min(a, clamp(blue));
    }

    /** Rounds a channel and holds it to 0 to 255. */
    private static int clamp(double value) {
        return Math.clamp(Math.round(value), 0, MAX_LEVEL);
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
        return regionBoxes[region * BOX_VALUES + BOX_LEFT];
    }

    /**
     * Returns the top row of a region.
     *
     * @param region the number of the region
     * @return the top row
     */
    public int regionTop(int region) {
        return regionBoxes[region * BOX_VALUES + BOX_TOP];
    }

    /**
     * Returns the column after the last of a region.
     *
     * @param region the number of the region
     * @return the column after the last
     */
    public int regionRight(int region) {
        return regionBoxes[region * BOX_VALUES + BOX_RIGHT];
    }

    /**
     * Returns the row after the last of a region.
     *
     * @param region the number of the region
     * @return the row after the last
     */
    public int regionBottom(int region) {
        return regionBoxes[region * BOX_VALUES + BOX_BOTTOM];
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

    /**
     * Lends the cells to a sink, as premultiplied ARGB, row by row, valid inside the regions. The layer never hands its
     * array out, so nothing outside it can keep or change the cells.
     *
     * @param sink what takes a look at the cells for the length of the call
     */
    public void readPixels(PixelSink sink) {
        sink.accept(pixels);
    }

    /** @return a copy of the cells as premultiplied ARGB, row by row, for tests */
    int[] pixelsCopy() {
        return pixels.clone();
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

    /** @return how many times the cells were worked out so far, which a call that found no change does not count */
    public int composes() {
        return composes;
    }

    /** @return how many times the boxes of the lights were merged into regions, which a call that found no change does not count */
    public int groupRuns() {
        return groupRuns;
    }
}
