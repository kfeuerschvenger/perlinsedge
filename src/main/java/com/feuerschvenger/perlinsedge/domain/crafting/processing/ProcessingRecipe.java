package com.feuerschvenger.perlinsedge.domain.crafting.processing;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

/**
 * One thing a processing station turns into another: a unit of the input becomes a unit of the output after a time.
 *
 * @param input          the item the station takes, one unit for each product
 * @param output         the item that drops in front of the station when the time is up
 * @param seconds        how long one unit takes
 * @param outputsPerFuel how many products one unit of fuel pays for, 0 for a recipe that needs no fuel
 */
public record ProcessingRecipe(ItemType input, ItemType output, double seconds, int outputsPerFuel) {

    /** @return true when the station must have fuel to work on this recipe */
    public boolean needsFuel() {
        return outputsPerFuel > 0;
    }
}
