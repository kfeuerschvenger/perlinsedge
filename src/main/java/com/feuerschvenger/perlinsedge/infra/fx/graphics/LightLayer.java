package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.light.LightFalloff;
import com.feuerschvenger.perlinsedge.domain.world.light.LightRegistry;
import com.feuerschvenger.perlinsedge.domain.world.light.LightSource;
import com.feuerschvenger.perlinsedge.domain.world.light.LightStyle;

import java.util.Arrays;

/**
 * The darkness of the world with the lights cut out of it, at the resolution of the art: one cell is one art pixel and
 * the grid is aligned to the camera like the one of the shadows, so every ring of a light is a crisp stepped band of
 * whole art pixels at every zoom. The cells are the ones of the sky tint laid over the world, with the tint pushed back
 * where a light shines and a warm glow of the colour of the light added near its centre.
 * <p>
 * A light covers the cells of the ground around it, and the ground is the isometric plane, so a circle of tiles around a
 * light is an ellipse twice as wide as it is tall on the screen. The distance of a cell from a light is worked out on
 * the ground, in tiles, and the share of the radius it makes picks a ring, see {@link LightFalloff}. The light of a ring
 * is scaled by the intensity of the light at that moment and several lights combine as {@code 1 - (1 - a)(1 - b)}, so
 * two lights never light a cell beyond full. The tint alpha of a cell is the strength of the tint times what is left
 * unlit, so a light has no effect where the tint has no strength: at midday there is nothing to push back.
 * <p>
 * Only the box around the lights is worked out and kept, and the world outside it is laid with the plain tint by the
 * caller. The box is worked out again only when something it depends on changed since the last call, and no array is
 * allocated while the canvas keeps its size and the number of lights does not grow. The class holds plain arrays and no
 * framework type, so it is tested without a window.
 */
public final class LightLayer {

    /** Art pixels per tile along the width, which is twice the art pixels along the height. */
    private static final double TILE_ART_HALF_WIDTH = 16.0;
    /** Art pixels from the top vertex of a tile to its centre along the height, where an entity stands. */
    private static final double TILE_ART_HALF_HEIGHT = 8.0;
    /** Share of the light above which the warm glow begins, so only the centre rings glow. */
    private static final double GLOW_FROM = 0.4;
    /** The numbers kept for each light: its centre cell, its radius, its intensity, its colour and its box. */
    private static final int STRIDE = 11;
    /** The part of the stride that holds the box as left, top, right and bottom. */
    private static final int BOX_AT = 7;

    /** The number of cells across the grid. */
    private int cellsWide;
    /** The number of cells down the grid. */
    private int cellsHigh;
    /** How much of each cell is lit, 0 to 255, in the box. */
    private int[] levels = new int[0];
    /** The warm glow of each cell as premultiplied ARGB, in the box. */
    private int[] glow = new int[0];
    /** The finished cells as premultiplied ARGB: the tint pushed back by the lights with the glow over it. */
    private int[] pixels = new int[0];

    /** The numbers of the lights in view, {@link #STRIDE} for each. */
    private double[] lights = new double[STRIDE * 4];
    /** How many lights are in view. */
    private int count;

    /** The square of the share of the radius at which each ring of the light being cast ends. */
    private double[] ringEdge = new double[0];
    /** The level of light of each ring of the light being cast, 0 to 255. */
    private int[] ringLevel = new int[0];
    /** The alpha of the glow of each ring of the light being cast, 0 for a ring with no glow. */
    private int[] ringGlowAlpha = new int[0];
    /** The glow of each ring of the light being cast as premultiplied ARGB over no glow at all. */
    private int[] ringGlow = new int[0];
    /** The part of the squared distance that depends on the column, for the light being cast. */
    private double[] columnPart = new double[0];

    /** What the cells of the last call were worked out from. */
    private double[] signature = new double[0];
    /** What the cells of this call depend on, built here and then compared with the last. */
    private double[] scratch = new double[0];
    /** How many numbers of {@link #signature} are valid. */
    private int signatureLength;

