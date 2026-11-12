package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.CraftRecipeCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemDropCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemTransferCallback;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Behaviour of the {@link InventoryPanel}: dragging stacks between slots and out of the panel, slot and recipe
 * selection, and the craft action. The panel keeps the layout and drawing, this class keeps the decisions and the
 * callbacks.
 * <p>
 * The drag is tracked here instead of with the platform drag and drop, which delivers no mouse events while it runs
 * and so cannot see the right button. The press on a slot is remembered, and a filter on the scene follows the mouse
 * from then on, wherever it goes: past a few pixels the stack is picked up, a right click throws one item and keeps
 * the rest in hand, and releasing the left button drops the stack on a slot, throws it into the world when the mouse
 * is outside the panel, or puts it back when the mouse is over the panel but over no slot.
 */
@Slf4j
@RequiredArgsConstructor
class InventoryController {
    /** Distance the mouse must travel with the button held before a press on a slot becomes a drag. */
    private static final double DRAG_THRESHOLD_PIXELS = 4.0;
    /** Opacity of the slot a stack is being dragged from. */
    private static final double DRAGGED_SLOT_OPACITY = 0.45;

    private final InventoryPanel view;
    private final GameStateManager gameState;

    @Setter
    private ItemTransferCallback onItemDroppedInsideCallback;
    @Setter
    private ItemDropCallback onItemDroppedCallback;
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

    /** Slot the mouse is over, or null. */
    private StackPane hoveredSlot;
    /** Slot the left button went down on and has not come up yet, or null. */
    private StackPane pressedSlot;
    private double pressSceneX;
    private double pressSceneY;
    /** The stack in hand, or null when nothing is being dragged. */
    private DragSession session;
    /** Slot highlighted as the place the stack would go, or null. */
    private StackPane highlightedTarget;
    /** True from the moment a drag starts until the next press: the click that ends the gesture selects nothing. */
    private boolean swallowClick;

    /** Window whose focus is being followed, or null. */
    private Window watchedWindow;

    private final EventHandler<MouseEvent> sceneMouseFilter = this::handleSceneMouse;
    private final ChangeListener<Boolean> focusListener = (observable, wasFocused, focused) -> handleWindowFocus(focused);
    private final ChangeListener<Window> windowListener = (observable, oldWindow, newWindow) -> {
        unwatchFocus(oldWindow);
        watchFocus(newWindow);
    };

    // ==================================================================
    //  Hover
    // ==================================================================

    void handleSlotHover(StackPane slot, boolean entered) {
        if (entered) {
            hoveredSlot = slot;
        } else if (hoveredSlot == slot) {
            hoveredSlot = null;
        }
    }

    /** Forgets the hovered slot, for when the panel closes and no mouse exit event will come. */
    void resetHover() {
        hoveredSlot = null;
    }

    /**
     * The slot an action from the keyboard applies to: the one under the mouse, or the selected one when the mouse is
     * over none.
     *
     * @return the slot, or null when no slot is hovered or selected
     */
    SlotRef hoveredOrSelectedSlot() {
        if (hoveredSlot != null) {
            return InventoryPanel.refOf(hoveredSlot);
        }
        if (selectedSlotIndex == -1) {
            return null;
        }
        Container container = selectedSlotInPlayerInventory ? gameState.getPlayer() : view.getSecondaryContainer();
        return container == null ? null : new SlotRef(container, selectedSlotIndex);
    }

    // ==================================================================
    //  Drag
    // ==================================================================

