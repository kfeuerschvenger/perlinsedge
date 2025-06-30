package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Displays detailed information about selected inventory items or crafting recipes.
 * Provides a consistent UI panel for showing name, description, quantities, and ingredients.
 */
public class ItemInfoPanel extends VBox {
    // UI styling constants
    private static final Color PANEL_BG_COLOR = Color.rgb(0, 0, 0, 0.7);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SECONDARY_TEXT_COLOR = TEXT_COLOR.darker();
    private static final int PANEL_PADDING = 15;
    private static final int CONTENT_SPACING = 5;
    private static final int PANEL_CORNER_RADIUS = 10;
    private static final double PREF_WIDTH = 200;
    private static final double MIN_WIDTH = 150;
    private static final double MAX_WIDTH = 250;

    // Text styling constants
    private static final String FONT_FAMILY = "Arial";
    private static final Font TITLE_FONT = Font.font(FONT_FAMILY, FontWeight.BOLD, 18);
    private static final Font DESCRIPTION_FONT = Font.font(FONT_FAMILY, 14);
    private static final String DEFAULT_TITLE = "Select an Item";

    // UI components
    private Text titleText;
    private Text descriptionText;

    /**
     * Initializes the information panel with default configuration.
     */
    public ItemInfoPanel() {
        configureLayout();
        initializeComponents();
        getChildren().addAll(titleText, descriptionText);
    }

    /**
     * Configures the panel's layout and styling properties.
     */
    private void configureLayout() {
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(PANEL_PADDING));
        setSpacing(CONTENT_SPACING);
        setBackground(new Background(new BackgroundFill(
                PANEL_BG_COLOR, new CornerRadii(PANEL_CORNER_RADIUS), Insets.EMPTY
        )));
        setPrefWidth(PREF_WIDTH);
        setMinWidth(MIN_WIDTH);
        setMaxWidth(MAX_WIDTH);
    }

    /**
     * Initializes and configures UI components.
     */
    private void initializeComponents() {
        titleText = createStyledText(TITLE_FONT, TEXT_COLOR, DEFAULT_TITLE);
        descriptionText = createStyledText(DESCRIPTION_FONT, SECONDARY_TEXT_COLOR, "");

        // Set consistent wrapping width based on panel dimensions
        double wrappingWidth = PREF_WIDTH - PANEL_PADDING * 2;
        titleText.setWrappingWidth(wrappingWidth);
        descriptionText.setWrappingWidth(wrappingWidth);
    }

    /**
     * Creates a styled text component with consistent configuration.
     *
     * @param font Text font
     * @param color Text color
     * @param content Initial text content
     * @return Configured Text component
     */
    private Text createStyledText(Font font, Color color, String content) {
        Text text = new Text(content);
        text.setFont(font);
        text.setFill(color);
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

        titleText.setText(formatItemTitle(item));
        descriptionText.setText(item.getDescription());
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
     * Displays information for a crafting recipe.
     *
     * @param recipe Recipe to display (ignored if null)
     */
    public void displayRecipe(Recipe recipe) {
        if (recipe == null) {
            clearInfo();
            return;
        }

        titleText.setText(recipe.getName());
        descriptionText.setText(formatRecipeDetails(recipe));
    }

    /**
     * Formats detailed recipe information including description, output, and ingredients.
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
                .append("\n\n");

        // Ingredients list
        details.append("Ingredients:\n");
        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            details.append("- ")
                    .append(ingredient.quantity())
                    .append("x ")
                    .append(ingredient.itemType().getDisplayName())
                    .append("\n");
        }

        return details.toString();
    }

    /**
     * Clears all displayed information and resets to default state.
     */
    public void clearInfo() {
        titleText.setText(DEFAULT_TITLE);
        descriptionText.setText("");
    }

}