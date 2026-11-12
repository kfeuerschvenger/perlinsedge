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

    private Player player;
    private InventoryPanel panel;

    @BeforeEach
    void setUp() {
        GameStateManager state = new GameStateManager();
        player = new Player(1, 1);
        player.setItemAt(3, new Item(ItemType.STONE, 5));
        player.setItemAt(6, new Item(ItemType.WOOD_LOG, 2));
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
                .filter(node -> ((InventoryPanel.SlotContext) node.getUserData()).index == index)
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
        fire(slot(3), MouseEvent.MOUSE_ENTERED_TARGET);

        SlotRef ref = target();

        assertSame(player, ref.container());
        assertEquals(3, ref.slot());
        assertEquals(ItemType.STONE, ref.item().getType());
    }

    @Test
    void anEmptyHoveredSlotIsStillTheTargetSoNothingElseIsThrownByMistake() {
        fire(slot(5), MouseEvent.MOUSE_ENTERED_TARGET);

        assertEquals(5, target().slot());
    }

    @Test
    void theSelectedSlotIsTheTargetWhenNoSlotIsHovered() {
        fire(slot(6), MouseEvent.MOUSE_CLICKED);

        assertEquals(6, target().slot());
    }

    @Test
    void theHoveredSlotWinsOverTheSelectedOne() {
        fire(slot(6), MouseEvent.MOUSE_CLICKED);
        fire(slot(3), MouseEvent.MOUSE_ENTERED_TARGET);

        assertEquals(3, target().slot());

        fire(slot(3), MouseEvent.MOUSE_EXITED_TARGET);

        assertEquals(6, target().slot(), "back to the selected slot once the mouse leaves");
    }

    @Test
    void closingThePanelForgetsTheHoverAndTheSelection() {
        fire(slot(3), MouseEvent.MOUSE_ENTERED_TARGET);
        fire(slot(6), MouseEvent.MOUSE_CLICKED);

        FxToolkitSupport.onFx(() -> {
            panel.closeInventory();
            return null;
        });

        assertNull(target());
    }
}
