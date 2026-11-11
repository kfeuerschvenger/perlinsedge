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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.function.Consumer;

/**
 * Hotbar panel for building selection in construction mode.
 * Appears at the bottom center of the screen for quick building selection.
 * The number keys 1 to 9 pick a slot of the current page; when there are more buildings than slots,
 * the page buttons (or the page keys) switch to the next group so every building can be reached.
 */
public class BuildingHotbarPanel extends HBox {

    private final RecipeManager recipeManager;
    private final HotbarPager<BuildingRecipe> pager;
    private Consumer<BuildingType> onBuildingSelectedListener;
    private BuildingType currentlySelectedBuildingType;

    private static final int NUM_SLOTS = 9;
    private static final double SLOT_SIZE = 60;
    private static final double ICON_SIZE = 48;

    public BuildingHotbarPanel(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        this.pager = new HotbarPager<>(recipeManager.getAllBuildingRecipes(), NUM_SLOTS);

        setupLayout();
        populateHotbar();
    }

    private void setupLayout() {
        setAlignment(Pos.BOTTOM_CENTER);
        setPadding(new Insets(10, 0, 10, 0));
        setSpacing(5);
        setBackground(new Background(new BackgroundFill(Color.web("#333333", 0.8), new CornerRadii(10), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(2))));

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
        pageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        pageLabel.setTextFill(Color.WHITE);

        Button previous = new Button("<");
        previous.setFocusTraversable(false);
        previous.setOnAction(event -> changePage(-1));
        Button next = new Button(">");
        next.setFocusTraversable(false);
        next.setOnAction(event -> changePage(1));

        HBox buttons = new HBox(4, previous, next);
        buttons.setAlignment(Pos.CENTER);
        VBox controls = new VBox(4, pageLabel, buttons);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0, 8, 0, 4));
        return controls;
    }

    private StackPane createHotbarSlot(int number, BuildingType type, BuildingRecipe recipe) {
        StackPane slot = new StackPane();
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setStyle("-fx-background-color: #555555; -fx-border-color: gray; -fx-border-radius: 5;");

        // Placeholder icon
        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(ICON_SIZE, ICON_SIZE);
        iconWrapper.setBackground(new Background(new BackgroundFill(
                FxColors.toFx(type.getBaseColor().withOpacityFactor(0.5)),
                new CornerRadii(5),
                Insets.EMPTY
        )));

        Label typeLabel = new Label(type.getDisplayName().substring(0, Math.min(4, type.getDisplayName().length())));
        typeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        typeLabel.setTextFill(Color.WHITE);
        iconWrapper.getChildren().add(typeLabel);
        slot.getChildren().add(iconWrapper);

        // Slot number
        Label numberLabel = new Label(String.valueOf(number));
        numberLabel.setFont(Font.font("Arial", FontWeight.BLACK, 16));
        numberLabel.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5);");
        StackPane.setAlignment(numberLabel, Pos.TOP_LEFT);
        slot.getChildren().add(numberLabel);

        // Tooltip
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append(recipe.getDisplayName()).append("\nCost:\n");

        for (CraftingIngredient ingredient : recipe.ingredients()) {
            tooltipText.append(ingredient).append("\n");
        }

        Tooltip tooltip = new Tooltip(tooltipText.toString());
        Tooltip.install(slot, tooltip);

        // Click handler
        slot.setOnMouseClicked(event -> {
            selectBuildingType(type);
            highlightSelectedSlot(slot);
        });

        return slot;
    }

    private StackPane createEmptyHotbarSlot(int number) {
        StackPane slot = new StackPane();
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setStyle("-fx-background-color: #444444; -fx-border-color: darkgray; -fx-border-style: dashed;");

        Label numberLabel = new Label(String.valueOf(number));
        numberLabel.setFont(Font.font("Arial", FontWeight.BLACK, 16));
        numberLabel.setTextFill(Color.web("#888888"));
        StackPane.setAlignment(numberLabel, Pos.TOP_LEFT);
        slot.getChildren().add(numberLabel);

        return slot;
    }

    private void highlightSelectedSlot(StackPane selectedSlot) {
        for (Node node : getChildren()) {
            if (node instanceof StackPane slot) {
                String style = slot == selectedSlot ?
                        "-fx-border-color: limegreen; -fx-border-width: 3;" :
                        "-fx-border-color: gray; -fx-border-width: 1;";
                slot.setStyle(slot.getStyle() + style);
            }
        }
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
            if (node instanceof StackPane slot) {
                slot.setStyle(slot.getStyle().replaceAll("-fx-border-color:.*?;", ""));
                slot.setStyle(slot.getStyle() + "-fx-border-color: gray; -fx-border-width: 1;");
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
