package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxItemDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Panel for displaying a scrollable list of craftable items and a craft button.
 */
public class CraftingListPanel extends VBox {
    private static final Color LIST_BG_COLOR = Color.rgb(40, 40, 40, 0.95);
    private static final Color CELL_BG_COLOR = Color.rgb(60, 60, 60, 0.8);
    private static final Color PANEL_BG_COLOR = Color.rgb(0, 0, 0, 0.7);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final int PANEL_PADDING = 15;
    private static final int SPACING = 10;
    private static final int ITEM_IMAGE_SIZE = 24;

    private final VBox recipesContainer; // Container for individual recipe cells
    private final Button craftButton;
    private Recipe selectedRecipe; // Currently selected recipe

    // Callbacks
    private Consumer<Recipe> onRecipeSelected;
    private Runnable onCraftButtonClicked;

    public CraftingListPanel() {
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(PANEL_PADDING));
        setSpacing(SPACING);
        setBackground(new Background(new BackgroundFill(PANEL_BG_COLOR, new CornerRadii(10), Insets.EMPTY)));
        setPrefWidth(200);
        setMinWidth(150);
        setMaxWidth(250);

        Label title = new Label("Crafting Recipes");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(TEXT_COLOR);

        recipesContainer = new VBox(5); // Spacing between recipes
        recipesContainer.setAlignment(Pos.TOP_LEFT);
        recipesContainer.setPadding(new Insets(5));
        recipesContainer.setBackground(new Background(new BackgroundFill(
                LIST_BG_COLOR, new CornerRadii(5), Insets.EMPTY
        )));
        recipesContainer.setBorder(new Border(new BorderStroke(
                Color.rgb(100, 100, 100), BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1)
        )));

        ScrollPane scrollPane = new ScrollPane(recipesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setPrefHeight(250);

        craftButton = new Button("Craft");
        craftButton.setPrefWidth(Double.MAX_VALUE);
        craftButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        craftButton.setOnAction(e -> {
            if (selectedRecipe != null && onCraftButtonClicked != null) {
                onCraftButtonClicked.run();
            }
        });
        craftButton.setDisable(true);

        getChildren().addAll(title, scrollPane, craftButton);
    }

    /**
     * Sets the list of recipes to display.
     * @param recipes List of available recipes.
     */
    public void setRecipes(List<Recipe> recipes) {
        System.out.println("SETTING RECIPES: " + (recipes != null ? recipes.size() : 0));
        if (recipes == null) {
            recipes = Collections.emptyList();
        }
        recipesContainer.getChildren().clear();
        setSelectedRecipe(null);

        if (recipes.isEmpty()) {
            Label emptyLabel = new Label("No crafting recipes available");
            emptyLabel.setTextFill(Color.LIGHTGRAY);
            recipesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Recipe recipe : recipes) {
            System.out.println("ADDING RECIPE: " + recipe.getName());
            CraftableItemCell cell = new CraftableItemCell(recipe);
            cell.setOnMouseClicked(e -> {
                System.out.println("RECIPE SELECTED: " + recipe.getName());
                setSelectedRecipe(recipe);
                if (onRecipeSelected != null) {
                    onRecipeSelected.accept(recipe);
                }
            });
            recipesContainer.getChildren().add(cell);
        }
    }

    /**
     * Sets the selected recipe. Handles visual highlighting and button enabling.
     * @param recipe Recipe to select, or null to deselect.
     */
    public void setSelectedRecipe(Recipe recipe) {
        // System.out.println("Setting selected recipe: " + (recipe != null ? recipe.getName() : "null") +
        //        " | Current: " + (this.selectedRecipe != null ? this.selectedRecipe.getName() : "null"));
        if (Objects.equals(this.selectedRecipe, recipe)) {
            return;
        }

        recipesContainer.getChildren().forEach(node -> {
            if (node instanceof CraftableItemCell) {
                ((CraftableItemCell) node).setSelected(false);
            }
        });

        this.selectedRecipe = recipe;

        if (recipe != null) {
            craftButton.setDisable(false);
            recipesContainer.getChildren().stream()
                    .filter(node -> node instanceof CraftableItemCell)
                    .map(node -> (CraftableItemCell) node)
                    .filter(cell -> cell.getRecipe().equals(recipe))
                    .findFirst()
                    .ifPresent(cell -> cell.setSelected(true));
        } else {
            craftButton.setDisable(true);
        }
    }

    public Recipe getSelectedRecipe() {
        return selectedRecipe;
    }

    public void setOnRecipeSelected(Consumer<Recipe> onRecipeSelected) {
        this.onRecipeSelected = onRecipeSelected;
    }

    public void setOnCraftButtonClicked(Runnable onCraftButtonClicked) {
        this.onCraftButtonClicked = onCraftButtonClicked;
    }

    /**
     * Inner class representing a single craftable item in the list.
     */
    private class CraftableItemCell extends HBox {
        private final Recipe recipe;
        private boolean selected;

        public CraftableItemCell(Recipe recipe) {
            this.recipe = recipe;
            this.selected = false;
            setSpacing(5);
            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(3, 5, 3, 5));
            setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
            setBorder(new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

            setPickOnBounds(true);

            ImageView imageView = new ImageView(getItemImage(recipe.getOutputItemType()));
            imageView.setFitWidth(ITEM_IMAGE_SIZE);
            imageView.setFitHeight(ITEM_IMAGE_SIZE);
            imageView.setPreserveRatio(true);

            Label nameLabel = new Label(recipe.getName());
            nameLabel.setFont(Font.font("Arial", 14));
            nameLabel.setTextFill(TEXT_COLOR);

            nameLabel.setPickOnBounds(true);

            getChildren().addAll(imageView, nameLabel);

            setOnMouseEntered(e -> {
                if (!selected)
                    setBackground(new Background(new BackgroundFill(
                        CELL_BG_COLOR, new CornerRadii(3), Insets.EMPTY
                )));
            });
            setOnMouseExited(e -> {
                if (!selected) setBackground(new Background(new BackgroundFill(
                        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY
                )));
            });

            setOnMouseClicked(e -> {
                System.out.println("Recipe cell clicked: " + recipe.getName());
                if (!selected) {
                    setSelected(true);
                    CraftingListPanel.this.setSelectedRecipe(recipe);
                    if (onRecipeSelected != null) {
                        onRecipeSelected.accept(recipe);
                    }
                }
                e.consume();
            });
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                setBackground(new Background(new BackgroundFill(
                        CELL_BG_COLOR, new CornerRadii(3), Insets.EMPTY
                )));
                setBorder(new Border(new BorderStroke(
                        Color.LIGHTBLUE, BorderStrokeStyle.SOLID,
                        new CornerRadii(3), new BorderWidths(1)
                )));
            } else {
                setBackground(new Background(new BackgroundFill(
                        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY
                )));
                setBorder(new Border(new BorderStroke(
                        Color.TRANSPARENT, BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, BorderWidths.DEFAULT
                )));
            }
        }

        private Image getItemImage(ItemType itemType) {
            SpriteManager.SpriteMetadata metadata = SpriteManager.getInstance().getItemSprite(itemType);
            if (metadata != null && metadata.getImage() != null) {
                return metadata.getImage();
            }
            return generateFallbackImage(itemType);
        }

        private Image generateFallbackImage(ItemType itemType) {
            Canvas canvas = new Canvas(CraftingListPanel.ITEM_IMAGE_SIZE, CraftingListPanel.ITEM_IMAGE_SIZE);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, CraftingListPanel.ITEM_IMAGE_SIZE, CraftingListPanel.ITEM_IMAGE_SIZE);
            FxItemDrawingHelper.drawFallbackShape(gc, 0, 0, CraftingListPanel.ITEM_IMAGE_SIZE, CraftingListPanel.ITEM_IMAGE_SIZE, itemType, 1.0);
            return canvas.snapshot(null, null);
        }
    }
}