package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages all crafting recipes in the game, including both item recipes and building recipes.
 * Provides access to recipes through a well-defined API with immutable return types.
 */
public class RecipeManager {
    // Recipe collections
    private final List<Recipe> itemRecipes = new ArrayList<>();
    private final List<BuildingRecipe> buildingRecipes = new ArrayList<>();

    /**
     * Initializes all recipes during construction.
     */
    public RecipeManager() {
        initializeItemRecipes();
        initializeBuildingRecipes();
    }

    // Public API -----------------------------------------------------------------

    /**
     * Retrieves all item crafting recipes.
     *
     * @return Unmodifiable list of item recipes
     */
    public List<Recipe> getAllItemRecipes() {
        return Collections.unmodifiableList(itemRecipes);
    }

    /**
     * Retrieves all building recipes.
     *
     * @return Unmodifiable list of building recipes
     */
    public List<BuildingRecipe> getAllBuildingRecipes() {
        return Collections.unmodifiableList(buildingRecipes);
    }

    /**
     * Finds a building recipe by building type.
     *
     * @param type The building type to search for
     * @return Optional containing the recipe if found, empty otherwise
     */
    public Optional<BuildingRecipe> getBuildingRecipeByType(BuildingType type) {
        Objects.requireNonNull(type, "Building type cannot be null");

        return buildingRecipes.stream()
                .filter(recipe -> recipe.buildingType() == type)
                .findFirst();
    }

    // Recipe Initialization ------------------------------------------------------

    /**
     * Initializes all item crafting recipes.
     */
    private void initializeItemRecipes() {
        // Basic tools
        itemRecipes.add(createToolRecipe(ItemType.AXE,
                new CraftingIngredient(ItemType.STONE, 3),
                new CraftingIngredient(ItemType.STICK, 2))
        );

        itemRecipes.add(createToolRecipe(ItemType.PICKAXE,
                new CraftingIngredient(ItemType.STONE, 3),
                new CraftingIngredient(ItemType.STICK, 2))
        );

        itemRecipes.add(createToolRecipe(ItemType.SWORD,
                new CraftingIngredient(ItemType.STONE, 2),
                new CraftingIngredient(ItemType.STICK, 1))
        );

        // Light sources
        itemRecipes.add(new Recipe(
                ItemType.TORCH,
                4,
                List.of(
                        new CraftingIngredient(ItemType.STICK, 1),
                        new CraftingIngredient(ItemType.COAL, 1)
                )));

        // Material processing
        itemRecipes.add(createMaterialRecipe(ItemType.WOOD_PLANK,
                4,
                new CraftingIngredient(ItemType.WOOD_LOG, 1)));

        itemRecipes.add(createMaterialRecipe(ItemType.STONE_BRICK,
                4,
                new CraftingIngredient(ItemType.STONE, 1)));

        itemRecipes.add(createMaterialRecipe(ItemType.STICK,
                4,
                new CraftingIngredient(ItemType.WOOD_LOG, 1)));

        itemRecipes.add(createMaterialRecipe(ItemType.FLINT,
                1,
                new CraftingIngredient(ItemType.STONE, 2)));

        // Combat items
        itemRecipes.add(new Recipe(
                ItemType.ARROW,
                8,
                List.of(
                        new CraftingIngredient(ItemType.STICK, 1),
                        new CraftingIngredient(ItemType.FLINT, 1)
                )));

        itemRecipes.add(new Recipe(
                ItemType.BOW,
                1,
                List.of(
                        new CraftingIngredient(ItemType.STICK, 3),
                        new CraftingIngredient(ItemType.FIBER, 2)
                )));

        // Consumables
        itemRecipes.add(new Recipe(
                ItemType.POTION_HEALTH,
                1,
                List.of(
                        new CraftingIngredient(ItemType.CRYSTALS, 1),
                        new CraftingIngredient(ItemType.FIBER, 2),
                        new CraftingIngredient(ItemType.SLIME_BALL, 1)
                )));
    }

    /**
     * Creates a tool recipe with standard naming convention.
     */
    private Recipe createToolRecipe(ItemType toolType, CraftingIngredient... ingredients) {
        return new Recipe(toolType, 1, List.of(ingredients));
    }

    /**
     * Creates a material processing recipe with standard naming convention.
     */
    private Recipe createMaterialRecipe(ItemType materialType, int outputCount, CraftingIngredient... ingredient) {
        return new Recipe(materialType, outputCount, List.of(ingredient));
    }

    /**
     * Initializes all building construction recipes.
     */
    private void initializeBuildingRecipes() {
        // Structural buildings
        buildingRecipes.add(createBuildingRecipe(BuildingType.WALL,
                new CraftingIngredient(ItemType.STONE, 2)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.DOOR,
                new CraftingIngredient(ItemType.WOOD_PLANK, 5),
                new CraftingIngredient(ItemType.IRON_INGOT, 1)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.BRIDGE,
                new CraftingIngredient(ItemType.WOOD_PLANK, 10)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.DOCK,
                new CraftingIngredient(ItemType.WOOD_PLANK, 8)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.FENCE,
                new CraftingIngredient(ItemType.WOOD_LOG, 2)));

        // Containers
        buildingRecipes.add(createBuildingRecipe(BuildingType.CHEST,
                new CraftingIngredient(ItemType.WOOD_PLANK, 6)));

        // Crafting stations
        buildingRecipes.add(createBuildingRecipe(BuildingType.FURNACE,
                new CraftingIngredient(ItemType.STONE, 8),
                new CraftingIngredient(ItemType.COAL, 2)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.CAMPFIRE,
                new CraftingIngredient(ItemType.WOOD_LOG, 3),
                new CraftingIngredient(ItemType.FLINT, 1)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.ANVIL,
                new CraftingIngredient(ItemType.IRON_INGOT, 5),
                new CraftingIngredient(ItemType.STONE, 3)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.WORKBENCH,
                new CraftingIngredient(ItemType.WOOD_PLANK, 4)));

        // Utility
        buildingRecipes.add(createBuildingRecipe(BuildingType.STANDING_TORCH,
                new CraftingIngredient(ItemType.STICK, 1),
                new CraftingIngredient(ItemType.COAL, 1)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.BED,
                new CraftingIngredient(ItemType.WOOD_PLANK, 3),
                new CraftingIngredient(ItemType.FIBER, 5)));
    }

    /**
     * Creates a building recipe with consistent formatting.
     */
    private BuildingRecipe createBuildingRecipe(BuildingType type, CraftingIngredient... ingredients) {
        return new BuildingRecipe(type, List.of(ingredients));
    }

}