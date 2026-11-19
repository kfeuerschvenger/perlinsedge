package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingStation;
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
    // Ingredient counts of the item recipes
    private static final int AXE_STONE_COST = 3;
    private static final int AXE_STICK_COST = 2;
    private static final int PICKAXE_STONE_COST = 3;
    private static final int PICKAXE_STICK_COST = 2;
    private static final int SWORD_STONE_COST = 2;
    private static final int IRON_AXE_INGOT_COST = 3;
    private static final int IRON_PICKAXE_INGOT_COST = 3;
    private static final int IRON_SWORD_INGOT_COST = 2;
    private static final int HAMMER_STICK_COST = 2;
    private static final int HAMMER_STONE_COST = 2;
    private static final int FLINT_STONE_COST = 2;
    private static final int POTION_FIBER_COST = 2;

    // Items produced per craft
    private static final int TORCH_OUTPUT = 4;
    private static final int WOOD_PLANK_OUTPUT = 4;
    private static final int STONE_BRICK_OUTPUT = 4;
    private static final int STICK_OUTPUT = 4;

    // Ingredient counts of the building recipes
    private static final int BRIDGE_PLANK_COST = 10;
    private static final int FENCE_LOG_COST = 2;
    private static final int FENCE_GATE_PLANK_COST = 2;
    private static final int FENCE_GATE_STICK_COST = 4;
    private static final int CHEST_PLANK_COST = 6;
    private static final int FURNACE_STONE_COST = 8;
    private static final int FURNACE_COAL_COST = 2;
    private static final int CAMPFIRE_LOG_COST = 3;
    private static final int ANVIL_IRON_COST = 5;
    private static final int ANVIL_STONE_COST = 3;
    private static final int WORKBENCH_PLANK_COST = 4;
    private static final int SLEEPING_BAG_FIBER_COST = 6;

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
     * Retrieves the item recipes crafted at one place, in the order they are listed there.
     *
     * @param station The place to list the recipes of
     * @return Unmodifiable list of the recipes of that place
     */
    public List<Recipe> getItemRecipesFor(CraftingStation station) {
        Objects.requireNonNull(station, "Crafting station cannot be null");

        return itemRecipes.stream()
                .filter(recipe -> recipe.getStation() == station)
                .toList();
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
        // Inventory: the basic recipes
        itemRecipes.add(createMaterialRecipe(CraftingStation.INVENTORY, ItemType.WOOD_PLANK,
                WOOD_PLANK_OUTPUT,
                new CraftingIngredient(ItemType.WOOD_LOG, 1)));

        itemRecipes.add(createMaterialRecipe(CraftingStation.INVENTORY, ItemType.STICK,
                STICK_OUTPUT,
                new CraftingIngredient(ItemType.WOOD_LOG, 1)));

        itemRecipes.add(createMaterialRecipe(CraftingStation.INVENTORY, ItemType.FLINT,
                1,
                new CraftingIngredient(ItemType.STONE, FLINT_STONE_COST)));

        itemRecipes.add(new Recipe(
                ItemType.TORCH,
                TORCH_OUTPUT,
                List.of(
                        new CraftingIngredient(ItemType.STICK, 1),
                        new CraftingIngredient(ItemType.COAL, 1)
                ),
                CraftingStation.INVENTORY));

        // The tool that puts the player in build mode while it is held
        itemRecipes.add(createToolRecipe(CraftingStation.INVENTORY, ItemType.HAMMER,
                new CraftingIngredient(ItemType.STICK, HAMMER_STICK_COST),
                new CraftingIngredient(ItemType.STONE, HAMMER_STONE_COST))
        );

        // Workbench: stone tools, bricks and potions
        itemRecipes.add(createToolRecipe(CraftingStation.WORKBENCH, ItemType.AXE,
                new CraftingIngredient(ItemType.STONE, AXE_STONE_COST),
                new CraftingIngredient(ItemType.STICK, AXE_STICK_COST))
        );

        itemRecipes.add(createToolRecipe(CraftingStation.WORKBENCH, ItemType.PICKAXE,
                new CraftingIngredient(ItemType.STONE, PICKAXE_STONE_COST),
                new CraftingIngredient(ItemType.STICK, PICKAXE_STICK_COST))
        );

        itemRecipes.add(createToolRecipe(CraftingStation.WORKBENCH, ItemType.SWORD,
                new CraftingIngredient(ItemType.STONE, SWORD_STONE_COST),
                new CraftingIngredient(ItemType.STICK, 1))
        );

        itemRecipes.add(createMaterialRecipe(CraftingStation.WORKBENCH, ItemType.STONE_BRICK,
                STONE_BRICK_OUTPUT,
                new CraftingIngredient(ItemType.STONE, 1)));

        itemRecipes.add(new Recipe(
                ItemType.POTION_HEALTH,
                1,
                List.of(
                        new CraftingIngredient(ItemType.CRYSTALS, 1),
                        new CraftingIngredient(ItemType.FIBER, POTION_FIBER_COST),
                        new CraftingIngredient(ItemType.SLIME_BALL, 1)
                ),
                CraftingStation.WORKBENCH));

        // Anvil: iron tools
        itemRecipes.add(createToolRecipe(CraftingStation.ANVIL, ItemType.IRON_SWORD,
                new CraftingIngredient(ItemType.IRON_INGOT, IRON_SWORD_INGOT_COST),
                new CraftingIngredient(ItemType.STICK, 1))
        );

        itemRecipes.add(createToolRecipe(CraftingStation.ANVIL, ItemType.IRON_AXE,
                new CraftingIngredient(ItemType.IRON_INGOT, IRON_AXE_INGOT_COST),
                new CraftingIngredient(ItemType.STICK, AXE_STICK_COST))
        );

        itemRecipes.add(createToolRecipe(CraftingStation.ANVIL, ItemType.IRON_PICKAXE,
                new CraftingIngredient(ItemType.IRON_INGOT, IRON_PICKAXE_INGOT_COST),
                new CraftingIngredient(ItemType.STICK, PICKAXE_STICK_COST))
        );
    }

    /**
     * Creates a tool recipe with standard naming convention.
     */
    private static Recipe createToolRecipe(CraftingStation station, ItemType toolType, CraftingIngredient... ingredients) {
        return new Recipe(toolType, 1, List.of(ingredients), station);
    }

    /**
     * Creates a material processing recipe with standard naming convention.
     */
    private static Recipe createMaterialRecipe(CraftingStation station, ItemType materialType, int outputCount,
                                               CraftingIngredient... ingredient) {
        return new Recipe(materialType, outputCount, List.of(ingredient), station);
    }

    /**
     * Initializes all building construction recipes.
     */
    private void initializeBuildingRecipes() {
        // Structural buildings
        buildingRecipes.add(createBuildingRecipe(BuildingType.BRIDGE,
                new CraftingIngredient(ItemType.WOOD_PLANK, BRIDGE_PLANK_COST)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.FENCE,
                new CraftingIngredient(ItemType.WOOD_LOG, FENCE_LOG_COST)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.FENCE_GATE,
                new CraftingIngredient(ItemType.WOOD_PLANK, FENCE_GATE_PLANK_COST),
                new CraftingIngredient(ItemType.STICK, FENCE_GATE_STICK_COST)));

        // Containers
        buildingRecipes.add(createBuildingRecipe(BuildingType.CHEST,
                new CraftingIngredient(ItemType.WOOD_PLANK, CHEST_PLANK_COST)));

        // Crafting stations
        buildingRecipes.add(createBuildingRecipe(BuildingType.FURNACE,
                new CraftingIngredient(ItemType.STONE, FURNACE_STONE_COST),
                new CraftingIngredient(ItemType.COAL, FURNACE_COAL_COST)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.CAMPFIRE,
                new CraftingIngredient(ItemType.WOOD_LOG, CAMPFIRE_LOG_COST),
                new CraftingIngredient(ItemType.FLINT, 1)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.ANVIL,
                new CraftingIngredient(ItemType.IRON_INGOT, ANVIL_IRON_COST),
                new CraftingIngredient(ItemType.STONE, ANVIL_STONE_COST)));

        buildingRecipes.add(createBuildingRecipe(BuildingType.WORKBENCH,
                new CraftingIngredient(ItemType.WOOD_PLANK, WORKBENCH_PLANK_COST)));

        // Utility
        buildingRecipes.add(createBuildingRecipe(BuildingType.STANDING_TORCH,
                new CraftingIngredient(ItemType.STICK, 1),
                new CraftingIngredient(ItemType.COAL, 1)));

        // The bed has no recipe until leather exists, the sleeping bag is the basic one
        buildingRecipes.add(createBuildingRecipe(BuildingType.SLEEPING_BAG,
                new CraftingIngredient(ItemType.FIBER, SLEEPING_BAG_FIBER_COST)));
    }

    /**
     * Creates a building recipe with consistent formatting.
     */
    private static BuildingRecipe createBuildingRecipe(BuildingType type, CraftingIngredient... ingredients) {
        return new BuildingRecipe(type, List.of(ingredients));
    }

}
