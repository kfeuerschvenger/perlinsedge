package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceGate;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The placement rules of the pieces and the two layers of a tile: the floor, a bridge, and the piece on top.
 * The map is land in columns 0 to 4 and water in columns 5 to 9.
 */
class BuildingLayersTest {

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
        map = new TileMap(10, 8);
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 8; y++) {
                map.setTile(x, y, new Tile(x, y, x >= 5 ? TileType.SHALLOW_WATER : TileType.GRASS));
            }
        }
        player = new Player(0, 0);
        state.setCurrentMap(map);
        state.setPlayer(player);
        manager.activateBuildingMode();
    }

    private Building build(BuildingType type, int x, int y) {
        for (CraftingIngredient ingredient : recipes.getBuildingRecipeByType(type).orElseThrow().ingredients()) {
            player.addItem(new Item(ingredient.itemType(), ingredient.quantity()));
        }
        manager.setSelectedBuildingType(type);
        manager.placeBuilding(x, y);
        Tile tile = map.getTile(x, y);
        return type.isFloor() ? tile.getFloor() : tile.getBuilding();
    }

    private boolean canPlace(BuildingType type, int x, int y) {
        return manager.canPlaceOnTile(type, map.getTile(x, y));
    }

    private int held(ItemType type) {
        return player.getItemCount(new Item(type, 1));
    }

    @Test
    void aBridgeIsPlacedOnWaterThatSharesAnEdgeWithLand() {
        assertTrue(canPlace(BuildingType.BRIDGE, 5, 2));
    }

    @Test
    void aBridgeCannotFloatOnWaterThatTouchesNothing() {
        assertFalse(canPlace(BuildingType.BRIDGE, 8, 3));
        assertFalse(canPlace(BuildingType.BRIDGE, 6, 3), "one tile from the shore is still floating");
    }

    @Test
    void aBridgeGrowsOutAsAChainOfEdgeNeighbours() {
        build(BuildingType.BRIDGE, 5, 2);

        assertTrue(canPlace(BuildingType.BRIDGE, 6, 2), "it touches the bridge");
        build(BuildingType.BRIDGE, 6, 2);
        assertTrue(canPlace(BuildingType.BRIDGE, 7, 2), "and the chain goes on from the second bridge");
    }

    @Test
    void aDiagonalNeighbourIsNotEnough() {
        build(BuildingType.BRIDGE, 5, 2);

        // (6, 1) and (6, 3) touch the bridge only at a corner, and (5, 1) and (5, 3) touch land across an edge
        assertFalse(canPlace(BuildingType.BRIDGE, 6, 1));
        assertFalse(canPlace(BuildingType.BRIDGE, 6, 3));
        assertTrue(canPlace(BuildingType.BRIDGE, 5, 1), "it shares an edge with land");
        assertTrue(canPlace(BuildingType.BRIDGE, 6, 2));
    }

    @Test
    void aBridgeCannotBeBuiltOnLandOrOnTopOfAFloor() {
        assertFalse(canPlace(BuildingType.BRIDGE, 2, 2));
        build(BuildingType.BRIDGE, 5, 2);

        assertFalse(canPlace(BuildingType.BRIDGE, 5, 2));
    }

    @Test
    void theStandingTorchStandsOnLandOnABridgeButNotOnBareWater() {
        assertTrue(canPlace(BuildingType.STANDING_TORCH, 2, 2));
        assertFalse(canPlace(BuildingType.STANDING_TORCH, 5, 2), "bare water");
        build(BuildingType.BRIDGE, 5, 2);
        assertTrue(canPlace(BuildingType.STANDING_TORCH, 5, 2), "on a bridge");
    }

    @Test
    void theCampfireIsRefusedOnABridgeAndOnWater() {
        build(BuildingType.BRIDGE, 5, 2);

        assertFalse(canPlace(BuildingType.CAMPFIRE, 5, 2));
        assertFalse(canPlace(BuildingType.CAMPFIRE, 7, 2));
        assertTrue(canPlace(BuildingType.CAMPFIRE, 2, 2));
    }

    @Test
    void everyOtherPieceIsRefusedOnAFloorAndOnWaterAndAcceptedOnLand() {
        build(BuildingType.BRIDGE, 5, 2);

        for (BuildingType type : new BuildingType[]{BuildingType.CHEST, BuildingType.FURNACE, BuildingType.ANVIL,
                BuildingType.WORKBENCH, BuildingType.BED, BuildingType.FENCE}) {
            assertFalse(canPlace(type, 5, 2), type + " on a bridge");
            assertFalse(canPlace(type, 7, 4), type + " on water");
            assertTrue(canPlace(type, 2, 2), type + " on land");
        }
    }

    @Test
    void aRefusedPlacementConsumesNothing() {
        player.addItem(new Item(ItemType.WOOD_PLANK, 3));
        manager.setSelectedBuildingType(BuildingType.BED);

        assertFalse(manager.placeBuilding(7, 4));

        assertEquals(3, held(ItemType.WOOD_PLANK));
    }

    @Test
    void aPieceOnABridgeSitsInTheTopLayerAndTheBridgeStaysTheFloor() {
        Building bridge = build(BuildingType.BRIDGE, 5, 2);
        assertNull(map.getTile(5, 2).getBuilding(), "a floor is not the top piece");
        assertEquals(bridge, map.getTile(5, 2).getFloor());

        build(BuildingType.STANDING_TORCH, 5, 2);

        assertEquals(BuildingType.STANDING_TORCH, map.getTile(5, 2).getBuilding().getType());
        assertEquals(BuildingType.BRIDGE, map.getTile(5, 2).getFloor().getType());
    }

    @Test
    void aFloorMakesWaterWalkableAndRemovingItMakesItWaterAgain() {
        assertFalse(map.getTile(5, 2).isWalkable());

        build(BuildingType.BRIDGE, 5, 2);
        assertTrue(map.getTile(5, 2).isWalkable());

        manager.removeBuilding(5, 2);
        assertFalse(map.getTile(5, 2).isWalkable());
    }

    @Test
    void demolishingTakesTheTopPieceFirstAndTheFloorOnlyWhenNothingStandsOnIt() {
        build(BuildingType.BRIDGE, 5, 2);
        build(BuildingType.STANDING_TORCH, 5, 2);

        assertTrue(manager.removeBuilding(5, 2));
        assertFalse(map.getTile(5, 2).hasBuilding(), "the torch came down");
        assertTrue(map.getTile(5, 2).hasFloor(), "the bridge stays");
        assertTrue(manager.removeBuilding(5, 2));
        assertFalse(map.getTile(5, 2).hasFloor());
    }

    @Test
    void aFloorWithAPieceOnItRefusesToBeDemolishedAndSaysWhy() {
        List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        build(BuildingType.BRIDGE, 5, 2);
        build(BuildingType.STANDING_TORCH, 5, 2);

        assertFalse(manager.removeFloor(5, 2));

        assertTrue(map.getTile(5, 2).hasFloor());
        assertEquals(1, hints.size());
    }

    @Test
    void demolishingAFloorRefundsItsCost() {
        build(BuildingType.BRIDGE, 5, 2);
        assertEquals(0, held(ItemType.WOOD_PLANK));

        manager.removeBuilding(5, 2);

        assertEquals(10, held(ItemType.WOOD_PLANK));
    }

    @Test
    void demolishingAChestDropsItsContentsAsPilesOnItsTile() {
        Chest chest = (Chest) build(BuildingType.CHEST, 2, 3);
        chest.setItemAt(0, new Item(ItemType.STONE, 5));
        chest.setItemAt(4, new Item(ItemType.COAL, 3));

        manager.removeBuilding(2, 3);

        var piles = state.getDroppedItems();
        assertEquals(2, piles.size());
        for (var pile : piles) {
            assertEquals(2, pile.getTileX());
            assertEquals(3, pile.getTileY());
        }
        assertEquals(5, piles.stream().filter(p -> p.getItem().getType() == ItemType.STONE)
                .mapToInt(p -> p.getItem().getQuantity()).sum());
        assertEquals(3, piles.stream().filter(p -> p.getItem().getType() == ItemType.COAL)
                .mapToInt(p -> p.getItem().getQuantity()).sum());
        assertEquals(6, held(ItemType.WOOD_PLANK), "the refund still goes to the player");
    }

    @Test
    void anEmptyChestDropsNothing() {
        build(BuildingType.CHEST, 2, 3);

        manager.removeBuilding(2, 3);

        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void placingAndRemovingFencesKeepsTheirSidesUpToDate() {
        Building left = build(BuildingType.FENCE, 2, 3);
        Building right = build(BuildingType.FENCE, 3, 3);

        assertEquals(com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.LOWER_RIGHT,
                ((com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence) left).getConnections());
        assertEquals(com.feuerschvenger.perlinsedge.domain.entities.buildings.FenceConnections.UPPER_LEFT,
                ((com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence) right).getConnections());

        manager.removeBuilding(3, 3);

        assertEquals(0, ((com.feuerschvenger.perlinsedge.domain.entities.buildings.Fence) left).getConnections());
    }

    @Test
    void aGateIsPlacedOnLandWithItsRotationAndRefusedOnWater() {
        manager.setSelectedBuildingType(BuildingType.FENCE_GATE);
        manager.rotateSelected();

        Building gate = build(BuildingType.FENCE_GATE, 2, 3);

        assertEquals(BuildingType.FENCE_GATE, gate.getType());
        assertEquals(1, gate.getRotation());
        assertFalse(canPlace(BuildingType.FENCE_GATE, 5, 2));
    }

    @Test
    void placingAGateConsumesExactlyTheIngredientsOfItsRecipe() {
        final var ingredients = recipes.getBuildingRecipeByType(BuildingType.FENCE_GATE).orElseThrow().ingredients();
        for (CraftingIngredient ingredient : ingredients) {
            player.addItem(new Item(ingredient.itemType(), ingredient.quantity() + 1));
        }
        manager.setSelectedBuildingType(BuildingType.FENCE_GATE);

        assertTrue(manager.placeBuilding(2, 3));

        for (CraftingIngredient ingredient : ingredients) {
            assertEquals(1, held(ingredient.itemType()), "only the spare " + ingredient.itemType() + " is left");
        }
    }

    // ---- the player or an enemy in the tile ----

    private void give(BuildingType type) {
        for (CraftingIngredient ingredient : recipes.getBuildingRecipeByType(type).orElseThrow().ingredients()) {
            player.addItem(new Item(ingredient.itemType(), ingredient.quantity()));
        }
    }

    private void standPlayerOn(int x, int y) {
        player.setCurrentTileX(x);
        player.setCurrentTileY(y);
        player.setTargetTileX(x);
        player.setTargetTileY(y);
    }

    private Slime slimeOn(int x, int y) {
        final Slime slime = new Slime(x, y);
        state.setEnemies(List.of(slime));
        return slime;
    }

    @Test
    void aSolidPieceIsRefusedOnTheTileThePlayerStandsOnAndSaysSo() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        standPlayerOn(2, 2);
        final BuildingType[] solidPieces = {BuildingType.FENCE, BuildingType.FENCE_GATE, BuildingType.CHEST,
                BuildingType.FURNACE, BuildingType.WORKBENCH, BuildingType.BED, BuildingType.ANVIL};
        for (BuildingType type : solidPieces) {
            give(type);
            manager.setSelectedBuildingType(type);

            assertEquals(Rgba.RED.r(), manager.getPlacementPreviewColor(2, 2).r(), 1e-9, type + " ghost");
            assertFalse(manager.placeBuilding(2, 2), type + " on the player");
        }

        assertEquals(solidPieces.length, hints.size());
        assertFalse(map.getTile(2, 2).hasBuilding());
    }

    @Test
    void aSolidPieceIsRefusedOnTheTileAnEnemyStandsOnOrIsSteppingTo() {
        final Slime slime = slimeOn(3, 3);
        give(BuildingType.FENCE);
        give(BuildingType.FENCE);
        give(BuildingType.FENCE);
        manager.setSelectedBuildingType(BuildingType.FENCE);

        assertFalse(manager.placeBuilding(3, 3), "where the enemy stands");

        slime.setTargetTileX(3);
        slime.setTargetTileY(4);
        assertFalse(manager.placeBuilding(3, 4), "where the enemy is stepping to");
        assertTrue(manager.placeBuilding(4, 4), "a free tile is still fine");
    }

    @Test
    void aDeadEnemyDoesNotBlockAPlacement() {
        final Slime slime = slimeOn(3, 3);
        slime.takeDamage(Integer.MAX_VALUE);
        give(BuildingType.FENCE);
        manager.setSelectedBuildingType(BuildingType.FENCE);

        assertTrue(manager.placeBuilding(3, 3));
    }

    @Test
    void aPieceThatDoesNotBlockWalkingCanStandUnderTheWalker() {
        standPlayerOn(2, 2);
        give(BuildingType.CAMPFIRE);
        manager.setSelectedBuildingType(BuildingType.CAMPFIRE);

        assertTrue(manager.placeBuilding(2, 2));
    }

    @Test
    void aBridgeIsNotDemolishedWhileThePlayerIsOnItAndSaysSo() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        build(BuildingType.BRIDGE, 5, 2);
        standPlayerOn(5, 2);

        assertFalse(manager.removeFloor(5, 2));

        assertTrue(map.getTile(5, 2).hasFloor());
        assertEquals(1, hints.size());
        standPlayerOn(0, 0);
        assertTrue(manager.removeFloor(5, 2));
    }

    @Test
    void aBridgeIsNotDemolishedWhileItIsAnywhereOnThePlayersRemainingWalk() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        build(BuildingType.BRIDGE, 5, 2);
        build(BuildingType.BRIDGE, 6, 2);
        player = new Player(4, 2);
        state.setPlayer(player);
        assertTrue(player.moveTo(map, 6, 2), "the walk crosses both bridges");

        assertFalse(manager.removeFloor(6, 2), "the far end of the walk, not even the next tile");
        assertFalse(manager.removeFloor(5, 2), "the next tile of the walk");

        assertTrue(map.getTile(6, 2).hasFloor());
        assertTrue(map.getTile(5, 2).hasFloor());
        assertEquals(List.of("Something is standing there", "Something is standing there"), hints);
        player.stop();
        assertTrue(manager.removeFloor(6, 2), "it can go once the walk is over");
    }

    @Test
    void theRealReasonIsShownBeforeSomethingIsStandingThere() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        standPlayerOn(2, 2);
        manager.setSelectedBuildingType(BuildingType.CHEST);

        assertFalse(manager.placeBuilding(2, 2), "no materials, and the player in the way");
        assertTrue(hints.isEmpty(), "the missing materials are the reason, not the player");

        give(BuildingType.CHEST);
        assertFalse(manager.placeBuilding(2, 2));
        assertEquals(List.of("Something is standing there"), hints);
    }

    @Test
    void badTerrainIsReportedBeforeSomethingIsStandingThere() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        standPlayerOn(8, 5);
        give(BuildingType.CHEST);
        manager.setSelectedBuildingType(BuildingType.CHEST);

        assertFalse(manager.placeBuilding(8, 5), "a chest does not stand on water");

        assertTrue(hints.isEmpty());
    }

    @Test
    void aBridgeIsNotDemolishedWhileAnEnemyIsSteppingToIt() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        build(BuildingType.BRIDGE, 5, 2);
        final Slime slime = slimeOn(4, 2);
        slime.setTargetTileX(5);
        slime.setTargetTileY(2);

        assertFalse(manager.removeFloor(5, 2));

        assertTrue(map.getTile(5, 2).hasFloor());
        assertEquals(1, hints.size());
        state.setEnemies(List.of());
        assertTrue(manager.removeFloor(5, 2));
    }

    @Test
    void theRefundOfABridgeIsNotDroppedOnWater() {
        build(BuildingType.BRIDGE, 5, 2);
        // The inventory is full of something else, so the whole refund is left over, and the player stands in water
        while (player.addItem(new Item(ItemType.STONE, ItemType.STONE.getMaxStackSize()))) {
            // keeps filling until no slot is left
        }
        standPlayerOn(8, 5);

        assertTrue(manager.removeFloor(5, 2));

        assertFalse(state.getDroppedItems().isEmpty());
        for (var pile : state.getDroppedItems()) {
            assertTrue(map.getTile(pile.getTileX(), pile.getTileY()).isWalkable(),
                    "the pile lies on (" + pile.getTileX() + ", " + pile.getTileY() + ")");
        }
    }

    @Test
    void aGateIsNotClosedOnThePlayerAndOpensFreely() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        final FenceGate gate = (FenceGate) build(BuildingType.FENCE_GATE, 2, 3);
        standPlayerOn(2, 3);

        assertTrue(manager.interactWithBuilding(2, 3), "opening it is allowed with the player in the tile");
        assertTrue(gate.isOpen());
        assertFalse(manager.interactWithBuilding(2, 3), "closing it on the player is refused");

        assertTrue(gate.isOpen());
        assertEquals(1, hints.size());
        standPlayerOn(0, 0);
        assertTrue(manager.interactWithBuilding(2, 3), "it closes once the tile is free");
        assertFalse(gate.isOpen());
    }

    @Test
    void aGateIsNotClosedOnAnEnemy() {
        final List<String> hints = new ArrayList<>();
        manager.setHintSink(hints::add);
        final FenceGate gate = (FenceGate) build(BuildingType.FENCE_GATE, 2, 3);
        manager.interactWithBuilding(2, 3);
        final Slime slime = slimeOn(1, 3);
        slime.setTargetTileX(2);
        slime.setTargetTileY(3);

        assertFalse(manager.interactWithBuilding(2, 3));

        assertTrue(gate.isOpen());
        assertEquals(1, hints.size());
    }

    @Test
    void theWallAndTheDoorAreGone() {
        for (BuildingType type : BuildingType.values()) {
            assertFalse(type.name().equals("WALL") || type.name().equals("DOOR"), type.name());
        }
        for (var recipe : recipes.getAllBuildingRecipes()) {
            assertFalse(recipe.buildingType().name().equals("WALL") || recipe.buildingType().name().equals("DOOR"));
        }
    }
}
