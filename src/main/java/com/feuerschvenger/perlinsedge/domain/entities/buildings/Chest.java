package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a chest building that provides storage functionality.
 * Implements the Container interface for item storage management.
 */
@Slf4j
public class Chest extends Building implements Container {
    private final List<Item> inventory;
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
        this.inventory = new ArrayList<>();
        this.capacity = capacity;
    }

    // ==================================================================
    //  Container Interface Implementation
    // ==================================================================

    @Override
    public List<Item> getItems() {
        return new ArrayList<>(inventory); // Defensive copy
    }

    @Override
    public Item getItemAt(int slotIndex) {
        if (!isValidSlot(slotIndex)) return null;
        return inventory.get(slotIndex);
    }

    @Override
    public void setItemAt(int slotIndex, Item item) {
        if (!isValidSlot(slotIndex)) return;

        if (slotIndex < inventory.size()) {
            inventory.set(slotIndex, item);
        } else if (slotIndex == inventory.size() && hasAvailableSlot()) {
            inventory.add(item);
        }
    }

    @Override
    public boolean addItem(Item item) {
        if (item == null || item.getQuantity() <= 0) return false;

        // Try to stack with existing items
        int remaining = stackIntoExistingItems(item);
        if (remaining == 0) return true;

        // Add to new slot if space available
        return addToNewSlot(new Item(item.getType(), remaining));
    }

    @Override
    public boolean removeItem(Item itemToRemove, int quantity) {
        if (itemToRemove == null || quantity <= 0) return false;

        int remaining = quantity;
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            Item item = inventory.get(i);
            if (item.getType() == itemToRemove.getType()) {
                int toRemove = Math.min(item.getQuantity(), remaining);
                item.removeQuantity(toRemove);
                remaining -= toRemove;

                if (item.isEmpty()) {
                    inventory.remove(i);
                    i--; // Adjust index after removal
                }
            }
        }

        return remaining == 0;
    }

    @Override
    public int getItemCount(Item item) {
        return inventory.stream()
                .filter(i -> i.getType() == item.getType())
                .mapToInt(Item::getQuantity)
                .sum();
    }

    @Override
    public boolean hasSpace(Item item) {
        // Check for stack space
        boolean hasStackSpace = inventory.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getType() == item.getType())
                .anyMatch(i -> i.getQuantity() < i.getMaxStackSize());

        return hasStackSpace || hasAvailableSlot();
    }

    @Override
    public boolean canTakeItem(Item item, int quantity) {
        return getItemCount(item) >= quantity;
    }

    @Override
    public void setItems(List<Item> items) {
        this.inventory.clear();
        items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getQuantity() > 0)
                .forEach(this::addItem);
    }

    // ==================================================================
    //  Interaction Methods
    // ==================================================================

    @Override
    public boolean onInteract(Tile tile) {
        log.debug("Interacted with Chest at ({}, {}). Opening inventory.", tileX, tileY);
        return true;
    }

    // ==================================================================
    //  Inventory Helper Methods
    // ==================================================================

    /**
     * Attempts to stack items into existing stacks.
     *
     * @param item Item to stack
     * @return Remaining quantity after stacking
     */
    private int stackIntoExistingItems(Item item) {
        int remaining = item.getQuantity();

        for (Item existing : inventory) {
            if (canStack(existing, item)) {
                int availableSpace = existing.getMaxStackSize() - existing.getQuantity();
                int toTransfer = Math.min(availableSpace, remaining);

                existing.addQuantity(toTransfer);
                remaining -= toTransfer;

                if (remaining <= 0) break;
            }
        }
        return remaining;
    }

    /**
     * Adds an item to a new slot.
     *
     * @param item Item to add
     * @return True if successfully added
     */
    private boolean addToNewSlot(Item item) {
        if (!hasAvailableSlot()) return false;
        if (item.getQuantity() <= 0) return false;

        int stackSize = Math.min(item.getQuantity(), item.getMaxStackSize());
        inventory.add(new Item(item.getType(), stackSize));
        return true;
    }

    /**
     * Checks if two items can be stacked together.
     */
    private boolean canStack(Item existing, Item newItem) {
        return existing != null &&
                newItem != null &&
                existing.getType() == newItem.getType() &&
                existing.getQuantity() < existing.getMaxStackSize();
    }

    /**
     * Checks if the chest has an available slot.
     */
    private boolean hasAvailableSlot() {
        return inventory.size() < capacity;
    }

    /**
     * Validates a slot index.
     */
    private boolean isValidSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < capacity;
    }

}
