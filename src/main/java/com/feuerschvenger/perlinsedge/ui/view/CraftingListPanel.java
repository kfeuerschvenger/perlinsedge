package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers.FxItemDrawingHelper;
import com.feuerschvenger.perlinsedge.infra.fx.utils.SpriteManager;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Displays a scrollable list of craftable recipes with selection capability.
 * Allows users to select recipes and initiate crafting through a dedicated button.
 */
public class CraftingListPanel extends VBox {
    // Layout constants; the colours, fonts and backgrounds come from the theme
    private static final int PANEL_SPACING = 10;
    private static final int LIST_SPACING = 5;
    private static final int SCROLL_PANE_HEIGHT = 250;
    private static final int ITEM_IMAGE_SIZE = 32;

    private static final String TITLE_TEXT = "Crafting Recipes";
    private static final String EMPTY_LIST_TEXT = "No crafting recipes available";
    private static final String CRAFT_BUTTON_TEXT = "Craft";

    // UI components
    private VBox recipesContainer;
    private Button craftButton;

    // State management
    private Recipe selectedRecipe;

    // Event callbacks
    private Consumer<Recipe> onRecipeSelected;
    private Runnable onCraftButtonClicked;

    /**
     * Initializes the crafting list panel with default configuration.
     */
    public CraftingListPanel() {
        configurePanelLayout();
        initializeComponents();
        assemblePanel();
    }

    /**
     * Configures the panel's layout properties.
     */
    private void configurePanelLayout() {
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add(UiTheme.PANEL);
        setSpacing(PANEL_SPACING);
        setPrefWidth(200);
        setMinWidth(150);
        setMaxWidth(250);
    }

    /**
     * Initializes all UI components.
     */
    private void initializeComponents() {
        recipesContainer = createRecipesContainer();
        craftButton = createCraftButton();
    }

    /**
     * Assembles all components into the main panel.
     */
    private void assemblePanel() {
        Label title = createTitleLabel();
        ScrollPane scrollPane = createScrollPane();

        getChildren().addAll(title, scrollPane, craftButton);
    }

    /**
     * Creates the recipes container with consistent styling.
     */
    private VBox createRecipesContainer() {
        VBox container = new VBox(LIST_SPACING);
        container.setAlignment(Pos.TOP_LEFT);
        container.getStyleClass().add(UiTheme.LIST);
        return container;
    }

    /**
     * Creates the scroll pane for the recipes list.
     */
    private ScrollPane createScrollPane() {
        ScrollPane scrollPane = new ScrollPane(recipesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(SCROLL_PANE_HEIGHT);
        return scrollPane;
    }

    /**
     * Creates the title label with consistent styling.
     */
    private Label createTitleLabel() {
        Label title = new Label(UiTheme.caps(TITLE_TEXT));
        title.getStyleClass().add(UiTheme.PILL);
        return title;
    }

    /**
     * Creates the craft button with consistent styling and behavior.
     */
    private Button createCraftButton() {
        Button button = new Button(UiTheme.caps(CRAFT_BUTTON_TEXT));
        button.setPrefWidth(Double.MAX_VALUE);
        button.setDisable(true);
        button.setOnAction(e -> {
            if (selectedRecipe != null && onCraftButtonClicked != null) {
                onCraftButtonClicked.run();
            }
        });
        return button;
    }

    /**
     * Sets the list of recipes to display.
     *
     * @param recipes List of available recipes (null will be treated as empty)
     */
    public void setRecipes(List<Recipe> recipes) {
        recipes = Objects.requireNonNullElse(recipes, Collections.emptyList());
        recipesContainer.getChildren().clear();
        setSelectedRecipe(null);

        if (recipes.isEmpty()) {
            showEmptyListIndicator();
            return;
        }

        populateRecipeList(recipes);
    }

    /**
     * Displays an indicator when no recipes are available.
     */
    private void showEmptyListIndicator() {
        Label emptyLabel = new Label(UiTheme.caps(EMPTY_LIST_TEXT));
        emptyLabel.getStyleClass().add(UiTheme.MUTED);
        recipesContainer.getChildren().add(emptyLabel);
    }

    /**
     * Populates the recipe list with craftable items.
     */
    private void populateRecipeList(List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            CraftableItemCell cell = new CraftableItemCell(recipe);
            cell.setOnMouseClicked(e -> handleRecipeSelection(recipe));
            recipesContainer.getChildren().add(cell);
        }
    }

