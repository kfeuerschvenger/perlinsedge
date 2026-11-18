package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game refreshes the inventory every tick, so the panel does nothing while it is closed and redraws only the
 * slots that changed while it is open. A hotbar slot picked in the open inventory is highlighted.
 */
class InventoryPanelRefreshTest {

    private static final int GRID_SLOT = Player.HOTBAR_SIZE + 2;
    private static final int HOTBAR_SLOT = 3;

    private Player player;
    private InventoryPanel panel;
    private ItemHotbarPanel hotbar;

    @BeforeEach
    void setUp() {
        GameStateManager state = new GameStateManager();
        player = new Player(1, 1);
        player.setItemAt(GRID_SLOT, new Item(ItemType.WOOD_LOG, 3));
        player.setItemAt(HOTBAR_SLOT, new Item(ItemType.STONE, 2));
        state.setPlayer(player);
        FxToolkitSupport.onFx(() -> {
            panel = new InventoryPanel(state);
            hotbar = new ItemHotbarPanel();
            Pane root = new Pane(hotbar, panel);
            new Scene(root, 1000, 600);
            panel.attachHotbar(hotbar);
            panel.setCraftingRecipes(new RecipeManager().getAllItemRecipes());
            hotbar.update(player);
            panel.openPlayerInventory();
            root.applyCss();
            root.layout();
            return null;
        });
    }

    private StackPane gridSlot(int index) {
        return FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .map(StackPane.class::cast)
                .filter(node -> ((InventoryPanel.SlotContext) node.getUserData()).index == index)
                .findFirst()
                .orElseThrow());
    }

    private List<Node> drawn(StackPane slot) {
        return FxToolkitSupport.onFx(() -> List.copyOf(slot.getChildren()));
    }

    private void refresh() {
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });
    }

    private void click(StackPane slot) {
        FxToolkitSupport.onFx(() -> {
            Event.fireEvent(slot, new MouseEvent(MouseEvent.MOUSE_CLICKED, 5, 5, 5, 5, MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, true, false, false, null));
            return null;
        });
    }

    @Test
    void aClosedPanelDoesNoWorkAndTheOpenDrawsWhatChangedMeanwhile() {
        StackPane slot = gridSlot(GRID_SLOT);
        List<Node> before = drawn(slot);
        FxToolkitSupport.onFx(() -> {
            panel.closeInventory();
            return null;
        });

        player.setItemAt(GRID_SLOT, new Item(ItemType.WOOD_LOG, 9));
        refresh();

        assertEquals(before, drawn(slot), "a closed panel redraws nothing");

        FxToolkitSupport.onFx(() -> {
            panel.openPlayerInventory();
            return null;
        });
        assertNotEquals(before, drawn(slot), "opening draws the current stack");
        Label count = FxToolkitSupport.onFx(() -> (Label) slot.getChildren().get(1));
        assertEquals("9", FxToolkitSupport.onFx(count::getText));
    }

    @Test
    void anOpenPanelRedrawsOnlyTheSlotsThatChanged() {
        StackPane changing = gridSlot(GRID_SLOT);
        StackPane untouched = gridSlot(GRID_SLOT + 1);
        List<Node> changingBefore = drawn(changing);
        player.setItemAt(GRID_SLOT + 1, new Item(ItemType.STONE, 4));
        refresh();
        List<Node> untouchedAfterFill = drawn(untouched);

        refresh();
        refresh();

        assertEquals(changingBefore, drawn(changing), "the same nodes, none rebuilt");
        assertEquals(untouchedAfterFill, drawn(untouched), "nothing changed since the last refresh");

        player.setItemAt(GRID_SLOT + 1, new Item(ItemType.STONE, 5));
        refresh();

        assertNotEquals(untouchedAfterFill, drawn(untouched));
        assertEquals(changingBefore, drawn(changing));
    }

    @Test
    void aHotbarSlotPickedInTheOpenInventoryIsHighlightedAndTheHeldSlotMarkerIsLeftAlone() {
        StackPane slot = FxToolkitSupport.onFx(() -> hotbar.slotNode(HOTBAR_SLOT));
        boolean heldMarker = FxToolkitSupport.onFx(() -> hotbar.slotNode(0).getStyleClass().contains(UiTheme.SLOT_SELECTED));

        click(slot);

        assertTrue(FxToolkitSupport.onFx(() -> slot.getStyleClass().contains(UiTheme.SLOT_PICKED)));
        assertFalse(FxToolkitSupport.onFx(() -> slot.getStyleClass().contains(UiTheme.SLOT_SELECTED)),
                "the border that marks the held slot is not used for the pick");
        assertEquals(heldMarker, FxToolkitSupport.onFx(
                () -> hotbar.slotNode(0).getStyleClass().contains(UiTheme.SLOT_SELECTED)));

        FxToolkitSupport.onFx(() -> {
            panel.closeInventory();
            return null;
        });
        assertFalse(FxToolkitSupport.onFx(() -> slot.getStyleClass().contains(UiTheme.SLOT_PICKED)),
                "closing the inventory clears the pick");
    }

    @Test
    void pickingAnotherSlotMovesTheHighlightFromTheHotbarToTheGrid() {
        StackPane hotbarSlot = FxToolkitSupport.onFx(() -> hotbar.slotNode(HOTBAR_SLOT));
        StackPane grid = gridSlot(GRID_SLOT);

        click(hotbarSlot);
        click(grid);
        FxToolkitSupport.onFx(() -> null);

        assertFalse(FxToolkitSupport.onFx(() -> hotbarSlot.getStyleClass().contains(UiTheme.SLOT_PICKED)));
        assertTrue(FxToolkitSupport.onFx(() -> grid.getStyleClass().contains(UiTheme.SLOT_SELECTED)));
    }
}
