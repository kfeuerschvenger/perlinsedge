package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryManagerTest {

    private GameStateManager state;
    private Player player;
    private InventoryManager crafting;
    private AtomicInteger refreshes;
    private List<String> hints;
    private RecipeManager recipes;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        player = new Player(0, 0);
        state.setPlayer(player);
        refreshes = new AtomicInteger();
        hints = new ArrayList<>();
        crafting = new InventoryManager(state, refreshes::incrementAndGet, hints::add);
        recipes = new RecipeManager();
    }

    private Recipe recipeFor(ItemType output) {
        return recipes.getAllItemRecipes().stream()
                .filter(recipe -> recipe.getOutputItemType() == output)
                .findFirst()
                .orElseThrow();
    }

    private int count(ItemType type) {
        return player.getItemCount(new Item(type, 1));
    }

    @Test
    void aCraftConsumesTheIngredientsAndAddsTheResult() {
        player.addItem(new Item(ItemType.WOOD_LOG, 3));

        assertTrue(crafting.handleCraftRecipe(recipeFor(ItemType.WOOD_PLANK)));

        assertEquals(2, count(ItemType.WOOD_LOG));
        assertEquals(4, count(ItemType.WOOD_PLANK));
        assertEquals(1, refreshes.get());
        assertTrue(hints.isEmpty());
    }

    @Test
    void ingredientsSpreadOverSeveralStacksAreAllUsed() {
        player.setItemAt(0, new Item(ItemType.STONE, 2));
        player.setItemAt(5, new Item(ItemType.STONE, 1));
        player.setItemAt(9, new Item(ItemType.STICK, 2));

        assertTrue(crafting.handleCraftRecipe(recipeFor(ItemType.AXE)));

        assertEquals(0, count(ItemType.STONE));
        assertEquals(0, count(ItemType.STICK));
        assertEquals(1, count(ItemType.AXE));
    }

    @Test
    void everyItemRecipeCanBeCraftedWhenTheIngredientsAreInTheInventory() {
        for (Recipe recipe : recipes.getAllItemRecipes()) {
            player.clearInventory();
            for (CraftingIngredient ingredient : recipe.getIngredients()) {
                player.addItem(new Item(ingredient.itemType(), ingredient.quantity()));
            }

            assertTrue(crafting.handleCraftRecipe(recipe), recipe.getName());

            assertEquals(recipe.getOutputQuantity(), count(recipe.getOutputItemType()), recipe.getName());
        }
    }

    @Test
    void missingIngredientsChangeNothingAndTellThePlayer() {
        player.addItem(new Item(ItemType.STONE, 2));

        assertFalse(crafting.handleCraftRecipe(recipeFor(ItemType.AXE)));

        assertEquals(2, count(ItemType.STONE), "a refused craft must not eat the ingredients that were there");
        assertEquals(0, count(ItemType.AXE));
        assertEquals(List.of(InventoryManager.MISSING_INGREDIENTS_HINT), hints);
        assertEquals(0, refreshes.get());
    }

    @Test
    void aResultThatDoesNotFitIsRefusedWithoutConsumingAnything() {
        player.setItemAt(0, new Item(ItemType.WOOD_LOG, 2));
        for (int slot = 1; slot < player.getCapacity(); slot++) {
            player.setItemAt(slot, new Item(ItemType.COAL, 64));
        }

        assertFalse(crafting.handleCraftRecipe(recipeFor(ItemType.WOOD_PLANK)));

        assertEquals(2, count(ItemType.WOOD_LOG));
        assertEquals(0, count(ItemType.WOOD_PLANK));
        assertEquals(List.of(InventoryManager.NO_ROOM_HINT), hints);
        assertEquals(0, refreshes.get());
    }

    @Test
    void aResultThatFitsOnlyBecauseTheIngredientFreesItsSlotIsAccepted() {
        player.setItemAt(0, new Item(ItemType.WOOD_LOG, 1));
        for (int slot = 1; slot < player.getCapacity(); slot++) {
            player.setItemAt(slot, new Item(ItemType.COAL, 64));
        }

        assertTrue(crafting.handleCraftRecipe(recipeFor(ItemType.WOOD_PLANK)));

        assertEquals(4, count(ItemType.WOOD_PLANK));
        assertEquals(0, count(ItemType.WOOD_LOG));
    }

    @Test
    void aNullRecipeIsIgnored() {
        assertFalse(crafting.handleCraftRecipe(null));

        assertTrue(hints.isEmpty());
        assertEquals(0, refreshes.get());
    }

    @Test
    void withoutAPlayerNothingHappens() {
        state.setPlayer(null);

        assertFalse(crafting.handleCraftRecipe(recipeFor(ItemType.WOOD_PLANK)));
        assertEquals(0, refreshes.get());
    }

    @Test
    void aDeadPlayerCannotCraft() {
        player.addItem(new Item(ItemType.WOOD_LOG, 3));
        player.takeDamage(1000);

        assertFalse(crafting.handleCraftRecipe(recipeFor(ItemType.WOOD_PLANK)));

        assertEquals(3, count(ItemType.WOOD_LOG));
    }
}
