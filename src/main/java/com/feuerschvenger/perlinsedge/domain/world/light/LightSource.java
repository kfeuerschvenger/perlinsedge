package com.feuerschvenger.perlinsedge.domain.world.light;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;

/**
 * Something that gives light: a held torch, a placed torch, a campfire, a furnace, a fire arrow or a spell. A feature
 * that gives light registers a source in the {@link LightRegistry} and the renderer lights the world from it, so the
 * feature needs to know nothing about how the light is drawn.
 * <p>
 * The methods are plain reads that allocate nothing, since the renderer asks every source on every frame. A source may
 * move or change between frames, and the renderer takes the values of the moment.
 */
public interface LightSource {

    /**
     * Returns where the light shines from along X, in tile coordinates and in the same convention as the position of an
     * entity: a light at the position of an entity shines from its feet, and a light at whole numbers shines from the
     * centre of that tile.
     *
     * @return the tile X
     */
    double x();

    /**
     * Returns where the light shines from along Y, in tile coordinates, see {@link #x()}.
     *
     * @return the tile Y
     */
    double y();

    /**
     * Returns how far the light reaches on the ground.
     *
     * @return the radius in tiles, above zero, before the flicker moves it
     */
    double radius();

    /**
     * Returns the colour the light gives to what it falls on. A light whose flicker drifts its colour uses the colours of
     * the flicker instead, and this one is what a steady light keeps.
     *
     * @return the colour, whose opacity is not used
     */
    Rgba colour();

    /**
     * Returns how strongly the light pushes back the dark at its brightest.
     *
     * @return the intensity from 0 to 1, before the flicker moves it
     */
    double intensity();

    /**
     * Returns how the light wavers.
     *
     * @return the flicker, {@link Flicker#STEADY} for a light that does not
     */
    Flicker flicker();
}
