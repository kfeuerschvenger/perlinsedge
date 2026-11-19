package com.feuerschvenger.perlinsedge.domain.crafting.processing;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that makes one kind of processing station differ from another: what it processes, what it burns, how it
 * spends the fuel and how much of each role it holds. The stations share one engine and are told apart by this table.
 * An item is either an input or a fuel of a station, never both: when it is listed as both, it counts as an input.
 *
 * @param recipes     what the station processes, in the order it looks for something to work on
 * @param fuels       the items the station burns, in the order it takes them
 * @param fuelMode    how the fuel is spent
 * @param inputCap    units held of all the input items together, the cap they share
 * @param fuelCap     units held of all the fuel items together, the cap they share
 * @param burnSeconds seconds one unit of fuel burns, only used by {@link FuelMode#BURNING}
 */
public record StationProfile(List<ProcessingRecipe> recipes, List<ItemType> fuels, FuelMode fuelMode, int inputCap,
                             int fuelCap, double burnSeconds) {

    /** Copies the lists, so a profile cannot be changed after it is made. */
    public StationProfile {
        recipes = List.copyOf(recipes);
        fuels = List.copyOf(fuels);
    }

    /**
     * Gives the input items of the station.
     *
     * @return each input once, in the order of the recipes
     */
    public List<ItemType> inputs() {
        final List<ItemType> inputs = new ArrayList<>();
        for (ProcessingRecipe recipe : recipes) {
            if (!inputs.contains(recipe.input())) {
                inputs.add(recipe.input());
            }
        }
        return inputs;
    }

    /**
     * Gives the fuel items of the station, which are the burnt items that are not inputs.
     *
     * @return each fuel once, in the order the station takes them
     */
    public List<ItemType> fuelItems() {
        final List<ItemType> fuelItems = new ArrayList<>();
        for (ItemType fuel : fuels) {
            if (!isInput(fuel) && !fuelItems.contains(fuel)) {
                fuelItems.add(fuel);
            }
        }
        return fuelItems;
    }

    /**
     * Tells whether the station takes an item as an input.
     *
     * @param type the item
     * @return true when a recipe processes it
     */
    public boolean isInput(ItemType type) {
        return recipes.stream().anyMatch(recipe -> recipe.input() == type);
    }

    /**
     * Tells whether the station takes an item as fuel. An input is never fuel.
     *
     * @param type the item
     * @return true when the station burns it and does not process it
     */
    public boolean isFuel(ItemType type) {
        return !isInput(type) && fuels.contains(type);
    }

    /**
     * Gives how many units of an item the station holds at most.
     *
     * @param type the item
     * @return the cap, 0 for an item the station does not take
     */
    public int capOf(ItemType type) {
        if (isInput(type)) {
            return inputCap;
        }
        return isFuel(type) ? fuelCap : 0;
    }

    /**
     * Gives every item the station takes.
     *
     * @return the inputs, then the fuels
     */
    public List<ItemType> accepted() {
        final List<ItemType> accepted = new ArrayList<>(inputs());
        for (ItemType fuel : fuels) {
            if (!accepted.contains(fuel)) {
                accepted.add(fuel);
            }
        }
        return accepted;
    }
}
