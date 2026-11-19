package com.feuerschvenger.perlinsedge.domain.crafting.processing;

/**
 * How a processing station spends its fuel.
 */
public enum FuelMode {
    /** Fuel is used up when a product is finished, one unit for the number of products the recipe says. */
    PER_OUTPUT,
    /** Fuel burns away with time, whether or not anything is being processed, and a recipe waits while it is out. */
    BURNING
}
