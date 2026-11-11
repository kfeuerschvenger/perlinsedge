package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingManagerTest {

    private GameStateManager state;
    private RecipeManager recipes;
    private BuildingManager manager;
    private Player player;
    private TileMap map;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        recipes = new RecipeManager();
        manager = new BuildingManager(state, recipes, new ContainerManager());
        map = new TileMap(6, 6);
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                map.setTile(x, y, new Tile(x, y, x == 5 ? TileType.SHALLOW_WATER : TileType.GRASS));
            }
        }
        player = new Player(0, 0);
        state.setCurrentMap(map);
        state.setPlayer(player);
        manager.activateBuildingMode();
    }

    private void giveIngredients(BuildingRecipe recipe) {
        for (CraftingIngredient ingredient : recipe.ingredients()) {
            player.addItem(new Item(ingredient.itemType(), ingredient.quantity()));
        }
    }

    @Test
    void everyRecipeBuildingTypeCanBePlaced() {
        for (BuildingRecipe recipe : recipes.getAllBuildingRecipes()) {
            BuildingType type = recipe.buildingType();
            boolean water = type == BuildingType.BRIDGE || type == BuildingType.DOCK;
            int x = water ? 5 : 2;
            map.getTile(x, 2).setBuilding(null);
            giveIngredients(recipe);
            manager.setSelectedBuildingType(type);

            manager.placeBuilding(x, 2);

            Building placed = map.getTile(x, 2).getBuilding();
            assertNotNull(placed, type + " has a recipe but cannot be built");
            assertEquals(type, placed.getType());
        }
    }

    @Test
    void standingTorchDoesNotBlockMovementAndCanBeRemoved() {
        BuildingRecipe recipe = recipes.getBuildingRecipeByType(BuildingType.STANDING_TORCH).orElseThrow();
        giveIngredients(recipe);
        manager.setSelectedBuildingType(BuildingType.STANDING_TORCH);

        manager.placeBuilding(3, 3);

        assertTrue(map.getTile(3, 3).isWalkable());
        manager.removeBuilding(3, 3);
        assertFalse(map.getTile(3, 3).hasBuilding());
    }

    @Test
    void placingConsumesTheIngredients() {
        BuildingRecipe recipe = recipes.getBuildingRecipeByType(BuildingType.WALL).orElseThrow();
        giveIngredients(recipe);
        manager.setSelectedBuildingType(BuildingType.WALL);

        manager.placeBuilding(1, 1);

        for (CraftingIngredient ingredient : recipe.ingredients()) {
            assertEquals(0, player.getItemCount(new Item(ingredient.itemType(), 1)));
        }
    }

    @Test
    void leavingBuildingModeClearsTheSelection() {
        manager.setSelectedBuildingType(BuildingType.WALL);

        manager.deactivateBuildingMode();

        assertEquals(null, manager.getSelectedBuildingType());
        assertFalse(manager.isBuildingModeActive());
    }
}
