package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * How a light source wavers, as a function of the game time alone, so a given seed and moment always give the same
 * answer. A steady light never changes. A crackling light mixes sine waves of related but not harmonic frequencies,
 * which gives a smooth, irregular wavering with no jump and no repeat that the eye picks out, where white noise would
 * look like static. Each thing that wavers has phases of its own, so they move independently:
 * <ul>
 * <li>the intensity and the radius crackle a few times a second;</li>
 * <li>the colour drifts slowly between two colours, with a small fast jitter on top, when a colour drift is set;</li>
 * <li>the shape of the light, when a shape is set, makes the radius vary with the angle around the source in a few
 * low lobes that turn and change slowly, so the edge of the light breathes instead of being a perfect ellipse.</li>
 * </ul>
 * The value type is immutable. The phases are worked out once from the seed, and asking for a factor allocates nothing.
 */
public final class Flicker {

    /** Frequencies of the waves of a channel as multiples of its base frequency: not harmonics, so a mix never repeats. */
    private static final double[] RATIOS = {1.0, 1.73, 2.91};

    /** How much each wave counts in the mix. The weights add up to one, so the mix stays between minus one and one. */
    private static final double[] WEIGHTS = {0.5, 0.3, 0.2};

    /** The slow waves of the colour drift as multiples of its frequency, and how much each counts. */
    private static final double[] COLOUR_RATIOS = {1.0, 1.37};
    /** How much each slow wave of the colour counts, of the part that is not jitter. */
    private static final double[] COLOUR_WEIGHTS = {0.7, 0.3};
    /** The share of the colour drift that is a small fast jitter on top of the slow drift. */
    private static final double COLOUR_JITTER_SHARE = 0.15;
    /** The frequency of the colour jitter as a multiple of the frequency of the slow drift. */
    private static final double COLOUR_JITTER_RATIO = 7.0;

    /** How many lobes each harmonic of the shape has around the source. */
    private static final int[] SHAPE_LOBES = {1, 2, 3};
    /** How much each harmonic of the shape counts. The weights add up to one. */
    private static final double[] SHAPE_WEIGHTS = {0.5, 0.3, 0.2};
    /** How fast each harmonic of the shape turns as a multiple of the drift frequency, in either direction. */
    private static final double[] SHAPE_DRIFT = {0.37, -0.53, 0.71};

    /** The index of the channel of the intensity in the phases. */
    private static final int INTENSITY = 0;
    /** The index of the channel of the radius in the phases. */
    private static final int RADIUS = 1;
    /** The index of the channel of the colour in the phases. */
    private static final int COLOUR = 2;
    /** The index of the channel of the shape in the phases. */
    private static final int SHAPE = 3;
    /** The number of independent channels a flicker has. */
    private static final int CHANNELS = 4;

    /** A light that never wavers. It is declared after the constants above, which its construction reads. */
    public static final Flicker STEADY = new Flicker(0.0, 0.0, 1.0, 0L, 0.0, 1.0, null, null, 1.0);

    /** The most the intensity of the light moves away from its value, as a share of it, 0 for none. */
    private final double intensityAmplitude;
    /** The most the radius of the light moves away from its value, as a share of it, 0 for none. */
    private final double radiusAmplitude;
    /** The frequency of the slowest wave of the crackle in cycles per second. */
    private final double frequencyHz;
    /** What the phases were worked out from. */
    private final long seed;
    /** The most the radius varies with the angle, as a share of it, 0 for a round light. */
    private final double shapeAmplitude;
    /** How fast the lobes of the shape turn, in turns per second. */
    private final double shapeDriftHz;
    /** The colour at one end of the colour drift, or null for a light that keeps its colour. */
    private final Rgba colourLow;
    /** The colour at the other end of the colour drift, or null for a light that keeps its colour. */
    private final Rgba colourHigh;
    /** The frequency of the slow colour drift in cycles per second. */
    private final double colourHz;
    /** The phase of each wave of each channel in radians, worked out from the seed. */
    private final double[][] phases = new double[CHANNELS][RATIOS.length];

