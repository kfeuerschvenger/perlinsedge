package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxItemDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
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
 * Manages the visual representation and interaction of the player's inventory,
 * crafting recipes, and optionally, a secondary container (like a chest).
 */
public class InventoryPanel extends BorderPane {

    public interface ItemClickCallback { void onClick(Item item); }

    public static final int DEFAULT_CAPACITY = 9;

    // Layout constants
    private static final int SLOT_SIZE = 40;
    private static final int ITEM_SIZE = 32;
    private static final int TITLE_FONT_SIZE = 24;
    private static final int QUANTITY_FONT_SIZE = 12;

    // Styling constants
    private static final Color PANEL_BG_COLOR = Color.rgb(0, 0, 0, 0.7);
    private static final Color SLOT_BG_COLOR = Color.rgb(70, 70, 70, 0.8);
    private static final Color SLOT_BORDER_COLOR = Color.rgb(100, 100, 100, 0.8);
    private static final Color TEXT_COLOR = Color.WHITE;

    // Spacing and padding
    private static final int PANEL_PADDING = 20;
    private static final int ITEM_SPACING = 10;

    private final GameStateManager gameState;
    private Container secondaryContainer; // For chests or other containers

    private final GridPane playerInventoryGrid;
    private GridPane secondaryInventoryGrid;
    private final Label secondaryInventoryLabel;

    private final ItemInfoPanel itemInfoPanel;
    private final CraftingListPanel craftingListPanel;

    private ItemClickCallback onItemClickCallback;
    private ItemTransferCallback onItemDroppedInsideCallback;
    private ItemDroppedOutsideCallback onItemDroppedOutsideCallback;
    private CraftRecipeCallback onCraftRecipeCallback;

    private List<Recipe> currentCraftingRecipes;

    private int selectedSlotIndex = -1;
    private boolean selectedSlotIsPlayerInventory = true;

    public InventoryPanel(GameStateManager gameStateManager) { // Changed constructor parameter
        this.gameState = Objects.requireNonNull(gameStateManager, "Game state manager cannot be null.");
        this.secondaryContainer = null;

        configurePanel();

        // 1. Left Section: Item Info Panel
        itemInfoPanel = new ItemInfoPanel();
        setLeft(itemInfoPanel);
        setAlignment(itemInfoPanel, Pos.CENTER_LEFT);
        BorderPane.setMargin(itemInfoPanel, new Insets(0, ITEM_SPACING, 0, 0));

        // 2. Center Section: Player Inventory Grid
        VBox centerContainer = new VBox(ITEM_SPACING);
        centerContainer.setAlignment(Pos.CENTER);
        Label title = createTitle("Player Inventory");

        int playerCapacity = gameState.getPlayer() != null ?
                gameState.getPlayer().getCapacity() :
                2 * DEFAULT_CAPACITY;

        List<StackPane> slots = new ArrayList<>();
        for (int i = 0; i < playerCapacity; i++) {
            StackPane slot = createInventorySlot(i, gameState.getPlayer());
            slots.add(slot);
        }

        playerInventoryGrid = GridUtils.createPlayerGrid(slots);
        centerContainer.getChildren().addAll(title, playerInventoryGrid);
        setCenter(centerContainer);
        setAlignment(centerContainer, Pos.CENTER);
        BorderPane.setMargin(centerContainer, new Insets(0, ITEM_SPACING, 0, ITEM_SPACING));

        // 3. Right Section: Crafting List Panel & Secondary Inventory (dynamic)
        VBox rightContainer = new VBox(ITEM_SPACING);
        rightContainer.setAlignment(Pos.TOP_CENTER);
        setRight(rightContainer);
        setAlignment(rightContainer, Pos.CENTER_RIGHT);
        BorderPane.setMargin(rightContainer, new Insets(0, 0, 0, ITEM_SPACING));

        // Crafting Recipes Section (always part of right container)
        craftingListPanel = new CraftingListPanel();
        craftingListPanel.setPrefWidth(375);
        rightContainer.getChildren().addAll(craftingListPanel);

        // Secondary Inventory Section (initially hidden, added dynamically)
        secondaryInventoryLabel = createTitle("Chest Inventory");
        secondaryInventoryGrid = GridUtils.createGrid(null);

        // Configure callbacks for recipe selection and craft button
        craftingListPanel.setOnRecipeSelected(this::handleRecipeSelected);
        craftingListPanel.setOnCraftButtonClicked(this::handleCraftButtonClick);

        setVisible(false);
        setManaged(false);
    }

