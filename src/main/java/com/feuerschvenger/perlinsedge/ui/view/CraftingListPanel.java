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
 * Displays a scrollable list of craftable recipes with selection capability.
 * Allows users to select recipes and initiate crafting through a dedicated button.
 */
public class CraftingListPanel extends VBox {
    // UI styling constants
    private static final Color PANEL_BG_COLOR = Color.rgb(0, 0, 0, 0.7);
    private static final Color LIST_BG_COLOR = Color.rgb(40, 40, 40, 0.95);
    private static final Color CELL_BG_COLOR = Color.rgb(60, 60, 60, 0.8);
    private static final Color CELL_HOVER_COLOR = CELL_BG_COLOR;
    private static final Color CELL_SELECTED_BORDER = Color.LIGHTBLUE;
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color EMPTY_TEXT_COLOR = Color.LIGHTGRAY;
    private static final Color LIST_BORDER_COLOR = Color.rgb(100, 100, 100);

    // Layout constants
    private static final int PANEL_PADDING = 15;
    private static final int PANEL_SPACING = 10;
    private static final int LIST_SPACING = 5;
    private static final int LIST_PADDING = 5;
    private static final int LIST_CORNER_RADIUS = 5;
    private static final int CELL_PADDING = 3;
    private static final int CELL_CORNER_RADIUS = 3;
    private static final int SCROLL_PANE_HEIGHT = 250;
    private static final int ITEM_IMAGE_SIZE = 24;

    // Text styling constants
    private static final String FONT_FAMILY = "Arial";
    private static final Font TITLE_FONT = Font.font(FONT_FAMILY, FontWeight.BOLD, 18);
    private static final Font NAME_FONT = Font.font(FONT_FAMILY, 14);
    private static final Font BUTTON_FONT = Font.font(FONT_FAMILY, FontWeight.BOLD, 16);
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
        setPadding(new Insets(PANEL_PADDING));
        setSpacing(PANEL_SPACING);
        setBackground(new Background(new BackgroundFill(
                PANEL_BG_COLOR, new CornerRadii(LIST_CORNER_RADIUS), Insets.EMPTY
        )));
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
        container.setPadding(new Insets(LIST_PADDING));
        container.setBackground(new Background(new BackgroundFill(
                LIST_BG_COLOR, new CornerRadii(LIST_CORNER_RADIUS), Insets.EMPTY
        )));
        container.setBorder(new Border(new BorderStroke(
                LIST_BORDER_COLOR, BorderStrokeStyle.SOLID,
                new CornerRadii(LIST_CORNER_RADIUS), new BorderWidths(1)
        )));
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
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setPrefHeight(SCROLL_PANE_HEIGHT);
        return scrollPane;
    }

    /**
     * Creates the title label with consistent styling.
     */
    private Label createTitleLabel() {
        Label title = new Label(TITLE_TEXT);
        title.setFont(TITLE_FONT);
        title.setTextFill(TEXT_COLOR);
        return title;
    }

    /**
     * Creates the craft button with consistent styling and behavior.
     */
    private Button createCraftButton() {
        Button button = new Button(CRAFT_BUTTON_TEXT);
        button.setPrefWidth(Double.MAX_VALUE);
        button.setFont(BUTTON_FONT);
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
        Label emptyLabel = new Label(EMPTY_LIST_TEXT);
        emptyLabel.setTextFill(EMPTY_TEXT_COLOR);
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
        private static final Border SELECTED_BORDER = new Border(new BorderStroke(
                CELL_SELECTED_BORDER, BorderStrokeStyle.SOLID,
                new CornerRadii(CELL_CORNER_RADIUS), new BorderWidths(1))
        );
        private static final Border DEFAULT_BORDER = new Border(new BorderStroke(
                Color.TRANSPARENT, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderWidths.DEFAULT)
        );
        private static final Background SELECTED_BACKGROUND = new Background(
                new BackgroundFill(CELL_BG_COLOR, new CornerRadii(CELL_CORNER_RADIUS), Insets.EMPTY)
        );
        private static final Background DEFAULT_BACKGROUND = new Background(
                new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)
        );
        private static final Background HOVER_BACKGROUND = new Background(
                new BackgroundFill(CELL_HOVER_COLOR, new CornerRadii(CELL_CORNER_RADIUS), Insets.EMPTY)
        );

        private final Recipe recipe;
        private boolean selected;

        /**
         * Creates a new craftable item cell.
         *
         * @param recipe Recipe to represent
         */
        public CraftableItemCell(Recipe recipe) {
            this.recipe = Objects.requireNonNull(recipe, "Recipe cannot be null");
            this.selected = false;

            configureCellLayout();
            initializeCellComponents();
            setupEventHandlers();
        }

        /**
         * Configures the cell's layout properties.
         */
        private void configureCellLayout() {
            setSpacing(LIST_SPACING);
            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(CELL_PADDING));
            setBackground(DEFAULT_BACKGROUND);
            setBorder(DEFAULT_BORDER);
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
            return imageView;
        }

        /**
         * Creates the recipe name label.
         */
        private Label createNameLabel() {
            Label label = new Label(recipe.getName());
            label.setFont(NAME_FONT);
            label.setTextFill(TEXT_COLOR);
            label.setPickOnBounds(true);
            return label;
        }

        /**
         * Sets up mouse event handlers.
         */
        private void setupEventHandlers() {
            setOnMouseEntered(e -> handleMouseEnter());
            setOnMouseExited(e -> handleMouseExit());
            setOnMouseClicked(e -> handleMouseClick());
        }

        /**
         * Handles mouse enter event.
         */
        private void handleMouseEnter() {
            if (!selected) {
                setBackground(HOVER_BACKGROUND);
            }
        }

        /**
         * Handles mouse exit event.
         */
        private void handleMouseExit() {
            if (!selected) {
                setBackground(DEFAULT_BACKGROUND);
            }
        }

        /**
         * Handles mouse click event.
         */
        private void handleMouseClick() {
            if (!selected) {
                setSelected(true);
                CraftingListPanel.this.setSelectedRecipe(recipe);
                if (onRecipeSelected != null) {
                    onRecipeSelected.accept(recipe);
                }
            }
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
            this.selected = selected;
            if (selected) {
                setBackground(SELECTED_BACKGROUND);
                setBorder(SELECTED_BORDER);
            } else {
                setBackground(DEFAULT_BACKGROUND);
                setBorder(DEFAULT_BORDER);
            }
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