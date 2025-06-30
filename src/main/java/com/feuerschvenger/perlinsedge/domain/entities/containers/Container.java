package com.feuerschvenger.perlinsedge.domain.entities.containers;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import java.util.List;

/**
 * Defines the contract for game entities capable of storing items.
 * Implementations manage item storage operations including addition, removal,
 * and inventory management. Used by chests, player inventories, and other storage entities.
 */
public interface Container {

    // ==================================================================
    //  Inventory Access Methods
    // ==================================================================

    /**
     * Retrieves all items in the container.
     *
     * @return A copy of the current inventory (prevents external modification)
     */
    List<Item> getItems();

    /**
     * Retrieves the item at a specific inventory slot.
     *
     * @param slotIndex Zero-based slot position
     * @return Item at specified position, or null if empty/invalid
     */
    Item getItemAt(int slotIndex);

    /**
     * Replaces the item at a specific inventory slot.
     *
     * @param slotIndex Zero-based slot position
     * @param item New item to place (null clears the slot)
     */
    void setItemAt(int slotIndex, Item item);

    /**
     * Replaces the entire inventory contents.
     *
     * @param items New collection of items (filters null/empty items)
     */
    void setItems(List<Item> items);

    // ==================================================================
    //  Inventory Modification Methods
    // ==================================================================

    /**
     * Attempts to add an item to the container.
     *
     * @param item Item to add (non-null, positive quantity)
     * @return True if any portion was added, false if no space
     */
    boolean addItem(Item item);

    /**
     * Attempts to remove a quantity of an item from the container.
     *
     * @param itemToRemove Type of item to remove
     * @param quantity Number of items to remove (positive)
     * @return True if full quantity was removed, false otherwise
     */
    boolean removeItem(Item itemToRemove, int quantity);

    // ==================================================================
    //  Inventory Query Methods
    // ==================================================================

    /**
     * Gets total count of a specific item type.
     *
     * @param item Type of item to count
     * @return Total quantity across all slots
     */
    int getItemCount(Item item);

    /**
     * Gets maximum number of item slots.
     *
     * @return Total capacity (slot count)
     */
    int getCapacity();

    /**
     * Checks if there's space to add at least one unit of an item.
     *
     * @param item Item type to check
     * @return True if space is available (stack or empty slot)
     */
    boolean hasSpace(Item item);

    /**
     * Checks if a specific quantity can be removed.
     *
     * @param item Item type to check
     * @param quantity Number of items to remove
     * @return True if container has sufficient quantity
     */
    boolean canTakeItem(Item item, int quantity);

}