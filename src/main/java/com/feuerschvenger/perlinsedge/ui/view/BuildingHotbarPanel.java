package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Hotbar panel for building selection in construction mode.
 * Appears at the bottom center of the screen for quick building selection.
 */
public class BuildingHotbarPanel extends HBox {

    private final RecipeManager recipeManager;
    private final Map<Integer, BuildingType> hotbarSlots;
    private Consumer<BuildingType> onBuildingSelectedListener;
    private BuildingType currentlySelectedBuildingType;

    private static final int NUM_SLOTS = 9;
    private static final double SLOT_SIZE = 60;
    private static final double ICON_SIZE = 48;

    public BuildingHotbarPanel(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        this.hotbarSlots = new HashMap<>();

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
        List<BuildingRecipe> buildingRecipes = recipeManager.getAllBuildingRecipes();
        int slotIndex = 1;
        getChildren().clear();

        for (BuildingRecipe recipe : buildingRecipes) {
            if (slotIndex > NUM_SLOTS) break;

            BuildingType buildingType = recipe.buildingType();
            hotbarSlots.put(slotIndex, buildingType);
            getChildren().add(createHotbarSlot(slotIndex, buildingType, recipe));
            slotIndex++;
        }

        // Fill remaining slots
        while (slotIndex <= NUM_SLOTS) {
            getChildren().add(createEmptyHotbarSlot(slotIndex));
            slotIndex++;
        }
    }

    private StackPane createHotbarSlot(int number, BuildingType type, BuildingRecipe recipe) {
        StackPane slot = new StackPane();
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setStyle("-fx-background-color: #555555; -fx-border-color: gray; -fx-border-radius: 5;");

        // Placeholder icon
        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(ICON_SIZE, ICON_SIZE);
        iconWrapper.setBackground(new Background(new BackgroundFill(
                type.getBaseColor().deriveColor(0, 1, 1, 0.5),
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

    public void selectBuildingBySlotNumber(int slotNumber) {
        if (hotbarSlots.containsKey(slotNumber)) {
            BuildingType type = hotbarSlots.get(slotNumber);
            selectBuildingType(type);

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

    private void resetSlotHighlights() {
        for (Node node : getChildren()) {
            if (node instanceof StackPane slot) {
                slot.setStyle(slot.getStyle().replaceAll("-fx-border-color:.*?;", ""));
                slot.setStyle(slot.getStyle() + "-fx-border-color: gray; -fx-border-width: 1;");
            }
        }
    }

    // Getters and setters

    public void setOnBuildingSelected(Consumer<BuildingType> listener) {
        this.onBuildingSelectedListener = listener;
    }

    public void refreshHotbarDisplay() {
        populateHotbar();
        if (currentlySelectedBuildingType != null) {
            for (Map.Entry<Integer, BuildingType> entry : hotbarSlots.entrySet()) {
                if (entry.getValue() == currentlySelectedBuildingType) {
                    selectBuildingBySlotNumber(entry.getKey());
                    break;
                }
            }
        }
    }

}