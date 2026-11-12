package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Displays a scrollable list of craftable recipes with selection capability.
 * Allows users to select recipes and initiate crafting through a dedicated button.
 */
public class CraftingListPanel extends VBox {
    // Layout constants; the colours, fonts and backgrounds come from the theme
    private static final int PANEL_SPACING = 10;
    private static final int LIST_SPACING = 5;
    private static final int SCROLL_PANE_HEIGHT = 130;
    private static final int ITEM_IMAGE_SIZE = 32;

    private static final String TITLE_TEXT = "Crafting Recipes";
    private static final String EMPTY_LIST_TEXT = "No crafting recipes available";
    private static final String CRAFT_BUTTON_TEXT = "Craft";

    // UI components
    private VBox recipesContainer;
    private Button craftButton;

    // State management
    private Recipe selectedRecipe;
    /** One cell per recipe, in the order the recipes were given. */
    private final List<CraftableItemCell> cells = new ArrayList<>();
    private Predicate<Recipe> canCraft = recipe -> true;

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
     * Sets the list of recipes to display. The recipes that can be crafted are listed first and the others after them,
     * greyed but still selectable, each group in the order the recipes were given.
     *
     * @param recipes List of available recipes (null will be treated as empty)
     */
    public void setRecipes(List<Recipe> recipes) {
        recipes = Objects.requireNonNullElse(recipes, Collections.emptyList());
        recipesContainer.getChildren().clear();
        cells.clear();
        setSelectedRecipe(null);

        if (recipes.isEmpty()) {
            showEmptyListIndicator();
            return;
        }

        populateRecipeList(recipes);
        arrange();
    }

    /**
     * Tells the list which recipes can be crafted now, and lists them first. The list calls this again whenever the
     * inventory changes. The selection and the scroll position are kept, and the order inside each group stays that of
     * the recipes as they were given.
     *
     * @param canCraft tells whether a recipe can be crafted with what the player holds
     */
    public void setCraftability(Predicate<Recipe> canCraft) {
        this.canCraft = Objects.requireNonNull(canCraft, "canCraft");
        arrange();
    }

    /**
     * Marks every cell as craftable or not, puts the craftable ones first and keeps the craft button in line with the
     * selected recipe.
     */
    private void arrange() {
        for (CraftableItemCell cell : cells) {
            cell.setCraftable(canCraft.test(cell.getRecipe()));
        }
        // A stable sort: false sorts before true, so the craftable cells come first and keep their relative order
        List<CraftableItemCell> ordered = cells.stream()
                .sorted(Comparator.comparing((CraftableItemCell cell) -> !cell.isCraftable()))
                .toList();
        // With no cells the container holds the empty list notice, which must stay
        if (!cells.isEmpty() && !ordered.equals(recipesContainer.getChildren())) {
            recipesContainer.getChildren().setAll(ordered);
        }
        updateCraftButton();
    }

    private void updateCraftButton() {
        craftButton.setDisable(selectedRecipe == null || !canCraft.test(selectedRecipe));
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
            cells.add(cell);
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
        }
        updateCraftButton();
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
        private static final double UNAVAILABLE_ICON_OPACITY = 0.45;

        private final Recipe recipe;
        private ImageView icon;
        private boolean craftable = true;

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
            icon = createItemImageView();
            Label nameLabel = createNameLabel();
            getChildren().addAll(icon, nameLabel);
        }

        /**
         * Creates the item image view.
         */
        private ImageView createItemImageView() {
            return ItemIcons.view(recipe.getOutputItemType(), ITEM_IMAGE_SIZE);
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

        boolean isCraftable() {
            return craftable;
        }

        /**
         * Greys the cell when the recipe cannot be crafted. The cell stays selectable so the player can read what the
         * recipe needs.
         */
        void setCraftable(boolean craftable) {
            this.craftable = craftable;
            UiTheme.setClass(this, UiTheme.CELL_UNAVAILABLE, !craftable);
            icon.setOpacity(craftable ? 1.0 : UNAVAILABLE_ICON_OPACITY);
        }

        /**
         * Sets the selection state of the cell.
         *
         * @param selected True to select, false to deselect
         */
        public void setSelected(boolean selected) {
            UiTheme.setClass(this, UiTheme.CELL_SELECTED, selected);
        }

    }

}