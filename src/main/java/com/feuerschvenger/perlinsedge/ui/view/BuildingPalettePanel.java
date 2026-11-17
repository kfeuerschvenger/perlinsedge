package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * The palette of building pieces, shown above the item hotbar while the hammer is in hand. Every piece has a card with
 * its icon and its name, greyed the way the crafting list greys a recipe when the player cannot afford the piece; what
 * the selected piece costs is shown by {@link BuildingCostPanel}. A piece is chosen by clicking its card and stays
 * chosen until another is. The click is consumed here, so it never reaches the world. The panel keeps no selection of its own: the selected
 * piece is given to {@link #update(Player, BuildingType)}, which redraws only what changed.
 */
public class BuildingPalettePanel extends HBox {
    /** Width of a card in whole pixels; the UI layer never scales with the world zoom. */
    static final double CARD_WIDTH = 88;
    /** Edge of the icon of a card. */
    static final int ICON_SIZE = 40;
    private static final double CARD_PADDING = 4;
    private static final double CARD_GAP = 4;
    /** Height of the name of a card: two lines of the small font, which is 12 px. */
    static final double NAME_HEIGHT = 32;
    /** What the selected card of a piece that can turn tells about the rotation key. */
    static final String ROTATE_HINT = "R rotate";

    private final Map<BuildingType, Card> cards = new EnumMap<>(BuildingType.class);
    private Consumer<BuildingType> onBuildingSelected;
    private BuildingCostPanel costPanel;
    private Player shownPlayer;
    private long shownInventoryVersion = -1;
    private BuildingType shownSelected;
    private boolean selectionShown;

    /** One piece of the palette: its card and what the piece costs, which the card uses only to tell if it is affordable. */
    private static final class Card {
        final VBox node = new VBox(2);
        final List<CraftingIngredient> cost = new ArrayList<>();
        ImageView icon;
        /** The name of the piece, wrapped to at most two lines. */
        Label name;
        /** True when the player holds everything the piece costs, null until the first inventory read. */
        Boolean affordable;
        /** The line that tells the rotation key, shown only on the selected card of a piece that can turn. */
        Label rotateHint;
        int shownRotation = -1;
    }

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

        // The name wraps to a second line instead of cutting off, and takes the height of two lines whether it needs
        // them or not, so the cards stay aligned
        final Label name = new Label(UiTheme.caps(type.getDisplayName()));
        name.getStyleClass().add(UiTheme.SMALL);
        name.setMouseTransparent(true);
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setAlignment(Pos.TOP_CENTER);
        name.setPrefWidth(CARD_WIDTH - 2 * CARD_PADDING);
        name.setMinHeight(NAME_HEIGHT);
        name.setPrefHeight(NAME_HEIGHT);
        name.setMaxHeight(NAME_HEIGHT);
        card.name = name;
        card.icon = BuildingIcons.view(type, 0, ICON_SIZE);
        card.node.getChildren().addAll(card.icon, name);
        if (type.isRotatable()) {
            card.rotateHint = new Label(UiTheme.caps(ROTATE_HINT));
            card.rotateHint.getStyleClass().addAll(UiTheme.SMALL, UiTheme.MUTED);
            card.rotateHint.setMouseTransparent(true);
            card.rotateHint.setVisible(false);
            card.rotateHint.setManaged(false);
            card.node.getChildren().add(card.rotateHint);
        }

        card.cost.addAll(recipe.ingredients());

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

    /**
     * Sets the panel that tells what the selected piece will consume. The palette drives it: it is shown and hidden with
     * the palette and redrawn on every {@code update}.
     *
     * @param costPanel the panel, or null for none
     */
    public void setCostPanel(BuildingCostPanel costPanel) {
        this.costPanel = costPanel;
        if (costPanel != null) {
            costPanel.setEnabled(isVisible());
        }
    }

    /** Shows the palette. */
    public void showPalette() {
        // The next update redraws the costs against the inventory of the moment
        shownInventoryVersion = -1;
        setVisible(true);
        setManaged(true);
        if (costPanel != null) {
            costPanel.setEnabled(true);
        }
    }

    /** Hides the palette. */
    public void hidePalette() {
        setVisible(false);
        setManaged(false);
        if (costPanel != null) {
            costPanel.setEnabled(false);
        }
    }

    /**
     * Redraws the costs and the selection border when the inventory or the selection changed since the last call, so it
     * is cheap enough to call on every frame.
     *
     * @param player   the player whose inventory the costs are read against, null shows every ingredient as missing
     * @param selected the piece that is selected, or null for none
     */
    public void update(Player player, BuildingType selected) {
        update(player, selected, type -> 0);
    }

    /**
     * Redraws the costs, the selection border and the icons when the inventory, the selection or a rotation changed since
     * the last call. The icon of every card shows the way its piece faces now, and the selected card of a piece that can
     * turn tells the rotation key.
     *
     * @param player     the player whose inventory the costs are read against, null shows every ingredient as missing
     * @param selected   the piece that is selected, or null for none
     * @param rotationOf gives the way the next piece of a type faces
     */
    public void update(Player player, BuildingType selected, ToIntFunction<BuildingType> rotationOf) {
        for (Map.Entry<BuildingType, Card> entry : cards.entrySet()) {
            final Card card = entry.getValue();
            final int rotation = rotationOf.applyAsInt(entry.getKey());
            if (rotation != card.shownRotation) {
                card.shownRotation = rotation;
                BuildingIcons.show(card.icon, entry.getKey(), rotation);
            }
        }
        final long version = player != null ? player.getInventoryVersion() : -1;
        if (player != shownPlayer || version != shownInventoryVersion) {
            shownPlayer = player;
            shownInventoryVersion = version;
            for (Card card : cards.values()) {
                boolean affordable = true;
                for (CraftingIngredient ingredient : card.cost) {
                    affordable &= BuildingCostPanel.held(player, ingredient) >= ingredient.quantity();
                }
                showAffordable(card, affordable);
            }
        }
        showSelection(selected);
        if (costPanel != null) {
            costPanel.update(player, selected);
        }
    }

    /** Greys the card of a piece the player cannot afford, the way the crafting list greys a recipe it cannot craft. */
    private static void showAffordable(Card card, boolean affordable) {
        if (card.affordable != null && card.affordable == affordable) {
            return;
        }
        card.affordable = affordable;
        UiTheme.setClass(card.node, UiTheme.CELL_UNAVAILABLE, !affordable);
        UiTheme.setIconUnavailable(card.icon, !affordable);
    }

    private void showSelection(BuildingType selected) {
        if (selectionShown && selected == shownSelected) {
            return;
        }
        selectionShown = true;
        shownSelected = selected;
        cards.forEach((type, card) -> {
            UiTheme.setClass(card.node, UiTheme.SLOT_SELECTED, type == selected);
            if (card.rotateHint != null) {
                final boolean shown = type == selected;
                card.rotateHint.setVisible(shown);
                card.rotateHint.setManaged(shown);
            }
        });
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
     * Tells whether the card of a piece shows the line about the rotation key, for the tests.
     *
     * @param type the piece
     * @return true when the line is visible, false when it is hidden or the piece cannot turn
     */
    boolean isRotateHintShown(BuildingType type) {
        final Label hint = cards.get(type).rotateHint;
        return hint != null && hint.isVisible();
    }

    /**
     * Tells whether the card of a piece is shown as affordable, for the tests.
     *
     * @param type the piece
     * @return false when the card is greyed, true otherwise
     */
    boolean isAffordable(BuildingType type) {
        final Boolean affordable = cards.get(type).affordable;
        return affordable == null || affordable;
    }

    /**
     * Gives the name label of the card of a piece, for the tests.
     *
     * @param type the piece
     * @return the label that holds the wrapped name
     */
    Label nameOf(BuildingType type) {
        return cards.get(type).name;
    }

    /**
     * Gives the icon of the card of a piece, for the tests.
     *
     * @param type the piece
     * @return the icon node
     */
    ImageView iconOf(BuildingType type) {
        return cards.get(type).icon;
    }
}
