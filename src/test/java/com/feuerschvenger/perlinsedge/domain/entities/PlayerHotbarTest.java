package com.feuerschvenger.perlinsedge.domain.entities;

import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHotbarTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
    }

    @Test
    void theFirstNineSlotsAreTheHotbarAndTheFirstOneIsInHandAtTheStart() {
        assertEquals(9, Player.HOTBAR_SIZE);
        assertEquals(0, player.getSelectedHotbarIndex());
        assertNull(player.getActiveItem(), "an empty slot is a bare hand");
        assertNull(player.getActiveItemType());
    }

    @Test
    void theActiveItemIsWhateverTheSelectedSlotHolds() {
        player.setItemAt(2, new Item(ItemType.AXE, 1));

        player.selectHotbarSlot(2);

        assertEquals(ItemType.AXE, player.getActiveItemType());
        assertEquals(ItemType.AXE, player.getActiveItem().getType());
    }

    @Test
    void anIndexOutsideTheHotbarChangesNothing() {
        player.selectHotbarSlot(4);

        player.selectHotbarSlot(-1);
        player.selectHotbarSlot(Player.HOTBAR_SIZE);
        player.selectHotbarSlot(12);

        assertEquals(4, player.getSelectedHotbarIndex());
    }

    @Test
    void cyclingWrapsAroundAtBothEnds() {
        player.cycleHotbar(-1);
        assertEquals(8, player.getSelectedHotbarIndex());

        player.cycleHotbar(1);
        assertEquals(0, player.getSelectedHotbarIndex());

        player.cycleHotbar(10);
        assertEquals(1, player.getSelectedHotbarIndex());
    }

    @Test
    void theSelectionIsAPositionSoItSurvivesTheItemsChanging() {
        player.setItemAt(1, new Item(ItemType.PICKAXE, 1));
        player.selectHotbarSlot(1);

        player.removeItemFromSlot(1);
        assertNull(player.getActiveItem());
        assertEquals(1, player.getSelectedHotbarIndex());

        player.setItemAt(1, new Item(ItemType.SWORD, 1));
        assertEquals(ItemType.SWORD, player.getActiveItemType());
    }

    @Test
    void anItemMovedIntoTheSelectedSlotBecomesTheActiveItem() {
        Item sword = new Item(ItemType.SWORD, 1);
        player.setItemAt(12, sword);
        player.selectHotbarSlot(3);

        player.moveItemInInventory(sword, 3);

        assertEquals(ItemType.SWORD, player.getActiveItemType());
    }

    @Test
    void anItemOutsideTheHotbarIsNeverInHand() {
        player.setItemAt(9, new Item(ItemType.AXE, 1));

        for (int slot = 0; slot < Player.HOTBAR_SIZE; slot++) {
            player.selectHotbarSlot(slot);
            assertNull(player.getActiveItem(), "slot " + slot);
        }
    }

    @Test
    void consumingTakesOneFromTheStackAndEmptiesTheSlotOnTheLastItem() {
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 2));

        assertTrue(player.consumeActiveItem());
        assertEquals(1, player.getActiveItem().getQuantity());

        assertTrue(player.consumeActiveItem());
        assertNull(player.getActiveItem());

        assertFalse(player.consumeActiveItem(), "a bare hand has nothing to consume");
    }
}
