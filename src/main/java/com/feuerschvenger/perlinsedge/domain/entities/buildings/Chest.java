package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a chest building that provides storage functionality.
 * Items live in a fixed number of slots, an empty slot holds null, so a slot index keeps its meaning
 * when other slots are emptied.
 */
@Slf4j
public class Chest extends Building implements Container {
    private final List<Item> slots;
    @Getter
    private final int capacity;

    /**
     * Creates a new chest at the specified tile position.
     *
     * @param tileX    X-coordinate in tile units
     * @param tileY    Y-coordinate in tile units
     * @param capacity Maximum number of item stacks the chest can hold
     */
    public Chest(int tileX, int tileY, int capacity) {
        super(tileX, tileY, BuildingType.CHEST, true, true);
        this.slots = new ArrayList<>(Collections.nCopies(capacity, null));
        this.capacity = capacity;
    }

    // ==================================================================
    //  Container Interface Implementation
    // ==================================================================

    @Override
    public List<Item> getItems() {
        return new ArrayList<>(slots); // Defensive copy, one entry per slot
    }

    @Override
    public Item getItemAt(int slotIndex) {
        if (!isValidSlot(slotIndex)) return null;
        return slots.get(slotIndex);
    }

    @Override
    public void setItemAt(int slotIndex, Item item) {
        if (!isValidSlot(slotIndex)) return;
        slots.set(slotIndex, item);
    }

    @Override
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) return false;

        int originalQuantity = item.getQuantity();
        int remaining = originalQuantity;

        for (int i = 0; i < capacity && remaining > 0; i++) {
            Item existing = slots.get(i);
            if (canStack(existing, item)) {
                int transfer = Math.min(existing.getMaxStackSize() - existing.getQuantity(), remaining);
                existing.addQuantity(transfer);
                remaining -= transfer;
            }
        }

        for (int i = 0; i < capacity && remaining > 0; i++) {
            if (slots.get(i) == null) {
                int stackSize = Math.min(remaining, item.getMaxStackSize());
                slots.set(i, new Item(item.getType(), stackSize));
                remaining -= stackSize;
            }
        }

        item.setQuantity(remaining);
        return remaining < originalQuantity;
    }

    @Override
    public boolean removeItem(Item itemToRemove, int quantity) {
        if (itemToRemove == null || quantity <= 0) return false;
        if (getItemCount(itemToRemove) < quantity) return false;

        int remaining = quantity;
        for (int i = 0; i < capacity && remaining > 0; i++) {
            Item item = slots.get(i);
            if (item != null && item.getType() == itemToRemove.getType()) {
                int toRemove = Math.min(item.getQuantity(), remaining);
                item.removeQuantity(toRemove);
                remaining -= toRemove;

                if (item.isEmpty()) {
                    slots.set(i, null);
                }
            }
        }

        return true;
    }

    @Override
    public int getItemCount(Item item) {
        if (item == null) return 0;
        return slots.stream()
                .filter(i -> i != null && i.getType() == item.getType())
                .mapToInt(Item::getQuantity)
                .sum();
    }

    @Override
    public boolean hasSpace(Item item) {
        if (item == null) return false;

        boolean hasStackSpace = slots.stream().anyMatch(i -> canStack(i, item));
        return hasStackSpace || hasAvailableSlot();
    }

    @Override
    public boolean canTakeItem(Item item, int quantity) {
        return getItemCount(item) >= quantity;
    }

    @Override
    public void setItems(List<Item> items) {
        Collections.fill(slots, null);
        for (int i = 0; i < items.size() && i < capacity; i++) {
            Item source = items.get(i);
            if (source != null && source.getQuantity() > 0) {
                slots.set(i, new Item(source.getType(), Math.min(source.getQuantity(), source.getMaxStackSize())));
            }
        }
    }

    // ==================================================================
    //  Interaction Methods
    // ==================================================================

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean onInteract(Tile tile) {
        log.debug("Interacted with Chest at ({}, {}). Opening inventory.", tileX, tileY);
        return true;
    }

    // ==================================================================
    //  Inventory Helper Methods
    // ==================================================================

    /**
     * Checks if an item can be stacked onto an existing stack.
     */
    private boolean canStack(Item existing, Item newItem) {
        return existing != null &&
                newItem != null &&
                existing.getType() == newItem.getType() &&
                existing.getQuantity() < existing.getMaxStackSize();
    }

    /**
     * Checks if the chest has an empty slot.
     */
    private boolean hasAvailableSlot() {
        return slots.contains(null);
    }

    /**
     * Validates a slot index.
     */
    private boolean isValidSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < capacity;
    }

}
