package com.feuerschvenger.perlinsedge.domain.entities.buildings;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
    void sameTypeStacksUntilTheLimitThenUsesANewSlot() {
        chest.addItem(new Item(ItemType.STONE, STACK - 2));
        chest.addItem(new Item(ItemType.STONE, 5));

        assertEquals(2, chest.getItems().size());
        assertEquals(STACK, chest.getItems().get(0).getQuantity());
        assertEquals(3, chest.getItems().get(1).getQuantity());
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
    void removeSpansStacksAndDropsEmptySlots() {
        chest.addItem(new Item(ItemType.STONE, STACK));
        chest.addItem(new Item(ItemType.STONE, 5));

        assertTrue(chest.removeItem(new Item(ItemType.STONE, 1), STACK + 5));

        assertEquals(0, chest.getItems().size());
    }

    @Disabled("bug: Chest.addItem stores at most one stack when the quantity exceeds the stack size, the excess "
            + "is lost and the call still returns true; fixed in PR 3")
    @Test
    void addingMoreThanOneStackKeepsEveryUnit() {
        assertTrue(chest.addItem(new Item(ItemType.STONE, STACK + 5)));

        assertEquals(STACK + 5, chest.getItemCount(new Item(ItemType.STONE, 1)));
    }

    @Test
    void invalidSlotsReturnNullAndAreIgnored() {
        assertNull(chest.getItemAt(-1));
        assertNull(chest.getItemAt(3));

        chest.setItemAt(7, new Item(ItemType.STONE, 1));
        assertEquals(0, chest.getItems().size());
    }

    @Test
    void getItemsReturnsADefensiveCopy() {
        chest.addItem(new Item(ItemType.STONE, 1));

        chest.getItems().clear();

        assertEquals(1, chest.getItems().size());
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

    @Disabled("bug: Chest.getItemAt throws IndexOutOfBoundsException for an empty slot that is below the capacity "
            + "(the slot check uses capacity, the list is shorter); fixed in PR 3")
    @Test
    void emptySlotBelowCapacityReturnsNull() {
        chest.addItem(new Item(ItemType.STONE, 1));

        assertNull(chest.getItemAt(2));
    }

    @Disabled("bug: Chest.setItemAt silently ignores a slot that is beyond the next free one, so an item dropped "
            + "on that slot is lost; fixed in PR 3")
    @Test
    void itemCanBePlacedInAnyEmptySlot() {
        Item stone = new Item(ItemType.STONE, 4);

        chest.setItemAt(2, stone);

        assertSame(stone, chest.getItemAt(2));
    }
}
