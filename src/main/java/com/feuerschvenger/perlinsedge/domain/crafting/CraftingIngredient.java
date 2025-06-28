package com.feuerschvenger.perlinsedge.domain.crafting;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

/**
 * Represents a crafting ingredient, which consists of an item type and a quantity.
 * The quantity must be positive.
 */
public class CraftingIngredient {
    private final ItemType itemType;
    private final int quantity;

    /**
     * Constructs a CraftingIngredient with the specified item type and quantity.
     * @param itemType The type of item required for crafting.
     * @param quantity The amount of the item required, must be positive.
     * @throws IllegalArgumentException if quantity is not positive.
     */
    public CraftingIngredient(ItemType itemType, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("The amount must be positive for a crafting ingredient.");
        }
        this.itemType = itemType;
        this.quantity = quantity;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDisplayName() {
        return itemType.getDisplayName();
    }

    @Override
    public String toString() {
        return quantity + " x " + itemType.getDisplayName();
    }

}