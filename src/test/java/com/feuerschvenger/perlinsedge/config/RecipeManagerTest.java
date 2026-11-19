package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingStation;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeManagerTest {

    @Test
    void theHammerIsCraftedFromTwoSticksAndTwoStonesWithNoStation() {
        Recipe hammer = new RecipeManager().getAllItemRecipes().stream()
                .filter(recipe -> recipe.getOutputItemType() == ItemType.HAMMER)
                .findFirst()
                .orElseThrow();

        assertEquals(1, hammer.getOutputQuantity());
        assertEquals(List.of(new CraftingIngredient(ItemType.STICK, 2), new CraftingIngredient(ItemType.STONE, 2)),
                hammer.getIngredients());
    }

    private static List<CraftingIngredient> ingredientsOf(ItemType output) {
        return new RecipeManager().getAllItemRecipes().stream()
                .filter(recipe -> recipe.getOutputItemType() == output)
                .findFirst()
                .orElseThrow()
                .getIngredients();
    }

    @Test
    void theIronToolsAreMadeFromIronIngotsAndSticks() {
        assertEquals(List.of(new CraftingIngredient(ItemType.IRON_INGOT, 3), new CraftingIngredient(ItemType.STICK, 2)),
                ingredientsOf(ItemType.IRON_AXE));
        assertEquals(List.of(new CraftingIngredient(ItemType.IRON_INGOT, 3), new CraftingIngredient(ItemType.STICK, 2)),
                ingredientsOf(ItemType.IRON_PICKAXE));
        assertEquals(List.of(new CraftingIngredient(ItemType.IRON_INGOT, 2), new CraftingIngredient(ItemType.STICK, 1)),
                ingredientsOf(ItemType.IRON_SWORD));
    }

    private static List<ItemType> outputsAt(CraftingStation station) {
        return new RecipeManager().getItemRecipesFor(station).stream().map(Recipe::getOutputItemType).toList();
    }

    @Test
    void theInventoryCraftsOnlyTheBasicRecipes() {
        assertEquals(List.of(ItemType.WOOD_PLANK, ItemType.STICK, ItemType.FLINT, ItemType.TORCH, ItemType.HAMMER),
                outputsAt(CraftingStation.INVENTORY));
    }

    @Test
    void theWorkbenchCraftsTheStoneToolsBricksAndPotions() {
        assertEquals(List.of(ItemType.AXE, ItemType.PICKAXE, ItemType.SWORD, ItemType.STONE_BRICK,
                ItemType.POTION_HEALTH), outputsAt(CraftingStation.WORKBENCH));
    }

    @Test
    void theAnvilCraftsTheIronTools() {
        assertEquals(List.of(ItemType.IRON_SWORD, ItemType.IRON_AXE, ItemType.IRON_PICKAXE),
                outputsAt(CraftingStation.ANVIL));
    }

    @Test
    void everyItemRecipeBelongsToExactlyOneStationAndTheListsDoNotRepeat() {
        final RecipeManager manager = new RecipeManager();
        int listed = 0;
        for (CraftingStation station : CraftingStation.values()) {
            listed += manager.getItemRecipesFor(station).size();
        }

        assertEquals(manager.getAllItemRecipes().size(), listed);
    }

    @Test
    void charcoalHasNoRecipeBecauseTheFurnaceMakesIt() {
        assertTrue(new RecipeManager().getAllItemRecipes().stream()
                .noneMatch(recipe -> recipe.getOutputItemType() == ItemType.CHARCOAL));
    }

    @Test
    void theSleepingBagCostsSixFiberAndTheBedIsOutOfTheRecipes() {
        final RecipeManager recipes = new RecipeManager();

        assertEquals(List.of(new CraftingIngredient(ItemType.FIBER, 6)),
                recipes.getBuildingRecipeByType(BuildingType.SLEEPING_BAG).orElseThrow().ingredients());
        assertTrue(recipes.getBuildingRecipeByType(BuildingType.BED).isEmpty());
    }

    @Test
    void everyItemAndTheSleepingBagHaveTheirArt() {
        for (ItemType type : ItemType.values()) {
            assertNotNull(getClass().getResource("/assets/items/" + type.getSpritePath() + ".png"), type.name());
        }
        for (String name : List.of("sleeping_bag_0", "sleeping_bag_1", "sleeping_bag_2", "sleeping_bag_3")) {
            assertNotNull(getClass().getResource("/assets/buildings/" + name + ".png"), name);
        }
    }

    @Test
    void thereIsNoFishInTheGame() {
        for (ItemType type : ItemType.values()) {
            assertFalse(type.name().toLowerCase(Locale.ROOT).contains("fish"), type.name());
        }
    }

    @Test
    void theHammerHasItsIconFile() {
        assertNotNull(getClass().getResource("/assets/items/" + ItemType.HAMMER.getSpritePath() + ".png"));
    }
}
