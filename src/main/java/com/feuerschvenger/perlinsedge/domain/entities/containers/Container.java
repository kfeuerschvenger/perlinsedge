package com.feuerschvenger.perlinsedge.domain.entities.containers;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import java.util.List;

/**
 * Interface for any game entity that can contain items (e.g., chests, inventory).
 */
public interface Container {
    List<Item> getItems();
    boolean addItem(Item item);
    boolean removeItem(Item item, int quantity);
    int getItemCount(Item item);
    int getCapacity(); // Max number of slots/stacks
    boolean hasSpace(Item item); // Checks if there's space for this item (stackable or new slot)
    boolean canTakeItem(Item item, int quantity); // Checks if item and quantity can be removed
    void setItems(List<Item> items); // For setting initial inventory or loading saved state
    Item getItemAt(int slotIndex);
    void setItemAt(int slotIndex, Item item);
}
