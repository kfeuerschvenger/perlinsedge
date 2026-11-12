package com.feuerschvenger.perlinsedge.domain.entities.containers;

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
        final int maxStack = stack.getMaxStackSize();
        int remaining = original;

        for (int i = start; i < end && remaining > 0; i++) {
            final Item existing = target.getItemAt(i);
            if (isSource(source, sourceSlot, target, i) || existing == null || existing.getType() != stack.getType()) {
                continue;
            }
            final int moved = Math.min(maxStack - existing.getQuantity(), remaining);
            if (moved > 0) {
                existing.addQuantity(moved);
                remaining -= moved;
            }
        }
        for (int i = start; i < end && remaining > 0; i++) {
            if (!isSource(source, sourceSlot, target, i) && target.getItemAt(i) == null) {
                final int placed = Math.min(maxStack, remaining);
                target.setItemAt(i, new Item(stack.getType(), placed));
                remaining -= placed;
            }
        }

        if (remaining == original) {
            return false;
        }
        if (remaining <= 0) {
            source.setItemAt(sourceSlot, null);
        } else {
            stack.setQuantity(remaining);
        }
        return true;
    }

    private static boolean isSource(Container source, int sourceSlot, Container target, int slot) {
        return source == target && slot == sourceSlot;
    }
}
