package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.ItemDropManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the drag of a stack out of the inventory with synthetic mouse events on a panel inside a scene, the way
 * the mouse would: a press on a slot, movement, an optional right click, and a release.
 */
class InventoryDragTest {
    /** A scene point outside the panel and every slot. */
    private static final double[] OUTSIDE = {990, 590};

    private GameStateManager state;
    private Player player;
    private InventoryPanel panel;
    private final List<String> transfers = new ArrayList<>();

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
        player.setItemAt(3, new Item(ItemType.STONE, 5));
        player.setItemAt(6, new Item(ItemType.WOOD_LOG, 1));
        state.setCurrentMap(map);
        state.setPlayer(player);

        panel = FxToolkitSupport.onFx(() -> {
            InventoryPanel created = new InventoryPanel(state);
            Pane root = new Pane(created);
            new Scene(root, 1000, 600);
            created.openPlayerInventory();
            root.applyCss();
            root.layout();
            return created;
        });
        panel.setOnItemDropped((slot, wholeStack) ->
                new ItemDropManager(state, panel::updateInventoryDisplay)
                        .dropFromSlot(slot.container(), slot.slot(), wholeStack));
        panel.setOnItemDroppedInside((srcSlot, srcContainer, tgtSlot, tgtContainer, item) ->
                transfers.add(srcSlot + "->" + tgtSlot + " " + item.getDisplayName()));
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private StackPane slot(Container container, int index) {
        return FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .map(StackPane.class::cast)
                .filter(node -> {
                    InventoryPanel.SlotContext context = (InventoryPanel.SlotContext) node.getUserData();
                    return context.container == container && context.index == index;
                })
                .findFirst()
                .orElseThrow());
    }

    private double[] centerOf(StackPane slot) {
        return FxToolkitSupport.onFx(() -> {
            Bounds bounds = slot.localToScene(slot.getLayoutBounds());
            return new double[]{bounds.getCenterX(), bounds.getCenterY()};
        });
    }

    /** A scene point over the panel itself that is over no slot. */
    private double[] panelBackground() {
        return FxToolkitSupport.onFx(() -> {
            Bounds bounds = panel.localToScene(panel.getLayoutBounds());
            double[] point = {bounds.getMinX() + 2, bounds.getMinY() + 2};
            assertNull(panel.slotAt(point[0], point[1]), "the test point must not be over a slot");
            return point;
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

    private void rightClick(StackPane slot, double[] at) {
        fire(slot, MouseEvent.MOUSE_PRESSED, MouseButton.SECONDARY, at, true, true);
        fire(slot, MouseEvent.MOUSE_RELEASED, MouseButton.SECONDARY, at, true, false);
    }

    private boolean dragging() {
        return FxToolkitSupport.onFx(panel::isDragging);
    }

    private boolean hasGhost() {
        return FxToolkitSupport.onFx(() -> panel.getChildren().stream().anyMatch(node -> !node.isManaged()));
    }

    private int groundQuantity(ItemType type) {
        return state.getDroppedItems().stream()
                .filter(drop -> drop.getItemType() == type)
                .mapToInt(DroppedItem::getQuantity)
                .sum();
    }

    // ------------------------------------------------------------------
    //  Dragging out of the panel
    // ------------------------------------------------------------------

    @Test
    void draggingAStackOutsideThePanelThrowsTheWholeStack() {
        StackPane source = slot(player, 3);
        press(source);
        moveTo(source, new double[]{centerOf(source)[0] + 30, centerOf(source)[1]});
        assertTrue(dragging());
        assertTrue(hasGhost());

        release(source, OUTSIDE);

        assertFalse(dragging());
        assertFalse(hasGhost());
        assertNull(player.getItemAt(3));
        assertEquals(5, groundQuantity(ItemType.STONE));
        assertTrue(transfers.isEmpty());
    }

    @Test
    void releasingOverThePanelBackgroundThrowsTheStackToo() {
        StackPane source = slot(player, 3);
        press(source);
        moveTo(source, panelBackground());

        release(source, panelBackground());

        assertNull(player.getItemAt(3));
        assertEquals(5, groundQuantity(ItemType.STONE));
    }

    @Test
    void theThrownStackKeepsItsSlotUntilTheMouseIsReleased() {
        StackPane source = slot(player, 3);
        press(source);

        moveTo(source, OUTSIDE);

        assertEquals(5, player.getItemAt(3).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    // ------------------------------------------------------------------
    //  Dragging between slots
    // ------------------------------------------------------------------

    @Test
    void releasingOverAnotherSlotMovesTheStackThereAndThrowsNothing() {
        StackPane source = slot(player, 3);
        StackPane target = slot(player, 9);
        press(source);
        moveTo(source, centerOf(target));

        release(source, centerOf(target));

        assertEquals(List.of("3->9 Stone"), transfers);
        assertTrue(state.getDroppedItems().isEmpty());
        assertFalse(dragging());
    }

    @Test
    void releasingOverTheSourceSlotChangesNothing() {
        StackPane source = slot(player, 3);
        press(source);
        double[] away = {centerOf(source)[0] + 12, centerOf(source)[1] + 12};
        moveTo(source, away);
        assertTrue(dragging());

        release(source, centerOf(source));

        assertTrue(transfers.isEmpty());
        assertTrue(state.getDroppedItems().isEmpty());
        assertEquals(5, player.getItemAt(3).getQuantity());
        assertFalse(dragging());
    }

    @Test
    void aStackCanBeDraggedOutOfAnOpenChest() {
        Chest chest = new Chest(0, 0, 9);
        chest.setItemAt(2, new Item(ItemType.WOOD_PLANK, 4));
        FxToolkitSupport.onFx(() -> {
            panel.openDualInventory(chest);
            panel.getParent().applyCss();
            ((Pane) panel.getParent()).layout();
            return null;
        });
        StackPane source = slot(chest, 2);
        press(source);
        moveTo(source, OUTSIDE);

        release(source, OUTSIDE);

        assertNull(chest.getItemAt(2));
        assertEquals(4, groundQuantity(ItemType.WOOD_PLANK));
    }

    // ------------------------------------------------------------------
    //  Right click while dragging
    // ------------------------------------------------------------------

    @Test
    void aRightClickWhileDraggingThrowsOneItemAndKeepsDraggingTheRest() {
        StackPane source = slot(player, 3);
        press(source);
        moveTo(source, OUTSIDE);

        rightClick(source, OUTSIDE);

        assertEquals(4, player.getItemAt(3).getQuantity());
        assertEquals(1, groundQuantity(ItemType.STONE));
        assertTrue(dragging(), "the rest of the stack is still in hand");
        assertTrue(hasGhost());

        rightClick(source, OUTSIDE);
        release(source, OUTSIDE);

        assertNull(player.getItemAt(3), "releasing throws what is left");
        assertEquals(5, groundQuantity(ItemType.STONE));
        assertFalse(dragging());
    }

    @Test
    void throwingTheLastItemWithARightClickEndsTheDrag() {
        StackPane source = slot(player, 6);
        press(source);
        moveTo(source, OUTSIDE);

        rightClick(source, OUTSIDE);

        assertNull(player.getItemAt(6));
        assertEquals(1, groundQuantity(ItemType.WOOD_LOG));
        assertFalse(dragging());
        assertFalse(hasGhost());
    }

    @Test
    void aRightClickWithoutADragThrowsNothing() {
        StackPane source = slot(player, 3);

        rightClick(source, centerOf(source));

        assertEquals(5, player.getItemAt(3).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    // ------------------------------------------------------------------
    //  Cases that must not drag
    // ------------------------------------------------------------------

    @Test
    void aTinyMovementIsAClickAndNotADrag() {
        StackPane source = slot(player, 3);
        press(source);
        double[] nudged = {centerOf(source)[0] + 1, centerOf(source)[1] + 1};

        moveTo(source, nudged);
        release(source, nudged);

        assertFalse(dragging());
        assertEquals(5, player.getItemAt(3).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void anEmptySlotCannotBeDragged() {
        StackPane empty = slot(player, 11);
        press(empty);

        moveTo(empty, OUTSIDE);
        release(empty, OUTSIDE);

        assertFalse(dragging());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void closingThePanelMidDragPutsTheStackBack() {
        StackPane source = slot(player, 3);
        press(source);
        moveTo(source, OUTSIDE);
        assertTrue(dragging());

        FxToolkitSupport.onFx(() -> {
            panel.closeInventory();
            return null;
        });
        release(source, OUTSIDE);

        assertFalse(dragging());
        assertFalse(hasGhost());
        assertEquals(5, player.getItemAt(3).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void aStackThatLeftItsSlotMidDragIsNotThrownTwice() {
        StackPane source = slot(player, 3);
        press(source);
        moveTo(source, OUTSIDE);
        player.setItemAt(3, null);

        release(source, OUTSIDE);

        assertTrue(state.getDroppedItems().isEmpty());
        assertNull(player.getItemAt(3));
        assertFalse(dragging());
    }

    @Test
    void theSourceSlotIsDimmedWhileDraggingAndRestoredAfter() {
        StackPane source = slot(player, 3);
        press(source);
        moveTo(source, OUTSIDE);
        assertTrue(FxToolkitSupport.onFx(() -> source.getOpacity() < 1.0));

        release(source, OUTSIDE);

        assertEquals(1.0, FxToolkitSupport.onFx(source::getOpacity));
    }
}
