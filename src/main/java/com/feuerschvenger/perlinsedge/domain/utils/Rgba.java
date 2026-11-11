package com.feuerschvenger.perlinsedge.domain.utils;

/**
 * Immutable framework-free colour value with components in the range 0.0 to 1.0.
 * The domain describes colours with this type, and the rendering layer converts it to its own colour class.
 *
 * @param r red component
 * @param g green component
 * @param b blue component
 * @param a opacity, where 1.0 is fully opaque
 */
public record Rgba(double r, double g, double b, double a) {

    private static final double DARKER_BRIGHTER_FACTOR = 0.7;

    /** Named colours used by the domain. */
    public static final Rgba GRAY = rgb(128, 128, 128);
    public static final Rgba BROWN = rgb(165, 42, 42);
    public static final Rgba DARK_GOLDENROD = rgb(184, 134, 11);
    public static final Rgba DARK_BLUE = rgb(0, 0, 139);
    public static final Rgba DARK_RED = rgb(139, 0, 0);
    public static final Rgba DIM_GRAY = rgb(105, 105, 105);
    public static final Rgba PERU = rgb(205, 133, 63);
    public static final Rgba ORANGE = rgb(255, 165, 0);
    public static final Rgba RED = rgb(255, 0, 0);
    public static final Rgba LIGHT_PINK = rgb(255, 182, 193);
    public static final Rgba LIGHT_GREEN = rgb(144, 238, 144);
    public static final Rgba LIGHT_BLUE = rgb(173, 216, 230);
    public static final Rgba MAGENTA = rgb(255, 0, 255);
    public static final Rgba YELLOW = rgb(255, 255, 0);
    public static final Rgba LIME_GREEN = rgb(50, 205, 50);

    /**
     * Validates that every component lies in 0.0 to 1.0.
     *
     * @throws IllegalArgumentException if a component is out of range
     */
    public Rgba {
        if (!inRange(r) || !inRange(g) || !inRange(b) || !inRange(a)) {
            throw new IllegalArgumentException("Colour components must be within 0.0 and 1.0");
        }
    }

    /**
     * Creates an opaque colour from 8-bit channels.
     *
     * @param red   red channel, 0 to 255
     * @param green green channel, 0 to 255
     * @param blue  blue channel, 0 to 255
     * @return the colour
     */
    public static Rgba rgb(int red, int green, int blue) {
        return new Rgba(red / 255.0, green / 255.0, blue / 255.0, 1.0);
    }

    /**
     * Returns a lighter version of this colour, with the same rule used by the JavaFX palette.
     *
     * @return the lighter colour
     */
    public Rgba brighter() {
        return scaleBrightness(1.0 / DARKER_BRIGHTER_FACTOR);
    }

    /**
     * Returns a darker version of this colour.
     *
     * @return the darker colour
     */
    public Rgba darker() {
        return scaleBrightness(DARKER_BRIGHTER_FACTOR);
    }

    /**
     * Scales the brightness (HSB value) of this colour, keeping hue and saturation.
     * Pure black brightened becomes a very dark grey, as in the JavaFX palette.
     *
     * @param factor brightness multiplier, the resulting brightness is capped at 1.0
     * @return the scaled colour
     */
    public Rgba scaleBrightness(double factor) {
        double max = Math.max(r, Math.max(g, b));
        if (max == 0.0) {
            if (factor <= 1.0) {
                return this;
            }
            double v = Math.min(1.0, 0.05 * factor);
            return new Rgba(v, v, v, a);
        }
        double scale = Math.min(1.0, max * factor) / max;
        return new Rgba(clamp(r * scale), clamp(g * scale), clamp(b * scale), a);
    }

    /**
     * Multiplies the opacity of this colour.
     *
     * @param factor opacity multiplier, results are clamped to the valid range
     * @return the colour with the new opacity
     */
    public Rgba withOpacityFactor(double factor) {
        return new Rgba(r, g, b, clamp(a * factor));
    }

    private static boolean inRange(double v) {
        return v >= 0.0 && v <= 1.0;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
