package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * The hotbar of the items the player carries: nine slots of their own, ahead of the inventory slots, always shown at
 * the bottom of the screen. The selected slot has the white border, the count sits at the bottom
 * right and the number key that selects the slot at the top left, in the same dark rounded squares as the building
 * palette.
 * The panel shows what the player holds and keeps no state the player does not have. {@link #update(Player)} redraws
 * only the slots whose item, count or selection changed, so it is cheap enough to call on every frame.
 */
public final class ItemHotbarPanel extends HBox {
    /** Slot edge in whole pixels; the UI layer never scales with the world zoom, so it stays this size. */
    public static final double SLOT_SIZE = 48;
    /** Gap between slots in whole pixels. */
    private static final double SLOT_GAP = 4;
    /** Icon edge: the 16 px art at 2x, so every art pixel stays a whole square block. */
    private static final int ICON_SIZE = 32;
    private static final double COUNT_MARGIN = 3;

    private final StackPane[] slots = new StackPane[Player.HOTBAR_SIZE];
    private final ItemType[] shownTypes = new ItemType[Player.HOTBAR_SIZE];
    private final int[] shownQuantities = new int[Player.HOTBAR_SIZE];
    private int shownSelected = -1;

    /** Creates the panel with nine empty slots. */
    public ItemHotbarPanel() {
        // No panel behind the row: the slots float over the world, each one dark and translucent on its own
        setAlignment(Pos.BOTTOM_CENTER);
        setSpacing(SLOT_GAP);
        setPickOnBounds(false);
        for (var i = 0; i < slots.length; i++) {
            slots[i] = createSlot(i);
            getChildren().add(slots[i]);
        }
    }

    /**
     * Shows the hotbar slots of a player.
     *
     * @param player the player whose first slots are shown; null shows empty slots with nothing selected
     */
    public void update(Player player) {
        final int selected = player != null ? player.getSelectedHotbarIndex() : -1;
        for (var i = 0; i < slots.length; i++) {
            final Item item = player != null ? player.getItemAt(i) : null;
            final ItemType type = item != null ? item.getType() : null;
            final int quantity = item != null ? item.getQuantity() : 0;
            if (type != shownTypes[i] || quantity != shownQuantities[i]) {
                shownTypes[i] = type;
                shownQuantities[i] = quantity;
                fillSlot(i, type, quantity);
            }
            if (selected != shownSelected) {
                UiTheme.setClass(slots[i], UiTheme.SLOT_SELECTED, i == selected);
            }
        }
        shownSelected = selected;
    }

    /**
     * Tells which slot carries the selection border.
     *
     * @return the slot index, or -1 when none is selected
     */
    int getShownSelectedIndex() {
        return shownSelected;
    }

    /**
     * Gives the slot node, for the inventory panel that wires the slots to its drag and drop and for the tests.
     *
     * @param index slot index
     * @return the node of the slot
     */
    StackPane slotNode(int index) {
        return slots[index];
    }

    private static StackPane createSlot(int index) {
        var slot = new StackPane();
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMinSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMaxSize(SLOT_SIZE, SLOT_SIZE);
        slot.getStyleClass().addAll(UiTheme.SLOT, UiTheme.SLOT_EMPTY);
        slot.getChildren().add(createNumberLabel(index + 1));
        return slot;
    }

    private void fillSlot(int index, ItemType type, int quantity) {
        StackPane slot = slots[index];
        slot.getChildren().clear();
        UiTheme.setClass(slot, UiTheme.SLOT_EMPTY, type == null);
        if (type != null) {
            slot.getChildren().add(ItemIcons.view(type, ICON_SIZE));
            if (quantity > 1) {
                slot.getChildren().add(createCountLabel(quantity));
            }
        }
        slot.getChildren().add(createNumberLabel(index + 1));
    }

    /** The key number in the top left corner of a slot. */
    private static Label createNumberLabel(int number) {
        var numberLabel = new Label(String.valueOf(number));
        numberLabel.getStyleClass().add(UiTheme.SLOT_NUMBER);
        numberLabel.setMouseTransparent(true);
        StackPane.setAlignment(numberLabel, Pos.TOP_LEFT);
        return numberLabel;
    }

    /** The stack size in the pixel font with a 1 px dark shadow, at the bottom right corner of the slot. */
    private static Label createCountLabel(int quantity) {
        var count = new Label(String.valueOf(quantity));
        count.getStyleClass().add(UiTheme.COUNT);
        count.setMouseTransparent(true);
        StackPane.setAlignment(count, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(count, new Insets(0, COUNT_MARGIN, 0, 0));
        return count;
    }
}
