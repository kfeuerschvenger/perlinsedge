package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item; /**
 * Functional interface for handling item transfers between containers.
 * This is used for drag-and-drop actions within the InventoryPanel.
 */
@FunctionalInterface
public interface ItemTransferCallback {

    void transfer(
            int sourceSlotIndex,
            Container sourceContainer,
            int targetSlotIndex,
            Container targetContainer,
            Item item
    );

}
