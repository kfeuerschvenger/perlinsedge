package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * A light that stays where it is, such as a placed torch or a campfire. It is the value to register for anything that
 * does not move.
 *
 * @param x         where the light shines from along X in tile coordinates, see {@link LightSource#x()}
 * @param y         where the light shines from along Y in tile coordinates
 * @param radius    how far the light reaches on the ground in tiles, above zero
 * @param colour    the colour the light gives to what it falls on
 * @param intensity how strongly the light pushes back the dark at its brightest, 0 to 1
 * @param flicker   how the light wavers
 */
public record PointLight(double x, double y, double radius, Rgba colour, double intensity, Flicker flicker)
        implements LightSource {

    /**
     * Validates the light.
     *
     * @throws IllegalArgumentException if the radius is not above zero, the intensity is outside 0 to 1, or the colour
     *                                  or the flicker is missing
     */
    public PointLight {
        if (!(radius > 0.0)) {
            throw new IllegalArgumentException("A light needs a radius above zero");
        }
        if (!(intensity >= 0.0 && intensity <= 1.0)) {
            throw new IllegalArgumentException("The intensity of a light must be from 0 to 1");
        }
        if (colour == null || flicker == null) {
            throw new IllegalArgumentException("A light needs a colour and a flicker");
        }
    }
}
