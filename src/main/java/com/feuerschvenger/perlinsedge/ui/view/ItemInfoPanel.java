package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Displays detailed information about selected inventory items or crafting recipes.
 * Provides a consistent UI panel for showing name, description, quantities, and ingredients.
 */
public class ItemInfoPanel extends VBox {
    // Layout constants; the colours, fonts and background come from the theme
    private static final int PANEL_PADDING = 16;
    private static final int CONTENT_SPACING = 6;
    private static final double PREF_WIDTH = 200;
    private static final double MIN_WIDTH = 150;
    private static final double MAX_WIDTH = 250;
    /** Width of the text column: the panel without its padding. */
    private static final double CONTENT_WIDTH = PREF_WIDTH - PANEL_PADDING * 2;
    /** Room kept free at the right of the description for the scroll bar and a small gap. */
    private static final double SCROLL_BAR_ROOM = 12;
    /** Height of the title area, two lines of the heading font, so a long title never moves what is below it. */
    private static final double TITLE_AREA_HEIGHT = 64;
    /** Height of the area that holds the description and the ingredients; longer text scrolls inside it. */
    private static final double DETAILS_AREA_HEIGHT = 160;

    private static final String DEFAULT_TITLE = "Select an Item";

    // UI components
    private Text titleText;
    private Text descriptionText;
    private ScrollPane detailsScroll;
    /** The ingredient lines of a recipe, one text each so every line has its own colour. */
    private final VBox ingredientsBox = new VBox();

    /**
     * Initializes the information panel with default configuration.
     */
    public ItemInfoPanel() {
        configureLayout();
        initializeComponents();
        getChildren().addAll(createTitleArea(), detailsScroll);
    }

    /**
     * Configures the panel's layout and styling properties.
     */
    private void configureLayout() {
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add(UiTheme.PANEL);
        setSpacing(CONTENT_SPACING);
        setPrefWidth(PREF_WIDTH);
        setMinWidth(MIN_WIDTH);
        setMaxWidth(MAX_WIDTH);
    }

    /**
     * Initializes and configures UI components.
     */
    private void initializeComponents() {
        titleText = createStyledText(UiTheme.HEADING, UiTheme.caps(DEFAULT_TITLE));
        descriptionText = createStyledText(UiTheme.MUTED, "");

        // Set consistent wrapping width based on panel dimensions
        titleText.setWrappingWidth(CONTENT_WIDTH);
        descriptionText.setWrappingWidth(CONTENT_WIDTH - SCROLL_BAR_ROOM);

        // The description and the ingredients scroll inside an area of one fixed size, so neither the panel nor the
        // window around it depends on how much there is to read
        final VBox details = new VBox(CONTENT_SPACING, descriptionText, ingredientsBox);
        detailsScroll = new ScrollPane(details);
        detailsScroll.setFitToWidth(true);
        detailsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailsScroll.setMinSize(CONTENT_WIDTH, DETAILS_AREA_HEIGHT);
        detailsScroll.setPrefSize(CONTENT_WIDTH, DETAILS_AREA_HEIGHT);
        detailsScroll.setMaxSize(CONTENT_WIDTH, DETAILS_AREA_HEIGHT);
    }

    /** The title in an area of fixed height, which keeps the title from moving what is below it. */
    private StackPane createTitleArea() {
        final StackPane area = new StackPane(titleText);
        StackPane.setAlignment(titleText, Pos.TOP_LEFT);
        area.setMinHeight(TITLE_AREA_HEIGHT);
        area.setPrefHeight(TITLE_AREA_HEIGHT);
        area.setMaxHeight(TITLE_AREA_HEIGHT);
        return area;
    }

    /**
     * Creates a styled text component with consistent configuration.
     *
     * @param styleClass Theme style class of the text
     * @param content Initial text content
     * @return Configured Text component
     */
    private Text createStyledText(String styleClass, String content) {
        Text text = new Text(content);
        text.getStyleClass().add(styleClass);
        return text;
    }

    /**
     * Displays information for an inventory item.
     *
     * @param item Item to display (ignored if null)
     */
    public void displayItem(Item item) {
        if (item == null) {
            clearInfo();
            return;
        }

        show(formatItemTitle(item), item.getDescription());
        ingredientsBox.getChildren().clear();
    }

    /**
     * Shows a title and a description and scrolls the description back to its start. The description may be of any
     * length: the panel keeps its size and the text scrolls.
     */
    void show(String title, String description) {
        titleText.setText(UiTheme.caps(title));
        descriptionText.setText(description);
        detailsScroll.setVvalue(0);
    }

    /** Gives the area that holds the description, for the tests. */
    ScrollPane detailsArea() {
        return detailsScroll;
    }

    /**
     * Formats the title text for inventory items.
     *
     * @param item Item to format
     * @return Formatted title string
     */
    private String formatItemTitle(Item item) {
        return String.format("%s (%d)", item.getDisplayName(), item.getQuantity());
    }

    /**
     * Displays information for a crafting recipe. Each ingredient shows how many the player holds against how many the
     * recipe needs, green when there are enough and red when there are not.
     *
     * @param recipe Recipe to display (ignored if null)
     * @param have   gives how many items of a type the player holds, in the hotbar and the inventory together
     */
    public void displayRecipe(Recipe recipe, ToIntFunction<ItemType> have) {
        if (recipe == null) {
            clearInfo();
            return;
        }

        show(recipe.getName(), formatRecipeDetails(recipe));
        ingredientsBox.getChildren().setAll(createIngredientLines(recipe, have));
    }

    /**
     * Formats the description and the output of a recipe.
     *
     * @param recipe Recipe to format
     * @return Formatted recipe details
     */
    private String formatRecipeDetails(Recipe recipe) {
        StringBuilder details = new StringBuilder();

        // Recipe description
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            details.append(recipe.getDescription()).append("\n\n");
        }

        // Output information
        details.append("Crafts: ")
                .append(recipe.getOutputQuantity())
                .append("x ")
                .append(recipe.getOutputItemType().getDisplayName())
                .append("\n\nIngredients:");

        return details.toString();
    }

    /** One line per ingredient, such as "Planks 2/4", coloured by whether the player has enough. */
    private List<Text> createIngredientLines(Recipe recipe, ToIntFunction<ItemType> have) {
        List<Text> lines = new ArrayList<>();
        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            lines.add(createIngredientLine(ingredient, have.applyAsInt(ingredient.itemType())));
        }
        return lines;
    }

    /**
     * Builds the line of one ingredient.
     *
     * @param ingredient the ingredient
     * @param held       how many the player holds
     * @return the line, green when the player holds enough and red when not
     */
    Text createIngredientLine(CraftingIngredient ingredient, int held) {
        Text line = createStyledText(held >= ingredient.quantity() ? UiTheme.TEXT_GOOD : UiTheme.TEXT_BAD,
                ingredient.itemType().getDisplayName() + " " + held + "/" + ingredient.quantity());
        line.setWrappingWidth(CONTENT_WIDTH - SCROLL_BAR_ROOM);
        return line;
    }

    /** Gives the ingredient lines now shown, for the tests. */
    List<Text> shownIngredientLines() {
        return ingredientsBox.getChildren().stream().map(Text.class::cast).toList();
    }

    /**
     * Clears all displayed information and resets to default state.
     */
    public void clearInfo() {
        show(DEFAULT_TITLE, "");
        ingredientsBox.getChildren().clear();
    }

}