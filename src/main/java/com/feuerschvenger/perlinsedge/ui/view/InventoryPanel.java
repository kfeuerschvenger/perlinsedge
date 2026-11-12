package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxItemDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.CraftRecipeCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemDropCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemTransferCallback;
import com.feuerschvenger.perlinsedge.ui.view.utils.GridUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * View of the inventory systems including:
 * <ul>
 * <li> Player inventory</li>
 * <li> Secondary containers (chests, etc.)</li>
 * <li> Crafting recipes display</li>
 * <li> Item information panel</li>
 *</ul><br>
 * Owns the layout and drawing. Drag and drop, selection and crafting are handled by
 * {@link InventoryController}, which also keeps the callbacks exposed here.
 */
public class InventoryPanel extends BorderPane {
    // Constants
    public static final int DEFAULT_CAPACITY = 9;

    // Layout dimensions
    private static final int SLOT_SIZE = 44;
    /** Item icon size: the 16 px art at 2x, so every art pixel stays a whole square block. */
    static final int ITEM_SIZE = 32;
    private static final int ITEM_SPACING = 10;
    private static final int COUNT_MARGIN = 3;
    private static final double DRAG_GHOST_OPACITY = 0.85;

    // Text constants
    private static final String PLAYER_INVENTORY_TITLE = "Player Inventory";
    private static final String CHEST_INVENTORY_TITLE = "Chest Inventory";

    // UI components
    private GridPane playerInventoryGrid;
    private GridPane secondaryInventoryGrid;
    private Label secondaryInventoryLabel;
    private ItemInfoPanel itemInfoPanel;
    private CraftingListPanel craftingListPanel;
    /** The item that follows the mouse while a stack is dragged, or null. */
    private StackPane dragGhost;

    // State management
    private final GameStateManager gameState;
    /** The behaviour of the panel, package-private so the tests can reach its window focus wiring. */
    @Getter(lombok.AccessLevel.PACKAGE)
    private final InventoryController controller;
    @Getter(lombok.AccessLevel.PACKAGE)
    private Container secondaryContainer;

    /**
     * Initializes the inventory panel with game state reference.
     *
     * @param gameStateManager Game state manager (must not be null)
     */
    public InventoryPanel(GameStateManager gameStateManager) {
        this.gameState = Objects.requireNonNull(gameStateManager, "Game state manager cannot be null");
        this.controller = new InventoryController(this, gameState);
        this.secondaryContainer = null;

        configurePanel();
        initializeComponents();
        setupEventHandlers();
        sceneProperty().addListener((observable, oldScene, newScene) -> controller.bindScene(oldScene, newScene));

        setVisible(false);
        setManaged(false);
    }

    private void configurePanel() {
        getStyleClass().add(UiTheme.PANEL);
        setPrefSize(800, 300);
    }

    private void initializeComponents() {
        // Item information panel (left section)
        itemInfoPanel = new ItemInfoPanel();
        setLeft(itemInfoPanel);
        setAlignment(itemInfoPanel, Pos.CENTER_LEFT);
        BorderPane.setMargin(itemInfoPanel, new Insets(0, ITEM_SPACING, 0, 0));

        // Player inventory grid (center section)
        VBox centerContainer = createCenterContainer();
        setCenter(centerContainer);
        BorderPane.setMargin(centerContainer, new Insets(0, ITEM_SPACING, 0, ITEM_SPACING));

        // Crafting list and secondary inventory (right section)
        VBox rightContainer = createRightContainer();
        setRight(rightContainer);
        BorderPane.setMargin(rightContainer, new Insets(0, 0, 0, ITEM_SPACING));
    }

    private VBox createCenterContainer() {
        VBox container = new VBox(ITEM_SPACING);
        container.setAlignment(Pos.CENTER);

        int playerCapacity = (gameState.getPlayer() != null) ?
                gameState.getPlayer().getCapacity() :
                2 * DEFAULT_CAPACITY;

        List<StackPane> slots = createInventorySlots(playerCapacity, gameState.getPlayer());
        playerInventoryGrid = GridUtils.createPlayerGrid(slots);

        container.getChildren().addAll(createTitle(PLAYER_INVENTORY_TITLE), playerInventoryGrid);
        return container;
    }

