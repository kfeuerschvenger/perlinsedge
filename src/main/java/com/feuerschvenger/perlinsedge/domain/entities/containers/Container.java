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
     * @return A copy of the current inventory with one entry per slot, null for an empty slot
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
     * Replaces the entire inventory contents, slot by slot.
     * Entry {@code i} of the list becomes slot {@code i}, so slot positions are kept and nothing is compacted.
     * Each entry is copied, never stored, and the given items are never modified, unlike {@link #addItem(Item)}.
     * Null and empty entries leave their slot empty, a stack larger than the maximum stack size is cut to it,
     * and entries beyond the capacity are ignored.
     *
     * @param items New contents, one entry per slot
     */
    void setItems(List<Item> items);

    // ==================================================================
    //  Inventory Modification Methods
    // ==================================================================

    /**
     * Attempts to add an item to the container.
     * Mutates its argument: the quantity of the given item is reduced by the amount that was stored, so what is
     * left is what did not fit. Callers that need the original quantity afterwards must pass a copy.
     * Items are stored as copies in empty slots, and stacks that already hold the type are filled first.
     *
     * @param item Item to add (non-null, positive quantity)
     * @return True if any portion was added, false if no space
     */
    boolean addItem(Item item);

    /**
     * Attempts to remove a quantity of an item from the container.
     * All or nothing: when the container holds less than the requested quantity, nothing is removed.
     *
     * @param itemToRemove Type of item to remove
     * @param quantity Number of items to remove (positive)
     * @return True if the full quantity was removed, false if nothing was removed
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