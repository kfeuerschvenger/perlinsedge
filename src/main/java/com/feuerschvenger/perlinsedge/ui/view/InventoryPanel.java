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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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
    private static final int SLOT_SIZE = 40;
    static final int ITEM_SIZE = 32;
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

    // UI components
    private GridPane playerInventoryGrid;
    private GridPane secondaryInventoryGrid;
    private Label secondaryInventoryLabel;
    private ItemInfoPanel itemInfoPanel;
    private CraftingListPanel craftingListPanel;

    // State management
    private final GameStateManager gameState;
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

        setVisible(false);
        setManaged(false);
    }

    private void configurePanel() {
        setPadding(new Insets(PANEL_PADDING));
        setBackground(new Background(new BackgroundFill(
                PANEL_BG_COLOR, new CornerRadii(PANEL_CORNER_RADIUS), Insets.EMPTY
        )));
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
        Label label = new Label(text);
        label.setFont(new Font(FONT_FAMILY, TITLE_FONT_SIZE));
        label.setTextFill(TEXT_COLOR);
        return label;
    }

    // ==================================================================
    //  Slots
    // ==================================================================

    private StackPane createInventorySlot(int slotIndex, Container container) {
        StackPane slot = new StackPane();
        configureSlotLayout(slot);
        slot.setUserData(new SlotContext(slotIndex, container));

        slot.setOnDragDetected(e -> controller.handleDragDetected(e, slot, slotIndex, container));
        slot.setOnDragOver(e -> controller.handleDragOver(e, slot, slotIndex));
        slot.setOnDragExited(e -> restoreSlotBorder(slot));
        slot.setOnDragDropped(e -> controller.handleDragDropped(e, slot));
        slot.setOnMouseClicked(controller::handleSlotClick);

        return slot;
    }

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

    /** Highlights a slot as a valid drop target. */
    void showDropHighlight(StackPane slot) {
        slot.setBorder(new Border(new BorderStroke(
                DRAG_HIGHLIGHT_COLOR, BorderStrokeStyle.SOLID,
                new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(2))
        ));
    }

    /** Restores a slot's default border. */
    void restoreSlotBorder(StackPane slot) {
        slot.setBorder(new Border(new BorderStroke(
                SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(1))
        ));
    }

    /** Retrieves an item from a specific slot in a container. */
    Item getItemInSlot(int slotIndex, Container container) {
        if (container == null || slotIndex < 0 || slotIndex >= container.getCapacity()) {
            return null;
        }
        List<Item> items = container.getItems();
        return (slotIndex < items.size()) ? items.get(slotIndex) : null;
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

    // ==================================================================
    //  Display updates
    // ==================================================================

    /** Updates the inventory display based on current state. */
    public void updateInventoryDisplay() {
        updateGridContents(playerInventoryGrid, gameState.getPlayer());

        if (secondaryContainer != null) {
            updateGridContents(secondaryInventoryGrid, secondaryContainer);
        }

        updateInformationDisplay();
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
            slot.getChildren().addAll(createItemImage(item), createQuantityText(item));
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
            slot.setBorder(new Border(new BorderStroke(
                    SELECTED_SLOT_COLOR, BorderStrokeStyle.SOLID,
                    new CornerRadii(SLOT_CORNER_RADIUS), new BorderWidths(2))
            ));
        } else {
            restoreSlotBorder(slot);
        }
    }

    private ImageView createItemImage(Item item) {
        ImageView imageView = new ImageView(getItemImage(item));
        imageView.setFitWidth(ITEM_SIZE);
        imageView.setFitHeight(ITEM_SIZE);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private Text createQuantityText(Item item) {
        if (item == null || item.getQuantity() <= 1) {
            return new Text("");
        }
        Text text = new Text(String.valueOf(item.getQuantity()));
        text.setFont(new Font(FONT_FAMILY, QUANTITY_FONT_SIZE));
        text.setFill(TEXT_COLOR);
        return text;
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
     * Sets the callback run when an item is dropped outside the inventory.
     *
     * @param callback the callback
     */
    public void setOnItemDroppedOutside(ItemDroppedOutsideCallback callback) {
        controller.setOnItemDroppedOutsideCallback(callback);
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

    /** @return the callback run when an item is dropped on a slot */
    public ItemTransferCallback getOnItemDroppedInsideCallback() {
        return controller.getOnItemDroppedInsideCallback();
    }

    /** @return the callback run when an item is dropped outside the inventory */
    public ItemDroppedOutsideCallback getOnItemDroppedOutsideCallback() {
        return controller.getOnItemDroppedOutsideCallback();
    }

}
