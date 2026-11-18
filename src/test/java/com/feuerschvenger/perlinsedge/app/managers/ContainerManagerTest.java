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

    @Test
    void aContainerIsOpenOnlyBetweenOpeningAndClosing() {
        assertFalse(manager.isContainerOpen());
        assertNull(manager.getOpenContainer());

        manager.openContainer(chest);
        assertTrue(manager.isContainerOpen());
        assertSame(chest, manager.getOpenContainer());

        manager.closeContainer();
        assertFalse(manager.isContainerOpen());
        assertNull(manager.getOpenContainer());
    }

    @Test
    void openingNothingKeepsTheCurrentContainerAndClosingTwiceIsHarmless() {
        manager.openContainer(chest);

        manager.openContainer(null);
        assertSame(chest, manager.getOpenContainer());

        manager.closeContainer();
        manager.closeContainer();
        assertFalse(manager.isContainerOpen());
    }

    @Test
    void movingOntoTheSameKindOfStackAddsToItAndEmptiesTheSourceWhenItAllFits() {
        Item moving = new Item(ItemType.STONE, 5);
        Item target = new Item(ItemType.STONE, 10);
        chest.setItemAt(0, moving);
        player.setItemAt(1, target);

        manager.transferBetweenSlots(chest, 0, player, 1, moving);

        assertEquals(15, target.getQuantity());
        assertNull(chest.getItemAt(0));
    }

    @Test
    void aStackThatDoesNotFitLeavesTheRemainderAtTheSource() {
        int max = new Item(ItemType.STONE, 1).getMaxStackSize();
        Item moving = new Item(ItemType.STONE, 10);
        Item target = new Item(ItemType.STONE, max - 4);
        chest.setItemAt(0, moving);
        player.setItemAt(1, target);

        manager.transferBetweenSlots(chest, 0, player, 1, moving);

        assertEquals(max, target.getQuantity());
        assertEquals(6, moving.getQuantity());
        assertSame(moving, chest.getItemAt(0));
    }

    @Test
    void differentKindsOfItemSwapPlaces() {
        Item stone = new Item(ItemType.STONE, 5);
        Item wood = new Item(ItemType.WOOD_LOG, 2);
        chest.setItemAt(0, stone);
        player.setItemAt(1, wood);

        manager.transferBetweenSlots(chest, 0, player, 1, stone);

        assertSame(wood, chest.getItemAt(0));
        assertSame(stone, player.getItemAt(1));
    }

    @Test
    void aFullTargetStackOfTheSameKindSwapsInsteadOfStacking() {
        int max = new Item(ItemType.STONE, 1).getMaxStackSize();
        Item moving = new Item(ItemType.STONE, 3);
        Item full = new Item(ItemType.STONE, max);
        chest.setItemAt(0, moving);
        player.setItemAt(1, full);

        manager.transferBetweenSlots(chest, 0, player, 1, moving);

        assertSame(full, chest.getItemAt(0));
        assertSame(moving, player.getItemAt(1));
    }

    @Test
    void invalidTransfersChangeNothing() {
        Item stone = new Item(ItemType.STONE, 5);
        chest.setItemAt(0, stone);

        manager.transferBetweenSlots(null, 0, player, 0, stone);
        manager.transferBetweenSlots(chest, 0, null, 0, stone);
        manager.transferBetweenSlots(chest, 4, player, 0, stone);
        manager.transferBetweenSlots(chest, 0, player, 0, new Item(ItemType.WOOD_LOG, 1));

        assertSame(stone, chest.getItemAt(0));
        assertNull(player.getItemAt(0));
    }
}
