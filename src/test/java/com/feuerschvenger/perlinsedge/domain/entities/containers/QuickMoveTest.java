package com.feuerschvenger.perlinsedge.domain.entities.containers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickMoveTest {

    private static final int STACK = ItemType.STONE.getMaxStackSize();
    private static final int HOTBAR = Player.HOTBAR_SIZE;
    private static final int STORAGE = Player.HOTBAR_SIZE + Player.INVENTORY_SIZE;

    private Player player;
    private Chest chest;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        chest = new Chest(0, 0, 6);
    }

    @Test
    void aHotbarStackGoesToTheFirstFreeInventorySlot() {
        player.setItemAt(3, new Item(ItemType.AXE, 1));

        assertTrue(QuickMove.move(player, 3, player, HOTBAR, STORAGE));

        assertNull(player.getItemAt(3));
        assertEquals(ItemType.AXE, player.getItemAt(HOTBAR).getType());
    }

    @Test
    void anInventoryStackGoesToTheFirstFreeHotbarSlot() {
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        player.setItemAt(HOTBAR + 4, new Item(ItemType.STICK, 2));

        assertTrue(QuickMove.move(player, HOTBAR + 4, player, 0, HOTBAR));

        assertNull(player.getItemAt(HOTBAR + 4));
        assertEquals(ItemType.STICK, player.getItemAt(1).getType());
    }

    @Test
    void theStackMergesIntoMatchingStacksBeforeItTakesAFreeSlot() {
        player.setItemAt(2, new Item(ItemType.STONE, 10));
        player.setItemAt(HOTBAR + 3, new Item(ItemType.STONE, 5));

        assertTrue(QuickMove.move(player, 2, player, HOTBAR, STORAGE));

        assertNull(player.getItemAt(2));
        assertNull(player.getItemAt(HOTBAR), "the free slot is not used while a stack can take it");
        assertEquals(15, player.getItemAt(HOTBAR + 3).getQuantity());
    }

    @Test
    void whatDoesNotFitStaysInTheSourceSlot() {
        player.setItemAt(0, new Item(ItemType.STONE, 20));
        for (int slot = HOTBAR; slot < STORAGE; slot++) {
            player.setItemAt(slot, new Item(ItemType.STICK, 1));
        }
        player.setItemAt(HOTBAR + 1, new Item(ItemType.STONE, STACK - 6));

        assertTrue(QuickMove.move(player, 0, player, HOTBAR, STORAGE));

        assertEquals(STACK, player.getItemAt(HOTBAR + 1).getQuantity());
        assertEquals(14, player.getItemAt(0).getQuantity());
    }

    @Test
    void nothingMovesWhenTheRangeIsFull() {
        player.setItemAt(0, new Item(ItemType.STONE, 20));
        for (int slot = HOTBAR; slot < STORAGE; slot++) {
            player.setItemAt(slot, new Item(ItemType.STICK, STACK));
        }

        assertFalse(QuickMove.move(player, 0, player, HOTBAR, STORAGE));

        assertEquals(20, player.getItemAt(0).getQuantity());
    }

    @Test
    void aStackNeverMergesIntoItsOwnSlot() {
        player.setItemAt(0, new Item(ItemType.STONE, 5));

        assertFalse(QuickMove.move(player, 0, player, 0, 1));

        assertEquals(5, player.getItemAt(0).getQuantity());
    }

    @Test
    void aChestStackGoesToThePlayerInFillOrder() {
        chest.setItemAt(0, new Item(ItemType.STONE, 8));
        player.setItemAt(HOTBAR + 2, new Item(ItemType.STONE, 4));
        player.setItemAt(0, new Item(ItemType.AXE, 1));

        assertTrue(QuickMove.move(chest, 0, player, 0, STORAGE));

        assertNull(chest.getItemAt(0));
        assertEquals(12, player.getItemAt(HOTBAR + 2).getQuantity());
        assertNull(player.getItemAt(1));
    }

    @Test
    void aChestStackWithNoMatchTakesTheFirstFreeHotbarSlot() {
        chest.setItemAt(2, new Item(ItemType.STICK, 3));
        player.setItemAt(0, new Item(ItemType.AXE, 1));

        assertTrue(QuickMove.move(chest, 2, player, 0, STORAGE));

        assertEquals(ItemType.STICK, player.getItemAt(1).getType());
    }

    @Test
    void aPlayerStackGoesToTheChest() {
        player.setItemAt(5, new Item(ItemType.STONE, 7));
        chest.setItemAt(3, new Item(ItemType.STONE, 2));

        assertTrue(QuickMove.move(player, 5, chest, 0, chest.getCapacity()));

        assertNull(player.getItemAt(5));
        assertEquals(9, chest.getItemAt(3).getQuantity());
    }

    @Test
    void withNoChestOpenACtrlClickOnTheHotbarGoesToTheInventory() {
        player.setItemAt(1, new Item(ItemType.AXE, 1));

        assertTrue(QuickMove.route(player, null, player, 1));

        assertNull(player.getItemAt(1));
        assertEquals(ItemType.AXE, player.getItemAt(HOTBAR).getType());
    }

    @Test
    void withNoChestOpenACtrlClickOnTheInventoryGoesToTheHotbar() {
        player.setItemAt(HOTBAR + 5, new Item(ItemType.AXE, 1));

        assertTrue(QuickMove.route(player, null, player, HOTBAR + 5));

        assertNull(player.getItemAt(HOTBAR + 5));
        assertEquals(ItemType.AXE, player.getItemAt(0).getType());
    }

    @Test
    void withAChestOpenACtrlClickOnEitherPartOfThePlayerGoesToTheChest() {
        player.setItemAt(1, new Item(ItemType.AXE, 1));
        player.setItemAt(HOTBAR + 5, new Item(ItemType.STICK, 2));

        assertTrue(QuickMove.route(player, chest, player, 1));
        assertTrue(QuickMove.route(player, chest, player, HOTBAR + 5));

        assertNull(player.getItemAt(1));
        assertNull(player.getItemAt(HOTBAR + 5));
        assertEquals(ItemType.AXE, chest.getItemAt(0).getType());
        assertEquals(ItemType.STICK, chest.getItemAt(1).getType());
    }

    @Test
    void withAChestOpenACtrlClickInTheChestGoesToThePlayer() {
        chest.setItemAt(4, new Item(ItemType.STICK, 2));

        assertTrue(QuickMove.route(player, chest, chest, 4));

        assertNull(chest.getItemAt(4));
        assertEquals(ItemType.STICK, player.getItemAt(0).getType());
    }

    @Test
    void aContainerThatIsNotOpenIsNotMovedFrom() {
        chest.setItemAt(4, new Item(ItemType.STICK, 2));

        assertFalse(QuickMove.route(player, null, chest, 4));

        assertEquals(2, chest.getItemAt(4).getQuantity());
    }

    @Test
    void anEmptySlotMovesNothing() {
        assertFalse(QuickMove.move(player, 0, chest, 0, chest.getCapacity()));
    }
}