    private List<StackPane> createInventorySlots(int capacity, Container container) {
        List<StackPane> slots = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            slots.add(createInventorySlot(i, container));
        }
        return slots;
    }

    private VBox createRightContainer() {
        VBox container = new VBox(ITEM_SPACING);
        container.setAlignment(Pos.TOP_CENTER);

        craftingListPanel = new CraftingListPanel();
        craftingListPanel.setPrefWidth(375);

        // Secondary inventory section (initially hidden)
        secondaryInventoryLabel = createTitle(CHEST_INVENTORY_TITLE);
        secondaryInventoryGrid = GridUtils.createGrid(null);

        container.getChildren().add(craftingListPanel);
        return container;
    }

    private void setupEventHandlers() {
        craftingListPanel.setOnRecipeSelected(controller::handleRecipeSelected);
        craftingListPanel.setOnCraftButtonClicked(controller::handleCraftButtonClick);
    }

    private Label createTitle(String text) {
        Label label = new Label(UiTheme.caps(text));
        label.getStyleClass().add(UiTheme.PILL);
        return label;
    }

    // ==================================================================
    //  Slots
    // ==================================================================

    private StackPane createInventorySlot(int slotIndex, Container container) {
        StackPane slot = new StackPane();
        configureSlotLayout(slot);
        slot.setUserData(new SlotContext(slotIndex, container));

        slot.setOnMousePressed(e -> controller.handleSlotPressed(e, slot));
        slot.setOnMouseClicked(controller::handleSlotClick);
        slot.setOnMouseEntered(e -> controller.handleSlotHover(slot, true));
        slot.setOnMouseExited(e -> controller.handleSlotHover(slot, false));

        return slot;
    }

    private void configureSlotLayout(StackPane slot) {
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMinSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMaxSize(SLOT_SIZE, SLOT_SIZE);
        slot.getStyleClass().add(UiTheme.SLOT);
    }

    /** Highlights a slot as a valid drop target. */
    void showDropHighlight(StackPane slot) {
        UiTheme.setClass(slot, UiTheme.SLOT_SELECTED, false);
        UiTheme.setClass(slot, UiTheme.SLOT_DROP, true);
    }

    /** Restores a slot's default border. */
    void restoreSlotBorder(StackPane slot) {
        UiTheme.setClass(slot, UiTheme.SLOT_SELECTED, false);
        UiTheme.setClass(slot, UiTheme.SLOT_DROP, false);
    }

    /** Retrieves an item from a specific slot in a container. */
    Item getItemInSlot(int slotIndex, Container container) {
        if (container == null || slotIndex < 0 || slotIndex >= container.getCapacity()) {
            return null;
        }
        return container.getItemAt(slotIndex);
    }

    /** Context object for inventory slots, stored as the slot user data. */
    static class SlotContext {
        int index;
        Container container;

        SlotContext(int index, Container container) {
            this.index = index;
            this.container = container;
        }
    }

    /**
     * Reads the slot a slot node stands for.
     *
     * @param slot a slot node of one of the grids
     * @return the slot a slot node stands for
     */
    static SlotRef refOf(StackPane slot) {
        SlotContext context = (SlotContext) slot.getUserData();
        return new SlotRef(context.container, context.index);
    }

    /**
     * Finds the slot under a point of the scene, in the player grid or in the grid of the open container.
     *
     * @param sceneX X coordinate in the scene
     * @param sceneY Y coordinate in the scene
     * @return the slot node, or null when the point is over no slot
     */
    StackPane slotNodeAt(double sceneX, double sceneY) {
        StackPane slot = slotNodeAt(playerInventoryGrid, sceneX, sceneY);
        if (slot == null && secondaryContainer != null) {
            slot = slotNodeAt(secondaryInventoryGrid, sceneX, sceneY);
        }
        return slot;
    }

    private static StackPane slotNodeAt(GridPane grid, double sceneX, double sceneY) {
        if (grid == null) {
            return null;
        }
        for (Node node : grid.getChildren()) {
            if (node instanceof StackPane slot && slot.localToScene(slot.getLayoutBounds()).contains(sceneX, sceneY)) {
                return slot;
            }
        }
        return null;
    }

    // ==================================================================
    //  Item in hand while dragging
    // ==================================================================

    /**
     * Shows the dragged item under the mouse. The ghost belongs to no layout, so it can float over the whole panel
     * and past its edges.
     *
     * @param item   the dragged stack
     * @param sceneX mouse X in the scene
     * @param sceneY mouse Y in the scene
     */
    void showDragGhost(Item item, double sceneX, double sceneY) {
        hideDragGhost();
        dragGhost = new StackPane();
        dragGhost.setManaged(false);
        dragGhost.setMouseTransparent(true);
        dragGhost.setOpacity(DRAG_GHOST_OPACITY);
        dragGhost.setMinSize(ITEM_SIZE, ITEM_SIZE);
        dragGhost.setPrefSize(ITEM_SIZE, ITEM_SIZE);
        dragGhost.setMaxSize(ITEM_SIZE, ITEM_SIZE);
        dragGhost.resize(ITEM_SIZE, ITEM_SIZE);
        fillDragGhost(item);
        getChildren().add(dragGhost);
        moveDragGhost(sceneX, sceneY);
    }

    /**
     * Redraws the dragged item in place, for when its quantity changed.
     *
     * @param item the dragged stack
     */
    void refreshDragGhost(Item item) {
        if (dragGhost != null) {
            fillDragGhost(item);
        }
    }

    private void fillDragGhost(Item item) {
        dragGhost.getChildren().clear();
        dragGhost.getChildren().add(createItemImage(item));
        if (item.getQuantity() > 1) {
            dragGhost.getChildren().add(createCountLabel(item));
        }
    }

    /**
     * Moves the dragged item to the mouse.
     *
     * @param sceneX mouse X in the scene
     * @param sceneY mouse Y in the scene
     */
    void moveDragGhost(double sceneX, double sceneY) {
        if (dragGhost == null) return;
        Point2D local = sceneToLocal(sceneX, sceneY);
        if (local != null) {
            dragGhost.relocate(local.getX() - ITEM_SIZE / 2.0, local.getY() - ITEM_SIZE / 2.0);
        }
    }

    /** Removes the dragged item from the screen. */
    void hideDragGhost() {
        if (dragGhost != null) {
            getChildren().remove(dragGhost);
            dragGhost = null;
        }
    }

    // ==================================================================
    //  Display updates
    // ==================================================================

    /**
     * Updates the inventory display based on current state. A stack in hand follows its slot: its ghost shows the
     * new quantity, and the drag ends when the stack is gone.
     */
    public void updateInventoryDisplay() {
        updateGridContents(playerInventoryGrid, gameState.getPlayer());

        if (secondaryContainer != null) {
            updateGridContents(secondaryInventoryGrid, secondaryContainer);
        }

        updateInformationDisplay();
        controller.refreshDrag();
    }

    /**
     * Tells whether a point of the scene is inside the panel, over a slot or not.
     *
     * @param sceneX X coordinate in the scene
     * @param sceneY Y coordinate in the scene
     * @return true when the point is within the bounds of the panel
     */
    boolean containsScenePoint(double sceneX, double sceneY) {
        return localToScene(getLayoutBounds()).contains(sceneX, sceneY);
    }

    /**
     * Tells whether a stack is being dragged.
     *
     * @return true from the moment a stack is picked up until it is released, put back or thrown
     */
    public boolean isStackInHand() {
        return controller.isDragging();
    }

    private void updateInformationDisplay() {
        if (controller.getSelectedSlotIndex() != -1) {
            updateItemSelectionDisplay();
        } else if (controller.getSelectedRecipe() != null) {
            itemInfoPanel.displayRecipe(controller.getSelectedRecipe());
        } else {
            itemInfoPanel.clearInfo();
        }
    }

    private void updateItemSelectionDisplay() {
        Container currentContainer = controller.isSelectedSlotInPlayerInventory() ?
                gameState.getPlayer() : secondaryContainer;

        if (currentContainer == null) {
            clearSelection();
            return;
        }

        Item currentItem = getItemInSlot(controller.getSelectedSlotIndex(), currentContainer);
        if (currentItem != null) {
            itemInfoPanel.displayItem(currentItem);
            highlightSlot(controller.getSelectedSlotIndex(), true);
        } else {
            clearSelection();
        }
    }

    /** Clears the slot selection and the information panel. */
    void clearSelection() {
        highlightSlot(controller.getSelectedSlotIndex(), false);
        controller.setSelectedSlotIndex(-1);
        itemInfoPanel.clearInfo();
    }

    /** Shows a recipe in the information panel. */
    void showRecipeInfo(Recipe recipe) {
        itemInfoPanel.displayRecipe(recipe);
    }

    /** @return the recipe selected in the crafting list, or null */
    Recipe getSelectedCraftingRecipe() {
        return craftingListPanel.getSelectedRecipe();
    }

    private void updateGridContents(GridPane grid, Container container) {
        int cols = grid.getColumnConstraints().size();

        for (Node node : grid.getChildren()) {
            if (node instanceof StackPane slot) {
                int row = GridPane.getRowIndex(node);
                int col = GridPane.getColumnIndex(node);
                int index = row * cols + col;

                updateSlotContext(slot, index, container);
                updateSlotVisuals(slot, index, container);
            }
        }
    }

    private void updateSlotContext(StackPane slot, int index, Container container) {
        SlotContext context = (SlotContext) slot.getUserData();
        context.index = index;
        context.container = container;
    }

    private void updateSlotVisuals(StackPane slot, int index, Container container) {
        slot.getChildren().clear();
        Item item = getItemInSlot(index, container);

        if (item != null) {
            slot.getChildren().add(createItemImage(item));
            if (item.getQuantity() > 1) {
                slot.getChildren().add(createCountLabel(item));
            }
        }
    }

    // ==================================================================
    //  Open and close
    // ==================================================================

    /** Opens the player inventory display. */
    public void openPlayerInventory() {
        this.secondaryContainer = null;
        cleanRightContainer();
        updateInventoryDisplay();
        setVisible(true);
        setManaged(true);
    }

    /**
     * Opens a dual inventory display (player + secondary container).
     *
     * @param otherContainer the secondary container, such as a chest
     */
    public void openDualInventory(Container otherContainer) {
        this.secondaryContainer = Objects.requireNonNull(otherContainer, "Secondary container cannot be null");
        initializeSecondaryInventory();
        configureDualInventoryLayout();
        updateInventoryDisplay();
        setVisible(true);
        setManaged(true);
    }

    private void initializeSecondaryInventory() {
        List<StackPane> slots = createInventorySlots(secondaryContainer.getCapacity(), secondaryContainer);
        secondaryInventoryGrid = GridUtils.createGrid(slots);
    }

    private void configureDualInventoryLayout() {
        VBox rightContainer = (VBox) getRight();
        rightContainer.getChildren().clear();
        rightContainer.getChildren().addAll(secondaryInventoryLabel, secondaryInventoryGrid);

        HBox centerContainer = new HBox(20);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.getChildren().addAll(
                createInventoryBox(PLAYER_INVENTORY_TITLE, playerInventoryGrid),
                createInventoryBox(CHEST_INVENTORY_TITLE, secondaryInventoryGrid)
        );
        setCenter(centerContainer);

        craftingListPanel.setVisible(false);
        craftingListPanel.setManaged(false);
    }

    private VBox createInventoryBox(String title, GridPane grid) {
        VBox box = new VBox(10);
        box.getChildren().addAll(createTitle(title), grid);
        return box;
    }

    /** Closes the inventory panel and resets state. */
    public void closeInventory() {
        controller.cancelDrag();
        this.secondaryContainer = null;
        resetLayout();
        clearState();
        setVisible(false);
        setManaged(false);
    }

    private void resetLayout() {
        VBox centerContainer = new VBox(ITEM_SPACING);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.getChildren().addAll(createTitle(PLAYER_INVENTORY_TITLE), playerInventoryGrid);
        setCenter(centerContainer);

        cleanRightContainer();
        craftingListPanel.setVisible(true);
        craftingListPanel.setManaged(true);
    }

    private void cleanRightContainer() {
        if (getRight() instanceof VBox rightContainer) {
            rightContainer.getChildren().removeAll(secondaryInventoryLabel, secondaryInventoryGrid);
            rightContainer.getChildren().setAll(craftingListPanel);
        }
    }

    private void clearState() {
        itemInfoPanel.clearInfo();
        highlightSlot(controller.getSelectedSlotIndex(), false);
        controller.setSelectedSlotIndex(-1);
        controller.setSelectedRecipe(null);
        controller.resetHover();
    }

    // ==================================================================
    //  Slot highlight and item visuals
    // ==================================================================

    /**
     * Highlights or unhighlights a slot of the grid that holds the current selection.
     *
     * @param index     Slot index to modify
     * @param highlight True to highlight, false to restore
     */
    void highlightSlot(int index, boolean highlight) {
        if (index < 0) return;

        GridPane targetGrid = controller.isSelectedSlotInPlayerInventory() ?
                playerInventoryGrid : secondaryInventoryGrid;

        if (targetGrid == null) return;

        int cols = targetGrid.getColumnConstraints().size();
        int row = index / cols;
        int col = index % cols;

        for (Node node : targetGrid.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                if (node instanceof StackPane slot) {
                    applySlotHighlight(slot, highlight);
                }
                break;
            }
        }
    }

    private void applySlotHighlight(StackPane slot, boolean highlight) {
        if (highlight) {
            UiTheme.setClass(slot, UiTheme.SLOT_DROP, false);
            UiTheme.setClass(slot, UiTheme.SLOT_SELECTED, true);
        } else {
            restoreSlotBorder(slot);
        }
    }

    private ImageView createItemImage(Item item) {
        ImageView imageView = new ImageView(getItemImage(item));
        imageView.setFitWidth(ITEM_SIZE);
        imageView.setFitHeight(ITEM_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);
        return imageView;
    }

    /** The stack size in the pixel font with a 1 px dark shadow, at the bottom right corner of the slot. */
    private Label createCountLabel(Item item) {
        Label count = new Label(String.valueOf(item.getQuantity()));
        count.getStyleClass().add(UiTheme.COUNT);
        count.setMouseTransparent(true);
        StackPane.setAlignment(count, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(count, new Insets(0, COUNT_MARGIN, 0, 0));
        return count;
    }

    /** Retrieves an item's image representation, generating a fallback when no sprite exists. */
    Image getItemImage(Item item) {
        if (item == null) return null;

        SpriteManager.SpriteMetadata metadata = SpriteManager.getInstance().getItemSprite(item.getType());
        if (metadata != null && metadata.getImage() != null) {
            return metadata.getImage();
        }

        return generateFallbackImage(item.getType());
    }

    private Image generateFallbackImage(ItemType itemType) {
        Canvas canvas = new Canvas(ITEM_SIZE, ITEM_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ITEM_SIZE, ITEM_SIZE);
        FxItemDrawingHelper.drawFallbackShape(gc, 0, 0, ITEM_SIZE, ITEM_SIZE, itemType, 1.0);
        return canvas.snapshot(null, null);
    }

    // ==================================================================
    //  Public API delegated to the controller
    // ==================================================================

    /**
     * Sets the callback run when an item is dropped on a slot.
     *
     * @param callback the callback
     */
    public void setOnItemDroppedInside(ItemTransferCallback callback) {
        controller.setOnItemDroppedInsideCallback(callback);
    }

    /**
     * Sets the callback run when a stack is dragged out of the panel, or one item of it is thrown with a right click
     * while dragging. Releasing outside the panel throws the stack, releasing over the panel but over no slot puts it
     * back.
     *
     * @param callback the callback
     */
    public void setOnItemDropped(ItemDropCallback callback) {
        controller.setOnItemDroppedCallback(callback);
    }

    /**
     * Sets the callback run when the craft button is pressed.
     *
     * @param callback the callback
     */
    public void setOnCraftRecipe(CraftRecipeCallback callback) {
        controller.setOnCraftRecipeCallback(callback);
    }

    /**
     * Sets the recipes shown in the crafting list.
     *
     * @param recipes the recipes
     */
    public void setCraftingRecipes(List<Recipe> recipes) {
        craftingListPanel.setRecipes(recipes);
    }

    /**
     * The slot an action from the keyboard applies to.
     *
     * @return the slot under the mouse, else the selected slot, or null when there is neither
     */
    public SlotRef getHoveredOrSelectedSlot() {
        return controller.hoveredOrSelectedSlot();
    }

}
