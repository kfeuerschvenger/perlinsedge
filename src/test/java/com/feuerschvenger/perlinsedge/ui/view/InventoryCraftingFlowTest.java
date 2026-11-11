package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.InventoryManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crafting from the inventory panel end to end: select a recipe in the list, press the craft button, and the
 * ingredients are consumed and the result added, or a hint says why not.
 */
class InventoryCraftingFlowTest {

    private Player player;
    private InventoryPanel panel;
    private RecipeManager recipes;
    private List<String> hints;

    @BeforeEach
    void setUp() {
        GameStateManager state = new GameStateManager();
        player = new Player(1, 1);
        state.setPlayer(player);
        recipes = new RecipeManager();
        hints = new ArrayList<>();
        panel = FxToolkitSupport.onFx(() -> {
            InventoryPanel created = new InventoryPanel(state);
            Pane root = new Pane(created);
            new Scene(root, 1000, 600);
            created.setCraftingRecipes(recipes.getAllItemRecipes());
            created.openPlayerInventory();
            root.applyCss();
            root.layout();
            return created;
        });
        InventoryManager crafting = new InventoryManager(state, panel::updateInventoryDisplay, hints::add);
        panel.setOnCraftRecipe(crafting::handleCraftRecipe);
    }

    private void selectRecipe(ItemType output) {
        int index = 0;
        for (Recipe recipe : recipes.getAllItemRecipes()) {
            if (recipe.getOutputItemType() == output) {
                break;
            }
            index++;
        }
        int position = index;
        FxToolkitSupport.onFx(() -> {
            Region cell = (Region) panel.lookupAll("." + UiTheme.CELL).stream().toList().get(position);
            Event.fireEvent(cell, new MouseEvent(MouseEvent.MOUSE_CLICKED, 5, 5, 5, 5, MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, true, false, false, null));
            return null;
        });
    }

    /** Finds the craft button, which is the only button of the panel. Call it on the JavaFX thread. */
    private Button craftButton() {
        return panel.lookupAll(".button").stream()
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private void pressCraft() {
        FxToolkitSupport.onFx(() -> {
            craftButton().fire();
            return null;
        });
    }

    private int count(ItemType type) {
        return player.getItemCount(new Item(type, 1));
    }

    @Test
    void theCraftButtonStartsDisabledUntilARecipeIsSelected() {
        assertTrue(FxToolkitSupport.onFx(() -> craftButton().isDisabled()));

        selectRecipe(ItemType.WOOD_PLANK);

        assertFalse(FxToolkitSupport.onFx(() -> craftButton().isDisabled()));
    }

    @Test
    void selectingARecipeAndPressingCraftConsumesTheIngredientsAndAddsTheResult() {
        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        selectRecipe(ItemType.WOOD_PLANK);

        pressCraft();

        assertEquals(1, count(ItemType.WOOD_LOG));
        assertEquals(4, count(ItemType.WOOD_PLANK));
        assertTrue(hints.isEmpty());
    }

    @Test
    void theInventoryGridShowsTheCraftedResultAtOnce() {
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        selectRecipe(ItemType.WOOD_PLANK);

        pressCraft();

        long itemImages = FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .filter(slot -> !((StackPane) slot).getChildren().isEmpty())
                .count());
        assertEquals(1, itemImages, "the log was consumed and the planks took its place");
    }

    @Test
    void pressingCraftWithoutTheIngredientsChangesNothingAndShowsAHint() {
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        selectRecipe(ItemType.AXE);

        pressCraft();

        assertEquals(1, count(ItemType.WOOD_LOG));
        assertEquals(0, count(ItemType.AXE));
        assertEquals(1, hints.size());
    }

    @Test
    void craftingTwiceUsesTheIngredientsTwice() {
        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        selectRecipe(ItemType.WOOD_PLANK);

        pressCraft();
        pressCraft();

        assertEquals(0, count(ItemType.WOOD_LOG));
        assertEquals(8, count(ItemType.WOOD_PLANK));
    }
}
