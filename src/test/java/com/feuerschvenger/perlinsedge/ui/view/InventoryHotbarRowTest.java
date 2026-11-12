package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The first row of the player inventory is the hotbar and shows which slot is held in hand. */
class InventoryHotbarRowTest {

    private Player player;
    private InventoryPanel panel;

    @BeforeEach
    void setUp() {
        GameStateManager state = new GameStateManager();
        player = new Player(1, 1);
        player.setItemAt(2, new Item(ItemType.AXE, 1));
        state.setPlayer(player);
        panel = FxToolkitSupport.onFx(() -> {
            InventoryPanel created = new InventoryPanel(state);
            created.openPlayerInventory();
            return created;
        });
    }

    private List<StackPane> slots() {
        return FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .map(StackPane.class::cast)
                .sorted((a, b) -> Integer.compare(((InventoryPanel.SlotContext) a.getUserData()).index,
                        ((InventoryPanel.SlotContext) b.getUserData()).index))
                .toList());
    }

    @Test
    void theGridHasTwoRowsOfTheNineHotbarSlots() {
        assertEquals(18, slots().size());
        // Slot 9 starts the second row: nine columns
        long inFirstRow = FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT_HOTBAR).size());
        assertEquals(Player.HOTBAR_SIZE, inFirstRow);
    }

    @Test
    void onlyTheFirstNineSlotsCarryTheHotbarMark() {
        List<StackPane> slots = slots();

        for (int i = 0; i < slots.size(); i++) {
            assertEquals(i < Player.HOTBAR_SIZE, slots.get(i).getStyleClass().contains(UiTheme.SLOT_HOTBAR), "slot " + i);
        }
    }

    @Test
    void theSlotHeldInHandCarriesTheActiveMarkAndItFollowsTheSelection() {
        player.selectHotbarSlot(2);
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });
        assertEquals(2, panel.activeHotbarSlotShown());

        player.selectHotbarSlot(7);
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });
        assertEquals(7, panel.activeHotbarSlotShown());
    }
}
