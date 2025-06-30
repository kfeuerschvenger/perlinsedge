package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxItemDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.CraftRecipeCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemDroppedOutsideCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemTransferCallback;
import com.feuerschvenger.perlinsedge.ui.view.utils.GridUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages the visual representation and interaction for inventory systems including:
 * <ul>
 * <li> Player inventory</li>
 * <li> Secondary containers (chests, etc.)</li>
 * <li> Crafting recipes display</li>
 * <li> Item information panel</li>
 *</ul><br>
 * Handles drag-and-drop operations, item selection, and crafting interactions.
 */
public class InventoryPanel extends BorderPane {
    // Constants
    public static final int DEFAULT_CAPACITY = 9;

    // Layout dimensions
    private static final int SLOT_SIZE = 40;
    private static final int ITEM_SIZE = 32;
    private static final int PANEL_PADDING = 20;
    private static final int ITEM_SPACING = 10;
    private static final int PANEL_CORNER_RADIUS = 10;
    private static final int SLOT_CORNER_RADIUS = 3;

    // Font sizes
    private static final int TITLE_FONT_SIZE = 24;
    private static final int QUANTITY_FONT_SIZE = 12;

    // Colors
    private static final Color PANEL_BG_COLOR = Color.rgb(0, 0, 0, 0.7);
    private static final Color SLOT_BG_COLOR = Color.rgb(70, 70, 70, 0.8);
    private static final Color SLOT_BORDER_COLOR = Color.rgb(100, 100, 100, 0.8);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color DRAG_HIGHLIGHT_COLOR = Color.YELLOW;
    private static final Color SELECTED_SLOT_COLOR = Color.GOLD;

    // Text constants
    private static final String FONT_FAMILY = "Arial";
    private static final String PLAYER_INVENTORY_TITLE = "Player Inventory";
    private static final String CHEST_INVENTORY_TITLE = "Chest Inventory";
    private static final String DRAG_FAIL_MSG = "DRAG FAIL: No image for %s";
    private static final String DRAG_START_MSG = "DRAG START: %s from slot %d";
    private static final String DRAG_OVER_ACCEPT_MSG = "DRAG OVER: Slot %d - accepted";
    private static final String DRAG_OVER_REJECT_MSG = "DRAG OVER: Slot %d - rejected";
    private static final String DRAG_DROP_MSG = "DRAG DROP: Slot %d";
    private static final String DRAG_DROP_DETAIL_MSG = "DRAG DROP: %s from slot %d to %d";
    private static final String SLOT_CLICK_MSG = "SLOT CLICK: Slot %d";
    private static final String CRAFT_FAIL_MSG = "Crafting failed: %s";

    // UI components
    private GridPane playerInventoryGrid;
    private GridPane secondaryInventoryGrid;
    private Label secondaryInventoryLabel;
    private ItemInfoPanel itemInfoPanel;
    private CraftingListPanel craftingListPanel;

    // State management
    private final GameStateManager gameState;
    private Container secondaryContainer;
    private Recipe selectedRecipe;
    private int selectedSlotIndex = -1;
    private boolean selectedSlotIsPlayerInventory = true;

    // Callbacks
    private ItemTransferCallback onItemDroppedInsideCallback;
    private ItemDroppedOutsideCallback onItemDroppedOutsideCallback;
    private CraftRecipeCallback onCraftRecipeCallback;

    /**
     * Initializes the inventory panel with game state reference.
     *
     * @param gameStateManager Game state manager (must not be null)
     */
    public InventoryPanel(GameStateManager gameStateManager) {
        this.gameState = Objects.requireNonNull(gameStateManager, "Game state manager cannot be null");
        this.secondaryContainer = null;

        configurePanel();
        initializeComponents();
        setupEventHandlers();

        setVisible(false);
        setManaged(false);
    }

    /**
     * Configures the panel's base layout and styling.
     */
    private void configurePanel() {
        setPadding(new Insets(PANEL_PADDING));
        setBackground(new Background(new BackgroundFill(
                PANEL_BG_COLOR, new CornerRadii(PANEL_CORNER_RADIUS), Insets.EMPTY
        )));
        setPrefSize(800, 300);
    }

    /**
     * Initializes all UI components and layouts.
     */
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

    /**
     * Creates the center container with player inventory.
     */
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