    /**
     * Creates a flicker.
     *
     * @throws IllegalArgumentException if an amplitude is outside its range or a frequency is not above zero
     */
    private Flicker(double intensityAmplitude, double radiusAmplitude, double frequencyHz, long seed,
                    double shapeAmplitude, double shapeDriftHz, Rgba colourLow, Rgba colourHigh, double colourHz) {
        if (!(intensityAmplitude >= 0.0 && intensityAmplitude < 1.0)
                || !(radiusAmplitude >= 0.0 && radiusAmplitude < 1.0)
                || !(shapeAmplitude >= 0.0 && shapeAmplitude < 0.5)) {
            throw new IllegalArgumentException("A flicker amplitude must be from 0 up to but excluding 1, "
                    + "and the shape amplitude below one half");
        }
        if (!(frequencyHz > 0.0) || !(shapeDriftHz > 0.0) || !(colourHz > 0.0)) {
            throw new IllegalArgumentException("A flicker frequency must be above zero");
        }
        if ((colourLow == null) != (colourHigh == null)) {
            throw new IllegalArgumentException("A colour drift needs both of its colours");
        }
        this.intensityAmplitude = intensityAmplitude;
        this.radiusAmplitude = radiusAmplitude;
        this.frequencyHz = frequencyHz;
        this.seed = seed;
        this.shapeAmplitude = shapeAmplitude;
        this.shapeDriftHz = shapeDriftHz;
        this.colourLow = colourLow;
        this.colourHigh = colourHigh;
        this.colourHz = colourHz;
        for (int channel = 0; channel < CHANNELS; channel++) {
            for (int wave = 0; wave < RATIOS.length; wave++) {
                phases[channel][wave] = 2.0 * Math.PI * unit(seed, channel * RATIOS.length + wave);
            }
        }
    }

    /**
     * Creates a wavering light that keeps its colour and its round shape.
     *
     * @param intensityAmplitude the most the intensity moves, as a share of it, from 0 up to but excluding 1
     * @param radiusAmplitude    the most the radius moves, as a share of it, from 0 up to but excluding 1
     * @param frequencyHz        the frequency of the slowest wave in cycles per second, above zero
     * @param seed               what the phases are worked out from
     * @return the flicker
     * @throws IllegalArgumentException if an amplitude is outside its range or the frequency is not above zero
     */
    public static Flicker crackling(double intensityAmplitude, double radiusAmplitude, double frequencyHz, long seed) {
        return new Flicker(intensityAmplitude, radiusAmplitude, frequencyHz, seed, 0.0, 1.0, null, null, 1.0);
    }

    /**
     * Returns this flicker with a shape: the radius varies with the angle around the source.
     *
     * @param amplitude the most the radius varies with the angle, as a share of it, from 0 up to but excluding one half
     * @param driftHz   how fast the lobes turn, in turns per second, above zero
     * @return the flicker with the shape
     * @throws IllegalArgumentException if the amplitude is outside its range or the drift is not above zero
     */
    public Flicker withShape(double amplitude, double driftHz) {
        return new Flicker(intensityAmplitude, radiusAmplitude, frequencyHz, seed, amplitude, driftHz, colourLow,
                colourHigh, colourHz);
    }

    /**
     * Returns this flicker with a colour that drifts between two colours.
     *
     * @param low  the colour at one end of the drift
     * @param high the colour at the other end of the drift
     * @param hz   the frequency of the slow drift in cycles per second, above zero
     * @return the flicker with the colour drift
     * @throws IllegalArgumentException if a colour is missing or the frequency is not above zero
     */
    public Flicker withColourDrift(Rgba low, Rgba high, double hz) {
        if (low == null || high == null) {
            throw new IllegalArgumentException("A colour drift needs both of its colours");
        }
        return new Flicker(intensityAmplitude, radiusAmplitude, frequencyHz, seed, shapeAmplitude, shapeDriftHz, low,
                high, hz);
    }

    /** @return true when the light never wavers in any way */
    public boolean isSteady() {
        return intensityAmplitude == 0.0 && radiusAmplitude == 0.0 && shapeAmplitude == 0.0 && colourLow == null;
    }

