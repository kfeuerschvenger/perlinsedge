package com.feuerschvenger.perlinsedge.domain.crafting;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.Objects;

/**
 * Represents a single ingredient required for crafting, consisting of an item type and quantity.
 * Immutable by design to ensure consistency in crafting recipes.
 *
 * <p>Key features:
 * <ul>
 *   <li>Enforces positive quantity constraint</li>
 *   <li>Provides display-ready representations</li>
 *   <li>Null-safe item type validation</li>
 *   <li>Immutable value object</li>
 * </ul>
 *
 * @param itemType The type of item required (must not be null)
 * @param quantity The amount of items required (must be positive)
 */
public record CraftingIngredient(ItemType itemType, int quantity) {

    /**
     * Compact constructor for validation and normalization.
     *
     * @param itemType Item type reference
     * @param quantity Required quantity
     * @throws NullPointerException if itemType is null
     * @throws IllegalArgumentException if quantity is not positive
     */
    public CraftingIngredient {
        // Validate parameters
        Objects.requireNonNull(itemType, "Item type must not be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid quantity: %d. Quantity must be positive.", quantity)
            );
        }
    }

    // Display Methods -----------------------------------------------------------

    /**
     * Retrieves the display name of the ingredient's item type.
     *
     * @return Localized or formatted display name
     */
    public String getDisplayName() {
        return itemType.getDisplayName();
    }

    /**
     * Returns a human-readable representation of the ingredient.
     * Format: "{quantity} x {itemDisplayName}"
     *
     * @return Formatted ingredient description
     */
    @Override
    public String toString() {
        return String.format("%d x %s", quantity, itemType.getDisplayName());
    }

    // Equality Contract ---------------------------------------------------------

    /**
     * Generated equals method respects record semantics, comparing both components.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CraftingIngredient that = (CraftingIngredient) o;
        return quantity == that.quantity && itemType == that.itemType;
    }

    /**
     * Generated hashCode method consistent with equals implementation.
     */
    @Override
    public int hashCode() {
        return Objects.hash(itemType, quantity);
    }

}