    /**
     * Creates inventory slots for a container.
     *
     * @param capacity Number of slots to create
     * @param container Container associated with slots
     * @return List of created slots
     */
    private List<StackPane> createInventorySlots(int capacity, Container container) {
        List<StackPane> slots = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            slots.add(createInventorySlot(i, container));
        }
        return slots;
    }

    /**
     * Creates the right container with crafting panel.
     */
    private VBox createRightContainer() {
        VBox container = new VBox(ITEM_SPACING);
        container.setAlignment(Pos.TOP_CENTER);

        // Crafting recipes section
        craftingListPanel = new CraftingListPanel();
        craftingListPanel.setPrefWidth(375);

        // Secondary inventory section (initially hidden)
        secondaryInventoryLabel = createTitle(CHEST_INVENTORY_TITLE);
        secondaryInventoryGrid = GridUtils.createGrid(null);

        container.getChildren().add(craftingListPanel);
        return container;
    }

    /**
     * Sets up event handlers and callbacks.
     */
    private void setupEventHandlers() {
        // Recipe selection and crafting
        craftingListPanel.setOnRecipeSelected(this::handleRecipeSelected);
        craftingListPanel.setOnCraftButtonClicked(this::handleCraftButtonClick);
    }

    /**
     * Creates a styled title label.
     *
     * @param text Title text
     * @return Configured label
     */
    private Label createTitle(String text) {
        Label label = new Label(text);
        label.setFont(new Font(FONT_FAMILY, TITLE_FONT_SIZE));
        label.setTextFill(TEXT_COLOR);
        return label;
    }

    /**
     * Creates an inventory slot with drag-and-drop and click handlers.
     *
     * @param slotIndex Slot position index
     * @param container Container this slot belongs to
     * @return Configured slot pane
     */
    private StackPane createInventorySlot(int slotIndex, Container container) {
        StackPane slot = new StackPane();
        configureSlotLayout(slot);
        slot.setUserData(new SlotContext(slotIndex, container));

        // Event handlers
        slot.setOnDragDetected(e -> handleDragDetected(e, slot, slotIndex, container));
        slot.setOnDragOver(e -> handleDragOver(e, slot, slotIndex));
        slot.setOnDragExited(e -> restoreSlotBorder(slot));
        slot.setOnDragDropped(e -> handleDragDropped(e, slot));
        slot.setOnMouseClicked(this::handleSlotClick);

        return slot;
    }

    /**
     * Configures the layout and styling of a slot.
     */
    private void configureSlotLayout(StackPane slot) {
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMinSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMaxSize(SLOT_SIZE, SLOT_SIZE);

        slot.setBackground(new Background(new BackgroundFill(
                SLOT_BG_COLOR, new CornerRadii(SLOT_CORNER_RADIUS), Insets.EMPTY
        )));

        slot.setBorder(new Border(new BorderStroke(
                SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(1)
        )));
    }

    /**
     * Handles drag initiation for items.
     */
    private void handleDragDetected(javafx.scene.input.MouseEvent event, StackPane slot,
                                    int slotIndex, Container container) {
        Item item = getItemInSlot(slotIndex, container);
        if (item == null || item.getQuantity() <= 0) return;

        Image dragImage = getItemImage(item);
        if (dragImage == null) {
            System.out.printf((DRAG_FAIL_MSG) + "%n", item.getDisplayName());
            return;
        }

        Dragboard db = slot.startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString("item");
        db.setContent(content);

        slot.getProperties().put("draggedItem", item);
        slot.getProperties().put("draggedFromContainer", container);
        slot.getProperties().put("draggedFromSlotIndex", slotIndex);

        // Create drag image
        Canvas snapshotCanvas = new Canvas(ITEM_SIZE, ITEM_SIZE);
        GraphicsContext gc = snapshotCanvas.getGraphicsContext2D();
        gc.drawImage(dragImage, 0, 0, ITEM_SIZE, ITEM_SIZE);
        db.setDragView(snapshotCanvas.snapshot(null, null), event.getX(), event.getY());

        System.out.printf((DRAG_START_MSG) + "%n", item.getDisplayName(), slotIndex);
        event.consume();
    }

    /**
     * Handles drag-over events for potential drop targets.
     */
    private void handleDragOver(javafx.scene.input.DragEvent event, StackPane slot, int slotIndex) {
        if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
            slot.setBorder(new Border(new BorderStroke(
                    DRAG_HIGHLIGHT_COLOR, BorderStrokeStyle.SOLID,
                    new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(2))
            ));
            event.acceptTransferModes(TransferMode.MOVE);
            System.out.printf((DRAG_OVER_ACCEPT_MSG) + "%n", slotIndex);
        } else {
            System.out.printf((DRAG_OVER_REJECT_MSG) + "%n", slotIndex);
        }
        event.consume();
    }

    /**
     * Restores a slot's default border.
     */
    private void restoreSlotBorder(StackPane slot) {
        slot.setBorder(new Border(new BorderStroke(
                SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(1))
        ));
    }

    /**
     * Handles item drop events.
     */
    private void handleDragDropped(javafx.scene.input.DragEvent event, StackPane slot) {
        restoreSlotBorder(slot);
        System.out.printf((DRAG_DROP_MSG) + "%n", ((SlotContext) slot.getUserData()).index);

        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasString() && event.getGestureSource() instanceof Node sourceNode) {
            Item draggedItem = (Item) sourceNode.getProperties().get("draggedItem");
            Container sourceContainer = (Container) sourceNode.getProperties().get("draggedFromContainer");
            Integer sourceSlotIndex = (Integer) sourceNode.getProperties().get("draggedFromSlotIndex");
            SlotContext targetContext = (SlotContext) slot.getUserData();

            if (validateDragOperation(draggedItem, sourceContainer, sourceSlotIndex)) {
                System.out.printf((DRAG_DROP_DETAIL_MSG) + "%n",
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

    /**
     * Validates drag operation parameters.
     */
    private boolean validateDragOperation(Item draggedItem, Container sourceContainer, Integer sourceSlotIndex) {
        if (draggedItem == null) {
            System.out.println("DRAG DROP: Missing dragged item");
            return false;
        }
        if (sourceContainer == null) {
            System.out.println("DRAG DROP: Missing source container");
            return false;
        }
        if (sourceSlotIndex == null) {
            System.out.println("DRAG DROP: Missing source slot index");
            return false;
        }
        return true;
    }

    /**
     * Handles slot click events.
     */
    private void handleSlotClick(javafx.scene.input.MouseEvent event) {
        SlotContext context = (SlotContext) ((Node) event.getSource()).getUserData();
        System.out.printf((SLOT_CLICK_MSG) + "%n", context.index);

        handleSlotSelection(context.index, context.container == gameState.getPlayer());
        event.consume();
    }

    /**
     * Retrieves an item from a specific slot in a container.
     */
    private Item getItemInSlot(int slotIndex, Container container) {
        if (container == null || slotIndex < 0 || slotIndex >= container.getCapacity()) {
            return null;
        }
        List<Item> items = container.getItems();
        return (slotIndex < items.size()) ? items.get(slotIndex) : null;
    }

    /**
     * Context object for inventory slots.
     */
    private static class SlotContext {
        int index;
        Container container;

        SlotContext(int index, Container container) {
            this.index = index;
            this.container = container;
        }
    }

    /**
     * Updates the inventory display based on current state.
     */
    public void updateInventoryDisplay() {
        updateGridContents(playerInventoryGrid, gameState.getPlayer());

        if (secondaryContainer != null) {
            updateGridContents(secondaryInventoryGrid, secondaryContainer);
        }

        updateInformationDisplay();
    }

    /**
     * Updates the information panel based on current selection.
     */
    private void updateInformationDisplay() {
        if (selectedSlotIndex != -1) {
            updateItemSelectionDisplay();
        } else if (selectedRecipe != null) {
            itemInfoPanel.displayRecipe(selectedRecipe);
        } else {
            itemInfoPanel.clearInfo();
        }
    }

    /**
     * Updates display for item slot selection.
     */
    private void updateItemSelectionDisplay() {
        Container currentContainer = selectedSlotIsPlayerInventory ?
                gameState.getPlayer() : secondaryContainer;

        if (currentContainer == null) {
            clearSelection();
            return;
        }

        Item currentItem = getItemInSlot(selectedSlotIndex, currentContainer);
        if (currentItem != null) {
            itemInfoPanel.displayItem(currentItem);
            highlightSlot(selectedSlotIndex, true);
        } else {
            clearSelection();
        }
    }

    /**
     * Clears current selection state.
     */
    private void clearSelection() {
        highlightSlot(selectedSlotIndex, false);
        selectedSlotIndex = -1;
        itemInfoPanel.clearInfo();
    }

    /**
     * Updates grid contents for a container.
     */
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

    /**
     * Updates a slot's context data.
     */
    private void updateSlotContext(StackPane slot, int index, Container container) {
        SlotContext context = (SlotContext) slot.getUserData();
        context.index = index;
        context.container = container;
    }

    /**
     * Updates a slot's visual representation.
     */
    private void updateSlotVisuals(StackPane slot, int index, Container container) {
        slot.getChildren().clear();
        Item item = getItemInSlot(index, container);

        if (item != null) {
            slot.getChildren().addAll(createItemImage(item), createQuantityText(item));
        }
    }

    /**
     * Opens the player inventory display.
     */
    public void openPlayerInventory() {
        this.secondaryContainer = null;
        cleanRightContainer();
        updateInventoryDisplay();
        setVisible(true);
        setManaged(true);
    }

    /**
     * Opens a dual inventory display (player + secondary container).
     */
    public void openDualInventory(Container otherContainer) {
        this.secondaryContainer = Objects.requireNonNull(otherContainer, "Secondary container cannot be null");
        initializeSecondaryInventory();
        configureDualInventoryLayout();
        updateInventoryDisplay();
        setVisible(true);
        setManaged(true);
    }

    /**
     * Initializes the secondary inventory grid.
     */
    private void initializeSecondaryInventory() {
        List<StackPane> slots = createInventorySlots(secondaryContainer.getCapacity(), secondaryContainer);
        secondaryInventoryGrid = GridUtils.createGrid(slots);
    }

    /**
     * Configures layout for dual inventory mode.
     */
    private void configureDualInventoryLayout() {
        // Clear right container
        VBox rightContainer = (VBox) getRight();
        rightContainer.getChildren().clear();
        rightContainer.getChildren().addAll(secondaryInventoryLabel, secondaryInventoryGrid);

        // Configure center container
        HBox centerContainer = new HBox(20);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.getChildren().addAll(
                createInventoryBox(PLAYER_INVENTORY_TITLE, playerInventoryGrid),
                createInventoryBox(CHEST_INVENTORY_TITLE, secondaryInventoryGrid)
        );
        setCenter(centerContainer);

        // Hide crafting panel
        craftingListPanel.setVisible(false);
        craftingListPanel.setManaged(false);
    }

    /**
     * Creates a titled inventory box.
     */
    private VBox createInventoryBox(String title, GridPane grid) {
        VBox box = new VBox(10);
        box.getChildren().addAll(createTitle(title), grid);
        return box;
    }

    /**
     * Closes the inventory panel and resets state.
     */
    public void closeInventory() {
        this.secondaryContainer = null;
        resetLayout();
        clearState();
        setVisible(false);
        setManaged(false);
    }

    /**
     * Resets the layout to default configuration.
     */
    private void resetLayout() {
        // Restore center layout
        VBox centerContainer = new VBox(ITEM_SPACING);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.getChildren().addAll(createTitle(PLAYER_INVENTORY_TITLE), playerInventoryGrid);
        setCenter(centerContainer);

        // Restore right container
        cleanRightContainer();
        craftingListPanel.setVisible(true);
        craftingListPanel.setManaged(true);
    }

    /**
     * Clears the right container of secondary inventory elements.
     */
    private void cleanRightContainer() {
        if (getRight() instanceof VBox rightContainer) {
            rightContainer.getChildren().removeAll(secondaryInventoryLabel, secondaryInventoryGrid);
            rightContainer.getChildren().setAll(craftingListPanel);
        }
    }

    /**
     * Clears panel state and selection.
     */
    private void clearState() {
        itemInfoPanel.clearInfo();
        highlightSlot(selectedSlotIndex, false);
        selectedSlotIndex = -1;
        selectedRecipe = null;
    }

    // Event handler methods ==================================================

    /**
     * Handles slot selection logic.
     */
    private void handleSlotSelection(int clickedSlotIndex, boolean isPlayerInventorySlot) {
        Container container = isPlayerInventorySlot ? gameState.getPlayer() : secondaryContainer;
        Item item = getItemInSlot(clickedSlotIndex, container);

        if (item != null) {
            updateSlotSelection(clickedSlotIndex, isPlayerInventorySlot);
            selectedRecipe = null;
        } else {
            clearSelection();
        }
        Platform.runLater(this::updateInventoryDisplay);
    }

    /**
     * Updates state for new slot selection.
     */
    private void updateSlotSelection(int slotIndex, boolean isPlayerInventory) {
        // Clear previous selection
        if (selectedSlotIndex != -1) {
            highlightSlot(selectedSlotIndex, false);
        }

        // Set new selection
        selectedSlotIndex = slotIndex;
        selectedSlotIsPlayerInventory = isPlayerInventory;
        highlightSlot(selectedSlotIndex, true);
    }

    /**
     * Handles recipe selection.
     */
    private void handleRecipeSelected(Recipe recipe) {
        // Clear item selection
        if (selectedSlotIndex != -1) {
            highlightSlot(selectedSlotIndex, false);
            selectedSlotIndex = -1;
        }

        this.selectedRecipe = recipe;
        itemInfoPanel.displayRecipe(recipe);
    }

    /**
     * Handles craft button click.
     */
    private void handleCraftButtonClick() {
        Recipe recipe = craftingListPanel.getSelectedRecipe();
        if (recipe == null) {
            System.err.printf((CRAFT_FAIL_MSG) + "%n", "No recipe selected");
            return;
        }
        if (onCraftRecipeCallback == null) {
            System.err.printf((CRAFT_FAIL_MSG) + "%n", "Callback not set");
            return;
        }

        onCraftRecipeCallback.craft(recipe);
        updateInventoryDisplay();
    }

    // UI helper methods ======================================================

    /**
     * Highlights or unhighlights a slot.
     *
     * @param index Slot index to modify
     * @param highlight True to highlight, false to restore
     */
    private void highlightSlot(int index, boolean highlight) {
        if (index < 0) return;

        GridPane targetGrid = selectedSlotIsPlayerInventory ?
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

    /**
     * Applies highlight styling to a slot.
     */
    private void applySlotHighlight(StackPane slot, boolean highlight) {
        if (highlight) {
            slot.setBorder(new Border(new BorderStroke(
                    SELECTED_SLOT_COLOR, BorderStrokeStyle.SOLID,
                    new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(2))
            ));
        } else {
            slot.setBorder(new Border(new BorderStroke(
                    SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                    new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(1))
            ));
        }
    }

    /**
     * Creates an item image view.
     */
    private ImageView createItemImage(Item item) {
        ImageView imageView = new ImageView(getItemImage(item));
        imageView.setFitWidth(ITEM_SIZE);
        imageView.setFitHeight(ITEM_SIZE);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    /**
     * Creates quantity text display.
     */
    private Text createQuantityText(Item item) {
        if (item == null || item.getQuantity() <= 1) {
            return new Text("");
        }
        Text text = new Text(String.valueOf(item.getQuantity()));
        text.setFont(new Font(FONT_FAMILY, QUANTITY_FONT_SIZE));
        text.setFill(TEXT_COLOR);
        return text;
    }

    // Utility methods ========================================================

    /**
     * Retrieves an item's image representation.
     */
    private Image getItemImage(Item item) {
        if (item == null) return null;

        // Try getting sprite first
        SpriteManager.SpriteMetadata metadata = SpriteManager.getInstance().getItemSprite(item.getType());
        if (metadata != null && metadata.getImage() != null) {
            return metadata.getImage();
        }

        // Generate fallback
        return generateFallbackImage(item.getType());
    }

    /**
     * Generates a fallback image for an item type.
     */
    private Image generateFallbackImage(ItemType itemType) {
        Canvas canvas = new Canvas(ITEM_SIZE, ITEM_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ITEM_SIZE, ITEM_SIZE);
        FxItemDrawingHelper.drawFallbackShape(gc, 0, 0, ITEM_SIZE, ITEM_SIZE, itemType, 1.0);
        return canvas.snapshot(null, null);
    }

    // Callback setters =======================================================

    public void setOnItemDroppedInside(ItemTransferCallback callback) {
        this.onItemDroppedInsideCallback = callback;
    }

    public void setOnItemDroppedOutside(ItemDroppedOutsideCallback callback) {
        this.onItemDroppedOutsideCallback = callback;
    }

    public void setOnCraftRecipe(CraftRecipeCallback callback) {
        this.onCraftRecipeCallback = callback;
    }

    public void setCraftingRecipes(List<Recipe> recipes) {
        craftingListPanel.setRecipes(recipes);
    }

    // Getters for callbacks ==================================================

    public ItemTransferCallback getOnItemDroppedInsideCallback() {
        return onItemDroppedInsideCallback;
    }

    public ItemDroppedOutsideCallback getOnItemDroppedOutsideCallback() {
        return onItemDroppedOutsideCallback;
    }

}