    private Label createTitle(String text) {
        Label titleLabel = new Label(text);
        titleLabel.setFont(new Font("Arial", TITLE_FONT_SIZE));
        titleLabel.setTextFill(TEXT_COLOR);
        return titleLabel;
    }

    private void configurePanel() {
        setPadding(new Insets(PANEL_PADDING));
        setBackground(new Background(new BackgroundFill(PANEL_BG_COLOR,
                new CornerRadii(10), Insets.EMPTY)));
        setPrefSize(800, 300);
    }

    private StackPane createInventorySlot(int slotIndex, Container container) {
        StackPane slot = new StackPane();
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMinSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMaxSize(SLOT_SIZE, SLOT_SIZE);

        slot.setBackground(new Background(new BackgroundFill(
                SLOT_BG_COLOR, new CornerRadii(3), Insets.EMPTY)));

        slot.setBorder(new Border(new BorderStroke(
                SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                new CornerRadii(3), new BorderWidths(1))));

        slot.setUserData(new SlotContext(slotIndex, container));

        slot.setOnDragDetected(event -> {
            Item itemInSlot = getItemInSlot(slotIndex, container);
            if (itemInSlot != null && itemInSlot.getQuantity() > 0) {
                // FIX: Ensure we have a valid drag image
                Image dragImage = getItemImage(itemInSlot);
                if (dragImage == null) {
                    System.out.println("DRAG FAIL: No image for " + itemInSlot.getDisplayName());
                    return;
                }

                Dragboard db = slot.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString("item");
                db.setContent(content);

                slot.getProperties().put("draggedItem", itemInSlot);
                slot.getProperties().put("draggedFromContainer", container);
                slot.getProperties().put("draggedFromSlotIndex", slotIndex);

                ImageView dragImageView = new ImageView(dragImage);
                dragImageView.setFitWidth(ITEM_SIZE);
                dragImageView.setFitHeight(ITEM_SIZE);

                // FIX: Use a separate canvas for reliable snapshot
                Canvas snapshotCanvas = new Canvas(ITEM_SIZE, ITEM_SIZE);
                GraphicsContext gc = snapshotCanvas.getGraphicsContext2D();
                gc.drawImage(dragImage, 0, 0, ITEM_SIZE, ITEM_SIZE);
                db.setDragView(snapshotCanvas.snapshot(null, null), event.getX(), event.getY());

                System.out.println("DRAG START: " + itemInSlot.getDisplayName() + " from slot " + slotIndex);
                event.consume();
            }
        });

        slot.setOnDragOver(event -> {
            if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
                // FIX: Highlight potential drop target
                slot.setBorder(new Border(new BorderStroke(
                        Color.YELLOW, BorderStrokeStyle.SOLID,
                        new CornerRadii(3), new BorderWidths(2))
                ));
                event.acceptTransferModes(TransferMode.MOVE);
                System.out.println("DRAG OVER: Slot " + slotIndex + " - accepted");
            } else {
                System.out.println("DRAG OVER: Slot " + slotIndex + " - rejected");
            }
            event.consume();
        });

        slot.setOnDragExited(event -> {
            // FIX: Restore normal border when drag exits
            slot.setBorder(new Border(new BorderStroke(
                    SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                    new CornerRadii(3), new BorderWidths(1))
            ));
        });

