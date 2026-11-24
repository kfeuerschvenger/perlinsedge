package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How thickly trees grow.
 *
 * @param density the share of the tree noise range, from 0 to 1, in which a tree grows on fertile ground: 0 grows
 *                none and 1 grows one on every fertile tile
 */
public record VegetationParams(float density) {

    /** Checks the density is a share. */
    public VegetationParams {
        if (Float.isNaN(density) || density < 0F || density > 1F) {
            throw new IllegalArgumentException("the tree density must be from 0 to 1: " + density);
        }
    }
}