    /**
     * Handles recipe selection logic.
     */
    private void handleRecipeSelection(Recipe recipe) {
        setSelectedRecipe(recipe);
        if (onRecipeSelected != null) {
            onRecipeSelected.accept(recipe);
        }
    }

    /**
     * Sets the currently selected recipe and updates UI state.
     *
     * @param recipe Recipe to select (null to clear selection)
     */
    public void setSelectedRecipe(Recipe recipe) {
        if (Objects.equals(selectedRecipe, recipe)) {
            return;
        }

        clearPreviousSelection();
        selectedRecipe = recipe;

        if (recipe != null) {
            activateSelection(recipe);
            craftButton.setDisable(false);
        } else {
            craftButton.setDisable(true);
        }
    }

    /**
     * Clears all previous selection states.
     */
    private void clearPreviousSelection() {
        recipesContainer.getChildren().forEach(node -> {
            if (node instanceof CraftableItemCell) {
                ((CraftableItemCell) node).setSelected(false);
            }
        });
    }

    /**
     * Activates selection for a specific recipe.
     */
    private void activateSelection(Recipe recipe) {
        recipesContainer.getChildren().stream()
                .filter(CraftableItemCell.class::isInstance)
                .map(CraftableItemCell.class::cast)
                .filter(cell -> cell.getRecipe().equals(recipe))
                .findFirst()
                .ifPresent(cell -> cell.setSelected(true));
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
     * Represents a single craftable recipe item in the list.
     */
    private class CraftableItemCell extends HBox {
        private final Recipe recipe;

        /**
         * Creates a new craftable item cell.
         *
         * @param recipe Recipe to represent
         */
        public CraftableItemCell(Recipe recipe) {
            this.recipe = Objects.requireNonNull(recipe, "Recipe cannot be null");

            configureCellLayout();
            initializeCellComponents();
        }

        /**
         * Configures the cell's layout properties.
         */
        private void configureCellLayout() {
            setSpacing(LIST_SPACING);
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add(UiTheme.CELL);
            setPickOnBounds(true);
        }

        /**
         * Initializes the cell's UI components.
         */
        private void initializeCellComponents() {
            ImageView imageView = createItemImageView();
            Label nameLabel = createNameLabel();
            getChildren().addAll(imageView, nameLabel);
        }

        /**
         * Creates the item image view.
         */
        private ImageView createItemImageView() {
            ImageView imageView = new ImageView(getItemImage(recipe.getOutputItemType()));
            imageView.setFitWidth(ITEM_IMAGE_SIZE);
            imageView.setFitHeight(ITEM_IMAGE_SIZE);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(false);
            return imageView;
        }

        /**
         * Creates the recipe name label.
         */
        private Label createNameLabel() {
            Label label = new Label(UiTheme.caps(recipe.getName()));
            label.setPickOnBounds(true);
            return label;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        /**
         * Sets the selection state of the cell.
         *
         * @param selected True to select, false to deselect
         */
        public void setSelected(boolean selected) {
            UiTheme.setClass(this, UiTheme.CELL_SELECTED, selected);
        }

        /**
         * Retrieves the item image for display.
         */
        private Image getItemImage(ItemType itemType) {
            SpriteManager.SpriteMetadata metadata = SpriteManager.getInstance().getItemSprite(itemType);
            if (metadata != null && metadata.getImage() != null) {
                return metadata.getImage();
            }
            return generateFallbackImage(itemType);
        }

        /**
         * Generates a fallback image when no sprite is available.
         */
        private Image generateFallbackImage(ItemType itemType) {
            Canvas canvas = new Canvas(ITEM_IMAGE_SIZE, ITEM_IMAGE_SIZE);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, ITEM_IMAGE_SIZE, ITEM_IMAGE_SIZE);
            FxItemDrawingHelper.drawFallbackShape(
                    gc, 0, 0, ITEM_IMAGE_SIZE, ITEM_IMAGE_SIZE, itemType, 1.0
            );
            return canvas.snapshot(null, null);
        }
    }

}