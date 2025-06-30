package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;

/**
 * Manages container interactions including opening/closing containers and item transfers.
 * Supports both bulk quantity transfers and slot-based transfers between containers.
 */
public class ContainerManager {
    private Container openContainer; // Currently active container in UI

    public ContainerManager() {
        this.openContainer = null;
    }

    /**
     * Opens a container for interaction.
     * @param container Container to open (must not be null)
     */
    public void openContainer(Container container) {
        if (container == null) {
            logError("Cannot open null container");
            return;
        }

        openContainer = container;
        logAction("Opened container: " + container.getClass().getSimpleName());
    }

    /**
     * Closes the currently open container if any.
     */
    public void closeContainer() {
        if (openContainer != null) {
            logAction("Closed container: " + openContainer.getClass().getSimpleName());
            openContainer = null;
        }
    }

    /**
     * Checks if a container is currently open.
     * @return true if a container is open, false otherwise
     */
    public boolean isContainerOpen() {
        return openContainer != null;
    }

    /**
     * Retrieves the currently open container.
     * @return Open container instance or null if none
     */
    public Container getOpenContainer() {
        return openContainer;
    }

    /**
     * Transfers items between specific slots in two containers.
     * Handles moving to empty slots, stacking, and swapping items.
     *
     * @param source Source container
     * @param sourceSlot Source slot index
     * @param destination Destination container
     * @param destinationSlot Destination slot index
     * @param item Item being transferred (reference for validation)
     */
    public void transferBetweenSlots(Container source, int sourceSlot,
                                     Container destination, int destinationSlot,
                                     Item item) {
        if (!validateSlotTransferParameters(source, sourceSlot, destination, item)) {
            return;
        }

        Item sourceItem = source.getItemAt(sourceSlot);
        Item destItem = destination.getItemAt(destinationSlot);

        logTransferOperation(sourceItem, sourceSlot, destItem, destinationSlot);

        if (destItem == null) {
            moveToEmptySlot(source, sourceSlot, destination, destinationSlot, sourceItem);
        } else if (canStack(sourceItem, destItem)) {
            stackItems(source, sourceSlot, sourceItem, destItem);
        } else {
            swapItems(source, sourceSlot, destination, destinationSlot, sourceItem, destItem);
        }
    }

    /**
     * Moves an item to an empty destination slot.
     */
    private void moveToEmptySlot(Container source, int sourceSlot,
                                 Container destination, int destinationSlot,
                                 Item sourceItem) {
        destination.setItemAt(destinationSlot, sourceItem);
        source.setItemAt(sourceSlot, null);
        logAction("Moved to empty slot");
    }

    /**
     * Stacks items between slots when possible.
     */
    private void stackItems(Container source, int sourceSlot, Item sourceItem, Item destItem) {
        int maxStack = destItem.getMaxStackSize();
        int availableSpace = maxStack - destItem.getQuantity();
        int transferAmount = Math.min(availableSpace, sourceItem.getQuantity());

        destItem.addQuantity(transferAmount);
        sourceItem.removeQuantity(transferAmount);

        if (sourceItem.getQuantity() <= 0) {
            source.setItemAt(sourceSlot, null);
            logAction("Cleared source slot after stacking");
        }
        logAction("Stacked " + transferAmount + " items");
    }

    /**
     * Swaps items between slots when types differ or stacks are full.
     */
    private void swapItems(Container source, int sourceSlot,
                           Container destination, int destinationSlot,
                           Item sourceItem, Item destItem) {
        source.setItemAt(sourceSlot, destItem);
        destination.setItemAt(destinationSlot, sourceItem);
        logAction("Swapped items");
    }

    /**
     * Validates slot-based transfer parameters.
     * @return true if parameters are valid, false otherwise
     */
    private boolean validateSlotTransferParameters(Container source, int sourceSlot, Container destination, Item item) {
        if (source == null || destination == null) {
            logError("Slot transfer failed: Null container(s)");
            return false;
        }

        Item sourceItem = source.getItemAt(sourceSlot);
        if (sourceItem == null) {
            logError("Slot transfer failed: Empty source slot");
            return false;
        }
        if (sourceItem.getType() != item.getType()) {
            logError("Slot transfer failed: Item type mismatch");
            return false;
        }
        return true;
    }

    /**
     * Checks if two items can be stacked.
     * @return true if same type and destination has stack space
     */
    private boolean canStack(Item sourceItem, Item destItem) {
        return sourceItem.getType() == destItem.getType() &&
                destItem.getQuantity() < destItem.getMaxStackSize();
    }

    /**
     * Logs a slot transfer operation details.
     */
    private void logTransferOperation(Item sourceItem, int sourceSlot,
                                      Item destItem, int destSlot) {
        System.out.printf("TRANSFER OP: %s x%d (Slot %d) -> %s (Slot %d)%n",
                sourceItem.getDisplayName(),
                sourceItem.getQuantity(),
                sourceSlot,
                destItem != null ? destItem.getDisplayName() + " x" + destItem.getQuantity() : "Empty",
                destSlot);
    }

    /**
     * Logs an action message.
     */
    private void logAction(String message) {
        System.out.println("ContainerManager: " + message);
    }

    /**
     * Logs an error message.
     */
    private void logError(String message) {
        System.err.println("ContainerManager ERROR: " + message);
    }

}