package com.feuerschvenger.perlinsedge.ui.view.callbacks;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;

/**
 * Functional interface for handling item drops outside the inventory panel.
 */
@FunctionalInterface
public interface ItemDroppedOutsideCallback {

    void drop(Item item);

}
