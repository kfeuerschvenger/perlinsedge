package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
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
 * The placement rules of the pieces and the two layers of a tile: the floor, a bridge or a dock, and the piece on top.
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
    void aBridgeOrDockIsPlacedOnWaterThatSharesAnEdgeWithLand() {
        assertTrue(canPlace(BuildingType.BRIDGE, 5, 2));
        assertTrue(canPlace(BuildingType.DOCK, 5, 2));
    }

    @Test
    void aBridgeOrDockCannotFloatOnWaterThatTouchesNothing() {
        assertFalse(canPlace(BuildingType.BRIDGE, 8, 3));
        assertFalse(canPlace(BuildingType.DOCK, 8, 3));
        assertFalse(canPlace(BuildingType.BRIDGE, 6, 3), "one tile from the shore is still floating");
    }

    @Test
    void aFloorGrowsOutAsAChainOfEdgeNeighbours() {
        build(BuildingType.BRIDGE, 5, 2);

        assertTrue(canPlace(BuildingType.BRIDGE, 6, 2), "it touches the bridge");
        assertTrue(canPlace(BuildingType.DOCK, 6, 2), "a dock continues a bridge too");
        build(BuildingType.DOCK, 6, 2);
        assertTrue(canPlace(BuildingType.BRIDGE, 7, 2), "and a bridge continues a dock");
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
        assertFalse(canPlace(BuildingType.DOCK, 2, 2));
        build(BuildingType.BRIDGE, 5, 2);

        assertFalse(canPlace(BuildingType.BRIDGE, 5, 2));
        assertFalse(canPlace(BuildingType.DOCK, 5, 2));
    }

    @Test
    void theStandingTorchStandsOnLandOnABridgeAndOnADockButNotOnBareWater() {
        assertTrue(canPlace(BuildingType.STANDING_TORCH, 2, 2));
        assertFalse(canPlace(BuildingType.STANDING_TORCH, 5, 2), "bare water");
        build(BuildingType.BRIDGE, 5, 2);
        assertTrue(canPlace(BuildingType.STANDING_TORCH, 5, 2), "on a bridge");
        build(BuildingType.DOCK, 5, 3);
        assertTrue(canPlace(BuildingType.STANDING_TORCH, 5, 3), "on a dock");
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
}
