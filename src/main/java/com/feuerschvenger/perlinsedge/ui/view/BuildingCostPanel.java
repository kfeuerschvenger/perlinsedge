package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The small panel at the bottom left of the screen that tells what placing the selected piece will consume. It shows a
 * title and one line per ingredient, the quantity the piece takes, and the lines the player lacks are marked in red the
 * way the crafting list marks them. It is visible only while the palette is shown and a piece is selected, and it
 * redraws only when the selection or the inventory changed, so it is cheap enough to call on every frame.
 */
public final class BuildingCostPanel extends VBox {
    /** The title of the panel. */
    private static final String TITLE_TEXT = "Will consume:";
    private static final double LINE_SPACING = 2;

    private final RecipeManager recipeManager;
    private final Label title = new Label(UiTheme.caps(TITLE_TEXT));
    private final List<Label> lines = new ArrayList<>();
    private boolean enabled;
    private boolean shown;
    private Player shownPlayer;
    private long shownInventoryVersion = -1;
    private BuildingType shownSelected;
    private boolean contentShown;

    /**
     * Creates the panel, hidden until {@link #setEnabled(boolean)} is called with true and a piece is selected.
     *
     * @param recipeManager the building recipes the costs are read from
     */
    public BuildingCostPanel(RecipeManager recipeManager) {
        this.recipeManager = Objects.requireNonNull(recipeManager, "recipeManager");
        setAlignment(Pos.TOP_LEFT);
        setSpacing(LINE_SPACING);
        getStyleClass().add(UiTheme.PANEL);
        title.getStyleClass().add(UiTheme.PANEL_TITLE);
        getChildren().add(title);
        // Nothing here takes a click, so it never swallows one meant for the world
        setMouseTransparent(true);
        applyVisibility(false);
    }

    /**
     * Tells the panel whether build mode is active. The panel is visible only while it is and a piece is selected.
     *
     * @param enabled true while the palette is shown
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            // The next update redraws the lines against the inventory of the moment
            contentShown = false;
        }
        applyVisibility(enabled && shownSelected != null);
    }

    /**
     * Redraws the lines when the selection or the inventory changed since the last call.
     *
     * @param player   the player whose inventory the lines are read against, null shows every ingredient as missing
     * @param selected the piece that is selected, or null for none
     */
    public void update(Player player, BuildingType selected) {
        final long version = player != null ? player.getInventoryVersion() : -1;
        if (!contentShown || selected != shownSelected || player != shownPlayer || version != shownInventoryVersion) {
            contentShown = true;
            shownSelected = selected;
            shownPlayer = player;
            shownInventoryVersion = version;
            redraw(player, selected);
        }
        applyVisibility(enabled && selected != null);
    }

    private void redraw(Player player, BuildingType selected) {
        final List<CraftingIngredient> ingredients = selected == null ? List.of()
                : recipeManager.getBuildingRecipeByType(selected).map(BuildingRecipe::ingredients).orElse(List.of());
        while (lines.size() < ingredients.size()) {
            final var label = new Label();
            label.getStyleClass().add(UiTheme.SMALL);
            lines.add(label);
        }
        getChildren().setAll(title);
        for (var i = 0; i < ingredients.size(); i++) {
            final CraftingIngredient ingredient = ingredients.get(i);
            final int held = held(player, ingredient);
            final boolean enough = held >= ingredient.quantity();
            final var label = lines.get(i);
            label.setText(UiTheme.caps(ingredient.itemType().getDisplayName()) + " x" + ingredient.quantity()
                    + (enough ? "" : " (HAVE " + held + ")"));
            UiTheme.setClass(label, UiTheme.TEXT_GOOD, enough);
            UiTheme.setClass(label, UiTheme.TEXT_BAD, !enough);
            getChildren().add(label);
        }
    }

    private void applyVisibility(boolean visible) {
        if (shown != visible || isVisible() != visible) {
            shown = visible;
            setVisible(visible);
            setManaged(visible);
        }
    }

    /**
     * Counts how many of an ingredient the player holds.
     *
     * @param player     the player, may be null
     * @param ingredient the ingredient
     * @return the held quantity, zero for no player
     */
    static int held(Player player, CraftingIngredient ingredient) {
        return player != null ? player.getItemCount(new Item(ingredient.itemType(), 1)) : 0;
    }

    /**
     * Gives the lines of the panel as they are now shown, for the tests.
     *
     * @return one label per ingredient of the selected piece, in recipe order
     */
    List<Label> lineLabels() {
        return getChildren().stream().filter(node -> node != title).map(Label.class::cast).toList();
    }

    /**
     * Gives the title label, for the tests.
     *
     * @return the title
     */
    Label titleLabel() {
        return title;
    }
}
