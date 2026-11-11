package com.feuerschvenger.perlinsedge.ui.view.callbacks;

import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;

/**
 * Functional interface for throwing items out of an inventory slot into the world.
 */
@FunctionalInterface
public interface ItemDropCallback {

    /**
     * Throws items out of a slot.
     *
     * @param slot       the slot that holds the stack
     * @param wholeStack true to throw the whole stack, false to throw one item
     * @return true when something was thrown
     */
    boolean drop(SlotRef slot, boolean wholeStack);

}
