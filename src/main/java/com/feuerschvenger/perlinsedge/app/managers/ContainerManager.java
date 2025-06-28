package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;

/**
 * Manages interactions with various item containers in the game, such as player inventory and chests.
 * This class facilitates opening, closing, and transferring items between containers.
 */
public class ContainerManager {
    private Container openContainer; // The container currently open in the UI

    public ContainerManager() {
        this.openContainer = null;
    }

    /**
     * Opens a specific container in the UI.
     * @param container The container to open.
     */
    public void openContainer(Container container) {
        if (container == null) {
            System.err.println("ContainerManager: Cannot open null container.");
            return;
        }
        this.openContainer = container;
        System.out.println("Container " + container.getClass().getSimpleName() + " opened.");
        // UI logic for displaying the container would be in InventoryPanel or similar
    }

    /**
     * Closes the currently open container.
     */
    public void closeContainer() {
        if (openContainer != null) {
            System.out.println("Container " + openContainer.getClass().getSimpleName() + " closed.");
            this.openContainer = null;
        }
    }

    /**
     * Checks if any container is currently open.
     * @return True if a container is open, false otherwise.
     */
    public boolean isContainerOpen() {
        return openContainer != null;
    }

    /**
     * Gets the currently open container.
     * @return The open Container, or null if none is open.
     */
    public Container getOpenContainer() {
        return openContainer;
    }

    /**
     * Transfers an item from one container to another.
     *
     * @param source         The container to take the item from.
     * @param destination    The container to add the item to.
     * @param itemToTransfer The item (and its type) to transfer.
     * @param quantity       The quantity of the item to transfer.
     */
    public void transferItem(Container source, Container destination, Item itemToTransfer, int quantity) {
        if (source == null || destination == null || itemToTransfer == null || quantity <= 0) {
            System.err.println("ContainerManager: Invalid transfer parameters.");
            return;
        }

        // Check if source has enough quantity
        if (!source.canTakeItem(itemToTransfer, quantity)) {
            System.out.println("ContainerManager: Source does not have enough " + itemToTransfer.getDisplayName() + ".");
            return;
        }

        // Create a temporary item with the quantity to transfer
        Item actualItemToTransfer = new Item(itemToTransfer.getType(), quantity);

        // Check if source has enough quantity
        if (!source.canTakeItem(actualItemToTransfer, quantity)) {
            System.out.println("ContainerManager: Source does not have enough " + itemToTransfer.getDisplayName() + " to transfer " + quantity + ".");
            return;
        }

        // Attempt to add to destination. addItem returns true if *any* part was added.
        boolean added = destination.addItem(actualItemToTransfer);

        // If something was added, remove that quantity from the source
        if (added) {
            int quantityActuallyAdded = quantity - actualItemToTransfer.getQuantity(); // Remaining quantity in actualItemToTransfer
            if (quantityActuallyAdded > 0) {
                source.removeItem(itemToTransfer, quantityActuallyAdded);
                System.out.println("ContainerManager: Transferred " + quantityActuallyAdded + "x " + itemToTransfer.getDisplayName() +
                        " from " + source.getClass().getSimpleName() + " to " + destination.getClass().getSimpleName());
                return;
            }
        }
        System.out.println("ContainerManager: Failed to transfer " + quantity + "x " + itemToTransfer.getDisplayName() +
                " from " + source.getClass().getSimpleName() + " to " + destination.getClass().getSimpleName());
    }

    public void transferBetweenSlots(Container source, int sourceSlot, Container destination, int destinationSlot, Item item) {
        System.out.println("TRANSFER OPERATION: " + item.getDisplayName() +
                " from slot " + sourceSlot + " to " + destinationSlot);

        if (source == null || destination == null) {
            System.err.println("TRANSFER ERROR: Invalid containers");
            return;
        }

        // Obtener ítem real del slot de origen
        Item sourceItem = source.getItemAt(sourceSlot);
        if (sourceItem == null) {
            System.err.println("TRANSFER ERROR: Source item is null");
            return;
        }

        // Verificar coincidencia (usar ID o tipo para evitar problemas de referencia)
        if (sourceItem.getType() != item.getType()) {
            System.err.println("TRANSFER ERROR: Source item mismatch");
            return;
        }

        Item destItem = destination.getItemAt(destinationSlot);
        System.out.println("SOURCE ITEM: " + sourceItem.getDisplayName() + " x" + sourceItem.getQuantity());
        System.out.println("DEST ITEM: " + (destItem != null ?
                destItem.getDisplayName() + " x" + destItem.getQuantity() : "Empty"));

        if (destItem == null) {
            // FIX: Ensure we're moving the exact instance
            destination.setItemAt(destinationSlot, sourceItem);
            source.setItemAt(sourceSlot, null);
            System.out.println("TRANSFER: Moved to empty slot");
        } else if (canStack(sourceItem, destItem)) {
            // Calculate stackable amount
            int maxStack = destItem.getMaxStackSize();
            int spaceAvailable = maxStack - destItem.getQuantity();
            int transferAmount = Math.min(spaceAvailable, sourceItem.getQuantity());

            System.out.println("STACKING: " + transferAmount + " items");

            // Update quantities
            destItem.addQuantity(transferAmount);
            sourceItem.removeQuantity(transferAmount);

            // Clear source slot if empty
            if (sourceItem.getQuantity() <= 0) {
                source.setItemAt(sourceSlot, null);
                System.out.println("CLEARED source slot");
            }
        } else {
            // FIX: Full swap including item instances
            System.out.println("SWAPPING items");

            // Preserve source item reference
            Item temp = sourceItem;

            // Perform swap
            source.setItemAt(sourceSlot, destItem);
            destination.setItemAt(destinationSlot, temp);
        }

        System.out.println("TRANSFER COMPLETE");
    }

    private boolean canStack(Item item1, Item item2) {
        return item1.getType() == item2.getType() &&
                item2.getQuantity() < item2.getMaxStackSize();
    }

}
