package com.feuerschvenger.perlinsedge.domain.entities.containers;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;

/**
 * A reference to one slot of a container: which container and which slot index in it.
 * Used where an action targets a slot rather than an item instance, such as throwing items out of a slot.
 *
 * @param container the container that owns the slot
 * @param slot      zero-based slot index
 */
public record SlotRef(Container container, int slot) {

    /**
     * Reads the stack that lies in the slot.
     *
     * @return the stack in the slot, or null when the slot is empty or does not exist
     */
    public Item item() {
        return container == null ? null : container.getItemAt(slot);
    }
}
