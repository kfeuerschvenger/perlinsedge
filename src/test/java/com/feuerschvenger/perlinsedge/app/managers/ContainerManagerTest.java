package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
