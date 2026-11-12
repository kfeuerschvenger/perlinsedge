package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemUseManagerTest {

    private GameStateManager state;
    private Player player;
    private ItemUseManager use;
    private final List<String> hints = new ArrayList<>();
    private final AtomicInteger redraws = new AtomicInteger();

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        player = new Player(1, 1);
        state.setPlayer(player);
        use = new ItemUseManager(state, redraws::incrementAndGet, hints::add);
        player.takeDamage(60);
    }

    @Test
    void aHealthPotionHealsAndIsUsedUpOneAtATime() {
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 3));

        assertTrue(use.useActiveItem());

        assertEquals(100 - 60 + 50, player.getHealth(), 1e-9);
        assertEquals(2, player.getItemAt(0).getQuantity());
        assertEquals(1, redraws.get());
    }

    @Test
    void cookedMeatHealsALittleAndRawMeatLess() {
        player.setItemAt(0, new Item(ItemType.COOKED_MEAT, 1));
        player.setItemAt(1, new Item(ItemType.RAW_MEAT, 1));

        use.useActiveItem();
        double afterCooked = player.getHealth();
        player.selectHotbarSlot(1);
        use.useActiveItem();
        double afterRaw = player.getHealth();

        assertEquals(40 + ItemType.COOKED_MEAT.getHealAmount(), afterCooked, 1e-9);
        assertEquals(afterCooked + ItemType.RAW_MEAT.getHealAmount(), afterRaw, 1e-9);
        assertTrue(ItemType.RAW_MEAT.getHealAmount() < ItemType.COOKED_MEAT.getHealAmount());
        assertTrue(ItemType.COOKED_MEAT.getHealAmount() < ItemType.POTION_HEALTH.getHealAmount());
    }

    @Test
    void theLastItemOfAStackEmptiesTheSlot() {
        player.setItemAt(0, new Item(ItemType.COOKED_MEAT, 1));

        use.useActiveItem();

        assertNull(player.getItemAt(0));
    }

    @Test
    void healingNeverGoesBeyondTheMaximum() {
        player.heal(1000);
        player.takeDamage(10);
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 1));

        assertTrue(use.useActiveItem());

        assertEquals(player.getMaxHealth(), player.getHealth(), 1e-9);
    }

    @Test
    void atFullHealthNothingIsUsedUpAndAHintSaysSo() {
        player.heal(1000);
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 2));

        assertFalse(use.useActiveItem());

        assertEquals(2, player.getItemAt(0).getQuantity());
        assertEquals(List.of("Already at full health"), hints);
        assertEquals(0, redraws.get());
    }

    @Test
    void aBareHandOrAnItemThatCannotBeUsedDoesNothingAndSaysNothing() {
        assertFalse(use.useActiveItem());
        player.setItemAt(0, new Item(ItemType.AXE, 1));
        assertFalse(use.useActiveItem());
        player.setItemAt(0, new Item(ItemType.STONE, 5));
        assertFalse(use.useActiveItem());

        assertEquals(5, player.getItemAt(0).getQuantity());
        assertEquals(40, player.getHealth(), 1e-9);
        assertTrue(hints.isEmpty());
    }

    @Test
    void aPotionOutsideTheSlotInHandIsNotUsed() {
        player.setItemAt(10, new Item(ItemType.POTION_HEALTH, 1));

        assertFalse(use.useActiveItem());

        assertEquals(40, player.getHealth(), 1e-9);
    }

    @Test
    void nothingIsUsedWhileThePlayerIsDeadThePlayerIsMissingOrTheGameIsPaused() {
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 2));

        state.setGamePaused(true);
        assertFalse(use.useActiveItem());
        state.setGamePaused(false);
        state.setPlayer(null);
        assertFalse(use.useActiveItem());
        state.setPlayer(player);
        player.takeDamage(1000);
        assertFalse(use.useActiveItem());

        assertEquals(2, player.getItemAt(0).getQuantity());
    }

    @Test
    void nothingIsUsedWithAnInventoryOpenOrInBuildingMode() {
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 2));

        for (GameMode mode : new GameMode[]{GameMode.INVENTORY_OPEN, GameMode.CRAFTING_OPEN,
                GameMode.BUILDING_MODE, GameMode.PAUSED}) {
            state.setCurrentGameMode(mode);
            assertFalse(use.useActiveItem(), mode.name());
        }

        assertEquals(2, player.getItemAt(0).getQuantity());
        assertEquals(40, player.getHealth(), 1e-9);
    }

    @Test
    void pressingTheNumberOfTheSlotInHandUsesItsPotionButAnotherNumberOnlySelects() {
        ItemHotbarManager hotbar = new ItemHotbarManager(state, use::useActiveItem);
        player.setItemAt(0, new Item(ItemType.POTION_HEALTH, 2));

        hotbar.handleNumberKey(3);
        assertEquals(2, player.getItemAt(0).getQuantity(), "selecting another slot uses nothing");
        hotbar.handleNumberKey(1);
        assertEquals(2, player.getItemAt(0).getQuantity(), "moving to the potion slot only selects it");
        hotbar.handleNumberKey(1);

        assertEquals(1, player.getItemAt(0).getQuantity());
        assertEquals(90, player.getHealth(), 1e-9);
    }

    @Test
    void healingAnEntityIsClampedAndIgnoredOnceDead() {
        assertEquals(50, player.heal(50), 1e-9);
        assertEquals(10, player.heal(50), 1e-9, "only the missing health is restored");
        assertEquals(0, player.heal(0));
        assertEquals(0, player.heal(-5));
        player.takeDamage(1000);
        assertEquals(0, player.heal(50), "the dead come back through a respawn, not a heal");
    }
}
