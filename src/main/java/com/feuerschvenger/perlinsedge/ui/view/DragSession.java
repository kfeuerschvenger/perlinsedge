package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;

import java.util.Objects;

/**
 * One stack being dragged out of an inventory slot, and what happens to it when the mouse button is released.
 * The stack stays in its slot while it is dragged, so it can never be lost halfway: a drag that is cancelled leaves
 * everything where it was. The session knows nothing about JavaFX, the view tells it where the mouse was released.
 */
final class DragSession {

    /** What a release of the mouse button does with the dragged stack. */
    enum Release {
        /** Nothing: the stack goes back where it was. */
        CANCEL,
        /** The stack moves to, stacks with or swaps with the slot it was released over. */
        TRANSFER,
        /** The stack is thrown into the world: it was released anywhere that is not a slot. */
        DROP_STACK
    }

    private final SlotRef origin;
    private final Item item;

    /**
     * Starts a drag of the stack in a slot.
     *
     * @param origin the slot that holds the stack, which must not be empty
     * @throws NullPointerException     if the slot is null
     * @throws IllegalArgumentException if the slot is empty
     */
    DragSession(SlotRef origin) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.item = origin.item();
        if (item == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Nothing to drag in slot " + origin.slot());
        }
    }

    /** @return the slot the stack is dragged from */
    SlotRef origin() {
        return origin;
    }

    /** @return the dragged stack */
    Item item() {
        return item;
    }

    /**
     * Tells whether the dragged stack is still in its slot.
     * It is not when something else emptied or replaced the slot while the mouse button was held, for example a pickup
     * from the ground or the death of the player.
     *
     * @return true while the slot still holds this very stack
     */
    boolean isStillValid() {
        return origin.item() == item && item.getQuantity() > 0;
    }

    /**
     * Decides what a release does.
     *
     * @param target the slot the mouse was released over, or null when it was released anywhere else
     * @return the outcome
     */
    Release release(SlotRef target) {
        if (!isStillValid()) {
            return Release.CANCEL;
        }
        if (target == null) {
            return Release.DROP_STACK;
        }
        if (target.container() == origin.container() && target.slot() == origin.slot()) {
            return Release.CANCEL;
        }
        return Release.TRANSFER;
    }
}
