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
        /** The stack is thrown into the world: it was released outside the panel. */
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

    /**
     * The slot the stack came from.
     *
     * @return the slot the stack is dragged from
     */
    SlotRef origin() {
        return origin;
    }

    /**
     * The stack in hand.
     *
     * @return the dragged stack
     */
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
     * @param target    the slot the mouse was released over, or null when it was released over no slot
     * @param overPanel true when the mouse was released inside the inventory panel, whether over a slot or not
     * @return the outcome: a release over the panel but over no slot is a mis-aim and cancels, only a release outside
     *         the panel throws the stack
     */
    Release release(SlotRef target, boolean overPanel) {
        if (!isStillValid()) {
            return Release.CANCEL;
        }
        if (target == null) {
            return overPanel ? Release.CANCEL : Release.DROP_STACK;
        }
        if (target.container() == origin.container() && target.slot() == origin.slot()) {
            return Release.CANCEL;
        }
        return Release.TRANSFER;
    }
}
