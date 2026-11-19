package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
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

/** The cobblestone path is a cosmetic floor on walkable land: placement, what can stand on it, and demolition. */
class CobblestonePathTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 8;
    private static final int FIRST_WATER_COLUMN = 5;
    private static final int LAND_X = 2;
    private static final int LAND_Y = 2;

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
        map = new TileMap(WIDTH, HEIGHT);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                map.setTile(x, y, new Tile(x, y, x >= FIRST_WATER_COLUMN ? TileType.SHALLOW_WATER : TileType.GRASS));
            }
        }
        player = new Player(0, 0);
        state.setCurrentMap(map);
        state.setPlayer(player);
        manager.activateBuildingMode();
    }

    private void give(BuildingType type) {
        for (CraftingIngredient ingredient : recipes.getBuildingRecipeByType(type).orElseThrow().ingredients()) {
            player.addItem(new Item(ingredient.itemType(), ingredient.quantity()));
        }
    }

    private boolean place(BuildingType type, int x, int y) {
        give(type);
        manager.setSelectedBuildingType(type);
        return manager.placeBuilding(x, y);
    }

    private int bricks() {
        return player.getItemCount(new Item(ItemType.STONE_BRICK, 1));
    }

    @Test
    void aPathCostsOneStoneBrick() {
        final var ingredients = recipes.getBuildingRecipeByType(BuildingType.COBBLESTONE_PATH).orElseThrow().ingredients();

        assertEquals(1, ingredients.size());
        assertEquals(ItemType.STONE_BRICK, ingredients.getFirst().itemType());
        assertEquals(1, ingredients.getFirst().quantity());
    }

    @Test
    void aPathIsAFloorLaidOnWalkableLandAndTheTileStaysWalkable() {
        assertTrue(place(BuildingType.COBBLESTONE_PATH, LAND_X, LAND_Y));

        final Tile tile = map.getTile(LAND_X, LAND_Y);
        assertTrue(tile.hasFloor());
        assertFalse(tile.hasBuilding());
        assertTrue(tile.isWalkable());
        assertEquals(0, bricks(), "the brick was spent");
    }

    @Test
    void aPathIsNotPlacedOnWaterOnABridgeOrOnAnotherPath() {
        assertFalse(place(BuildingType.COBBLESTONE_PATH, FIRST_WATER_COLUMN, LAND_Y), "water");
        place(BuildingType.BRIDGE, FIRST_WATER_COLUMN, LAND_Y);
        assertFalse(place(BuildingType.COBBLESTONE_PATH, FIRST_WATER_COLUMN, LAND_Y), "a bridge");
        place(BuildingType.COBBLESTONE_PATH, LAND_X, LAND_Y);
        assertFalse(place(BuildingType.COBBLESTONE_PATH, LAND_X, LAND_Y), "another path");
    }

    @Test
    void aPathIsNotPlacedUnderAPieceThatAlreadyStandsThere() {
        place(BuildingType.CHEST, LAND_X, LAND_Y);

        assertFalse(place(BuildingType.COBBLESTONE_PATH, LAND_X, LAND_Y));
    }

    @Test
    void everyPieceThatStandsOnLandCanStandOnAPath() {
        place(BuildingType.COBBLESTONE_PATH, LAND_X, LAND_Y);
        final Tile tile = map.getTile(LAND_X, LAND_Y);

        for (BuildingType type : new BuildingType[]{BuildingType.CHEST, BuildingType.FENCE, BuildingType.FURNACE,
                BuildingType.CAMPFIRE, BuildingType.STANDING_TORCH, BuildingType.SLEEPING_BAG}) {
            assertTrue(manager.canPlaceOnTile(type, tile), type.name());
        }
        assertFalse(manager.canPlaceOnTile(BuildingType.BRIDGE, tile), "a bridge needs water");
    }

    @Test
    void aPieceOnAPathIsPlacedAsTheBuildingAndTheFloorStays() {
        place(BuildingType.COBBLESTONE_PATH, LAND_X, LAND_Y);

        assertTrue(place(BuildingType.CHEST, LAND_X, LAND_Y));

        final Tile tile = map.getTile(LAND_X, LAND_Y);
        assertNotNull(tile.getBuilding());
        assertEquals(BuildingType.COBBLESTONE_PATH, tile.getFloor().getType());
    }

    @Test
    void demolishingRefundsTheBrickAndWorksUnderTheFeetOfThePlayer() {
        place(BuildingType.COBBLESTONE_PATH, 0, 0);
        assertEquals(0, bricks());

        assertTrue(manager.removeBuilding(0, 0), "the player stands on the path, which stays walkable");

        assertFalse(map.getTile(0, 0).hasFloor());
        assertEquals(1, bricks());
    }

    @Test
    void aPathWithAPieceOnItIsDemolishedOnlyAfterThePiece() {
        place(BuildingType.COBBLESTONE_PATH, LAND_X, LAND_Y);
        place(BuildingType.CHEST, LAND_X, LAND_Y);

        assertTrue(manager.removeBuilding(LAND_X, LAND_Y));
        assertTrue(map.getTile(LAND_X, LAND_Y).hasFloor(), "the piece came down first");
        assertTrue(manager.removeBuilding(LAND_X, LAND_Y));
        assertFalse(map.getTile(LAND_X, LAND_Y).hasFloor());
    }
}
