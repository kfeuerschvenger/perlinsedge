package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

/**
 * How thickly trees grow, and how much thicker they grow beside a river.
 *
 * @param density    from 0 to 1: with {@code relative} false, the share of the tree noise range in which a tree grows
 *                   on fertile ground, so 0 grows none and 1 grows one on every fertile tile; with {@code relative}
 *                   true, the share of the land that the trees cover, taken from the places where the tree noise is
 *                   highest and as far as the fertile ground that is free goes
 * @param waterReach how many tiles from a river the trees grow thicker, at least 0; 0 turns the bonus off
 * @param waterBonus how much of the tree noise range, from 0 to 1, is added to the density within the reach of a
 *                   river, so the trees there form the gallery forest along the water
 * @param relative   true when the density is a share of the land, so a map holds the same share of trees whatever
 *                   its noise, its size and the clearings it reserved, and false when it is a share of the noise
 *                   range, which a small map or an unlucky seed can fill with far more or fewer trees
 */
public record VegetationParams(float density, int waterReach, float waterBonus, boolean relative) {

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

    /**
     * Creates the parameters of a density that is a share of the tree noise range.
     *
     * @param density    the share of the noise range in which a tree grows
     * @param waterReach how many tiles from a river the trees grow thicker
     * @param waterBonus how much of the noise range is added to the density near a river
     */
    public VegetationParams(float density, int waterReach, float waterBonus) {
        this(density, waterReach, waterBonus, false);
    }
}
