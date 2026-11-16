package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.GameStateManager;
import com.feuerschvenger.perlinsedge.app.managers.InventoryManager;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.containers.Container;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

    /** Clicks the cell of the recipe, wherever the list has put it. */
    private void selectRecipe(ItemType output) {
        String name = UiTheme.caps(output.getDisplayName());
        FxToolkitSupport.onFx(() -> {
            Region cell = panel.lookupAll("." + UiTheme.CELL).stream()
                    .map(Region.class::cast)
                    .filter(node -> node.getChildrenUnmodifiable().stream()
                            .anyMatch(child -> child instanceof Label label && name.equals(label.getText())))
                    .findFirst()
                    .orElseThrow();
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
    void theCraftButtonStartsDisabledUntilACraftableRecipeIsSelected() {
        assertTrue(FxToolkitSupport.onFx(() -> craftButton().isDisabled()));

        selectRecipe(ItemType.WOOD_PLANK);
        assertTrue(FxToolkitSupport.onFx(() -> craftButton().isDisabled()), "no log, so the recipe cannot be crafted");

        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });
        assertFalse(FxToolkitSupport.onFx(() -> craftButton().isDisabled()));
    }

    @Test
    void noButtonOfThePanelCanTakeKeyboardFocusSoSpaceStaysWithTheCamera() {
        List<Boolean> focusable = FxToolkitSupport.onFx(() -> panel.lookupAll(".button").stream()
                .map(node -> node.isFocusTraversable())
                .toList());

        assertFalse(focusable.isEmpty());
        assertFalse(focusable.contains(true));
    }

    @Test
    void theCraftButtonIsDisabledAgainWhenTheIngredientsRunOut() {
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        selectRecipe(ItemType.WOOD_PLANK);
        assertFalse(FxToolkitSupport.onFx(() -> craftButton().isDisabled()));

        pressCraft();

        assertTrue(FxToolkitSupport.onFx(() -> craftButton().isDisabled()));
    }

    @Test
    void theRecipesThatCanBeCraftedAreListedFirstAndTheOthersFollowGreyedButSelectable() {
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });

        List<String> order = FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.CELL).stream()
                .map(node -> node.getStyleClass().contains(UiTheme.CELL_UNAVAILABLE) ? "grey" : "ok")
                .toList());

        assertEquals("ok", order.getFirst());
        long craftable = recipes.getAllItemRecipes().stream().filter(player::canCraft).count();
        assertEquals(craftable, order.stream().filter("ok"::equals).count());
        assertTrue(order.subList((int) craftable, order.size()).stream().allMatch("grey"::equals),
                "the craftable recipes come first: " + order);

        selectRecipe(ItemType.AXE);
        assertEquals(ItemType.AXE.getDisplayName(),
                FxToolkitSupport.onFx(() -> panel.getSelectedCraftingRecipe().getName()));
    }

    @Test
    void anUncraftableRecipeHasAGreyscaleIconAndACraftableOneKeepsItsColour() {
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });

        List<javafx.scene.effect.Effect> effects = FxToolkitSupport.onFx(() -> {
            List<javafx.scene.effect.Effect> found = new ArrayList<>();
            for (javafx.scene.Node cell : panel.lookupAll("." + UiTheme.CELL)) {
                javafx.scene.image.ImageView icon = (javafx.scene.image.ImageView) ((Region) cell)
                        .getChildrenUnmodifiable().stream()
                        .filter(javafx.scene.image.ImageView.class::isInstance)
                        .findFirst()
                        .orElseThrow();
                found.add(icon.getEffect());
            }
            return found;
        });
        List<Boolean> grey = FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.CELL).stream()
                .map(node -> node.getStyleClass().contains(UiTheme.CELL_UNAVAILABLE))
                .toList());

        assertEquals(effects.size(), grey.size());
        for (int i = 0; i < effects.size(); i++) {
            if (grey.get(i)) {
                assertTrue(effects.get(i) instanceof javafx.scene.effect.ColorAdjust adjust
                        && adjust.getSaturation() == -1.0, "greyed cell " + i + " has a greyscale icon");
            } else {
                assertEquals(null, effects.get(i), "craftable cell " + i + " keeps its colour");
            }
        }
        assertTrue(grey.contains(true) && grey.contains(false));
    }

    @Test
    void theListIsSortedAgainWhenTheInventoryChangesAndKeepsItsOrderInsideEachGroup() {
        List<String> before = recipeNamesInListOrder();
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });
        List<String> withLog = recipeNamesInListOrder();
        player.removeItem(new Item(ItemType.WOOD_LOG, 1), 1);
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });

        assertEquals(UiTheme.caps(ItemType.WOOD_PLANK.getDisplayName()), withLog.getFirst());
        assertEquals(before, recipeNamesInListOrder(), "back to the order of the recipes with nothing craftable");
    }

    private List<String> recipeNamesInListOrder() {
        return FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.CELL).stream()
                .map(Region.class::cast)
                .map(cell -> cell.getChildrenUnmodifiable().stream()
                        .filter(Label.class::isInstance)
                        .map(child -> ((Label) child).getText())
                        .findFirst()
                        .orElseThrow())
                .toList());
    }

    @Test
    void theInfoPanelShowsWhatIsHeldAgainstWhatIsNeededInGreenAndRed() {
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        selectRecipe(ItemType.AXE);

        List<String> lines = FxToolkitSupport.onFx(() -> panel.ingredientLinesShown().stream()
                .map(text -> text.getText() + "|" + (text.getStyleClass().contains(UiTheme.TEXT_GOOD) ? "good" : "bad"))
                .toList());

        assertFalse(lines.isEmpty());
        assertTrue(lines.stream().allMatch(line -> line.contains("/")), "every line reads held/needed: " + lines);
        assertTrue(lines.stream().allMatch(line -> line.endsWith("|bad")), "nothing needed is held yet: " + lines);

        player.setItemAt(Player.HOTBAR_SIZE, new Item(ItemType.STONE, 50));
        player.setItemAt(Player.HOTBAR_SIZE + 1, new Item(ItemType.STICK, 50));
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });
        List<String> after = FxToolkitSupport.onFx(() -> panel.ingredientLinesShown().stream()
                .map(text -> text.getStyleClass().contains(UiTheme.TEXT_GOOD) ? "good" : "bad")
                .toList());
        assertTrue(after.stream().allMatch("good"::equals),
                "held ingredients turn green once the inventory refreshes: " + after);
    }

    private void refresh() {
        FxToolkitSupport.onFx(() -> {
            panel.updateInventoryDisplay();
            return null;
        });
    }

    private List<String> shownLines() {
        return FxToolkitSupport.onFx(() -> panel.ingredientLinesShown().stream()
                .map(javafx.scene.text.Text::getText)
                .toList());
    }

    @Test
    void theInfoPanelIsRedrawnOnlyWhenTheInventoryVersionOrTheSelectionChanged() {
        player.addItem(new Item(ItemType.STICK, 1));
        selectRecipe(ItemType.AXE);
        refresh();
        List<String> before = shownLines();
        assertTrue(before.contains(ItemType.STICK.getDisplayName() + " 1/2"), "the held sticks are shown: " + before);

        // An edit that bypasses the player leaves its version alone, so a refresh has nothing to redraw
        player.getItemAt(0).addQuantity(1);
        refresh();
        assertEquals(before, shownLines());

        player.noteStackChanged();
        refresh();
        assertTrue(shownLines().contains(ItemType.STICK.getDisplayName() + " 2/2"));
    }

    @Test
    void theCraftingListCatchesUpOnTheInventoryWhenTheChestIsClosedAndThePlayerInventoryReopens() {
        Container chest = new Chest(0, 0, 8);
        FxToolkitSupport.onFx(() -> {
            panel.openDualInventory(chest);
            return null;
        });

        // The list is out of the layout while the chest is open, so refreshing marks nothing there
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        refresh();
        assertTrue(recipeNamesInListOrder().isEmpty(), "the list is not part of the scene while a chest is open");

        FxToolkitSupport.onFx(() -> {
            panel.closeInventory();
            panel.openPlayerInventory();
            return null;
        });
        assertEquals(UiTheme.caps(ItemType.WOOD_PLANK.getDisplayName()), recipeNamesInListOrder().getFirst());
    }

    @Test
    void theHeldCountAddsTheHotbarAndTheInventory() {
        player.setItemAt(0, new Item(ItemType.STICK, 1));
        player.setItemAt(Player.HOTBAR_SIZE, new Item(ItemType.STICK, 2));
        selectRecipe(ItemType.AXE);

        List<String> lines = FxToolkitSupport.onFx(() -> panel.ingredientLinesShown().stream()
                .map(javafx.scene.text.Text::getText)
                .toList());

        assertTrue(lines.contains(ItemType.STICK.getDisplayName() + " 3/2"),
                "the sticks of the hotbar and of the inventory are counted together: " + lines);
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
        // The hotbar is full, so the planks go to the inventory grid that the test reads
        for (int slot = 0; slot < Player.HOTBAR_SIZE; slot++) {
            player.setItemAt(slot, new Item(ItemType.STONE, 1));
        }
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        selectRecipe(ItemType.WOOD_PLANK);

        pressCraft();

        long itemImages = FxToolkitSupport.onFx(() -> panel.lookupAll("." + UiTheme.SLOT).stream()
                .filter(slot -> !((StackPane) slot).getChildren().isEmpty())
                .count());
        assertEquals(1, itemImages, "the log was consumed and the planks took its place");
    }

    @Test
    void pressingCraftWithoutTheIngredientsChangesNothing() {
        player.addItem(new Item(ItemType.WOOD_LOG, 1));
        selectRecipe(ItemType.AXE);

        pressCraft();

        assertEquals(1, count(ItemType.WOOD_LOG));
        assertEquals(0, count(ItemType.AXE));
        assertTrue(FxToolkitSupport.onFx(() -> craftButton().isDisabled()), "the button does not offer the craft");
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
