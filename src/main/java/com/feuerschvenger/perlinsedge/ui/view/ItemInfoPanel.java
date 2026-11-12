package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.geometry.Pos;
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

    private static final String DEFAULT_TITLE = "Select an Item";

    // UI components
    private Text titleText;
    private Text descriptionText;
    /** The ingredient lines of a recipe, one text each so every line has its own colour. */
    private final VBox ingredientsBox = new VBox();

    /**
     * Initializes the information panel with default configuration.
     */
    public ItemInfoPanel() {
        configureLayout();
        initializeComponents();
        getChildren().addAll(titleText, descriptionText, ingredientsBox);
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
        double wrappingWidth = PREF_WIDTH - PANEL_PADDING * 2;
        titleText.setWrappingWidth(wrappingWidth);
        descriptionText.setWrappingWidth(wrappingWidth);
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

        titleText.setText(UiTheme.caps(formatItemTitle(item)));
        descriptionText.setText(item.getDescription());
        ingredientsBox.getChildren().clear();
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

        titleText.setText(UiTheme.caps(recipe.getName()));
        descriptionText.setText(formatRecipeDetails(recipe));
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
        line.setWrappingWidth(PREF_WIDTH - PANEL_PADDING * 2);
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
        titleText.setText(UiTheme.caps(DEFAULT_TITLE));
        descriptionText.setText("");
        ingredientsBox.getChildren().clear();
    }

}