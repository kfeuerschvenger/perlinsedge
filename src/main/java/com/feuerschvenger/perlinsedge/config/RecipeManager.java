package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeManager {
    private final List<Recipe> recipes = new ArrayList<>();
    private final List<BuildingRecipe> buildingRecipes = new ArrayList<>();

    public RecipeManager() {
        initializeRecipes();
        initializeBuildingRecipes();
    }

    private void initializeRecipes() {
        // Stone Axe
        recipes.add(new Recipe(
                ItemType.AXE,
                1,
                List.of(
                        new CraftingIngredient(ItemType.STONE, 3),
                        new CraftingIngredient(ItemType.STICK, 2)
                )
        ));

        // Stone Pickaxe
        recipes.add(new Recipe(
                ItemType.PICKAXE,
                1,
                List.of(
                        new CraftingIngredient(ItemType.STONE, 3),
                        new CraftingIngredient(ItemType.STICK, 2)
                )
        ));

        // Stone Sword
        recipes.add(new Recipe(
                ItemType.SWORD,
                1,
                List.of(
                        new CraftingIngredient(ItemType.STONE, 2),
                        new CraftingIngredient(ItemType.STICK, 1)
                )
        ));

        // Torch
        recipes.add(new Recipe(
                ItemType.TORCH,
                4,
                List.of(
                        new CraftingIngredient(ItemType.STICK, 1),
                        new CraftingIngredient(ItemType.COAL, 1)
                )
        ));

        // Wood Planks
        recipes.add(new Recipe(
                ItemType.WOOD_PLANK,
                4,
                List.of(
                        new CraftingIngredient(ItemType.WOOD_LOG, 1)
                )
        ));

        // Stone Bricks
        recipes.add(new Recipe(
                ItemType.STONE_BRICK,
                4,
                List.of(
                        new CraftingIngredient(ItemType.STONE, 1)
                )
        ));

        // Arrows
        recipes.add(new Recipe(
                ItemType.ARROW,
                8,
                List.of(
                        new CraftingIngredient(ItemType.STICK, 1),
                        new CraftingIngredient(ItemType.FLINT, 1)
                )
        ));

        // Bow
        recipes.add(new Recipe(
                ItemType.BOW,
                1,
                List.of(
                        new CraftingIngredient(ItemType.STICK, 3),
                        new CraftingIngredient(ItemType.FIBER, 2)
                )
        ));

        // Health Potion
        recipes.add(new Recipe(
                ItemType.POTION_HEALTH,
                1,
                List.of(
                        new CraftingIngredient(ItemType.CRYSTALS, 1),
                        new CraftingIngredient(ItemType.FIBER, 2),
                        new CraftingIngredient(ItemType.SLIME_BALL, 1)
                )
        ));

        // Sticks
        recipes.add(new Recipe(
                ItemType.STICK,
                4,
                List.of(
                        new CraftingIngredient(ItemType.WOOD_LOG, 1)
                )
        ));

        // Flint
        recipes.add(new Recipe(
                ItemType.FLINT,
                1,
                List.of(
                        new CraftingIngredient(ItemType.STONE, 2)
                )
        ));
    }

    private void initializeBuildingRecipes() {
        buildingRecipes.addAll(
            List.of(
                new BuildingRecipe(BuildingType.WALL, List.of(
                        new CraftingIngredient(ItemType.STONE, 2)
                )),
                new BuildingRecipe(BuildingType.DOOR, List.of(
                        new CraftingIngredient(ItemType.WOOD_PLANK, 5),
                        new CraftingIngredient(ItemType.IRON_INGOT, 1)
                )),
                new BuildingRecipe(BuildingType.BRIDGE, List.of(
                        new CraftingIngredient(ItemType.WOOD_PLANK, 10)
                )),
                new BuildingRecipe(BuildingType.DOCK, List.of(
                        new CraftingIngredient(ItemType.WOOD_PLANK, 8)
                )),
                new BuildingRecipe(BuildingType.FENCE, List.of(
                        new CraftingIngredient(ItemType.WOOD_LOG, 2) // Using WOOD_LOG for fences
                )),
                new BuildingRecipe(BuildingType.CHEST, List.of(
                        new CraftingIngredient(ItemType.WOOD_PLANK, 6)
                )),
                new BuildingRecipe(BuildingType.FURNACE, List.of(
                        new CraftingIngredient(ItemType.STONE, 8),
                        new CraftingIngredient(ItemType.COAL, 2)
                )),
                new BuildingRecipe(BuildingType.STANDING_TORCH, List.of(
                        new CraftingIngredient(ItemType.STICK, 1),
                        new CraftingIngredient(ItemType.COAL, 1)
                )),
                // Adding recipes for other BuildingTypes you provided:
                new BuildingRecipe(BuildingType.CAMPFIRE, List.of(
                        new CraftingIngredient(ItemType.WOOD_LOG, 3),
                        new CraftingIngredient(ItemType.FLINT, 1)
                )),
                new BuildingRecipe(BuildingType.ANVIL, List.of(
                        new CraftingIngredient(ItemType.IRON_INGOT, 5),
                        new CraftingIngredient(ItemType.STONE, 3)
                )),
                new BuildingRecipe(BuildingType.WORKBENCH, List.of(
                        new CraftingIngredient(ItemType.WOOD_PLANK, 4)
                )),
                new BuildingRecipe(BuildingType.BED, List.of(
                        new CraftingIngredient(ItemType.WOOD_PLANK, 3),
                        new CraftingIngredient(ItemType.FIBER, 5)
                ))
            )
        );
    }

    public List<Recipe> getAllRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    /**
     * Retorna una lista inmodificable de todas las recetas de construcción.
     * @return Lista de BuildingRecipe.
     */
    public List<BuildingRecipe> getBuildingRecipes() {
        return Collections.unmodifiableList(buildingRecipes);
    }

    /**
     * Busca y retorna una BuildingRecipe por su tipo de edificio.
     * @param type El tipo de BuildingType a buscar.
     * @return La BuildingRecipe correspondiente, o null si no se encuentra.
     */
    public BuildingRecipe getBuildingRecipeByType(BuildingType type) {
        return buildingRecipes.stream()
                .filter(recipe -> recipe.buildingType() == type)
                .findFirst()
                .orElse(null);
    }

}