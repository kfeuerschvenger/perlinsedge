package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** The inventory grid shows the 18 slots that follow the hotbar, six wide and three tall. */
class InventoryGridTest {

    private Player player;
    private InventoryPanel panel;

    @BeforeEach
    void setUp() {
        GameStateManager state = new GameStateManager();
        player = new Player(1, 1);
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
                .sorted((a, b) -> Integer.compare(((InventoryPanel.SlotContext) a.getUserData()).index(),
                        ((InventoryPanel.SlotContext) b.getUserData()).index()))
                .toList());
    }

    @Test
    void theGridHoldsTheEighteenInventorySlotsAndNoHotbarSlot() {
        List<StackPane> slots = slots();

        assertEquals(Player.INVENTORY_SIZE, slots.size());
        for (int i = 0; i < slots.size(); i++) {
            assertEquals(Player.HOTBAR_SIZE + i, ((InventoryPanel.SlotContext) slots.get(i).getUserData()).index());
        }
    }

    @Test
    void theSlotsAreArrangedSixWideAndThreeTall() {
        List<StackPane> slots = slots();

        for (int i = 0; i < slots.size(); i++) {
            assertEquals(i / 6, GridPane.getRowIndex(slots.get(i)), "row of slot " + i);
            assertEquals(i % 6, GridPane.getColumnIndex(slots.get(i)), "column of slot " + i);
        }
    }

    @Test
    void aSlotShowsTheItemOfItsOwnIndexInThePlayerStorage() {
        player.setItemAt(Player.HOTBAR_SIZE + 7, new Item(ItemType.AXE, 1));
        player.setItemAt(2, new Item(ItemType.STICK, 1));
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });

        List<StackPane> slots = slots();

        assertFalse(slots.get(7).getChildren().isEmpty());
        assertEquals(1, slots.stream().filter(slot -> !slot.getChildren().isEmpty()).count(),
                "only the inventory item is drawn, the hotbar item is not in the grid");
    }
}
