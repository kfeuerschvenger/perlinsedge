package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.ContainerManager;
import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.ItemDropManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hotbar at the bottom of the screen takes part in the dragging while the inventory is open: stacks move between
 * it and the inventory, it takes the throw actions, and it stays a plain display while the inventory is closed.
 */
class InventoryHotbarDragTest {
    /** A scene point outside the inventory panel, the hotbar and every slot. */
    private static final double[] OUTSIDE = {990, 300};
    private static final int HOTBAR_SLOT = 2;
    private static final int GRID_SLOT = Player.HOTBAR_SIZE + 4;

    private GameStateManager state;
    private Player player;
    private InventoryPanel panel;
    private ItemHotbarPanel hotbar;
    private Pane root;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        TileMap map = new TileMap(9, 9);
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(4, 4);
        player.setItemAt(HOTBAR_SLOT, new Item(ItemType.STONE, 5));
        player.setItemAt(GRID_SLOT, new Item(ItemType.WOOD_LOG, 3));
        state.setCurrentMap(map);
        state.setPlayer(player);

        FxToolkitSupport.onFx(() -> {
            panel = new InventoryPanel(state);
            hotbar = new ItemHotbarPanel();
            root = new Pane(hotbar, panel);
            hotbar.relocate(150, 540);
            new Scene(root, 1000, 600);
            panel.attachHotbar(hotbar);
            hotbar.update(player);
            panel.openPlayerInventory();
            root.applyCss();
            root.layout();
            return null;
        });
        ContainerManager containers = new ContainerManager();
        ItemDropManager drops = new ItemDropManager(state, panel::updateInventoryDisplay);
        panel.setOnItemDropped((slot, wholeStack) -> drops.dropFromSlot(slot.container(), slot.slot(), wholeStack));
        panel.setOnItemDroppedInside((srcSlot, srcContainer, tgtSlot, tgtContainer, item) ->
                containers.transferBetweenSlots(srcContainer, srcSlot, tgtContainer, tgtSlot, item));
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private StackPane gridSlot(int index) {
        return FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .map(StackPane.class::cast)
                .filter(node -> ((InventoryPanel.SlotContext) node.getUserData()).index() == index)
                .findFirst()
                .orElseThrow());
    }

    private StackPane hotbarSlot(int index) {
        return FxToolkitSupport.onFx(() -> hotbar.slotNode(index));
    }

    private double[] centerOf(StackPane slot) {
        return FxToolkitSupport.onFx(() -> {
            Bounds bounds = slot.localToScene(slot.getLayoutBounds());
            return new double[]{bounds.getCenterX(), bounds.getCenterY()};
        });
    }

    private void fire(StackPane target, EventType<MouseEvent> type, MouseButton button, double[] at,
                      boolean primaryDown, boolean secondaryDown) {
        FxToolkitSupport.onFx(() -> {
            Event.fireEvent(target, new MouseEvent(type, at[0], at[1], at[0], at[1], button, 1,
                    false, false, false, false, primaryDown, false, secondaryDown, false, false, false, null));
            return null;
        });
    }

    private void press(StackPane slot) {
        fire(slot, MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, centerOf(slot), true, false);
    }

    private void moveTo(StackPane slot, double[] at) {
        fire(slot, MouseEvent.MOUSE_DRAGGED, MouseButton.PRIMARY, at, true, false);
    }

    private void release(StackPane slot, double[] at) {
        fire(slot, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, at, false, false);
    }

    private void drag(StackPane from, double[] to) {
        press(from);
        moveTo(from, to);
        release(from, to);
    }

    private int groundQuantity(ItemType type) {
        return state.getDroppedItems().stream()
                .filter(drop -> drop.getItemType() == type)
                .mapToInt(DroppedItem::getQuantity)
                .sum();
    }

    // ------------------------------------------------------------------
    //  Tests
    // ------------------------------------------------------------------

    @Test
    void aStackDraggedFromTheInventoryLandsInAnEmptyHotbarSlot() {
        StackPane target = hotbarSlot(6);

        drag(gridSlot(GRID_SLOT), centerOf(target));

        assertNull(player.getItemAt(GRID_SLOT));
        assertEquals(ItemType.WOOD_LOG, player.getItemAt(6).getType());
        assertEquals(3, player.getItemAt(6).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void aStackDraggedFromTheHotbarLandsInTheInventoryAndSwapsWithAStackThere() {
        StackPane target = gridSlot(GRID_SLOT);

        drag(hotbarSlot(HOTBAR_SLOT), centerOf(target));

        assertEquals(ItemType.WOOD_LOG, player.getItemAt(HOTBAR_SLOT).getType());
        assertEquals(ItemType.STONE, player.getItemAt(GRID_SLOT).getType());
    }

    @Test
    void releasingAHotbarStackOutsideBothPanelsThrowsIt() {
        drag(hotbarSlot(HOTBAR_SLOT), OUTSIDE);

        assertNull(player.getItemAt(HOTBAR_SLOT));
        assertEquals(5, groundQuantity(ItemType.STONE));
    }

    @Test
    void releasingBetweenTwoHotbarSlotsIsAMissAimAndThrowsNothing() {
        double[] centre = centerOf(hotbarSlot(4));
        // The gap between two slots is part of the hotbar, so it is inside the inventory and cancels
        double[] gap = {centre[0] + 26, centre[1]};

        drag(hotbarSlot(HOTBAR_SLOT), gap);

        assertEquals(5, player.getItemAt(HOTBAR_SLOT).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void aRightClickWhileDraggingAHotbarStackThrowsOneItem() {
        StackPane source = hotbarSlot(HOTBAR_SLOT);
        press(source);
        moveTo(source, centerOf(gridSlot(GRID_SLOT + 1)));
        double[] at = centerOf(gridSlot(GRID_SLOT + 1));

        fire(source, MouseEvent.MOUSE_PRESSED, MouseButton.SECONDARY, at, true, true);

        assertEquals(4, player.getItemAt(HOTBAR_SLOT).getQuantity());
        assertEquals(1, groundQuantity(ItemType.STONE));
        assertTrue(panel.isStackInHand(), "the rest of the stack stays in hand");
    }

    @Test
    void theHoveredHotbarSlotIsTheTargetOfTheThrowKeys() {
        fire(hotbarSlot(HOTBAR_SLOT), MouseEvent.MOUSE_ENTERED_TARGET, MouseButton.NONE,
                centerOf(hotbarSlot(HOTBAR_SLOT)), false, false);

        SlotRef target = FxToolkitSupport.onFx(panel::getHoveredOrSelectedSlot);

        assertEquals(player, target.container());
        assertEquals(HOTBAR_SLOT, target.slot());
    }

    @Test
    void aDropOverTheHotbarKeepsTheMarkOfTheSlotHeldInHand() {
        player.selectHotbarSlot(6);
        FxToolkitSupport.onFx(() -> {
            hotbar.update(player);
            return null;
        });
        StackPane held = hotbarSlot(6);
        StackPane source = gridSlot(GRID_SLOT);
        press(source);
        moveTo(source, centerOf(held));

        assertTrue(held.getStyleClass().contains(UiTheme.SLOT_DROP));
        assertTrue(held.getStyleClass().contains(UiTheme.SLOT_SELECTED), "the hand mark stays under the drop mark");

        release(source, centerOf(held));

        assertFalse(held.getStyleClass().contains(UiTheme.SLOT_DROP));
        assertTrue(held.getStyleClass().contains(UiTheme.SLOT_SELECTED), "the hand mark survives the drop");
    }

    private void ctrlClick(StackPane slot) {
        double[] at = centerOf(slot);
        FxToolkitSupport.onFx(() -> {
            Event.fireEvent(slot, new MouseEvent(MouseEvent.MOUSE_CLICKED, at[0], at[1], at[0], at[1],
                    MouseButton.PRIMARY, 1, false, true, false, false, false, false, false, false, false, false, null));
            return null;
        });
        // The refresh of the panel is queued, a later task on the toolkit runs after it
        FxToolkitSupport.onFx(() -> null);
    }

    @Test
    void aCtrlClickOnAHotbarSlotSendsItsStackToTheInventory() {
        ctrlClick(hotbarSlot(HOTBAR_SLOT));

        assertNull(player.getItemAt(HOTBAR_SLOT));
        assertEquals(ItemType.STONE, player.getItemAt(Player.HOTBAR_SIZE).getType());
        assertNull(FxToolkitSupport.onFx(panel::getHoveredOrSelectedSlot), "a quick move selects nothing");
    }

    @Test
    void aCtrlClickOnAnInventorySlotSendsItsStackToTheHotbar() {
        ctrlClick(gridSlot(GRID_SLOT));

        assertNull(player.getItemAt(GRID_SLOT));
        assertEquals(ItemType.WOOD_LOG, player.getItemAt(0).getType());
    }

    @Test
    void withAChestOpenACtrlClickSendsTheStackToTheChestAndBack() {
        Chest chest = new Chest(0, 0, 9);
        chest.setItemAt(0, new Item(ItemType.STICK, 4));
        FxToolkitSupport.onFx(() -> {
            panel.openDualInventory(chest);
            root.applyCss();
            root.layout();
            return null;
        });

        ctrlClick(hotbarSlot(HOTBAR_SLOT));
        assertNull(player.getItemAt(HOTBAR_SLOT));
        assertEquals(ItemType.STONE, chest.getItemAt(1).getType());

        StackPane chestSlot = FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .map(StackPane.class::cast)
                .filter(node -> ((InventoryPanel.SlotContext) node.getUserData()).container() == chest
                        && ((InventoryPanel.SlotContext) node.getUserData()).index() == 0)
                .findFirst()
                .orElseThrow());
        ctrlClick(chestSlot);

        assertNull(chest.getItemAt(0));
        assertEquals(ItemType.STICK, player.getItemAt(0).getType(), "the first free hotbar slot");
    }

    @Test
    void theHotbarTakesNoDragWhileTheInventoryIsClosed() {
        FxToolkitSupport.onFx(() -> {
            panel.closeInventory();
            return null;
        });
        StackPane source = hotbarSlot(HOTBAR_SLOT);

        drag(source, OUTSIDE);

        assertEquals(5, player.getItemAt(HOTBAR_SLOT).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
        assertFalse(panel.isStackInHand());
    }
}
