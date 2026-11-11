package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.HotbarPager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import com.feuerschvenger.perlinsedge.infra.fx.utils.FxColors;

import java.util.List;
import java.util.function.Consumer;

/**
 * Hotbar panel for building selection in construction mode.
 * Appears at the bottom center of the screen for quick building selection.
 * The number keys 1 to 9 pick a slot of the current page; when there are more buildings than slots,
 * the page buttons, the Page Up and Page Down keys, or the mouse wheel switch to the next group so every
 * building can be reached.
 */
public class BuildingHotbarPanel extends HBox {

    private final RecipeManager recipeManager;
    private final HotbarPager<BuildingRecipe> pager;
    private Consumer<BuildingType> onBuildingSelectedListener;
    private BuildingType currentlySelectedBuildingType;

    private static final int NUM_SLOTS = 9;
    /** Slot edge in whole pixels; the UI layer never scales with the world zoom, so it stays this size. */
    static final double SLOT_SIZE = 48;
    /** Icon edge: the 16 px art at 2x, so every art pixel stays a whole square block. */
    static final double ICON_SIZE = 32;
    /** Gap between slots in whole pixels. */
    static final double SLOT_GAP = 4;

    public BuildingHotbarPanel(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        this.pager = new HotbarPager<>(recipeManager.getAllBuildingRecipes(), NUM_SLOTS);

        setupLayout();
        populateHotbar();
    }

    private void setupLayout() {
        // No panel behind the row: the slots float over the world, each one dark and translucent on its own
        setAlignment(Pos.BOTTOM_CENTER);
        setSpacing(SLOT_GAP);

        setVisible(false);
        setManaged(false);
    }

    private void populateHotbar() {
        List<BuildingRecipe> pageRecipes = pager.currentPage();
        getChildren().clear();

        int slotIndex = 1;
        for (BuildingRecipe recipe : pageRecipes) {
            getChildren().add(createHotbarSlot(slotIndex, recipe.buildingType(), recipe));
            slotIndex++;
        }

        // Fill remaining slots
        while (slotIndex <= NUM_SLOTS) {
            getChildren().add(createEmptyHotbarSlot(slotIndex));
            slotIndex++;
        }

        if (pager.getPageCount() > 1) {
            getChildren().add(createPageControls());
        }
    }

