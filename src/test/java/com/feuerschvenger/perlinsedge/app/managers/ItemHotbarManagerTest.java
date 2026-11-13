package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemHotbarManagerTest {

    private GameStateManager state;
    private Player player;
    private ItemHotbarManager hotbar;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        player = new Player(1, 1);
        state.setPlayer(player);
        hotbar = new ItemHotbarManager(state);
    }

    @Test
    void aNumberKeySelectsTheSlotWhileExploring() {
        assertTrue(hotbar.handleNumberKey(4));

        assertEquals(3, player.getSelectedHotbarIndex());
    }

    @Test
    void pressingTheNumberOfTheSlotInHandKeepsItSelectedAndUsesNothing() {
        player.setItemAt(3, new Item(ItemType.POTION_HEALTH, 2));
        player.takeDamage(60);
        player.drainDamageTaken();
        hotbar.handleNumberKey(4);

        assertTrue(hotbar.handleNumberKey(4));

        assertEquals(3, player.getSelectedHotbarIndex());
        assertEquals(2, player.getInventory().get(3).getQuantity(), "the potion was not drunk");
    }

    @Test
    void theKeyBelongsToTheBuildingHotbarInBuildingMode() {
        state.setCurrentGameMode(GameMode.BUILDING_MODE);

        assertFalse(hotbar.handleNumberKey(4));

        assertEquals(0, player.getSelectedHotbarIndex());
    }

    @Test
    void numberKeysDoNothingInAnInventoryWhilePausedOrWithoutAPlayerOrWhenDead() {
        for (GameMode mode : new GameMode[]{GameMode.INVENTORY_OPEN, GameMode.CRAFTING_OPEN, GameMode.PAUSED}) {
            state.setCurrentGameMode(mode);
            assertFalse(hotbar.handleNumberKey(5), mode.name());
        }
        state.setCurrentGameMode(GameMode.EXPLORATION);
        state.setGamePaused(true);
        assertFalse(hotbar.handleNumberKey(5));
        state.setGamePaused(false);
        player.takeDamage(1000);
        assertFalse(hotbar.handleNumberKey(5));
        state.setPlayer(null);
        assertFalse(hotbar.handleNumberKey(5));

        assertEquals(0, player.getSelectedHotbarIndex());
    }

    @Test
    void aNumberOutsideOneToNineIsNotForTheHotbar() {
        assertFalse(hotbar.handleNumberKey(0));
        assertFalse(hotbar.handleNumberKey(10));
    }

    @Test
    void aNotchOfWheelMovesOneSlotAndTheEventBelongsToTheHotbar() {
        player.selectHotbarSlot(3);

        assertTrue(hotbar.handleWheel(-40, false));
        assertEquals(4, player.getSelectedHotbarIndex(), "wheel down goes to the next slot");

        assertTrue(hotbar.handleWheel(40, false));
        assertEquals(3, player.getSelectedHotbarIndex(), "wheel up goes back");
    }

    @Test
    void theWheelWrapsAroundTheEndsOfTheHotbar() {
        assertTrue(hotbar.handleWheel(40, false));

        assertEquals(8, player.getSelectedHotbarIndex());
    }

    @Test
    void aSmallDeltaIsConsumedButDoesNotMoveTheHandYet() {
        assertTrue(hotbar.handleWheel(-10, false));

        assertEquals(0, player.getSelectedHotbarIndex());
    }

    @Test
    void ctrlWithTheWheelIsLeftToTheCameraToZoom() {
        assertFalse(hotbar.handleWheel(-40, true));

        assertEquals(0, player.getSelectedHotbarIndex());
    }

    @Test
    void theWheelZoomsInsteadWhenNotExploringOrWithoutALivingPlayer() {
        state.setCurrentGameMode(GameMode.BUILDING_MODE);
        assertFalse(hotbar.handleWheel(-40, false));
        state.setCurrentGameMode(GameMode.INVENTORY_OPEN);
        assertFalse(hotbar.handleWheel(-40, false));
        state.setCurrentGameMode(GameMode.EXPLORATION);
        state.setPlayer(null);
        assertFalse(hotbar.handleWheel(-40, false));

        assertEquals(0, player.getSelectedHotbarIndex());
    }
}
