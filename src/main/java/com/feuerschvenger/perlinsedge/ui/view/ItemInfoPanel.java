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
 * Panel for displaying detailed information about a selected item or recipe.
 */
public class ItemInfoPanel extends VBox {
    private static final Color PANEL_BG_COLOR = Color.rgb(0, 0, 0, 0.7);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final int PANEL_PADDING = 15;
    private static final int SPACING = 5;

    private final Text titleText;
    private final Text descriptionText;

    public ItemInfoPanel() {
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(PANEL_PADDING));
        setSpacing(SPACING);
        setBackground(new Background(new BackgroundFill(PANEL_BG_COLOR, new CornerRadii(10), Insets.EMPTY)));
        setPrefWidth(200);
        setMinWidth(150);
        setMaxWidth(250);

        titleText = new Text("Select an Item");
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleText.setFill(TEXT_COLOR);
        titleText.setWrappingWidth(getPrefWidth() - PANEL_PADDING * 2);

        descriptionText = new Text("");
        descriptionText.setFont(Font.font("Arial", 14));
        descriptionText.setFill(TEXT_COLOR.darker());
        descriptionText.setWrappingWidth(getPrefWidth() - PANEL_PADDING * 2);

        getChildren().addAll(titleText, descriptionText);
    }

    /**
     * Displays information for a regular inventory item.
     * @param item Item to display.
     */
    public void displayItem(Item item) {
        if (item == null) {
            clearInfo();
            return;
        }
        // System.out.println("DISPLAY ITEM: " + item.getDisplayName());
        titleText.setText(item.getDisplayName() + " (" + item.getQuantity() + ")");
        descriptionText.setText(item.getDescription());
    }

    /**
     * Displays information for a crafting recipe.
     * @param recipe Recipe to display.
     */
    public void displayRecipe(Recipe recipe) {
        if (recipe == null) {
            clearInfo();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Crafts: ").append(recipe.getOutputQuantity()).append("x ")
                .append(recipe.getOutputItemType().getDisplayName()).append("\n\n");

        sb.append("Ingredients:\n");
        for (CraftingIngredient ingredient : recipe.getIngredients()) {
            sb.append("- ").append(ingredient.getQuantity()).append("x ")
                    .append(ingredient.getItemType().getDisplayName()).append("\n");
        }

        titleText.setText(recipe.getName());
        descriptionText.setText(sb.toString());
    }

    /**
     * Clears displayed information.
     */
    public void clearInfo() {
        titleText.setText("Select an Item");
        descriptionText.setText("");
    }

}