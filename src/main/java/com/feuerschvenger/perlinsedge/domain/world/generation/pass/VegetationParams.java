package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How thickly trees grow, and how much thicker they grow beside a river.
 *
 * @param density    the share of the tree noise range, from 0 to 1, in which a tree grows on fertile ground: 0 grows
 *                   none and 1 grows one on every fertile tile
 * @param waterReach how many tiles from a river the trees grow thicker, at least 0; 0 turns the bonus off
 * @param waterBonus how much of the tree noise range, from 0 to 1, is added to the density within the reach of a
 *                   river, so the trees there form the gallery forest along the water
 */
public record VegetationParams(float density, int waterReach, float waterBonus) {

    /** Checks the density and the bonus are shares and the reach is not negative. */
    public VegetationParams {
        if (Float.isNaN(density) || density < 0F || density > 1F) {
            throw new IllegalArgumentException("the tree density must be from 0 to 1: " + density);
        }
        if (waterReach < 0) {
            throw new IllegalArgumentException("the water reach must not be negative: " + waterReach);
        }
        if (Float.isNaN(waterBonus) || waterBonus < 0F || waterBonus > 1F) {
            throw new IllegalArgumentException("the water bonus must be from 0 to 1: " + waterBonus);
        }
    }
}
