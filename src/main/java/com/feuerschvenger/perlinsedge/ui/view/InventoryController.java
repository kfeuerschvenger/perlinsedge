package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.CraftRecipeCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemDroppedOutsideCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemTransferCallback;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Behaviour of the {@link InventoryPanel}: drag and drop between slots, slot and recipe selection,
 * and the craft action. The panel keeps the layout and drawing, this class keeps the decisions and the callbacks.
 */
@Slf4j
@RequiredArgsConstructor
class InventoryController {

    private final InventoryPanel view;
    private final GameStateManager gameState;

    @Getter
    @Setter
    private ItemTransferCallback onItemDroppedInsideCallback;
    @Getter
    @Setter
    private ItemDroppedOutsideCallback onItemDroppedOutsideCallback;
    @Setter
    private CraftRecipeCallback onCraftRecipeCallback;

    /** Index of the selected slot, or -1 when no slot is selected. */
    @Getter
    @Setter
    private int selectedSlotIndex = -1;
    /** True when the selected slot belongs to the player inventory grid. */
    @Getter
    private boolean selectedSlotInPlayerInventory = true;
    /** Recipe selected in the crafting list, or null. */
    @Getter
    @Setter
    private Recipe selectedRecipe;

    // ==================================================================
    //  Drag and drop
    // ==================================================================

    void handleDragDetected(MouseEvent event, StackPane slot, int slotIndex, Container container) {
        Item item = view.getItemInSlot(slotIndex, container);
        if (item == null || item.getQuantity() <= 0) return;

        Image dragImage = view.getItemImage(item);
        if (dragImage == null) {
            log.debug("DRAG FAIL: No image for {}", item.getDisplayName());
            return;
        }

        Dragboard db = slot.startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString("item");
        db.setContent(content);

        slot.getProperties().put("draggedItem", item);
        slot.getProperties().put("draggedFromContainer", container);
        slot.getProperties().put("draggedFromSlotIndex", slotIndex);

        Canvas snapshotCanvas = new Canvas(InventoryPanel.ITEM_SIZE, InventoryPanel.ITEM_SIZE);
        GraphicsContext gc = snapshotCanvas.getGraphicsContext2D();
        gc.drawImage(dragImage, 0, 0, InventoryPanel.ITEM_SIZE, InventoryPanel.ITEM_SIZE);
        db.setDragView(snapshotCanvas.snapshot(null, null), event.getX(), event.getY());

        log.debug("DRAG START: {} from slot {}", item.getDisplayName(), slotIndex);
        event.consume();
    }

    void handleDragOver(DragEvent event, StackPane slot, int slotIndex) {
        if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
            view.showDropHighlight(slot);
            event.acceptTransferModes(TransferMode.MOVE);
            log.trace("DRAG OVER: Slot {} - accepted", slotIndex);
        } else {
            log.trace("DRAG OVER: Slot {} - rejected", slotIndex);
        }
        event.consume();
    }

    void handleDragDropped(DragEvent event, StackPane slot) {
        view.restoreSlotBorder(slot);
        log.debug("DRAG DROP: Slot {}", ((InventoryPanel.SlotContext) slot.getUserData()).index);

        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasString() && event.getGestureSource() instanceof Node sourceNode) {
            Item draggedItem = (Item) sourceNode.getProperties().get("draggedItem");
            Container sourceContainer = (Container) sourceNode.getProperties().get("draggedFromContainer");
            Integer sourceSlotIndex = (Integer) sourceNode.getProperties().get("draggedFromSlotIndex");
            InventoryPanel.SlotContext targetContext = (InventoryPanel.SlotContext) slot.getUserData();

            if (validateDragOperation(draggedItem, sourceContainer, sourceSlotIndex)) {
                log.debug("DRAG DROP: {} from slot {} to {}",
                        draggedItem.getDisplayName(), sourceSlotIndex, targetContext.index);

                if (onItemDroppedInsideCallback != null) {
                    onItemDroppedInsideCallback.transfer(
                            sourceSlotIndex, sourceContainer,
                            targetContext.index, targetContext.container,
                            draggedItem
                    );
                    success = true;
                }
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private boolean validateDragOperation(Item draggedItem, Container sourceContainer, Integer sourceSlotIndex) {
        if (draggedItem == null) {
            log.debug("DRAG DROP: Missing dragged item");
            return false;
        }
        if (sourceContainer == null) {
            log.debug("DRAG DROP: Missing source container");
            return false;
        }
        if (sourceSlotIndex == null) {
            log.debug("DRAG DROP: Missing source slot index");
            return false;
        }
        return true;
    }

    // ==================================================================
    //  Selection
    // ==================================================================

    void handleSlotClick(MouseEvent event) {
        InventoryPanel.SlotContext context = (InventoryPanel.SlotContext) ((Node) event.getSource()).getUserData();
        log.debug("SLOT CLICK: Slot {}", context.index);

        handleSlotSelection(context.index, context.container == gameState.getPlayer());
        event.consume();
    }

    private void handleSlotSelection(int clickedSlotIndex, boolean isPlayerInventorySlot) {
        Container container = isPlayerInventorySlot ? gameState.getPlayer() : view.getSecondaryContainer();
        Item item = view.getItemInSlot(clickedSlotIndex, container);

        if (item != null) {
            updateSlotSelection(clickedSlotIndex, isPlayerInventorySlot);
            selectedRecipe = null;
        } else {
            view.clearSelection();
        }
        Platform.runLater(view::updateInventoryDisplay);
    }

    private void updateSlotSelection(int slotIndex, boolean isPlayerInventory) {
        if (selectedSlotIndex != -1) {
            view.highlightSlot(selectedSlotIndex, false);
        }

        selectedSlotIndex = slotIndex;
        selectedSlotInPlayerInventory = isPlayerInventory;
        view.highlightSlot(selectedSlotIndex, true);
    }

    void handleRecipeSelected(Recipe recipe) {
        if (selectedSlotIndex != -1) {
            view.highlightSlot(selectedSlotIndex, false);
            selectedSlotIndex = -1;
        }

        this.selectedRecipe = recipe;
        view.showRecipeInfo(recipe);
    }

    // ==================================================================
    //  Crafting
    // ==================================================================

    void handleCraftButtonClick() {
        Recipe recipe = view.getSelectedCraftingRecipe();
        if (recipe == null) {
            log.warn("Crafting failed: No recipe selected");
            return;
        }
        if (onCraftRecipeCallback == null) {
            log.warn("Crafting failed: Callback not set");
            return;
        }

        onCraftRecipeCallback.craft(recipe);
        view.updateInventoryDisplay();
    }
}
