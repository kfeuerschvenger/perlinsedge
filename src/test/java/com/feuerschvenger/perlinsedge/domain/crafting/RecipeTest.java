package com.feuerschvenger.perlinsedge.domain.crafting;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeTest {

    private Player player;
    private Recipe axe;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        axe = new RecipeManager().getAllItemRecipes().stream()
                .filter(r -> r.getOutputItemType() == ItemType.AXE)
                .findFirst()
                .orElseThrow();
    }

    private int count(ItemType type) {
        return player.getItemCount(new Item(type, 1));
    }

    @Test
    void axeNeedsThreeStoneAndTwoSticks() {
        assertEquals(List.of(new CraftingIngredient(ItemType.STONE, 3), new CraftingIngredient(ItemType.STICK, 2)),
                axe.getIngredients());
        assertEquals(1, axe.getOutputQuantity());
    }

    @Test
    void canCraftIsFalseWithoutIngredients() {
        assertFalse(player.canCraft(axe));

        player.addItem(new Item(ItemType.STONE, 3));
        assertFalse(player.canCraft(axe), "sticks are still missing");
    }

    @Test
    void canCraftIsFalseWhenAnIngredientIsShort() {
        player.addItem(new Item(ItemType.STONE, 2));
        player.addItem(new Item(ItemType.STICK, 2));

        assertFalse(player.canCraft(axe));
    }

    @Test
    void canCraftIsTrueWithExactIngredients() {
        player.addItem(new Item(ItemType.STONE, 3));
        player.addItem(new Item(ItemType.STICK, 2));

        assertTrue(player.canCraft(axe));
    }

    @Test
    void craftConsumesIngredientsAndAddsTheOutput() {
        player.addItem(new Item(ItemType.STONE, 5));
        player.addItem(new Item(ItemType.STICK, 2));

        player.craftItem(axe);

        assertEquals(2, count(ItemType.STONE));
        assertEquals(0, count(ItemType.STICK));
        assertEquals(1, count(ItemType.AXE));
    }

    @Test
    void craftWithMissingIngredientsChangesNothing() {
        player.addItem(new Item(ItemType.STONE, 2));
        player.addItem(new Item(ItemType.STICK, 2));

        player.craftItem(axe);

        assertEquals(2, count(ItemType.STONE));
        assertEquals(2, count(ItemType.STICK));
        assertEquals(0, count(ItemType.AXE));
    }

    private void fillWithCoalExceptTwoSlots() {
        player.addItem(new Item(ItemType.STONE, 5));
        player.addItem(new Item(ItemType.STICK, 3));
        player.addItem(new Item(ItemType.COAL, 16 * ItemType.COAL.getMaxStackSize()));
        assertEquals(18, player.getItems().stream().filter(java.util.Objects::nonNull).count());
    }

    @Test
    void craftIsRefusedWhenTheOutputHasNoRoomAndNothingIsLost() {
        fillWithCoalExceptTwoSlots();

        boolean crafted = player.craftItem(axe);

        assertFalse(crafted);
        assertEquals(5, count(ItemType.STONE));
        assertEquals(3, count(ItemType.STICK));
        assertEquals(0, count(ItemType.AXE));
    }

    @Test
    void craftSucceedsWhenConsumingTheIngredientsFreesTheSlotForTheOutput() {
        player.addItem(new Item(ItemType.STONE, 3));
        player.addItem(new Item(ItemType.STICK, 2));
        player.addItem(new Item(ItemType.COAL, 16 * ItemType.COAL.getMaxStackSize()));

        assertTrue(player.craftItem(axe));

        assertEquals(1, count(ItemType.AXE));
        assertEquals(0, count(ItemType.STONE));
        assertEquals(0, count(ItemType.STICK));
    }

    @Test
    void craftReportsFalseWhenIngredientsAreMissing() {
        assertFalse(player.craftItem(axe));
    }

    @Test
    void craftingYieldsTheRecipeOutputQuantity() {
        Recipe planks = new RecipeManager().getAllItemRecipes().stream()
                .filter(r -> r.getOutputItemType() == ItemType.WOOD_PLANK)
                .findFirst()
                .orElseThrow();
        player.addItem(new Item(ItemType.WOOD_LOG, 2));

        player.craftItem(planks);

        assertEquals(1, count(ItemType.WOOD_LOG));
        assertEquals(planks.getOutputQuantity(), count(ItemType.WOOD_PLANK));
    }

    @Test
    void recipeRejectsInvalidArguments() {
        List<CraftingIngredient> ingredients = List.of(new CraftingIngredient(ItemType.STONE, 1));

        assertThrows(IllegalArgumentException.class, () -> new Recipe(ItemType.AXE, 0, ingredients));
        assertThrows(IllegalArgumentException.class, () -> new Recipe(ItemType.AXE, 1, List.of()));
        assertThrows(NullPointerException.class, () -> new Recipe(null, 1, ingredients));
        assertThrows(IllegalArgumentException.class, () -> new CraftingIngredient(ItemType.STONE, 0));
    }

    @Test
    void recipeNameAndDescriptionComeFromTheOutputItem() {
        assertEquals(ItemType.AXE.getDisplayName(), axe.getName());
        assertEquals(ItemType.AXE.getDescription(), axe.getDescription());
        assertTrue(axe.getIngredientsString().startsWith("Requires: "));
    }

    @Test
    void recipeManagerExposesItemAndBuildingRecipes() {
        RecipeManager manager = new RecipeManager();

        assertFalse(manager.getAllItemRecipes().isEmpty());
        assertFalse(manager.getAllBuildingRecipes().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> manager.getAllItemRecipes().clear());
    }

    @Test
    void everyBuildingRecipeIsFoundByItsType() {
        RecipeManager manager = new RecipeManager();
        Set<BuildingType> types = manager.getAllBuildingRecipes().stream()
                .map(BuildingRecipe::buildingType)
                .collect(Collectors.toSet());

        for (BuildingType type : types) {
            assertTrue(manager.getBuildingRecipeByType(type).isPresent(), type + " should resolve");
        }
        assertThrows(NullPointerException.class, () -> manager.getBuildingRecipeByType(null));
    }
}