    /** @return the most the intensity moves, as a share of it */
    public double intensityAmplitude() {
        return intensityAmplitude;
    }

    /** @return the most the radius moves, as a share of it */
    public double radiusAmplitude() {
        return radiusAmplitude;
    }

    /** @return the frequency of the slowest wave of the crackle in cycles per second */
    public double frequencyHz() {
        return frequencyHz;
    }

    /** @return the most the radius varies with the angle, as a share of it, 0 for a round light */
    public double shapeAmplitude() {
        return shapeAmplitude;
    }

    /** @return true when the colour of the light drifts */
    public boolean hasColourDrift() {
        return colourLow != null;
    }

    /** @return the colour at one end of the drift, or null when the colour does not drift */
    public Rgba colourLow() {
        return colourLow;
    }

    /** @return the colour at the other end of the drift, or null when the colour does not drift */
    public Rgba colourHigh() {
        return colourHigh;
    }

    /**
     * Returns what to multiply the intensity of the light by at a moment.
     *
     * @param time the game time in seconds
     * @return a factor from {@code 1 - intensityAmplitude} to {@code 1 + intensityAmplitude}
     */
    public double intensityFactor(double time) {
        return 1.0 + intensityAmplitude * wave(INTENSITY, time);
    }

    /**
     * Returns what to multiply the radius of the light by at a moment.
     *
     * @param time the game time in seconds
     * @return a factor from {@code 1 - radiusAmplitude} to {@code 1 + radiusAmplitude}
     */
    public double radiusFactor(double time) {
        return 1.0 + radiusAmplitude * wave(RADIUS, time);
    }

    /**
     * Returns where the colour of the light is between the two ends of its drift: a slow drift with a small fast jitter
     * on top.
     *
     * @param time the game time in seconds
     * @return 0 for the colour at one end and 1 for the other, 0.5 for a light whose colour does not drift
     */
    public double colourShare(double time) {
        if (colourLow == null) {
            return 0.5;
        }
        double slow = 0.0;
        for (int i = 0; i < COLOUR_RATIOS.length; i++) {
            slow += COLOUR_WEIGHTS[i]
                    * Math.sin(2.0 * Math.PI * colourHz * COLOUR_RATIOS[i] * time + phases[COLOUR][i]);
        }
        final double jitter = Math.sin(2.0 * Math.PI * colourHz * COLOUR_JITTER_RATIO * time + phases[COLOUR][2]);
        return 0.5 + 0.5 * ((1.0 - COLOUR_JITTER_SHARE) * slow + COLOUR_JITTER_SHARE * jitter);
    }

    /**
     * Returns what to multiply the radius of the light by in a direction at a moment. The lobes turn and change
     * slowly, so the edge of the light breathes.
     *
     * @param turn where the direction is around the source, from 0 up to 1 for a full turn, which wraps
     * @param time the game time in seconds
     * @return a factor from {@code 1 - shapeAmplitude} to {@code 1 + shapeAmplitude}, 1 for a round light
     */
    public double shapeFactor(double turn, double time) {
        if (shapeAmplitude == 0.0) {
            return 1.0;
        }
        double mix = 0.0;
        for (int i = 0; i < SHAPE_LOBES.length; i++) {
            mix += SHAPE_WEIGHTS[i] * Math.sin(2.0 * Math.PI * (SHAPE_LOBES[i] * turn + shapeDriftHz * SHAPE_DRIFT[i] * time)
                    + phases[SHAPE][i]);
        }
        return 1.0 + shapeAmplitude * mix;
    }

    /** Mixes the three waves of a channel at a moment, giving a value from minus one to one. */
    private double wave(int channel, double time) {
        double mix = 0.0;
        for (int i = 0; i < RATIOS.length; i++) {
            mix += WEIGHTS[i] * Math.sin(2.0 * Math.PI * frequencyHz * RATIOS[i] * time + phases[channel][i]);
        }
        return mix;
    }

    /** Hashes a seed and an index to a number from 0 up to but excluding 1, with the mixing steps of splitmix64. */
    private static double unit(long seed, int index) {
        long z = seed + (index + 1) * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) * 0x1.0p-53;
    }
}
