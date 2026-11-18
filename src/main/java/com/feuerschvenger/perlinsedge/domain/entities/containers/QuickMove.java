package com.feuerschvenger.perlinsedge.domain.entities.containers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;

/**
 * The quick move of a stack with Ctrl+click: the whole stack goes to a range of slots of a container, into the stacks
 * of the same type first and then into the first free slots, in index order. What does not fit stays where it was.
 */
public final class QuickMove {

    private QuickMove() {
        throw new AssertionError("QuickMove is a utility class and should not be instantiated");
    }

    /**
     * Moves the stack of a slot the way a Ctrl+click does, which depends on what is open.
     * With no other container open a hotbar slot sends its stack to the inventory and an inventory slot sends it to
     * the hotbar. With a container such as a chest open, a slot of the player sends its stack to the container and a
     * slot of the container sends it to the player, in the fill order of the player: matching stacks first, then the
     * first free hotbar slot, then the first free inventory slot.
     *
     * @param player the player
     * @param other  the open container, or null when there is none
     * @param source the container that holds the stack, the player or the open container
     * @param slot   the slot of the stack
     * @return true when at least one item moved
     */
    public static boolean route(Player player, Container other, Container source, int slot) {
        if (player == null || source == null) {
            return false;
        }
        if (source != player) {
            return source == other && move(source, slot, player, 0, player.getCapacity());
        }
        if (other != null) {
            return move(player, slot, other, 0, other.getCapacity());
        }
        return slot < Player.HOTBAR_SIZE
                ? move(player, slot, player, Player.HOTBAR_SIZE, player.getCapacity())
                : move(player, slot, player, 0, Player.HOTBAR_SIZE);
    }

    /**
     * Moves the stack of a slot into a range of slots of a container.
     * The stack merges into the stacks of its type in the range first, then fills the free slots of the range from the
     * first one. When the target is the source container the source slot is skipped, so a stack never merges into itself.
     * A stack that does not fit completely is left in its slot with the quantity that did not fit.
     *
     * @param source     the container that holds the stack
     * @param sourceSlot the slot of the stack
     * @param target     the container that receives the stack
     * @param from       first slot of the range, inclusive
     * @param to         end of the range, exclusive, cut to the capacity of the target
     * @return true when at least one item moved
     */
    public static boolean move(Container source, int sourceSlot, Container target, int from, int to) {
        final Item stack = source == null ? null : source.getItemAt(sourceSlot);
        if (stack == null || stack.getQuantity() <= 0 || target == null) {
            return false;
        }
        final int end = Math.min(to, target.getCapacity());
        final int start = Math.max(from, 0);
        final int original = stack.getQuantity();
        int remaining = mergeIntoMatchingStacks(source, sourceSlot, target, start, end, original);
        remaining = fillFreeSlots(source, sourceSlot, target, start, end, remaining);

        if (remaining == original) {
            return false;
        }
        if (remaining <= 0) {
            source.setItemAt(sourceSlot, null);
        } else {
            stack.setQuantity(remaining);
            source.noteStackChanged();
        }
        return true;
    }

    /**
     * Tops up the stacks of the same type in the range, skipping the source slot.
     *
     * @return the quantity that still has to be placed
     */
    private static int mergeIntoMatchingStacks(Container source, int sourceSlot, Container target,
                                               int start, int end, int quantity) {
        final Item stack = source.getItemAt(sourceSlot);
        final int maxStack = stack.getMaxStackSize();
        int remaining = quantity;
        for (int i = start; i < end && remaining > 0; i++) {
            final Item existing = target.getItemAt(i);
            if (isSource(source, sourceSlot, target, i) || existing == null || existing.getType() != stack.getType()) {
                continue;
            }
            final int moved = Math.min(maxStack - existing.getQuantity(), remaining);
            if (moved > 0) {
                existing.addQuantity(moved);
                target.noteStackChanged();
                remaining -= moved;
            }
        }
        return remaining;
    }

    /**
     * Places what is left into the free slots of the range, skipping the source slot.
     *
     * @return the quantity that did not fit
     */
    private static int fillFreeSlots(Container source, int sourceSlot, Container target,
                                     int start, int end, int quantity) {
        final Item stack = source.getItemAt(sourceSlot);
        final int maxStack = stack.getMaxStackSize();
        int remaining = quantity;
        for (int i = start; i < end && remaining > 0; i++) {
            if (!isSource(source, sourceSlot, target, i) && target.getItemAt(i) == null) {
                final int placed = Math.min(maxStack, remaining);
                target.setItemAt(i, new Item(stack.getType(), placed));
                remaining -= placed;
            }
        }
        return remaining;
    }

    private static boolean isSource(Container source, int sourceSlot, Container target, int slot) {
        return source == target && slot == sourceSlot;
    }
}
