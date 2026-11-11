package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestTest {

    private static final int STACK = ItemType.STONE.getMaxStackSize();

    private Chest chest;

    @BeforeEach
    void setUp() {
        chest = new Chest(1, 2, 3);
    }

    private List<Item> stacks() {
        return chest.getItems().stream().filter(Objects::nonNull).toList();
    }

    @Test
    void exposesPositionTypeAndCapacity() {
        assertEquals(1, chest.getTileX());
        assertEquals(2, chest.getTileY());
        assertEquals(BuildingType.CHEST, chest.getType());
        assertEquals(3, chest.getCapacity());
        assertTrue(chest.isSolid());
        assertTrue(chest.isDestructible());
    }

    @Test
    void getItemsHasOneEntryPerSlot() {
        chest.setItemAt(1, new Item(ItemType.STONE, 2));

        List<Item> items = chest.getItems();

        assertEquals(3, items.size());
        assertNull(items.get(0));
        assertEquals(2, items.get(1).getQuantity());
        assertNull(items.get(2));
    }

    @Test
    void sameTypeStacksUntilTheLimitThenUsesANewSlot() {
        chest.addItem(new Item(ItemType.STONE, STACK - 2));
        chest.addItem(new Item(ItemType.STONE, 5));

        assertEquals(2, stacks().size());
        assertEquals(STACK, chest.getItemAt(0).getQuantity());
        assertEquals(3, chest.getItemAt(1).getQuantity());
    }

    @Test
    void rejectsItemsWhenEverySlotIsFull() {
        chest.addItem(new Item(ItemType.STONE, STACK));
        chest.addItem(new Item(ItemType.STICK, STACK));
        chest.addItem(new Item(ItemType.COAL, STACK));

        assertFalse(chest.addItem(new Item(ItemType.FLINT, 1)));
        assertFalse(chest.hasSpace(new Item(ItemType.FLINT, 1)));
    }

    @Test
    void removeSpansStacksAndClearsEmptySlots() {
        chest.addItem(new Item(ItemType.STONE, STACK));
        chest.addItem(new Item(ItemType.STONE, 5));

        assertTrue(chest.removeItem(new Item(ItemType.STONE, 1), STACK + 5));

        assertEquals(0, stacks().size());
        assertNull(chest.getItemAt(0));
    }

    @Test
    void removeIsAtomicWhenQuantityIsInsufficient() {
        chest.addItem(new Item(ItemType.STONE, 4));

        assertFalse(chest.removeItem(new Item(ItemType.STONE, 1), 10));

        assertEquals(4, chest.getItemCount(new Item(ItemType.STONE, 1)));
    }

    @Test
    void addingMoreThanOneStackKeepsEveryUnit() {
        Item incoming = new Item(ItemType.STONE, STACK + 5);

        assertTrue(chest.addItem(incoming));

        assertEquals(STACK + 5, chest.getItemCount(new Item(ItemType.STONE, 1)));
        assertEquals(0, incoming.getQuantity());
    }

    @Test
    void whatDoesNotFitStaysOnTheIncomingItem() {
        Item incoming = new Item(ItemType.STONE, STACK * 3 + 7);

        assertTrue(chest.addItem(incoming));

        assertEquals(STACK * 3, chest.getItemCount(new Item(ItemType.STONE, 1)));
        assertEquals(7, incoming.getQuantity());
    }

    @Test
    void invalidSlotsReturnNullAndAreIgnored() {
        assertNull(chest.getItemAt(-1));
        assertNull(chest.getItemAt(3));

        chest.setItemAt(7, new Item(ItemType.STONE, 1));
        assertEquals(0, stacks().size());
    }

    @Test
    void getItemsReturnsADefensiveCopy() {
        chest.addItem(new Item(ItemType.STONE, 1));

        chest.getItems().clear();

        assertEquals(1, stacks().size());
    }

    @Test
    void nextFreeSlotCanBeFilledWithSetItemAt() {
        Item stone = new Item(ItemType.STONE, 4);

        chest.setItemAt(0, stone);

        assertSame(stone, chest.getItemAt(0));
    }

    @Test
    void interactingOpensTheContainer() {
        assertTrue(chest.onInteract(null));
    }

    @Test
    void emptySlotBelowCapacityReturnsNull() {
        chest.addItem(new Item(ItemType.STONE, 1));

        assertNull(chest.getItemAt(2));
    }

    @Test
    void itemCanBePlacedInAnyEmptySlot() {
        Item stone = new Item(ItemType.STONE, 4);

        chest.setItemAt(2, stone);

        assertSame(stone, chest.getItemAt(2));
    }

    @Test
    void clearingASlotKeepsTheOtherSlotsInPlace() {
        Item first = new Item(ItemType.STONE, 1);
        Item second = new Item(ItemType.STICK, 1);
        chest.setItemAt(0, first);
        chest.setItemAt(1, second);

        chest.setItemAt(0, null);

        assertNull(chest.getItemAt(0));
        assertSame(second, chest.getItemAt(1));
    }

    @Test
    void setItemsReplacesTheContents() {
        chest.addItem(new Item(ItemType.COAL, 3));

        chest.setItems(List.of(new Item(ItemType.STONE, 2), new Item(ItemType.STICK, 1)));

        assertEquals(0, chest.getItemCount(new Item(ItemType.COAL, 1)));
        assertEquals(2, chest.getItemCount(new Item(ItemType.STONE, 1)));
        assertEquals(1, chest.getItemCount(new Item(ItemType.STICK, 1)));
    }
}