        slot.setOnDragDropped(event -> {
            // FIX: Restore border immediately
            slot.setBorder(new Border(new BorderStroke(
                    SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                    new CornerRadii(3), new BorderWidths(1))
            ));

            Dragboard db = event.getDragboard();
            boolean success = false;
            System.out.println("DRAG DROP: Slot " + slotIndex);

            if (db.hasString()) {
                Node sourceNode = (Node) event.getGestureSource();
                if (sourceNode != null) {
                    Item draggedItem = (Item) sourceNode.getProperties().get("draggedItem");
                    Container sourceContainer = (Container) sourceNode.getProperties().get("draggedFromContainer");
                    Integer sourceSlotIndex = (Integer) sourceNode.getProperties().get("draggedFromSlotIndex");

                    if (draggedItem != null && sourceContainer != null && sourceSlotIndex != null) {
                        SlotContext targetContext = (SlotContext) slot.getUserData();
                        System.out.println("DRAG DROP: " + draggedItem.getDisplayName() +
                                " from slot " + sourceSlotIndex + " to " + targetContext.index);

                        if (onItemDroppedInsideCallback != null) {
                            onItemDroppedInsideCallback.transfer(
                                    sourceSlotIndex,
                                    sourceContainer,
                                    targetContext.index,
                                    targetContext.container,
                                    draggedItem
                            );
                            success = true;
                        }
                    } else {
                        System.out.println("DRAG DROP: Missing drag properties");
                    }
                } else {
                    System.out.println("DRAG DROP: Source node is null");
                }
            }

            event.setDropCompleted(success);
            if (success) {
                System.out.println("DRAG DROP: Success");
            } else {
                System.out.println("DRAG DROP: Failed");
            }
            event.consume();
        });

        // FIX: Ensure slot receives click events
        slot.setOnMouseClicked(event -> {
            SlotContext context = (SlotContext) slot.getUserData();
            System.out.println("SLOT CLICK: Slot " + context.index);
            handleSlotClick(context.index, context.container == gameState.getPlayer());
            event.consume();
        });

