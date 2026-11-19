package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

import java.util.function.BooleanSupplier;

/**
 * The light of a building that stands on the map: a standing torch, a campfire, a furnace or a stone oven. It shines
 * from the centre of the tile of the building and is registered once when the building is placed and taken out when
 * it is demolished. Whether it shines is asked of the building at every read, so it follows the state of the building
 * with nothing to keep in step: while it is off, its radius and its intensity are zero, which the renderer drops
 * before it evaluates anything else.
 */
public final class BuildingLight implements LightSource {

    /** Where the light shines from along X, the tile of the building. */
    private final double x;
    /** Where the light shines from along Y, the tile of the building. */
    private final double y;
    /** How far the light reaches on the ground in tiles while it is on. */
    private final double radius;
    /** The colour the light gives to what it falls on. */
    private final Rgba colour;
    /** How strongly the light pushes back the dark at its brightest while it is on, 0 to 1. */
    private final double intensity;
    /** How the light wavers. */
    private final Flicker flicker;
    /** Tells whether the light is on, which is a plain read of the state of the building. */
    private final BooleanSupplier lit;

    /**
     * Creates the light of a building.
     *
     * @param tileX     the tile X of the building
     * @param tileY     the tile Y of the building
     * @param radius    how far the light reaches on the ground in tiles while on, above zero
     * @param colour    the colour the light gives to what it falls on
     * @param intensity how strongly the light pushes back the dark at its brightest, 0 to 1
     * @param flicker   how the light wavers
     * @param lit       tells whether the light is on
     * @throws IllegalArgumentException if the radius is not above zero, the intensity is outside 0 to 1, or the colour,
     *                                  the flicker or the supplier is missing
     */
    public BuildingLight(int tileX, int tileY, double radius, Rgba colour, double intensity, Flicker flicker,
                         BooleanSupplier lit) {
        if (Double.isNaN(radius) || radius <= 0.0) {
            throw new IllegalArgumentException("A light needs a radius above zero");
        }
        if (!(intensity >= 0.0 && intensity <= 1.0)) {
            throw new IllegalArgumentException("The intensity of a light must be from 0 to 1");
        }
        if (colour == null || flicker == null || lit == null) {
            throw new IllegalArgumentException("A light needs a colour, a flicker and a state");
        }
        this.x = tileX;
        this.y = tileY;
        this.radius = radius;
        this.colour = colour;
        this.intensity = intensity;
        this.flicker = flicker;
        this.lit = lit;
    }

    /** @return true while the light is on */
    public boolean isOn() {
        return lit.getAsBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public double x() {
        return x;
    }

    /** {@inheritDoc} */
    @Override
    public double y() {
        return y;
    }

    /** {@inheritDoc} Zero while the light is off. */
    @Override
    public double radius() {
        return isOn() ? radius : 0.0;
    }

    /** {@inheritDoc} */
    @Override
    public Rgba colour() {
        return colour;
    }

    /** {@inheritDoc} Zero while the light is off. */
    @Override
    public double intensity() {
        return isOn() ? intensity : 0.0;
    }

    /** {@inheritDoc} */
    @Override
    public Flicker flicker() {
        return flicker;
    }
}
