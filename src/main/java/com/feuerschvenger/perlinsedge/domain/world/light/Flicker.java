package com.feuerschvenger.perlinsedge.domain.world.light;

/**
 * How a light source wavers, as a function of the game time alone, so a given seed and moment always give the same
 * answer. A steady light never changes. A crackling light mixes three sine waves of related but not harmonic
 * frequencies, which gives a smooth, irregular wavering with no jump and no repeat that the eye picks out, where white
 * noise would look like static. One wave drives the intensity of the light and another set of phases drives its radius,
 * so the two move independently.
 * <p>
 * The value type is immutable. The phases are worked out once from the seed, and asking for a factor allocates nothing.
 */
public final class Flicker {

    /** Frequencies of the three waves as multiples of the base frequency: not harmonics, so the mix does not repeat. */
    private static final double[] RATIOS = {1.0, 1.73, 2.91};

    /** How much each wave counts in the mix. The weights add up to one, so the mix stays between minus one and one. */
    private static final double[] WEIGHTS = {0.5, 0.3, 0.2};

    /** The number of independent channels a flicker has: the intensity and the radius. */
    private static final int CHANNELS = 2;

    /** A light that never wavers. It is declared after the constants above, which its construction reads. */
    public static final Flicker STEADY = new Flicker(0.0, 0.0, 1.0, 0L);

    /** The most the intensity of the light moves away from its value, as a share of it, 0 for none. */
    private final double intensityAmplitude;
    /** The most the radius of the light moves away from its value, as a share of it, 0 for none. */
    private final double radiusAmplitude;
    /** The frequency of the slowest wave in cycles per second. */
    private final double frequencyHz;
    /** The phase of each wave of each channel in radians, worked out from the seed. */
    private final double[][] phases = new double[CHANNELS][RATIOS.length];

    /**
     * Creates a flicker.
     *
     * @param intensityAmplitude the most the intensity moves, as a share of it, from 0 up to but excluding 1
     * @param radiusAmplitude    the most the radius moves, as a share of it, from 0 up to but excluding 1
     * @param frequencyHz        the frequency of the slowest wave in cycles per second, above zero
     * @param seed               what the phases are worked out from, so two lights with different seeds do not flicker
     *                           in step
     * @throws IllegalArgumentException if an amplitude is outside its range or the frequency is not above zero
     */
    private Flicker(double intensityAmplitude, double radiusAmplitude, double frequencyHz, long seed) {
        if (!(intensityAmplitude >= 0.0 && intensityAmplitude < 1.0)
                || !(radiusAmplitude >= 0.0 && radiusAmplitude < 1.0)) {
            throw new IllegalArgumentException("A flicker amplitude must be from 0 up to but excluding 1");
        }
        if (!(frequencyHz > 0.0)) {
            throw new IllegalArgumentException("A flicker frequency must be above zero");
        }
        this.intensityAmplitude = intensityAmplitude;
        this.radiusAmplitude = radiusAmplitude;
        this.frequencyHz = frequencyHz;
        for (int channel = 0; channel < CHANNELS; channel++) {
            for (int wave = 0; wave < RATIOS.length; wave++) {
                phases[channel][wave] = 2.0 * Math.PI * unit(seed, channel * RATIOS.length + wave);
            }
        }
    }

    /**
     * Creates a wavering light.
     *
     * @param intensityAmplitude the most the intensity moves, as a share of it, from 0 up to but excluding 1
     * @param radiusAmplitude    the most the radius moves, as a share of it, from 0 up to but excluding 1
     * @param frequencyHz        the frequency of the slowest wave in cycles per second, above zero
     * @param seed               what the phases are worked out from
     * @return the flicker
     * @throws IllegalArgumentException if an amplitude is outside its range or the frequency is not above zero
     */
    public static Flicker crackling(double intensityAmplitude, double radiusAmplitude, double frequencyHz, long seed) {
        return new Flicker(intensityAmplitude, radiusAmplitude, frequencyHz, seed);
    }

    /** @return true when the light never wavers */
    public boolean isSteady() {
        return intensityAmplitude == 0.0 && radiusAmplitude == 0.0;
    }

    /** @return the most the intensity moves, as a share of it */
    public double intensityAmplitude() {
        return intensityAmplitude;
    }

    /** @return the most the radius moves, as a share of it */
    public double radiusAmplitude() {
        return radiusAmplitude;
    }

    /** @return the frequency of the slowest wave in cycles per second */
    public double frequencyHz() {
        return frequencyHz;
    }

    /**
     * Returns what to multiply the intensity of the light by at a moment.
     *
     * @param time the game time in seconds
     * @return a factor from {@code 1 - intensityAmplitude} to {@code 1 + intensityAmplitude}
     */
    public double intensityFactor(double time) {
        return 1.0 + intensityAmplitude * wave(0, time);
    }

    /**
     * Returns what to multiply the radius of the light by at a moment.
     *
     * @param time the game time in seconds
     * @return a factor from {@code 1 - radiusAmplitude} to {@code 1 + radiusAmplitude}
     */
    public double radiusFactor(double time) {
        return 1.0 + radiusAmplitude * wave(1, time);
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
