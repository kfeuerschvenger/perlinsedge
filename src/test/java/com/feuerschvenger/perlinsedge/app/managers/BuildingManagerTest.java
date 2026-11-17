package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingStats;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
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
        BuildingRecipe recipe = recipes.getBuildingRecipeByType(BuildingType.FENCE).orElseThrow();
        giveIngredients(recipe);
        manager.setSelectedBuildingType(BuildingType.FENCE);

        manager.placeBuilding(1, 1);

        for (CraftingIngredient ingredient : recipe.ingredients()) {
            assertEquals(0, player.getItemCount(new Item(ingredient.itemType(), 1)));
        }
    }

    @Test
    void leavingBuildingModeClearsTheSelection() {
        manager.setSelectedBuildingType(BuildingType.FENCE);

        manager.deactivateBuildingMode();

        assertEquals(null, manager.getSelectedBuildingType());
        assertFalse(manager.isBuildingModeActive());
    }

    @Test
    void comingBackToTheHammerSelectsTheLastPieceAgain() {
        manager.setSelectedBuildingType(BuildingType.CHEST);
        manager.deactivateBuildingMode();

        manager.activateBuildingMode();

        assertEquals(BuildingType.CHEST, manager.getSelectedBuildingType());
    }

    @Test
    void droppingTheSelectionKeepsTheMemoryOfTheLastPiece() {
        manager.setSelectedBuildingType(BuildingType.CHEST);
        manager.setSelectedBuildingType(null);

        assertEquals(null, manager.getSelectedBuildingType());
        manager.deactivateBuildingMode();
        manager.activateBuildingMode();

        assertEquals(BuildingType.CHEST, manager.getSelectedBuildingType());
    }

    @Test
    void nothingIsPlacedWithoutASelectedPiece() {
        manager.setSelectedBuildingType(null);

        manager.placeBuilding(1, 1);

        assertFalse(map.getTile(1, 1).hasBuilding());
    }

    private Building build(BuildingType type, int x, int y) {
        giveIngredients(recipes.getBuildingRecipeByType(type).orElseThrow());
        manager.setSelectedBuildingType(type);
        manager.placeBuilding(x, y);
        return map.getTile(x, y).getBuilding();
    }

    private int held(ItemType type) {
        return player.getItemCount(new Item(type, 1));
    }

    @Test
    void demolishingAFullHealthPieceReturnsTheWholeRecipe() {
        build(BuildingType.BED, 2, 2);
        assertEquals(0, held(ItemType.WOOD_PLANK));

        manager.removeBuilding(2, 2);

        assertFalse(map.getTile(2, 2).hasBuilding());
        assertEquals(3, held(ItemType.WOOD_PLANK));
        assertEquals(5, held(ItemType.FIBER));
    }

    @Test
    void demolishingADamagedPieceReturnsTheShareOfTheHealthLeftRoundedDown() {
        Building bed = build(BuildingType.BED, 2, 2);
        bed.damage(bed.getMaxHealth() / 2);

        manager.removeBuilding(2, 2);

        // Half of 3 planks is 1, half of 5 fiber is 2
        assertEquals(1, held(ItemType.WOOD_PLANK));
        assertEquals(2, held(ItemType.FIBER));
    }

    @Test
    void whatTheInventoryCannotHoldIsDroppedAtThePlayersFeet() {
        build(BuildingType.WORKBENCH, 2, 2);
        for (int i = 0; i < player.getCapacity(); i++) {
            player.setItemAt(i, new Item(ItemType.WOOD_LOG, 1));
        }

        manager.removeBuilding(2, 2);

        assertEquals(0, held(ItemType.WOOD_PLANK));
        assertEquals(1, state.getDroppedItems().size());
        var pile = state.getDroppedItems().get(0);
        assertEquals(ItemType.WOOD_PLANK, pile.getItem().getType());
        assertEquals(4, pile.getItem().getQuantity());
        assertEquals(player.getCurrentTileX(), pile.getTileX());
        assertEquals(player.getCurrentTileY(), pile.getTileY());
    }

    @Test
    void aPlacedPieceStartsAtFullHealth() {
        Building wall = build(BuildingType.FENCE, 3, 3);

        assertEquals(BuildingStats.maxHealth(BuildingType.FENCE), wall.getHealth());
        assertFalse(wall.isDamaged());
    }

    @Test
    void rotatingTurnsTheSelectedPieceThroughItsWaysAndBackToTheFirst() {
        manager.setSelectedBuildingType(BuildingType.CHEST);

        assertEquals(0, manager.getRotation(BuildingType.CHEST));
        for (int expected : new int[]{1, 2, 3, 0}) {
            assertTrue(manager.rotateSelected());
            assertEquals(expected, manager.getRotation(BuildingType.CHEST));
        }
    }

    @Test
    void aTwoWayPieceTurnsBetweenItsTwoAxes() {
        manager.setSelectedBuildingType(BuildingType.BRIDGE);

        manager.rotateSelected();
        assertEquals(1, manager.getRotation(BuildingType.BRIDGE));
        manager.rotateSelected();
        assertEquals(0, manager.getRotation(BuildingType.BRIDGE));
    }

    @Test
    void aPieceThatDoesNotTurnIgnoresTheRotateKey() {
        manager.setSelectedBuildingType(BuildingType.CAMPFIRE);
        assertFalse(manager.rotateSelected());

        manager.setSelectedBuildingType(BuildingType.FENCE);
        assertFalse(manager.rotateSelected());
        manager.setSelectedBuildingType(null);
        assertFalse(manager.rotateSelected());
    }

    @Test
    void theRotationIsRememberedPerPieceTypeWhileTheHammerStaysInHand() {
        manager.setSelectedBuildingType(BuildingType.CHEST);
        manager.rotateSelected();
        manager.rotateSelected();
        manager.setSelectedBuildingType(BuildingType.BED);
        manager.rotateSelected();

        manager.setSelectedBuildingType(BuildingType.CHEST);
        assertEquals(2, manager.getRotation(BuildingType.CHEST), "each type keeps its own");
        assertEquals(1, manager.getRotation(BuildingType.BED));
        manager.setSelectedBuildingType(null);
        assertEquals(2, manager.getRotation(BuildingType.CHEST), "dropping the selection keeps it");
    }

    @Test
    void theRotationsAreForgottenWhenTheHammerLeavesTheHand() {
        manager.setSelectedBuildingType(BuildingType.CHEST);
        manager.rotateSelected();

        manager.deactivateBuildingMode();
        manager.activateBuildingMode();

        assertEquals(0, manager.getRotation(BuildingType.CHEST));
    }

    @Test
    void aPlacedPieceKeepsTheRotationItWasPlacedWith() {
        manager.setSelectedBuildingType(BuildingType.CHEST);
        manager.rotateSelected();
        manager.rotateSelected();
        manager.rotateSelected();

        Building chest = build(BuildingType.CHEST, 2, 2);

        assertEquals(3, chest.getRotation());
        manager.rotateSelected();
        assertEquals(3, chest.getRotation(), "turning the next piece does not turn the placed one");
    }
}
