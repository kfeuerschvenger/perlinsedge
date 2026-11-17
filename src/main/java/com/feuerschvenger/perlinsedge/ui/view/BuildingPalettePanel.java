package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The palette of building pieces, shown above the item hotbar while the hammer is in hand. Every piece has a card with
 * its icon, its name and its cost, each ingredient as {@code held/needed} in green when the player has enough and in red
 * when not, the way the crafting list shows it. A piece is chosen by clicking its card and stays chosen until another
 * is. The click is consumed here, so it never reaches the world. The panel keeps no selection of its own: the selected
 * piece is given to {@link #update(Player, BuildingType)}, which redraws only what changed.
 */
public class BuildingPalettePanel extends HBox {
    /** Width of a card in whole pixels; the UI layer never scales with the world zoom. */
    static final double CARD_WIDTH = 88;
    /** Edge of the icon of a card. */
    static final int ICON_SIZE = 40;
    private static final double CARD_PADDING = 4;
    private static final double CARD_GAP = 4;

    private final Map<BuildingType, Card> cards = new EnumMap<>(BuildingType.class);
    private Consumer<BuildingType> onBuildingSelected;
    private Player shownPlayer;
    private long shownInventoryVersion = -1;
    private BuildingType shownSelected;
    private boolean selectionShown;

    /** One piece of the palette: its card and the lines of its cost. */
    private static final class Card {
        final VBox node = new VBox(2);
        final List<IngredientLine> lines = new ArrayList<>();
    }

    /** One ingredient of a cost: what the recipe needs and the label that shows it. */
    private record IngredientLine(CraftingIngredient ingredient, Label label) { }

    /**
     * Creates the palette with a card for every building recipe, hidden until {@link #showPalette()}.
     *
     * @param recipeManager the building recipes, in the order the cards are shown
     */
    public BuildingPalettePanel(RecipeManager recipeManager) {
        // No panel behind the row: the cards float over the world, each one dark and translucent on its own
        setAlignment(Pos.BOTTOM_CENTER);
        setSpacing(CARD_GAP);
        for (BuildingRecipe recipe : recipeManager.getAllBuildingRecipes()) {
            final Card card = createCard(recipe);
            cards.put(recipe.buildingType(), card);
            getChildren().add(card.node);
        }
        setVisible(false);
        setManaged(false);
    }

    private Card createCard(BuildingRecipe recipe) {
        final BuildingType type = recipe.buildingType();
        final Card card = new Card();
        card.node.getStyleClass().add(UiTheme.SLOT);
        card.node.setAlignment(Pos.TOP_CENTER);
        card.node.setPadding(new Insets(CARD_PADDING));
        card.node.setPrefWidth(CARD_WIDTH);
        card.node.setMinWidth(CARD_WIDTH);
        card.node.setMaxWidth(CARD_WIDTH);

        final Label name = new Label(UiTheme.caps(type.getDisplayName()));
        name.getStyleClass().add(UiTheme.SMALL);
        name.setMouseTransparent(true);
        card.node.getChildren().addAll(BuildingIcons.view(type, ICON_SIZE), name);

        for (CraftingIngredient ingredient : recipe.ingredients()) {
            final Label label = new Label();
            label.getStyleClass().add(UiTheme.SMALL);
            label.setMouseTransparent(true);
            card.lines.add(new IngredientLine(ingredient, label));
            card.node.getChildren().add(label);
        }

        Tooltip.install(card.node, new Tooltip(type.getDescription()));
        card.node.setOnMousePressed(event -> {
            choose(type);
            event.consume();
        });
        card.node.setOnMouseClicked(event -> event.consume());
        return card;
    }

    private void choose(BuildingType type) {
        if (onBuildingSelected != null) {
            onBuildingSelected.accept(type);
        }
        // The border follows the click at once instead of waiting for the next frame
        showSelection(type);
    }

    /**
     * Sets the listener that learns which piece was clicked.
     *
     * @param listener receives the chosen piece
     */
    public void setOnBuildingSelected(Consumer<BuildingType> listener) {
        this.onBuildingSelected = listener;
    }

    /** Shows the palette. */
    public void showPalette() {
        // The next update redraws the costs against the inventory of the moment
        shownInventoryVersion = -1;
        setVisible(true);
        setManaged(true);
    }

    /** Hides the palette. */
    public void hidePalette() {
        setVisible(false);
        setManaged(false);
    }

    /**
     * Redraws the costs and the selection border when the inventory or the selection changed since the last call, so it
     * is cheap enough to call on every frame.
     *
     * @param player   the player whose inventory the costs are read against, null shows every ingredient as missing
     * @param selected the piece that is selected, or null for none
     */
    public void update(Player player, BuildingType selected) {
        final long version = player != null ? player.getInventoryVersion() : -1;
        if (player != shownPlayer || version != shownInventoryVersion) {
            shownPlayer = player;
            shownInventoryVersion = version;
            for (Card card : cards.values()) {
                for (IngredientLine line : card.lines) {
                    showIngredient(line, player);
                }
            }
        }
        showSelection(selected);
    }

    private static void showIngredient(IngredientLine line, Player player) {
        final CraftingIngredient ingredient = line.ingredient();
        final int held = player != null ? player.getItemCount(new Item(ingredient.itemType(), 1)) : 0;
        line.label().setText(UiTheme.caps(ingredient.itemType().getDisplayName()) + " " + held + "/" + ingredient.quantity());
        final boolean enough = held >= ingredient.quantity();
        UiTheme.setClass(line.label(), UiTheme.TEXT_GOOD, enough);
        UiTheme.setClass(line.label(), UiTheme.TEXT_BAD, !enough);
    }

    private void showSelection(BuildingType selected) {
        if (selectionShown && selected == shownSelected) {
            return;
        }
        selectionShown = true;
        shownSelected = selected;
        cards.forEach((type, card) -> UiTheme.setClass(card.node, UiTheme.SLOT_SELECTED, type == selected));
    }

    /**
     * Tells which card carries the selection border.
     *
     * @return the piece, or null when none does
     */
    BuildingType getShownSelected() {
        return shownSelected;
    }

    /**
     * Gives the node of the card of a piece, for the tests.
     *
     * @param type the piece
     * @return the card node
     */
    VBox cardNode(BuildingType type) {
        return cards.get(type).node;
    }

    /**
     * Gives the labels of the cost of a piece as they are now shown, for the tests.
     *
     * @param type the piece
     * @return one label per ingredient, in recipe order
     */
    List<Label> costLabels(BuildingType type) {
        return cards.get(type).lines.stream().map(IngredientLine::label).toList();
    }
}
