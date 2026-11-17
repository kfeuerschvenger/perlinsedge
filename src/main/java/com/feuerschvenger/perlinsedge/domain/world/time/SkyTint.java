package com.feuerschvenger.perlinsedge.domain.world.time;

import com.feuerschvenger.perlinsedge.domain.utils.ColourChannels;

/**
 * The tint of the sky at the moment the clock stands at: a colour and how strongly it is laid over the world. The
 * {@link DayCycle} keeps one instance and works it out in place when the clock moves, so asking for the tint of a frame
 * allocates nothing even in the middle of a transition. The values are only good until the clock moves again, so a
 * caller reads them in the frame it asked in and does not keep the object.
 */
public final class SkyTint implements ColourChannels {
    private double r;
    private double g;
    private double b;
    private double strength;

    /** Package private: only the cycle that owns the instance changes it. */
    SkyTint() {
    }

    /**
     * Sets the tint.
     *
     * @param r        red channel
     * @param g        green channel
     * @param b        blue channel
     * @param strength how strongly the tint is laid over the world
     */
    void set(double r, double g, double b, double strength) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.strength = strength;
    }

    @Override
    public double r() {
        return r;
    }

    @Override
    public double g() {
        return g;
    }

    @Override
    public double b() {
        return b;
    }

    /** @return how strongly the sky tint is laid over the world, 0 at midday and the most at night */
    public double strength() {
        return strength;
    }
}
