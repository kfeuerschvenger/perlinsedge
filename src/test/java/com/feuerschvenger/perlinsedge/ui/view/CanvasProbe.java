package com.feuerschvenger.perlinsedge.ui.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

/**
 * Renders drawing code onto a transparent canvas and answers questions about the pixels it painted. The questions
 * are about presence in a region, never about exact antialiased edges, so they hold on every JavaFX pipeline.
 */
public final class CanvasProbe {

    private static final int ALPHA_SHIFT = 24;
    private static final int BYTE_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    private final int width;
    private final int height;
    private final int[] argb;

    private CanvasProbe(int width, int height, int[] argb) {
        this.width = width;
        this.height = height;
        this.argb = argb;
    }

    /**
     * Runs the drawing on the JavaFX thread against a transparent canvas and captures the result.
     *
     * @param width  canvas width in pixels
     * @param height canvas height in pixels
     * @param draw   the drawing code
     * @return the captured pixels
     */
    public static CanvasProbe render(int width, int height, Consumer<GraphicsContext> draw) {
        return FxToolkitSupport.onFx(() -> {
            final Canvas canvas = new Canvas(width, height);
            draw.accept(canvas.getGraphicsContext2D());
            final WritableImage image = canvas.snapshot(transparent(), null);
            return capture(image, width, height);
        });
    }

    private static SnapshotParameters transparent() {
        final SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return params;
    }

    private static CanvasProbe capture(WritableImage image, int width, int height) {
        final PixelReader reader = image.getPixelReader();
        final int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = reader.getArgb(x, y);
            }
        }
        return new CanvasProbe(width, height, pixels);
    }

    /**
     * Counts the pixels of a region that are not fully transparent.
     *
     * @param x0 left edge, inclusive
     * @param y0 top edge, inclusive
     * @param x1 right edge, exclusive
     * @param y1 bottom edge, exclusive
     * @return how many pixels have a visible alpha
     */
    public int painted(int x0, int y0, int x1, int y1) {
        int count = 0;
        for (int y = Math.max(0, y0); y < Math.min(height, y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(width, x1); x++) {
                if ((argb[y * width + x] >>> ALPHA_SHIFT) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Counts every painted pixel of the canvas.
     *
     * @return how many pixels have a visible alpha
     */
    public int painted() {
        return painted(0, 0, width, height);
    }

    /**
     * Counts the pixels of a region whose colour is within a tolerance of the given one and that are opaque enough
     * to show it.
     *
     * @param color     the colour looked for
     * @param tolerance largest allowed difference per channel, 0 to 255
     * @param x0        left edge, inclusive
     * @param y0        top edge, inclusive
     * @param x1        right edge, exclusive
     * @param y1        bottom edge, exclusive
     * @return how many pixels match
     */
    public int near(Color color, int tolerance, int x0, int y0, int x1, int y1) {
        final int r = (int) Math.round(color.getRed() * BYTE_MASK);
        final int g = (int) Math.round(color.getGreen() * BYTE_MASK);
        final int b = (int) Math.round(color.getBlue() * BYTE_MASK);
        int count = 0;
        for (int y = Math.max(0, y0); y < Math.min(height, y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(width, x1); x++) {
                final int p = argb[y * width + x];
                if ((p >>> ALPHA_SHIFT) >= BYTE_MASK - tolerance
                        && Math.abs(((p >> RED_SHIFT) & BYTE_MASK) - r) <= tolerance
                        && Math.abs(((p >> GREEN_SHIFT) & BYTE_MASK) - g) <= tolerance
                        && Math.abs((p & BYTE_MASK) - b) <= tolerance) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Counts the pixels of the whole canvas close to a colour.
     *
     * @param color     the colour looked for
     * @param tolerance largest allowed difference per channel
     * @return how many pixels match
     */
    public int near(Color color, int tolerance) {
        return near(color, tolerance, 0, 0, width, height);
    }

    /**
     * Finds the highest painted row.
     *
     * @return the row of the first painted pixel from the top, or -1 when nothing is painted
     */
    public int firstPaintedRow() {
        for (int y = 0; y < height; y++) {
            if (painted(0, y, width, y + 1) > 0) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Reads one pixel.
     *
     * @param x column
     * @param y row
     * @return the pixel as ARGB
     */
    public int argb(int x, int y) {
        return argb[y * width + x];
    }

    /**
     * Whether another capture holds the same pixels.
     *
     * @param other the other capture
     * @return true when every pixel is equal
     */
    public boolean sameAs(CanvasProbe other) {
        return java.util.Arrays.equals(argb, other.argb);
    }
}
