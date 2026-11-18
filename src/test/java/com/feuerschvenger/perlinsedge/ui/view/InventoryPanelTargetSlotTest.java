package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The slot a keyboard action applies to: the hovered one, else the selected one.
 */
class InventoryPanelTargetSlotTest {

    /** Slots of the inventory grid, which starts after the hotbar slots. */
    private static final int STONE_SLOT = Player.HOTBAR_SIZE + 3;
    private static final int LOG_SLOT = Player.HOTBAR_SIZE + 6;
    private static final int EMPTY_SLOT = Player.HOTBAR_SIZE + 5;

    private Player player;
    private InventoryPanel panel;

    @BeforeEach
    void setUp() {
        GameStateManager state = new GameStateManager();
        player = new Player(1, 1);
        player.setItemAt(STONE_SLOT, new Item(ItemType.STONE, 5));
        player.setItemAt(LOG_SLOT, new Item(ItemType.WOOD_LOG, 2));
        state.setPlayer(player);
        panel = FxToolkitSupport.onFx(() -> {
            InventoryPanel created = new InventoryPanel(state);
            created.openPlayerInventory();
            return created;
        });
    }

    private StackPane slot(int index) {
        return FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .map(StackPane.class::cast)
                .filter(node -> ((InventoryPanel.SlotContext) node.getUserData()).index() == index)
                .findFirst()
                .orElseThrow());
    }

    private static MouseEvent mouse(EventType<MouseEvent> type) {
        return new MouseEvent(type, 5, 5, 5, 5, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null);
    }

    private void fire(StackPane slot, EventType<MouseEvent> type) {
        FxToolkitSupport.onFx(() -> {
            Event.fireEvent(slot, mouse(type));
            return null;
        });
    }

    private SlotRef target() {
        return FxToolkitSupport.onFx(panel::getHoveredOrSelectedSlot);
    }

    @Test
    void withoutAHoveredOrSelectedSlotThereIsNoTarget() {
        assertNull(target());
    }

    @Test
    void theHoveredSlotIsTheTarget() {
        fire(slot(STONE_SLOT),MouseEvent.MOUSE_ENTERED_TARGET);

        SlotRef ref = target();

        assertSame(player, ref.container());
        assertEquals(STONE_SLOT, ref.slot());
        assertEquals(ItemType.STONE, ref.item().getType());
    }

    @Test
    void anEmptyHoveredSlotIsStillTheTargetSoNothingElseIsThrownByMistake() {
        fire(slot(EMPTY_SLOT), MouseEvent.MOUSE_ENTERED_TARGET);

        assertEquals(EMPTY_SLOT, target().slot());
    }

    @Test
    void theSelectedSlotIsTheTargetWhenNoSlotIsHovered() {
        fire(slot(LOG_SLOT),MouseEvent.MOUSE_CLICKED);

        assertEquals(LOG_SLOT, target().slot());
    }

    @Test
    void theHoveredSlotWinsOverTheSelectedOne() {
        fire(slot(LOG_SLOT),MouseEvent.MOUSE_CLICKED);
        fire(slot(STONE_SLOT),MouseEvent.MOUSE_ENTERED_TARGET);

        assertEquals(STONE_SLOT, target().slot());

        fire(slot(STONE_SLOT),MouseEvent.MOUSE_EXITED_TARGET);

        assertEquals(LOG_SLOT, target().slot(),"back to the selected slot once the mouse leaves");
    }

    @Test
    void closingThePanelForgetsTheHoverAndTheSelection() {
        fire(slot(STONE_SLOT),MouseEvent.MOUSE_ENTERED_TARGET);
        fire(slot(LOG_SLOT),MouseEvent.MOUSE_CLICKED);

        FxToolkitSupport.onFx(() -> {
            panel.closeInventory();
            return null;
        });

        assertNull(target());
    }
}
