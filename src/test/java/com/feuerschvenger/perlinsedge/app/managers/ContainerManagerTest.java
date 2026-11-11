package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerManagerTest {

    private ContainerManager manager;
    private Chest chest;
    private Player player;

    @BeforeEach
    void setUp() {
        manager = new ContainerManager();
        chest = new Chest(0, 0, 9);
        player = new Player(0, 0);
    }

    @Test
    void removingAStackFromTheOpenChestClearsItsSlot() {
        Item stack = new Item(ItemType.STONE, 12);
        chest.setItemAt(4, stack);
        manager.openContainer(chest);

        assertTrue(manager.removeFromOpenContainer(stack));

        assertNull(chest.getItemAt(4));
        assertEquals(0, chest.getItemCount(new Item(ItemType.STONE, 1)));
    }

    @Test
    void removalIsByReferenceSoACopyIsNotFound() {
        chest.setItemAt(0, new Item(ItemType.STONE, 12));
        manager.openContainer(chest);

        assertFalse(manager.removeFromOpenContainer(new Item(ItemType.STONE, 12)));

        assertEquals(12, chest.getItemCount(new Item(ItemType.STONE, 1)));
    }

    @Test
    void secondRemovalOfTheSameStackFindsNothing() {
        Item stack = new Item(ItemType.STONE, 12);
        chest.setItemAt(0, stack);
        manager.openContainer(chest);

        assertTrue(manager.removeFromOpenContainer(stack));
        assertFalse(manager.removeFromOpenContainer(stack));
    }

    @Test
    void nothingIsRemovedWhenNoContainerIsOpen() {
        Item stack = new Item(ItemType.STONE, 12);
        chest.setItemAt(0, stack);

        assertFalse(manager.removeFromOpenContainer(stack));
        assertSame(stack, chest.getItemAt(0));
    }

    @Test
    void staticRemovalWorksOnThePlayerInventory() {
        Item stack = new Item(ItemType.COAL, 3);
        player.setItemAt(2, stack);

        assertTrue(ContainerManager.removeInstance(player, stack));

        assertNull(player.getItemAt(2));
        assertFalse(ContainerManager.removeInstance(null, stack));
        assertFalse(ContainerManager.removeInstance(player, null));
    }

    @Test
    void transferBetweenChestAndPlayerMovesTheStack() {
        Item stack = new Item(ItemType.STONE, 5);
        chest.setItemAt(3, stack);

        manager.transferBetweenSlots(chest, 3, player, 0, stack);

        assertNull(chest.getItemAt(3));
        assertSame(stack, player.getItemAt(0));
    }

    @Test
    void transferIntoASparseChestSlotWorks() {
        Item stack = new Item(ItemType.STONE, 5);
        player.setItemAt(0, stack);

        manager.transferBetweenSlots(player, 0, chest, 6, stack);

        assertNull(player.getItemAt(0));
        assertSame(stack, chest.getItemAt(6));
    }
}
