package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInventoryTest {

    private static final int STACK = ItemType.STONE.getMaxStackSize();
    private static final int SLOTS = Player.HOTBAR_SIZE + Player.INVENTORY_SIZE;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
    }

    private int count(ItemType type) {
        return player.getItemCount(new Item(type, 1));
    }

    private int usedSlots() {
        return (int) player.getItems().stream().filter(i -> i != null).count();
    }

    @Test
    void startsEmptyWithTheNineHotbarSlotsAndEighteenInventorySlots() {
        assertEquals(9, Player.HOTBAR_SIZE);
        assertEquals(18, Player.INVENTORY_SIZE);
        assertEquals(SLOTS, player.getCapacity());
        assertEquals(0, usedSlots());
    }

    @Test
    void anItemMergesIntoAnInventoryStackBeforeItTakesAFreeHotbarSlot() {
        player.setItemAt(Player.HOTBAR_SIZE + 2, new Item(ItemType.STONE, 5));

        player.addItem(new Item(ItemType.STONE, 3));

        assertNull(player.getItemAt(0), "the free hotbar slot is only used when no stack can take the item");
        assertEquals(8, player.getItemAt(Player.HOTBAR_SIZE + 2).getQuantity());
    }

    @Test
    void aStackOfTheHotbarIsFilledBeforeOneOfTheInventory() {
        player.setItemAt(4, new Item(ItemType.STONE, 5));
        player.setItemAt(Player.HOTBAR_SIZE, new Item(ItemType.STONE, 5));

        player.addItem(new Item(ItemType.STONE, 3));

        assertEquals(8, player.getItemAt(4).getQuantity());
        assertEquals(5, player.getItemAt(Player.HOTBAR_SIZE).getQuantity());
    }

    @Test
    void aNewTypeTakesTheFirstFreeHotbarSlotThenTheFirstFreeInventorySlot() {
        for (int slot = 0; slot < Player.HOTBAR_SIZE - 1; slot++) {
            player.setItemAt(slot, new Item(ItemType.STONE, 1));
        }

        player.addItem(new Item(ItemType.STICK, 1));
        player.addItem(new Item(ItemType.COAL, 1));

        assertEquals(ItemType.STICK, player.getItemAt(Player.HOTBAR_SIZE - 1).getType());
        assertEquals(ItemType.COAL, player.getItemAt(Player.HOTBAR_SIZE).getType());
    }

    @Test
    void addedItemGoesToTheFirstSlot() {
        assertTrue(player.addItem(new Item(ItemType.STONE, 5)));

        assertEquals(ItemType.STONE, player.getItemAt(0).getType());
        assertEquals(5, player.getItemAt(0).getQuantity());
    }

    @Test
    void sameTypeStacksIntoTheExistingSlot() {
        player.addItem(new Item(ItemType.STONE, 5));
        player.addItem(new Item(ItemType.STONE, 7));

        assertEquals(1, usedSlots());
        assertEquals(12, count(ItemType.STONE));
    }

    @Test
    void quantityBeyondTheStackLimitOverflowsIntoTheNextSlot() {
        player.addItem(new Item(ItemType.STONE, STACK + 10));

        assertEquals(2, usedSlots());
        assertEquals(STACK, player.getItemAt(0).getQuantity());
        assertEquals(10, player.getItemAt(1).getQuantity());
    }

    @Test
    void differentTypesUseDifferentSlots() {
        player.addItem(new Item(ItemType.STONE, 1));
        player.addItem(new Item(ItemType.STICK, 1));

        assertEquals(2, usedSlots());
    }

    @Test
    void fullInventoryRejectsAndLeavesTheRemainderOnTheItem() {
        player.addItem(new Item(ItemType.STONE, STACK * SLOTS));
        assertEquals(SLOTS, usedSlots());

        Item extra = new Item(ItemType.STONE, 5);
        assertFalse(player.addItem(extra));
        assertEquals(5, extra.getQuantity(), "nothing fitted, so the whole quantity is left");
        assertFalse(player.hasSpace(extra));
    }

    @Test
    void partialFitReturnsTrueAndShrinksTheSourceItem() {
        player.addItem(new Item(ItemType.STONE, STACK * SLOTS - 3));

        Item extra = new Item(ItemType.STONE, 10);
        assertTrue(player.addItem(extra));
        assertEquals(7, extra.getQuantity(), "only three units fitted");
        assertEquals(STACK * SLOTS, count(ItemType.STONE));
    }

    @Test
    void nullAndEmptyItemsAreRejected() {
        assertFalse(player.addItem(null));
        assertFalse(player.removeItem(null, 1));
        assertFalse(player.removeItem(new Item(ItemType.STONE, 1), 0));
        assertEquals(0, player.getItemCount(null));
        assertFalse(player.hasSpace(null));
    }

    @Test
    void removeTakesFromTheFirstStackBeforeTheNext() {
        player.addItem(new Item(ItemType.STONE, STACK + 10));

        assertTrue(player.removeItem(new Item(ItemType.STONE, 1), 15));

        assertEquals(STACK - 5, count(ItemType.STONE));
        assertEquals(STACK - 15, player.getItemAt(0).getQuantity());
        assertEquals(10, player.getItemAt(1).getQuantity());
    }

    @Test
    void removeClearsSlotsThatBecomeEmpty() {
        player.addItem(new Item(ItemType.STONE, STACK + 10));

        assertTrue(player.removeItem(new Item(ItemType.STONE, 1), STACK + 10));

        assertEquals(0, usedSlots());
        assertNull(player.getItemAt(0));
        assertNull(player.getItemAt(1));
    }

    @Test
    void removeReportsFalseWhenQuantityIsInsufficient() {
        player.addItem(new Item(ItemType.STONE, 4));

        assertFalse(player.removeItem(new Item(ItemType.STONE, 1), 10));
    }

    @Test
    void removeIsAtomicWhenQuantityIsInsufficient() {
        player.addItem(new Item(ItemType.STONE, 4));

        assertFalse(player.removeItem(new Item(ItemType.STONE, 1), 10));

        assertEquals(4, count(ItemType.STONE));
    }

    @Test
    void canTakeItemComparesAgainstTheTotalAcrossStacks() {
        player.addItem(new Item(ItemType.STONE, STACK + 1));

        assertTrue(player.canTakeItem(new Item(ItemType.STONE, 1), STACK + 1));
        assertFalse(player.canTakeItem(new Item(ItemType.STONE, 1), STACK + 2));
    }

    @Test
    void hasSpaceIsTrueWithAFreeSlotOrARoomyStack() {
        assertTrue(player.hasSpace(new Item(ItemType.STONE, 1)));

        player.addItem(new Item(ItemType.STONE, STACK * (SLOTS - 1)));
        player.addItem(new Item(ItemType.STICK, 1));
        assertFalse(player.hasSpace(new Item(ItemType.STONE, 1)));
        assertTrue(player.hasSpace(new Item(ItemType.STICK, 1)), "the stick stack has room");
    }

    @Test
    void setItemsReplacesTheContents() {
        player.addItem(new Item(ItemType.STONE, 9));

        player.setItems(List.of(new Item(ItemType.STICK, 2)));

        assertEquals(0, count(ItemType.STONE));
        assertEquals(2, count(ItemType.STICK));
    }

    @Test
    void setItemsKeepsSlotIndicesAndDoesNotCompact() {
        List<Item> source = new ArrayList<>();
        source.add(null);
        source.add(new Item(ItemType.STONE, 2));
        source.add(null);
        source.add(new Item(ItemType.STICK, 1));

        player.setItems(source);

        assertNull(player.getItemAt(0));
        assertEquals(ItemType.STONE, player.getItemAt(1).getType());
        assertNull(player.getItemAt(2));
        assertEquals(ItemType.STICK, player.getItemAt(3).getType());
    }

    @Test
    void setItemsCopiesTheItemsAndLeavesTheSourceUntouched() {
        Item stone = new Item(ItemType.STONE, 7);

        player.setItems(List.of(stone));

        assertEquals(7, stone.getQuantity());
        assertNotSame(stone, player.getItemAt(0));
    }

    @Test
    void addItemReducesTheGivenItemByWhatWasStored() {
        Item stone = new Item(ItemType.STONE, 7);

        assertTrue(player.addItem(stone));

        assertEquals(0, stone.getQuantity(), "addItem consumes the quantity it stores");
        assertEquals(7, count(ItemType.STONE));
    }

    @Test
    void invalidSlotsAreIgnored() {
        assertNull(player.getItemAt(-1));
        assertNull(player.getItemAt(SLOTS));

        player.setItemAt(99, new Item(ItemType.STONE, 1));
        assertEquals(0, usedSlots());
    }

    @Test
    void itemRejectsNonPositiveQuantityAndNegativeChanges() {
        final Item stone = new Item(ItemType.STONE, 1);

        assertThrows(IllegalArgumentException.class, () -> new Item(ItemType.STONE, 0));
        assertThrows(IllegalArgumentException.class, () -> stone.addQuantity(-1));
        assertThrows(IllegalArgumentException.class, () -> stone.removeQuantity(-1));
        assertThrows(NullPointerException.class, () -> new Item(null, 1));
    }

    @Test
    void itemEqualityIsByTypeOnlyAndToStringShowsQuantity() {
        assertEquals(new Item(ItemType.STONE, 1), new Item(ItemType.STONE, 50));
        assertEquals("3x " + ItemType.STONE.getDisplayName(), new Item(ItemType.STONE, 3).toString());
    }

    @Test
    void addItemLeavesInTheGivenItemWhatDidNotFit() {
        for (int slot = 0; slot < SLOTS; slot++) {
            player.addItem(new Item(ItemType.STONE, STACK));
        }
        final Item surplus = new Item(ItemType.STONE, 7);

        assertFalse(player.addItem(surplus), "nothing was stored");

        assertEquals(7, surplus.getQuantity(), "the whole quantity is the leftover");
        final Item almost = new Item(ItemType.STONE, STACK + 3);
        player.setItemAt(0, null);

        assertTrue(player.addItem(almost), "a slot was free");

        assertEquals(3, almost.getQuantity(), "what did not fit stays in the item");
    }
}