    private VBox createPageControls() {
        Label pageLabel = new Label((pager.getPage() + 1) + "/" + pager.getPageCount());
        pageLabel.getStyleClass().add(UiTheme.PILL);

        Button previous = new Button("<");
        previous.getStyleClass().add(UiTheme.PAGE_BUTTON);
        previous.setFocusTraversable(false);
        previous.setOnAction(event -> changePage(-1));
        Button next = new Button(">");
        next.getStyleClass().add(UiTheme.PAGE_BUTTON);
        next.setFocusTraversable(false);
        next.setOnAction(event -> changePage(1));

        HBox buttons = new HBox(SLOT_GAP, previous, next);
        buttons.setAlignment(Pos.CENTER);
        VBox controls = new VBox(SLOT_GAP, pageLabel, buttons);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0, 0, 0, SLOT_GAP));
        return controls;
    }

    private StackPane createHotbarSlot(int number, BuildingType type, BuildingRecipe recipe) {
        StackPane slot = createSlotShell();

        // Placeholder icon, centred in the slot
        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(ICON_SIZE, ICON_SIZE);
        iconWrapper.setMaxSize(ICON_SIZE, ICON_SIZE);
        iconWrapper.setBackground(new Background(new BackgroundFill(
                FxColors.toFx(type.getBaseColor().withOpacityFactor(0.5)),
                new CornerRadii(4),
                Insets.EMPTY
        )));

        Label typeLabel = new Label(UiTheme.caps(type.getDisplayName().substring(0, Math.min(4, type.getDisplayName().length()))));
        typeLabel.getStyleClass().add(UiTheme.SLOT_LABEL);
        iconWrapper.getChildren().add(typeLabel);
        slot.getChildren().add(iconWrapper);

        slot.getChildren().add(createNumberLabel(number));

        // Tooltip
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append(recipe.getDisplayName()).append("\nCost:\n");

        for (CraftingIngredient ingredient : recipe.ingredients()) {
            tooltipText.append(ingredient).append("\n");
        }

        Tooltip tooltip = new Tooltip(UiTheme.caps(tooltipText.toString()));
        Tooltip.install(slot, tooltip);

        // Click handler
        slot.setOnMouseClicked(event -> {
            selectBuildingType(type);
            highlightSelectedSlot(slot);
        });

        return slot;
    }

    private StackPane createEmptyHotbarSlot(int number) {
        StackPane slot = createSlotShell();
        slot.getStyleClass().add(UiTheme.SLOT_EMPTY);
        slot.getChildren().add(createNumberLabel(number));
        return slot;
    }

    /** A square slot of the fixed size, dark and translucent with a 1 px darker edge. */
    private static StackPane createSlotShell() {
        StackPane slot = new StackPane();
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMinSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMaxSize(SLOT_SIZE, SLOT_SIZE);
        slot.getStyleClass().add(UiTheme.SLOT);
        return slot;
    }

    /** The key number in the top left corner of a slot. */
    private static Label createNumberLabel(int number) {
        Label numberLabel = new Label(String.valueOf(number));
        numberLabel.getStyleClass().add(UiTheme.SLOT_NUMBER);
        numberLabel.setMouseTransparent(true);
        StackPane.setAlignment(numberLabel, Pos.TOP_LEFT);
        return numberLabel;
    }

    /** Gives the selected slot the white border and every other building slot the plain look. */
    private void highlightSelectedSlot(StackPane selectedSlot) {
        for (Node node : getChildren()) {
            if (isBuildingSlot(node)) {
                UiTheme.setClass(node, UiTheme.SLOT_SELECTED, node == selectedSlot);
            }
        }
    }

    private static boolean isBuildingSlot(Node node) {
        return node instanceof StackPane slot && !slot.getStyleClass().contains(UiTheme.SLOT_EMPTY);
    }

    /** @return true when there are more buildings than slots, so the hotbar has pages to flip between */
    public boolean hasMultiplePages() {
        return pager.getPageCount() > 1;
    }

    public void selectBuildingType(BuildingType type) {
        this.currentlySelectedBuildingType = type;
        if (onBuildingSelectedListener != null) {
            onBuildingSelectedListener.accept(type);
        }
    }

    /**
     * Selects the building behind a slot number of the current page.
     * An empty or unknown slot clears the selection.
     *
     * @param slotNumber slot number from 1 to 9
     */
    public void selectBuildingBySlotNumber(int slotNumber) {
        BuildingRecipe recipe = pager.select(slotNumber);
        if (recipe != null) {
            selectBuildingType(recipe.buildingType());

            if (slotNumber >= 1 && slotNumber <= getChildren().size()) {
                Node node = getChildren().get(slotNumber - 1);
                if (node instanceof StackPane slotPane) {
                    highlightSelectedSlot(slotPane);
                }
            }
        } else {
            selectBuildingType(null);
            resetSlotHighlights();
        }
    }

    /**
     * Shows the next or previous group of buildings. The selection is kept, and highlighted again when the
     * selected building is on the page now shown.
     *
     * @param delta 1 for the next page, -1 for the previous one
     */
    public void changePage(int delta) {
        if (pager.getPageCount() <= 1) return;

        if (delta > 0) {
            pager.next();
        } else {
            pager.previous();
        }
        populateHotbar();
        highlightCurrentSelection();
    }

    private void resetSlotHighlights() {
        for (Node node : getChildren()) {
            if (isBuildingSlot(node)) {
                UiTheme.setClass(node, UiTheme.SLOT_SELECTED, false);
            }
        }
    }

    private void highlightCurrentSelection() {
        if (currentlySelectedBuildingType == null) return;

        int index = 0;
        for (BuildingRecipe recipe : pager.currentPage()) {
            if (recipe.buildingType() == currentlySelectedBuildingType && getChildren().get(index) instanceof StackPane slot) {
                highlightSelectedSlot(slot);
                return;
            }
            index++;
        }
    }

    // Getters and setters

    public void setOnBuildingSelected(Consumer<BuildingType> listener) {
        this.onBuildingSelectedListener = listener;
    }

    /** Forgets the selected building and removes the highlight, without notifying the listener. */
    public void clearSelection() {
        currentlySelectedBuildingType = null;
        resetSlotHighlights();
    }

    /** Rebuilds the slots, on the page that holds the selected building when there is one. */
    public void refreshHotbarDisplay() {
        if (currentlySelectedBuildingType != null) {
            for (BuildingRecipe recipe : recipeManager.getAllBuildingRecipes()) {
                if (recipe.buildingType() == currentlySelectedBuildingType) {
                    pager.showPageOf(recipe);
                    break;
                }
            }
        }
        populateHotbar();
        highlightCurrentSelection();
    }

}
