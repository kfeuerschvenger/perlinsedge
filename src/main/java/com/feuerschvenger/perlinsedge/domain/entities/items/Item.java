package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.scene.image.Image;
import java.util.Objects;

/**
 * Represents a stackable item in the game with type and quantity properties.
 * Provides core functionality for item management including quantity adjustments,
 * stacking operations, and display properties.
 */
public class Item {
    private final ItemType type;
    private int quantity;

    /**
     * Constructs a new Item instance.
     *
     * @param type     The type of item (non-null)
     * @param quantity The initial quantity (must be positive)
     * @throws IllegalArgumentException if quantity is not positive
     * @throws NullPointerException if type is null
     */
    public Item(ItemType type, int quantity) {
        this.type = Objects.requireNonNull(type, "ItemType cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Item quantity must be positive");
        }
        this.quantity = quantity;
    }

    // ==================================================================
    //  Accessor Methods
    // ==================================================================

    /** @return The item's type */
    public ItemType getType() {
        return type;
    }

    /** @return Current quantity of the item */
    public int getQuantity() {
        return quantity;
    }

    /** @return Maximum stack size for this item type */
    public int getMaxStackSize() {
        return type.getMaxStackSize();
    }

    /** @return User-friendly display name */
    public String getDisplayName() {
        return type.getDisplayName();
    }

    /** @return Item description */
    public String getDescription() {
        return type.getDescription();
    }

    /** @return Graphical representation of the item */
    public Image getSprite() {
        return SpriteManager.getInstance().getItemSprite(type).getImage();
    }

    /** @return True if quantity is zero or negative */
    public boolean isEmpty() {
        return quantity <= 0;
    }

    // ==================================================================
    //  Mutator Methods
    // ==================================================================

    /**
     * Sets a new quantity for the item.
     *
     * @param quantity New quantity (must be positive)
     * @throws IllegalArgumentException if quantity is not positive or zero
     */
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Item quantity must be positive or zero");
        }
        this.quantity = quantity;
    }

    /**
     * Adds to the current quantity.
     *
     * @param amount Quantity to add (must be positive)
     * @throws IllegalArgumentException if amount is negative
     */
    public void addQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative quantity");
        }
        quantity += amount;
    }

    /**
     * Removes from the current quantity.
     *
     * @param amount Quantity to remove (must be positive)
     * @throws IllegalArgumentException if amount is negative
     */
    public void removeQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot remove negative quantity");
        }
        quantity = Math.max(0, quantity - amount);
    }

    // ==================================================================
    //  Object Overrides
    // ==================================================================

    /**
     * Items are considered equal if they share the same type.
     *
     * @param obj Object to compare
     * @return True if items have the same type
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Item other = (Item) obj;
        return type == other.type;
    }

    /** @return Hash code based on item type */
    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    /** @return Human-readable representation: "Qty x DisplayName" */
    @Override
    public String toString() {
        return String.format("%dx %s", quantity, type.getDisplayName());
    }

}