    /**
     * Follows the mouse on a scene for as long as the panel is part of it.
     *
     * @param oldScene scene the panel left, or null
     * @param newScene scene the panel joined, or null
     */
    void bindScene(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.removeEventFilter(MouseEvent.ANY, sceneMouseFilter);
            oldScene.windowProperty().removeListener(windowListener);
            unwatchFocus(oldScene.getWindow());
        }
        if (newScene != null) {
            newScene.addEventFilter(MouseEvent.ANY, sceneMouseFilter);
            newScene.windowProperty().addListener(windowListener);
            watchFocus(newScene.getWindow());
        }
    }

    private void watchFocus(Window window) {
        if (window != null) {
            window.focusedProperty().addListener(focusListener);
            watchedWindow = window;
        }
    }

    private void unwatchFocus(Window window) {
        if (window != null) {
            window.focusedProperty().removeListener(focusListener);
            if (watchedWindow == window) {
                watchedWindow = null;
            }
        }
    }

    /**
     * Test seam: the window whose focus property currently has {@link #focusListener} registered. The focus of a
     * window cannot be flipped from a test, so a test checks the registration here and fires the listener through
     * {@link #focusListener()}.
     *
     * @return the watched window, or null when none is watched
     */
    Window watchedWindow() {
        return watchedWindow;
    }

    /**
     * Test seam: the very listener that {@link #bindScene} registers on the focus property of the window, so a test
     * can run what the window would run when it loses the focus.
     *
     * @return the focus listener
     */
    ChangeListener<Boolean> focusListener() {
        return focusListener;
    }

    /**
     * Tells whether a stack is being dragged.
     *
     * @return true while a stack is in hand
     */
    boolean isDragging() {
        return session != null;
    }

    /**
     * Reacts to the window gaining or losing the focus. A window that loses it never reports the release of a mouse
     * button held at that moment, so a stack in hand goes back to its slot instead of staying stuck to the mouse.
     *
     * @param focused true when the window has the focus
     */
    void handleWindowFocus(boolean focused) {
        if (!focused) {
            cancelDrag();
        }
    }

    /** Remembers a left button press on a slot: it becomes a drag once the mouse has moved far enough. */
    void handleSlotPressed(MouseEvent event, StackPane slot) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        pressedSlot = slot;
        pressSceneX = event.getSceneX();
        pressSceneY = event.getSceneY();
    }

    /** Puts a stack in hand back without changing anything, for when the panel closes or the window loses focus. */
    void cancelDrag() {
        if (session != null) {
            log.debug("DRAG CANCEL: {}", session.item().getDisplayName());
        }
        endDrag();
        pressedSlot = null;
    }

    /**
     * Brings the stack in hand in line with its slot after the inventory changed: the ghost shows the quantity the
     * slot holds now, and a stack that left its slot, for example thrown with the Q key or picked up, ends the drag.
     */
    void refreshDrag() {
        if (session == null) {
            return;
        }
        if (session.isStillValid()) {
            view.refreshDragGhost(session.item());
        } else {
            cancelDrag();
        }
    }

    private void handleSceneMouse(MouseEvent event) {
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            handleScenePressed(event);
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            handleSceneDragged(event);
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            handleSceneReleased(event);
        } else if (session != null && event.getButton() == MouseButton.SECONDARY) {
            // The click that follows a right press must not select the slot under the mouse
            event.consume();
        }
    }

    private void handleScenePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            swallowClick = false;
        }
        if (session == null) {
            return;
        }
        if (event.getButton() == MouseButton.SECONDARY) {
            event.consume();
            dropOneFromHand();
        } else if (event.getButton() == MouseButton.PRIMARY) {
            // A new left press while a stack is in hand means the release of the last drag was never seen
            cancelDrag();
        }
    }

    private void handleSceneDragged(MouseEvent event) {
        if (session == null && pressedSlot != null && event.isPrimaryButtonDown()
                && Math.hypot(event.getSceneX() - pressSceneX, event.getSceneY() - pressSceneY) >= DRAG_THRESHOLD_PIXELS) {
            startDrag(event);
        }
        if (session == null) {
            return;
        }
        view.moveDragGhost(event.getSceneX(), event.getSceneY());
        highlightTargetAt(event.getSceneX(), event.getSceneY());
    }

    private void handleSceneReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            if (session != null) {
                event.consume();
            }
            return;
        }
        if (session != null) {
            finishDrag(event);
            event.consume();
        }
        pressedSlot = null;
    }

    private void startDrag(MouseEvent event) {
        SlotRef origin = InventoryPanel.refOf(pressedSlot);
        Item item = origin.item();
        if (item == null || item.getQuantity() <= 0) {
            pressedSlot = null;
            return;
        }
        session = new DragSession(origin);
        swallowClick = true;
        pressedSlot.setOpacity(DRAGGED_SLOT_OPACITY);
        view.showDragGhost(item, event.getSceneX(), event.getSceneY());
        log.debug("DRAG START: {}x {} from slot {}", item.getQuantity(), item.getDisplayName(), origin.slot());
    }

    private void finishDrag(MouseEvent event) {
        DragSession finished = session;
        StackPane under = view.slotNodeAt(event.getSceneX(), event.getSceneY());
        SlotRef target = under == null ? null : InventoryPanel.refOf(under);
        boolean overPanel = view.containsScenePoint(event.getSceneX(), event.getSceneY());
        endDrag();
        // The mouse button was held, so no enter or exit event told which slot the mouse ended up over
        hoveredSlot = under;

        DragSession.Release outcome = finished.release(target, overPanel);
        log.debug("DRAG RELEASE: {} {}", finished.item().getDisplayName(), outcome);
        switch (outcome) {
            case TRANSFER -> {
                if (onItemDroppedInsideCallback != null) {
                    onItemDroppedInsideCallback.transfer(finished.origin().slot(), finished.origin().container(),
                            target.slot(), target.container(), finished.item());
                }
            }
            case DROP_STACK -> {
                if (onItemDroppedCallback != null) {
                    onItemDroppedCallback.drop(finished.origin(), true);
                }
            }
            case CANCEL -> { }
        }
    }

    /** A right click while dragging throws one item and keeps the rest in hand. */
    private void dropOneFromHand() {
        DragSession inHand = session;
        if (!inHand.isStillValid()) {
            cancelDrag();
            return;
        }
        boolean thrown = onItemDroppedCallback != null && onItemDroppedCallback.drop(inHand.origin(), false);
        log.debug("DRAG DROP ONE: {} thrown={}", inHand.item().getDisplayName(), thrown);
        // The callback may already have refreshed the panel, which ends the drag when the stack is gone
        refreshDrag();
    }

    private void highlightTargetAt(double sceneX, double sceneY) {
        StackPane under = view.slotNodeAt(sceneX, sceneY);
        if (under == pressedSlot) {
            under = null;
        }
        if (under == highlightedTarget) {
            return;
        }
        clearTargetHighlight();
        if (under != null) {
            view.showDropHighlight(under);
            highlightedTarget = under;
        }
    }

    private void clearTargetHighlight() {
        if (highlightedTarget != null) {
            view.restoreSlotBorder(highlightedTarget);
            highlightedTarget = null;
            // The border of a slot is shared by the drop highlight and the selection, so the selection is drawn again
            view.highlightSlot(selectedSlotIndex, true);
        }
    }

    private void endDrag() {
        clearTargetHighlight();
        if (pressedSlot != null) {
            pressedSlot.setOpacity(1.0);
        }
        view.hideDragGhost();
        session = null;
    }

    // ==================================================================
    //  Selection
    // ==================================================================

    void handleSlotClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (swallowClick) {
            // The click that follows the release of a drag is part of the drag, not a selection
            swallowClick = false;
            event.consume();
            return;
        }
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

        log.debug("CRAFT CLICK: {}", recipe.getName());
        onCraftRecipeCallback.craft(recipe);
    }
}