    /** Left column of the box of cells that holds the lights. */
    private int left;
    /** Top row of the box. */
    private int top;
    /** Column after the last of the box. */
    private int right;
    /** Row after the last of the box. */
    private int bottom;
    /** The horizontal phase of the camera grid the cells were worked out at. */
    private double phaseX;
    /** The vertical phase of the camera grid the cells were worked out at. */
    private double phaseY;
    /** Whether the last call worked the cells out again, which the caller uploads. */
    private boolean changed;
    /** How many times the cells were worked out so far. */
    private int composes;

    /** The cell value of the plain tint, which every cell of the box takes where no light reaches. */
    private int plain;

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
            levels = new int[wide * high];
            glow = new int[wide * high];
            pixels = new int[wide * high];
            signatureLength = 0;
        }
        final double snappedX = Math.rint(originX);
        final double snappedY = Math.rint(originY);
        phaseX = snappedX - Math.floor(snappedX / artScale) * artScale;
        phaseY = snappedY - Math.floor(snappedY / artScale) * artScale;
        collect(registry, gameTime, artScale, snappedX, snappedY);
        if (count == 0) {
            return false;
        }
        if (!differsFromLast(style, tint, tintStrength, artScale)) {
            return true;
        }
        work(style, tint, tintStrength);
        changed = true;
        composes++;
        return true;
    }

    /** Works out the centre cell, the effective radius and intensity and the box of every light that reaches the canvas. */
    private void collect(LightRegistry registry, double gameTime, double artScale, double snappedX, double snappedY) {
        count = 0;
        left = Integer.MAX_VALUE;
        top = Integer.MAX_VALUE;
        right = Integer.MIN_VALUE;
        bottom = Integer.MIN_VALUE;
        for (int i = 0, total = registry.size(); i < total; i++) {
            final LightSource source = registry.get(i);
            final double radius = source.radius() * source.flicker().radiusFactor(gameTime);
            final double intensity = Math.max(0.0, Math.min(1.0,
                    source.intensity() * source.flicker().intensityFactor(gameTime)));
            if (!(radius > 0.0) || intensity <= 0.0) {
                continue;
            }
            // The light shines from where an entity at its position would stand, and is placed on a whole screen pixel
            final double screenX = Math.rint((source.x() - source.y()) * TILE_ART_HALF_WIDTH * artScale + snappedX);
            final double screenY = Math.rint(((source.x() + source.y()) * TILE_ART_HALF_HEIGHT + TILE_ART_HALF_HEIGHT)
                    * artScale + snappedY);
            final double centreX = (screenX - phaseX) / artScale + 1.0;
            final double centreY = (screenY - phaseY) / artScale + 1.0;
            // A circle of the ground is an ellipse of the screen: sqrt(2) tiles along each diagonal of a tile
            final double reachX = radius * Math.sqrt(2.0) * TILE_ART_HALF_WIDTH;
            final double reachY = radius * Math.sqrt(2.0) * TILE_ART_HALF_HEIGHT;
            final int boxLeft = Math.max(0, (int) Math.floor(centreX - reachX));
            final int boxTop = Math.max(0, (int) Math.floor(centreY - reachY));
            final int boxRight = Math.min(cellsWide, (int) Math.ceil(centreX + reachX));
            final int boxBottom = Math.min(cellsHigh, (int) Math.ceil(centreY + reachY));
            if (boxRight <= boxLeft || boxBottom <= boxTop) {
                continue;
            }
            if ((count + 1) * STRIDE > lights.length) {
                lights = Arrays.copyOf(lights, lights.length * 2);
            }
            final int at = count * STRIDE;
            final Rgba colour = source.colour();
            lights[at] = centreX;
            lights[at + 1] = centreY;
            lights[at + 2] = radius;
            lights[at + 3] = intensity;
            lights[at + 4] = colour.r();
            lights[at + 5] = colour.g();
            lights[at + 6] = colour.b();
            lights[at + BOX_AT] = boxLeft;
            lights[at + BOX_AT + 1] = boxTop;
            lights[at + BOX_AT + 2] = boxRight;
            lights[at + BOX_AT + 3] = boxBottom;
            count++;
            left = Math.min(left, boxLeft);
            top = Math.min(top, boxTop);
            right = Math.max(right, boxRight);
            bottom = Math.max(bottom, boxBottom);
        }
    }

    /** Tells whether what the cells depend on differs from the last call, and remembers it. */
    private boolean differsFromLast(LightStyle style, Rgba tint, double tintStrength, double artScale) {
        final int length = count * STRIDE + 9;
        if (scratch.length < length) {
            scratch = new double[length];
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
        scratch[at++] = style.rings() * 1000.0 + style.referenceDarkness();
        scratch[at] = style.glowStrength();
        boolean same = signatureLength == length;
        for (int i = 0; same && i < length; i++) {
            same = scratch[i] == signature[i];
        }
        if (same) {
            return false;
        }
        final double[] swap = signature;
        signature = scratch;
        scratch = swap;
        signatureLength = length;
        return true;
    }

    /** Works out every cell of the box. */
    private void work(LightStyle style, Rgba tint, double tintStrength) {
        for (int y = top; y < bottom; y++) {
            Arrays.fill(levels, y * cellsWide + left, y * cellsWide + right, 0);
            Arrays.fill(glow, y * cellsWide + left, y * cellsWide + right, 0);
        }
        final double darkness = Math.min(1.0, tintStrength / style.referenceDarkness());
        for (int light = 0; light < count; light++) {
            castLight(light * STRIDE, style, darkness);
        }
        final double tintA = tintStrength;
        plain = pack(tintA * 255.0, tint.r() * tintA * 255.0, tint.g() * tintA * 255.0, tint.b() * tintA * 255.0);
        // Neighbouring cells mostly share a ring, so the last cell worked out is reused while the inputs are the same
        int lastLevel = 0;
        int lastGlow = 0;
        int lastPixel = plain;
        for (int y = top; y < bottom; y++) {
            for (int i = y * cellsWide + left, end = y * cellsWide + right; i < end; i++) {
                if (levels[i] != lastLevel || glow[i] != lastGlow) {
                    lastLevel = levels[i];
                    lastGlow = glow[i];
                    lastPixel = lastLevel == 0 && lastGlow == 0 ? plain : finish(lastLevel, lastGlow, tint, tintStrength);
                }
                pixels[i] = lastPixel;
            }
        }
    }

    /** Adds the light of one source to the levels and the glow of the cells of its box. */
    private void castLight(int at, LightStyle style, double darkness) {
        final double centreX = lights[at];
        final double centreY = lights[at + 1];
        final double radius = lights[at + 2];
        final double intensity = lights[at + 3];
        final int red = (int) Math.round(lights[at + 4] * 255.0);
        final int green = (int) Math.round(lights[at + 5] * 255.0);
        final int blue = (int) Math.round(lights[at + 6] * 255.0);
        final int boxLeft = (int) lights[at + BOX_AT];
        final int boxTop = (int) lights[at + BOX_AT + 1];
        final int boxRight = (int) lights[at + BOX_AT + 2];
        final int boxBottom = (int) lights[at + BOX_AT + 3];
        final int rings = style.rings();
        prepareRings(rings, intensity, style.glowStrength() * darkness, red, green, blue);
        // A distance on the ground in tiles, from two distances in art pixels: tiles are 32 across and 16 down, and a
        // step along a tile axis is half the sum of the squares of its two diagonal parts. The square of the share of
        // the radius is compared with the squares of the ring edges, so no square root is taken for a cell.
        final double scale = 0.5 / (radius * radius);
        final int boxWide = boxRight - boxLeft;
        if (columnPart.length < boxWide) {
            columnPart = new double[boxWide];
        }
        for (int x = 0; x < boxWide; x++) {
            final double dx = boxLeft + x + 0.5 - centreX;
            columnPart[x] = dx * dx / (TILE_ART_HALF_WIDTH * TILE_ART_HALF_WIDTH);
        }
        for (int y = boxTop; y < boxBottom; y++) {
            final double dy = y + 0.5 - centreY;
            final double downPart = dy * dy / (TILE_ART_HALF_HEIGHT * TILE_ART_HALF_HEIGHT);
            final int row = y * cellsWide;
            for (int x = 0; x < boxWide; x++) {
                final double share = (downPart + columnPart[x]) * scale;
                int ring = 0;
                while (ring < rings && share >= ringEdge[ring]) {
                    ring++;
                }
                if (ring == rings) {
                    continue;
                }
                final int level = ringLevel[ring];
                final int i = row + boxLeft + x;
                levels[i] = levels[i] == 0 ? level : 255 - ((255 - levels[i]) * (255 - level) + 127) / 255;
                if (ringGlowAlpha[ring] > 0) {
                    glow[i] = glow[i] == 0 ? ringGlow[ring]
                            : over(ringGlowAlpha[ring], red, green, blue, glow[i]);
                }
            }
        }
    }

    /**
     * Works out what every ring of a light gives a cell: the square of the share of the radius at which it ends, its
     * level of light and its glow, so a cell only looks its ring up.
     */
    private void prepareRings(int rings, double intensity, double glowStrength, int red, int green, int blue) {
        if (ringEdge.length != rings) {
            ringEdge = new double[rings];
            ringLevel = new int[rings];
            ringGlowAlpha = new int[rings];
            ringGlow = new int[rings];
        }
        for (int ring = 0; ring < rings; ring++) {
            final double edge = (ring + 1.0) / rings;
            ringEdge[ring] = edge * edge;
            // The level of the ring is the one the domain gives for a distance in the middle of it
            final double lit = Math.min(1.0, LightFalloff.level((ring + 0.5) / rings, rings) * intensity);
            ringLevel[ring] = (int) Math.round(lit * 255.0);
            final double glowShare = Math.max(0.0, (lit - GLOW_FROM) / (1.0 - GLOW_FROM));
            ringGlowAlpha[ring] = (int) Math.round(glowStrength * glowShare * 255.0);
            ringGlow[ring] = ringGlowAlpha[ring] > 0 ? over(ringGlowAlpha[ring], red, green, blue, 0) : 0;
        }
    }

    /** Lays a glow of a colour and an alpha over a premultiplied glow, as source over. */
    private static int over(int alpha, int red, int green, int blue, int below) {
        final int keep = 255 - alpha;
        final int a = alpha + ((below >>> 24) * keep + 127) / 255;
        final int r = (red * alpha + 127) / 255 + (((below >> 16) & 0xFF) * keep + 127) / 255;
        final int g = (green * alpha + 127) / 255 + (((below >> 8) & 0xFF) * keep + 127) / 255;
        final int b = (blue * alpha + 127) / 255 + ((below & 0xFF) * keep + 127) / 255;
        return pack(a, r, g, b);
    }

    /** Lays the glow of a cell over the tint that is left unlit, and returns the premultiplied cell. */
    private static int finish(int level, int glowCell, Rgba tint, double tintStrength) {
        final double tintAlpha = tintStrength * (1.0 - level / 255.0);
        final double glowAlpha = (glowCell >>> 24) / 255.0;
        final double keep = 1.0 - glowAlpha;
        final double a = (glowCell >>> 24) + tintAlpha * 255.0 * keep;
        final double r = ((glowCell >> 16) & 0xFF) + tint.r() * tintAlpha * 255.0 * keep;
        final double g = ((glowCell >> 8) & 0xFF) + tint.g() * tintAlpha * 255.0 * keep;
        final double b = (glowCell & 0xFF) + tint.b() * tintAlpha * 255.0 * keep;
        return pack(a, r, g, b);
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

    /** @return true when the last call worked the cells out again, so the caller uploads the box */
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

    /** @return the left column of the box of the lights */
    public int boxLeft() {
        return left;
    }

    /** @return the top row of the box of the lights */
    public int boxTop() {
        return top;
    }

    /** @return the column after the last of the box of the lights */
    public int boxRight() {
        return right;
    }

    /** @return the row after the last of the box of the lights */
    public int boxBottom() {
        return bottom;
    }

    /** @return the horizontal phase of the camera grid of the last call */
    public double phaseX() {
        return phaseX;
    }

    /** @return the vertical phase of the camera grid of the last call */
    public double phaseY() {
        return phaseY;
    }

    /** @return the cells as premultiplied ARGB, row by row, valid inside the box, which the caller must not change */
    public int[] pixels() {
        return pixels;
    }

    /**
     * Returns one cell of the box.
     *
     * @param x column, inside the box
     * @param y row, inside the box
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
