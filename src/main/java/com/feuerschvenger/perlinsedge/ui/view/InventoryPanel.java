package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.containers.SlotRef;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemDropCallback;
import com.feuerschvenger.perlinsedge.ui.view.callbacks.ItemTransferCallback;
import com.feuerschvenger.perlinsedge.ui.view.utils.GridUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
    // Layout dimensions
    private static final int SLOT_SIZE = (int) ItemHotbarPanel.SLOT_SIZE;
    /** Item icon size: the 16 px art at 2x, so every art pixel stays a whole square block. */
    private static final int ITEM_SIZE = 32;
    private static final double ITEM_CENTER_DIVISOR = 2.0;
    private static final double CRAFTING_PANEL_WIDTH = 375;
    private static final int DUAL_INVENTORY_SPACING = 20;
    private static final int INVENTORY_BOX_SPACING = 10;
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
    /** The hotbar row at the bottom of the screen, which joins this panel while it is open, or null. */
    private ItemHotbarPanel hotbar;

    // State management
    private final GameStateManager gameState;
    /** The behaviour of the panel, package-private so the tests can reach its window focus wiring. */
    @Getter(lombok.AccessLevel.PACKAGE)
    private final InventoryController controller;
    @Getter(lombok.AccessLevel.PACKAGE)
    private Container secondaryContainer;
    // What the last refresh drew, so an unchanged inventory and selection cost nothing
    /** The player and inventory version the crafting list was last marked for, valid only while the flag is set. */
    private Player craftabilityPlayer;
    private long craftabilityVersion;
    private boolean craftabilityValid;
    /** The player, inventory version and selection the information panel last showed, valid only while flagged. */
    private boolean infoValid;
    private Player infoPlayer;
    private long infoVersion;
    private int infoSlotIndex;
    private boolean infoSlotInPlayer;
    private Recipe infoRecipe;

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
        // Both sizes follow the content, so there is no empty band above or below the grid
        setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
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
        var container = new VBox(ITEM_SPACING);
        container.setAlignment(Pos.CENTER);

        // The grid holds the inventory slots only: the hotbar slots come first in the storage and have their own row
        List<StackPane> slots = new ArrayList<>(Player.INVENTORY_SIZE);
        for (int i = Player.HOTBAR_SIZE; i < Player.HOTBAR_SIZE + Player.INVENTORY_SIZE; i++) {
            slots.add(createInventorySlot(i, gameState.getPlayer()));
        }
        playerInventoryGrid = GridUtils.createPlayerGrid(slots);

        container.getChildren().addAll(createTitle(PLAYER_INVENTORY_TITLE), playerInventoryGrid);
        return container;
    }

    /** Creates the slots of the grid of an open chest, indexed from zero. */
    private List<StackPane> createChestSlots(int capacity, Container container) {
        List<StackPane> slots = new ArrayList<>(capacity);
        for (var i = 0; i < capacity; i++) {
            slots.add(createInventorySlot(i, container));
        }
        return slots;
    }

    private VBox createRightContainer() {
        var container = new VBox(ITEM_SPACING);
        container.setAlignment(Pos.TOP_CENTER);

        craftingListPanel = new CraftingListPanel();
        craftingListPanel.setPrefWidth(CRAFTING_PANEL_WIDTH);

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

    private static Label createTitle(String text) {
        var label = new Label(UiTheme.caps(text));
        label.getStyleClass().add(UiTheme.PILL);
        return label;
    }

    // ==================================================================
    //  Slots
    // ==================================================================

    private StackPane createInventorySlot(int slotIndex, Container container) {
        var slot = new StackPane();
        configureSlotLayout(slot);
        slot.setUserData(new SlotContext(slotIndex, container));

        slot.setOnMousePressed(e -> controller.handleSlotPressed(e, slot));
        slot.setOnMouseClicked(controller::handleSlotClick);
        slot.setOnMouseEntered(e -> controller.handleSlotHover(slot, true));
        slot.setOnMouseExited(e -> controller.handleSlotHover(slot, false));

        return slot;
    }

    private static void configureSlotLayout(StackPane slot) {
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMinSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMaxSize(SLOT_SIZE, SLOT_SIZE);
        slot.getStyleClass().add(UiTheme.SLOT);
    }

    /**
     * Makes the slots of the hotbar at the bottom of the screen part of this panel while it is open: they take the
     * same drag, drop, hover and throw actions as the slots of the grid. The hotbar keeps drawing itself, and its
     * selection border, which marks the slot held in hand, is never touched by the highlights of this panel.
     *
     * @param hotbarPanel the hotbar of the items the player carries
     */
    public void attachHotbar(ItemHotbarPanel hotbarPanel) {
        this.hotbar = Objects.requireNonNull(hotbarPanel, "Hotbar panel cannot be null");
        for (var i = 0; i < Player.HOTBAR_SIZE; i++) {
            StackPane slot = hotbarPanel.slotNode(i);
            var context = new SlotContext(i, gameState.getPlayer());
            context.hotbar = true;
            slot.setUserData(context);
            // While no inventory is open the row is a display: a press on it must not start a drag
            slot.setOnMousePressed(e -> {
                if (isVisible()) {
                    controller.handleSlotPressed(e, slot);
                }
            });
            slot.setOnMouseClicked(e -> {
                if (isVisible()) {
                    controller.handleSlotClick(e);
                }
            });
            slot.setOnMouseEntered(e -> {
                if (isVisible()) {
                    controller.handleSlotHover(slot, true);
                }
            });
            slot.setOnMouseExited(e -> controller.handleSlotHover(slot, false));
        }
    }

    /** Highlights a slot as a valid drop target. */
    void showDropHighlight(StackPane slot) {
        if (!isHotbarSlot(slot)) {
            UiTheme.setClass(slot, UiTheme.SLOT_SELECTED, false);
        }
        UiTheme.setClass(slot, UiTheme.SLOT_DROP, true);
    }

    /** Restores a slot's default border. */
    void restoreSlotBorder(StackPane slot) {
        // The selection border of a hotbar slot marks the slot held in hand, which only the hotbar changes
        if (!isHotbarSlot(slot)) {
            UiTheme.setClass(slot, UiTheme.SLOT_SELECTED, false);
        }
        UiTheme.setClass(slot, UiTheme.SLOT_DROP, false);
    }

    private static boolean isHotbarSlot(StackPane slot) {
        return slot.getUserData() instanceof SlotContext context && context.hotbar;
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
        private final int index;
        private Container container;
        /** True for the slots of the hotbar row at the bottom of the screen, false for the slots of the grids. */
        private boolean hotbar;
        /** The item type the slot node draws, null for an empty slot. */
        private ItemType shownType;
        /** The count the slot node draws, 0 for an empty slot. */
        private int shownQuantity;

        SlotContext(int index, Container container) {
            this.index = index;
            this.container = container;
        }

        /** @return the index of the slot in its container */
        int index() {
            return index;
        }

        /** @return the container the slot belongs to */
        Container container() {
            return container;
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
        if (slot == null && hotbar != null && isVisible()) {
            slot = slotNodeAt(hotbar, sceneX, sceneY);
        }
        return slot;
    }

    private static StackPane slotNodeAt(Pane parent, double sceneX, double sceneY) {
        if (parent == null) {
            return null;
        }
        for (Node node : parent.getChildren()) {
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
        if (dragGhost == null) {
            return;
        }
        Point2D local = sceneToLocal(sceneX, sceneY);
        if (local != null) {
            // On whole pixels: an icon that sits between two pixels has some of its art pixels drawn wider than others
            dragGhost.relocate(Math.rint(local.getX() - ITEM_SIZE / ITEM_CENTER_DIVISOR),
                    Math.rint(local.getY() - ITEM_SIZE / ITEM_CENTER_DIVISOR));
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
     * new quantity, and the drag ends when the stack is gone. The game calls this every tick, so a panel that is not
     * open does nothing: opening it draws everything, and from then on only what changed is redrawn.
     */
    public void updateInventoryDisplay() {
        if (isVisible()) {
            refreshDisplay();
        }
    }

    private void refreshDisplay() {
        updateGridContents(playerInventoryGrid, gameState.getPlayer());
        updateHotbarContext();

        if (secondaryContainer != null) {
            updateGridContents(secondaryInventoryGrid, secondaryContainer);
        }

        final var player = gameState.getPlayer();
        final long version = player == null ? -1 : player.getInventoryVersion();
        // The crafting list is out of the layout while a chest is open, so nothing there needs the answers
        if (secondaryContainer == null
                && (!craftabilityValid || player != craftabilityPlayer || version != craftabilityVersion)) {
            refreshCraftability();
        }
        if (secondaryContainer != null || !infoUpToDate(player, version)) {
            updateInformationDisplay();
            rememberInfoSubject(player, version);
        }
        controller.refreshDrag();
    }

    /**
     * Tells whether the information panel already shows what the selection and the inventory call for. A chest has no
     * change count, so while one is open the panel is always redrawn.
     */
    private boolean infoUpToDate(Player player, long version) {
        if (!infoValid || player != infoPlayer || version != infoVersion) {
            return false;
        }
        final var sameSlot = controller.getSelectedSlotIndex() == infoSlotIndex
                && controller.isSelectedSlotInPlayerInventory() == infoSlotInPlayer;
        return sameSlot && Objects.equals(controller.getSelectedRecipe(), infoRecipe);
    }

    private void rememberInfoSubject(Player player, long version) {
        infoValid = true;
        infoPlayer = player;
        infoVersion = version;
        infoSlotIndex = controller.getSelectedSlotIndex();
        infoSlotInPlayer = controller.isSelectedSlotInPlayerInventory();
        infoRecipe = controller.getSelectedRecipe();
    }

    /** Makes the next refresh redraw the crafting list and the information panel. */
    private void forgetShownState() {
        craftabilityValid = false;
        infoValid = false;
    }

    /**
     * Tells whether a point of the scene is inside the panel, over a slot or not.
     *
     * @param sceneX X coordinate in the scene
     * @param sceneY Y coordinate in the scene
     * @return true when the point is within the bounds of the panel
     */
    boolean containsScenePoint(double sceneX, double sceneY) {
        if (localToScene(getLayoutBounds()).contains(sceneX, sceneY)) {
            return true;
        }
        // The hotbar is part of the inventory while it is open, so releasing over its gaps is not a throw either
        return hotbar != null && isVisible() && hotbar.localToScene(hotbar.getLayoutBounds()).contains(sceneX, sceneY);
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
            itemInfoPanel.displayRecipe(controller.getSelectedRecipe(), this::countHeld);
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

        var currentItem = getItemInSlot(controller.getSelectedSlotIndex(), currentContainer);
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
        itemInfoPanel.displayRecipe(recipe, this::countHeld);
    }

    /** Gives the ingredient lines of the recipe now shown in the information panel, for the tests. */
    List<javafx.scene.text.Text> ingredientLinesShown() {
        return itemInfoPanel.shownIngredientLines();
    }

    /** How many items of a type the player holds, hotbar and inventory together. */
    private int countHeld(ItemType type) {
        var player = gameState.getPlayer();
        return player == null ? 0 : player.getItemCount(new Item(type, 1));
    }

    /** Lists the recipes that can be crafted with what the player holds first, and greys the others. */
    private void refreshCraftability() {
        var player = gameState.getPlayer();
        craftabilityValid = true;
        craftabilityPlayer = player;
        craftabilityVersion = player == null ? -1 : player.getInventoryVersion();
        craftingListPanel.setCraftability(recipe -> player != null && player.canCraft(recipe));
    }

    /** @return the recipe selected in the crafting list, or null */
    Recipe getSelectedCraftingRecipe() {
        return craftingListPanel.getSelectedRecipe();
    }

    /** Points the hotbar slots at the current player, who is a new object after a rebuild of the world. */
    private void updateHotbarContext() {
        if (hotbar == null) {
            return;
        }
        for (var i = 0; i < Player.HOTBAR_SIZE; i++) {
            ((SlotContext) hotbar.slotNode(i).getUserData()).container = gameState.getPlayer();
        }
    }

    /** Redraws the slots of a grid. A slot keeps the index it was created with: the grid may show a range of a container. */
    private void updateGridContents(GridPane grid, Container container) {
        for (Node node : grid.getChildren()) {
            if (node instanceof StackPane slot) {
                SlotContext context = (SlotContext) slot.getUserData();
                context.container = container;
                updateSlotVisuals(slot, context.index, container);
            }
        }
    }

    /** Redraws a slot only when its item type or its count differ from what the slot shows. */
    private void updateSlotVisuals(StackPane slot, int index, Container container) {
        final SlotContext context = (SlotContext) slot.getUserData();
        final var item = getItemInSlot(index, container);
        final ItemType type = item != null ? item.getType() : null;
        final int quantity = item != null ? item.getQuantity() : 0;
        if (type == context.shownType && quantity == context.shownQuantity) {
            return;
        }
        context.shownType = type;
        context.shownQuantity = quantity;

        slot.getChildren().clear();
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
        forgetShownState();
        cleanRightContainer();
        setVisible(true);
        setManaged(true);
        refreshDisplay();
    }

    /**
     * Opens a dual inventory display (player + secondary container).
     *
     * @param otherContainer the secondary container, such as a chest
     */
    public void openDualInventory(Container otherContainer) {
        this.secondaryContainer = Objects.requireNonNull(otherContainer, "Secondary container cannot be null");
        forgetShownState();
        initializeSecondaryInventory();
        configureDualInventoryLayout();
        setVisible(true);
        setManaged(true);
        refreshDisplay();
    }

    private void initializeSecondaryInventory() {
        List<StackPane> slots = createChestSlots(secondaryContainer.getCapacity(), secondaryContainer);
        secondaryInventoryGrid = GridUtils.createGrid(slots);
    }

    private void configureDualInventoryLayout() {
        VBox rightContainer = (VBox) getRight();
        rightContainer.getChildren().clear();
        rightContainer.getChildren().addAll(secondaryInventoryLabel, secondaryInventoryGrid);

        var centerContainer = new HBox(DUAL_INVENTORY_SPACING);
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
        var box = new VBox(INVENTORY_BOX_SPACING);
        box.getChildren().addAll(createTitle(title), grid);
        return box;
    }

    /** Closes the inventory panel and resets state. */
    public void closeInventory() {
        controller.cancelDrag();
        this.secondaryContainer = null;
        forgetShownState();
        resetLayout();
        clearState();
        setVisible(false);
        setManaged(false);
    }

    private void resetLayout() {
        var centerContainer = new VBox(ITEM_SPACING);
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
     * Highlights or unhighlights the slot that holds the current selection: a slot of the hotbar row for an index
     * below the hotbar size, a slot of the grid of the selected container otherwise. The hotbar slot gets its own
     * marker, because its selection border already marks the slot held in hand.
     *
     * @param index     Slot index to modify
     * @param highlight True to highlight, false to restore
     */
    void highlightSlot(int index, boolean highlight) {
        if (index < 0) {
            return;
        }

        if (index < Player.HOTBAR_SIZE && controller.isSelectedSlotInPlayerInventory()) {
            if (hotbar != null) {
                applySlotHighlight(hotbar.slotNode(index), highlight);
            }
            return;
        }

        GridPane targetGrid = controller.isSelectedSlotInPlayerInventory() ?
                playerInventoryGrid : secondaryInventoryGrid;

        if (targetGrid == null) {
            return;
        }

        for (Node node : targetGrid.getChildren()) {
            if (node instanceof StackPane slot && ((SlotContext) slot.getUserData()).index == index) {
                applySlotHighlight(slot, highlight);
                break;
            }
        }
    }

    private void applySlotHighlight(StackPane slot, boolean highlight) {
        if (isHotbarSlot(slot)) {
            UiTheme.setClass(slot, UiTheme.SLOT_PICKED, highlight);
            if (highlight) {
                UiTheme.setClass(slot, UiTheme.SLOT_DROP, false);
            }
            return;
        }
        if (highlight) {
            UiTheme.setClass(slot, UiTheme.SLOT_DROP, false);
            UiTheme.setClass(slot, UiTheme.SLOT_SELECTED, true);
        } else {
            restoreSlotBorder(slot);
        }
    }

    private static ImageView createItemImage(Item item) {
        return ItemIcons.view(item.getType(), ITEM_SIZE);
    }

    /** The stack size in the pixel font with a 1 px dark shadow, at the bottom right corner of the slot. */
    private static Label createCountLabel(Item item) {
        var count = new Label(String.valueOf(item.getQuantity()));
        count.getStyleClass().add(UiTheme.COUNT);
        count.setMouseTransparent(true);
        StackPane.setAlignment(count, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(count, new Insets(0, COUNT_MARGIN, 0, 0));
        return count;
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
    public void setOnCraftRecipe(Consumer<Recipe> callback) {
        controller.setOnCraftRecipeCallback(callback);
    }

    /**
     * Sets the recipes shown in the crafting list.
     *
     * @param recipes the recipes
     */
    public void setCraftingRecipes(List<Recipe> recipes) {
        craftingListPanel.setRecipes(recipes);
        refreshCraftability();
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