        return slot;
    }

    private Item getItemInSlot(int slotIndex, Container container) {
        if (container == null || slotIndex < 0 || slotIndex >= container.getCapacity()) {
            return null;
        }
        List<Item> items = container.getItems();
        if (slotIndex < items.size()) {
            return items.get(slotIndex);
        }
        return null;
    }

    private static class SlotContext {
        int index;
        Container container;

        SlotContext(int index, Container container) {
            this.index = index;
            this.container = container;
        }
    }

    public void updateInventoryDisplay() {
        updateGridContents(playerInventoryGrid, gameState.getPlayer());

        if (secondaryContainer != null) {
            updateGridContents(secondaryInventoryGrid, secondaryContainer);
        }

        if (selectedSlotIndex != -1) {
            // Need to re-evaluate the container for the selected slot as player may not have been available initially
            Container currentSelectedContainer = selectedSlotIsPlayerInventory ? gameState.getPlayer() : secondaryContainer;
            if (currentSelectedContainer != null) {
                Item currentSelectedItem = getItemInSlot(selectedSlotIndex, currentSelectedContainer);
                if (currentSelectedItem != null) {
                    itemInfoPanel.displayItem(currentSelectedItem);
                    highlightSlot(selectedSlotIndex, true);
                } else {
                    itemInfoPanel.clearInfo();
                    selectedSlotIndex = -1;
                }
            } else {
                itemInfoPanel.clearInfo();
                selectedSlotIndex = -1;
            }
        } else {
            itemInfoPanel.clearInfo();
        }
    }

    private void updateGridContents(GridPane grid, Container container) {
        int cols = grid.getColumnConstraints().size();

        for (Node node : grid.getChildren()) {
            if (node instanceof StackPane slot) {
                int index = GridPane.getRowIndex(node) * cols + GridPane.getColumnIndex(node);

                SlotContext context = (SlotContext) slot.getUserData();
                context.index = index;
                context.container = container;

                Item item = getItemInSlot(context.index, container);
                slot.getChildren().clear();

                if (item != null) {
                    ImageView imageView = createItemImage(item);
                    Text quantityLabel = createQuantityText(item);
                    slot.getChildren().addAll(imageView, quantityLabel);
                }
            }
        }
    }

    public void openPlayerInventory() {
        this.secondaryContainer = null;
        if (getRight() instanceof VBox rightContainer) {
            rightContainer.getChildren().remove(secondaryInventoryLabel);
            rightContainer.getChildren().remove(secondaryInventoryGrid);
        }
        updateInventoryDisplay();
        setVisible(true);
        setManaged(true);
        System.out.println("InventoryPanel: Player inventory opened.");
    }

    public void openDualInventory(Container playerInv, Container otherContainer) {
        this.secondaryContainer = Objects.requireNonNull(otherContainer, "Secondary container cannot be null for dual inventory.");

        List<StackPane> slots = new ArrayList<>();
        for (int i = 0; i < secondaryContainer.getCapacity(); i++) {
            StackPane slot = createInventorySlot(i, secondaryContainer);
            slots.add(slot);
        }
        secondaryInventoryGrid = GridUtils.createGrid(slots);

        // Obtener el contenedor derecho y limpiarlo
        VBox rightContainer = (VBox) getRight();
        rightContainer.getChildren().clear();

        // Añadir solo el inventario secundario al lado derecho
        rightContainer.getChildren().addAll(secondaryInventoryLabel, secondaryInventoryGrid);

        // Actualizar centro para mostrar ambos inventarios
        HBox centerContainer = new HBox(20);
        centerContainer.setAlignment(Pos.CENTER);

        // Panel para inventario del jugador
        VBox playerBox = new VBox(10, createTitle("Player Inventory"), playerInventoryGrid);

        // Panel para inventario secundario
        VBox secondaryBox = new VBox(10, secondaryInventoryLabel, secondaryInventoryGrid);

        centerContainer.getChildren().addAll(playerBox, secondaryBox);
        setCenter(centerContainer);

        // Ocultar lista de crafteo
        craftingListPanel.setVisible(false);
        craftingListPanel.setManaged(false);

        updateInventoryDisplay();
        setVisible(true);
        setManaged(true);
        System.out.println("InventoryPanel: Dual inventory opened (Player + " + otherContainer.getClass().getSimpleName() + ").");
    }

    public void closeInventory() {

        this.secondaryContainer = null;

        // Restaurar diseño original
        VBox centerContainer = new VBox(ITEM_SPACING);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.getChildren().addAll(createTitle("Player Inventory"), playerInventoryGrid);
        setCenter(centerContainer);

        VBox rightContainer = (VBox) getRight();
        rightContainer.getChildren().clear();
        rightContainer.getChildren().add(craftingListPanel);

        // Mostrar lista de crafteo
        craftingListPanel.setVisible(true);
        craftingListPanel.setManaged(true);
//
//        if (getRight() instanceof VBox rightContainer) {
//            rightContainer.getChildren().remove(secondaryInventoryLabel);
//            rightContainer.getChildren().remove(secondaryInventoryGrid);
//        }
        itemInfoPanel.clearInfo();
        selectedSlotIndex = -1;
        highlightSlot(-1, false);
        setVisible(false);
        setManaged(false);
        System.out.println("InventoryPanel: Inventory panel closed.");
    }

    public void setOnItemDroppedInside(ItemTransferCallback callback) {
        this.onItemDroppedInsideCallback = callback;
    }

    public void setOnItemDroppedOutside(ItemDroppedOutsideCallback callback) {
        this.onItemDroppedOutsideCallback = callback;
    }

    public void setOnCraftRecipe(CraftRecipeCallback callback) {
        this.onCraftRecipeCallback = callback;
    }

    public ItemTransferCallback getOnItemDroppedInsideCallback() {
        return onItemDroppedInsideCallback;
    }

    public ItemClickCallback getOnItemClickCallback() {
        return onItemClickCallback;
    }

    public ItemDroppedOutsideCallback getOnItemDroppedOutsideCallback() {
        return onItemDroppedOutsideCallback;
    }

    public CraftRecipeCallback getOnCraftRecipeCallback() {
        return onCraftRecipeCallback;
    }

    private void handleSlotClick(int clickedSlotIndex, boolean isPlayerInventorySlot) {
        System.out.println("Clcik on slot: " + clickedSlotIndex +
                " (Inventory: " + (isPlayerInventorySlot ? "Player" : "Secondary") + ")");
        Container clickedContainer = isPlayerInventorySlot ? gameState.getPlayer() : secondaryContainer;
        Item itemInSlot = getItemInSlot(clickedSlotIndex, clickedContainer);

        if (itemInSlot != null) {
            // Clear previous selection
            if (selectedSlotIndex != -1) {
                highlightSlot(selectedSlotIndex, false);
            }

            // Set new selection
            selectedSlotIndex = clickedSlotIndex;
            selectedSlotIsPlayerInventory = isPlayerInventorySlot;
            highlightSlot(selectedSlotIndex, true);
            itemInfoPanel.displayItem(itemInSlot);
        } else {
            selectedSlotIndex = -1;
            itemInfoPanel.clearInfo();
        }

        // Always update display
        Platform.runLater(this::updateInventoryDisplay);
    }

    private void handleRecipeSelected(Recipe recipe) {
        if (selectedSlotIndex != -1) {
            highlightSlot(selectedSlotIndex, false);
            selectedSlotIndex = -1;
        }
        itemInfoPanel.displayRecipe(recipe);
    }

    private void handleCraftButtonClick() {
        Recipe selectedRecipe = craftingListPanel.getSelectedRecipe();
        if (selectedRecipe != null && onCraftRecipeCallback != null) {
            onCraftRecipeCallback.craft(selectedRecipe);
            updateInventoryDisplay();
        } else {
            System.err.println("Crafting failed: " + (selectedRecipe == null ? "No recipe selected" : "Callback not set"));
        }
    }

    private void highlightSlot(int index, boolean highlight) {
        GridPane targetGrid = selectedSlotIsPlayerInventory ? playerInventoryGrid : secondaryInventoryGrid;
        if (targetGrid == null || index < 0 || (index >= targetGrid.getChildren().size() && targetGrid.getChildren().isEmpty())) {
            return;
        }

        if (index >= targetGrid.getChildren().size()) {
            return;
        }

        int cols = targetGrid.getColumnConstraints().size();
        int row = index / cols;
        int col = index % cols;

        // Buscar el nodo en la posición calculada
        for (Node node : targetGrid.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                if (node instanceof StackPane slot) {
                    if (highlight) {
                        slot.setBorder(new Border(new BorderStroke(
                                Color.GOLD, BorderStrokeStyle.SOLID,
                                new CornerRadii(3), new BorderWidths(2))));
                    } else {
                        slot.setBorder(new Border(new BorderStroke(
                                SLOT_BORDER_COLOR, BorderStrokeStyle.SOLID,
                                new CornerRadii(3), new BorderWidths(1))));
                    }
                }
                break;
            }
        }
    }

    public void setCraftingRecipes(List<Recipe> recipes) {
        this.currentCraftingRecipes = recipes;
        craftingListPanel.setRecipes(recipes);
    }

    private Image generateFallbackImage(ItemType itemType) {
        Canvas canvas = new Canvas(ITEM_SIZE, ITEM_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ITEM_SIZE, ITEM_SIZE);
        FxItemDrawingHelper.drawFallbackShape(gc, 0, 0, ITEM_SIZE, ITEM_SIZE, itemType, 1.0);
        return canvas.snapshot(null, null);
    }

    private Image getItemImage(Item item) {
        if (item == null) return null;

        // Try getting sprite first
        SpriteManager.SpriteMetadata metadata = SpriteManager.getInstance().getItemSprite(item.getType());
        if (metadata != null && metadata.getImage() != null) {
            return metadata.getImage();
        }

        // Generate fallback with proper scaling
        return generateFallbackImage(item.getType());
    }

    private ImageView createItemImage(Item item) {
        if (item == null) {
            return new ImageView();
        }
        Image image = getItemImage(item);
        ImageView imageView = new ImageView(image);
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
        text.setFont(new Font("Arial", QUANTITY_FONT_SIZE));
        text.setFill(TEXT_COLOR);
        return text;
    }
}