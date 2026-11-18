package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.BuildingRecipe;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rules of the building manager that the placement tests do not reach: rotation, demolition, terrain and preview. */
class BuildingManagerRulesTest {

    private static final int SIZE = 8;

    private GameStateManager state;
    private RecipeManager recipes;
    private ContainerManager containers;
    private BuildingManager manager;
    private Player player;
    private TileMap map;
    private final List<String> hints = new ArrayList<>();
    private int inventoryChanges;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        recipes = new RecipeManager();
        containers = new ContainerManager();
        manager = new BuildingManager(state, recipes, containers, new ItemDropManager(state, () -> { }),
                () -> inventoryChanges++);
        manager.setHintSink(hints::add);
        map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, x >= SIZE - 2 ? TileType.SHALLOW_WATER : TileType.GRASS));
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

    private BuildingType rotatable() {
        for (BuildingType type : BuildingType.values()) {
            if (type.isRotatable()) {
                return type;
            }
        }
        throw new AssertionError("no rotatable building type");
    }

    private BuildingType fixed() {
        for (BuildingType type : BuildingType.values()) {
            if (!type.isRotatable()) {
                return type;
            }
        }
        throw new AssertionError("no fixed building type");
    }

    // ---- rotation ----

    @Test
    void rotatingTurnsThePieceThroughEveryFacingAndBackToTheFirst() {
        final BuildingType type = rotatable();
        manager.setSelectedBuildingType(type);

        for (int turn = 1; turn <= type.rotationCount(); turn++) {
            assertTrue(manager.rotateSelected());
            assertEquals(turn % type.rotationCount(), manager.getRotation(type));
        }
    }

    @Test
    void aPieceThatCannotTurnOrNoPieceAtAllIsNotRotated() {
        manager.setSelectedBuildingType(fixed());
        assertFalse(manager.rotateSelected());
        assertEquals(0, manager.getRotation(fixed()));

        manager.setSelectedBuildingType(null);
        assertFalse(manager.rotateSelected());
        assertEquals(0, manager.getRotation(null));
    }

    @Test
    void nothingTurnsOutsideBuildingModeAndLeavingForgetsTheTurns() {
        final BuildingType type = rotatable();
        manager.setSelectedBuildingType(type);
        manager.rotateSelected();
        assertEquals(1, manager.getRotation(type));

        manager.deactivateBuildingMode();
        assertEquals(0, manager.getRotation(type));
        assertFalse(manager.rotateSelected());
    }

    @Test
    void activatingTwiceKeepsTheSelectionAndDeactivatingTwiceIsHarmless() {
        manager.setSelectedBuildingType(BuildingType.CHEST);

        manager.activateBuildingMode();
        assertEquals(BuildingType.CHEST, manager.getSelectedBuildingType());

        manager.deactivateBuildingMode();
        manager.deactivateBuildingMode();
        assertFalse(manager.isBuildingModeActive());
    }

    // ---- lookup ----

    @Test
    void theBuildingAtATileIsFoundAndOutsideTheMapThereIsNone() {
        final Building chest = BuildingFactory.create(BuildingType.CHEST, 2, 2);
        map.getTile(2, 2).setBuilding(chest);

        assertSame(chest, manager.getBuildingAt(2, 2));
        assertNull(manager.getBuildingAt(3, 3));
        assertNull(manager.getBuildingAt(-1, 0));
        state.setCurrentMap(null);
        assertNull(manager.getBuildingAt(2, 2));
    }

    // ---- demolition ----

    @Test
    void demolishingNeedsAMapAndAPieceOnTheTile() {
        assertFalse(manager.removeBuilding(1, 1));
        assertFalse(manager.removeBuilding(-4, 1));

        state.setCurrentMap(null);
        assertFalse(manager.removeBuilding(1, 1));
    }

    @Test
    void aBridgeFloorIsDemolishedAndRefundedWhenNothingStandsOnIt() {
        final Tile water = map.getTile(SIZE - 1, 3);
        water.setFloor(BuildingFactory.create(BuildingType.BRIDGE, SIZE - 1, 3));

        assertTrue(manager.removeBuilding(SIZE - 1, 3));

        assertFalse(water.hasFloor());
        assertTrue(inventoryChanges > 0);
    }

    @Test
    void aFloorWithAPieceOnItStaysAndTheHintSaysWhy() {
        final Tile tile = map.getTile(SIZE - 1, 3);
        tile.setFloor(BuildingFactory.create(BuildingType.BRIDGE, SIZE - 1, 3));
        tile.setBuilding(BuildingFactory.create(BuildingType.STANDING_TORCH, SIZE - 1, 3));

        assertFalse(manager.removeFloor(SIZE - 1, 3));

        assertTrue(tile.hasFloor());
        assertEquals(List.of("Remove what stands on it first"), hints);
    }

    @Test
    void removingAFloorThatIsNotThereIsRefused() {
        assertFalse(manager.removeFloor(1, 1));
        assertFalse(manager.removeFloor(-1, 1));
        state.setCurrentMap(null);
        assertFalse(manager.removeFloor(1, 1));
    }

    @Test
    void aDemolishedChestSpillsItsContentsOnItsTile() {
        final Chest chest = (Chest) BuildingFactory.create(BuildingType.CHEST, 3, 3);
        chest.setItemAt(0, new Item(ItemType.STONE, 4));
        map.getTile(3, 3).setBuilding(chest);

        assertTrue(manager.removeBuilding(3, 3));

        assertTrue(state.getDroppedItems().stream()
                        .anyMatch(drop -> drop.getItemType() == ItemType.STONE && drop.getTileX() == 3
                                && drop.getTileY() == 3 && drop.getQuantity() == 4),
                "the stone lies where the chest stood");
    }

    @Test
    void demolishingWithoutAPlayerStillRemovesThePieceButRefundsNothing() {
        map.getTile(2, 2).setBuilding(BuildingFactory.create(BuildingType.BED, 2, 2));
        state.setPlayer(null);

        assertTrue(manager.removeBuilding(2, 2));

        assertFalse(map.getTile(2, 2).hasBuilding());
    }

    // ---- interaction ----

    @Test
    void interactingNeedsAMapAndAPieceAndOpensTheContainerOfAChest() {
        assertFalse(manager.interactWithBuilding(1, 1));
        assertFalse(manager.interactWithBuilding(-3, 1));

        final Chest chest = (Chest) BuildingFactory.create(BuildingType.CHEST, 2, 2);
        map.getTile(2, 2).setBuilding(chest);
        assertTrue(manager.interactWithBuilding(2, 2));
        assertSame(chest, containers.getOpenContainer());

        state.setCurrentMap(null);
        assertFalse(manager.interactWithBuilding(2, 2));
    }

    @Test
    void aPieceThatDoesNothingOnInteractionOpensNoContainer() {
        map.getTile(2, 2).setBuilding(BuildingFactory.create(BuildingType.FENCE, 2, 2));

        assertFalse(manager.interactWithBuilding(2, 2));
        assertFalse(containers.isContainerOpen());
    }

    // ---- terrain rules ----

    @Test
    void aBridgeOnlyGrowsOutFromTheShoreOverFreeWater() {
        final Tile shoreWater = map.getTile(SIZE - 2, 3);
        final Tile farWater = map.getTile(SIZE - 1, 3);
        final Tile land = map.getTile(1, 1);

        assertTrue(manager.canPlaceOnTile(BuildingType.BRIDGE, shoreWater), "water next to land");
        assertFalse(manager.canPlaceOnTile(BuildingType.BRIDGE, farWater), "water with no land or bridge beside it");
        assertFalse(manager.canPlaceOnTile(BuildingType.BRIDGE, land), "land is not bridged");

        shoreWater.setFloor(BuildingFactory.create(BuildingType.BRIDGE, SIZE - 2, 3));
        assertFalse(manager.canPlaceOnTile(BuildingType.BRIDGE, shoreWater), "already bridged");
        assertTrue(manager.canPlaceOnTile(BuildingType.BRIDGE, farWater), "now it touches a bridge");
    }

    @Test
    void aBridgeTileCannotBeJudgedWithoutAMap() {
        final Tile water = map.getTile(SIZE - 2, 3);
        state.setCurrentMap(null);

        assertFalse(manager.canPlaceOnTile(BuildingType.BRIDGE, water));
    }

    @Test
    void solidPiecesStandOnWalkableLandNeverOnWaterOrAFloor() {
        final Tile land = map.getTile(1, 1);
        final Tile water = map.getTile(SIZE - 1, 1);

        assertTrue(manager.canPlaceOnTile(BuildingType.CHEST, land));
        assertFalse(manager.canPlaceOnTile(BuildingType.CHEST, water));

        land.setFloor(BuildingFactory.create(BuildingType.BRIDGE, 1, 1));
        assertFalse(manager.canPlaceOnTile(BuildingType.CHEST, land), "no chest on a floor");
    }

    @Test
    void theStandingTorchAlsoStandsOnAFloor() {
        final Tile water = map.getTile(SIZE - 2, 1);
        assertFalse(manager.canPlaceOnTile(BuildingType.STANDING_TORCH, water));

        water.setFloor(BuildingFactory.create(BuildingType.BRIDGE, SIZE - 2, 1));

        assertTrue(manager.canPlaceOnTile(BuildingType.STANDING_TORCH, water));
    }

    @Test
    void nothingIsBuiltOnAMissingTileOrOnAnOccupiedOne() {
        final Tile taken = map.getTile(2, 2);
        taken.setBuilding(BuildingFactory.create(BuildingType.CHEST, 2, 2));

        assertFalse(manager.canPlaceOnTile(BuildingType.CHEST, null));
        assertFalse(manager.canPlaceOnTile(BuildingType.CHEST, taken));
    }

    // ---- preview colour ----

    private Rgba preview(int x, int y) {
        return manager.getPlacementPreviewColor(x, y);
    }

    @Test
    void thePreviewIsAbsentOutsideBuildingModeAndYellowWithNoPieceChosen() {
        manager.setSelectedBuildingType(null);
        final Rgba yellow = preview(1, 1);
        assertNotNull(yellow);
        assertEquals(Rgba.YELLOW.r(), yellow.r(), 1e-9);
        assertEquals(Rgba.YELLOW.g(), yellow.g(), 1e-9);

        manager.deactivateBuildingMode();
        assertNull(preview(1, 1));
    }

    @Test
    void thePreviewIsRedWhereThePieceCannotGo() {
        manager.setSelectedBuildingType(BuildingType.CHEST);
        final Rgba red = Rgba.RED;

        assertEquals(red.r(), preview(SIZE - 1, 1).r(), 1e-9, "water");
        assertEquals(red.r(), preview(-1, 1).r(), 1e-9, "outside the map");
        map.getTile(1, 1).setBuilding(BuildingFactory.create(BuildingType.FENCE, 1, 1));
        assertEquals(red.r(), preview(1, 1).r(), 1e-9, "occupied");
        state.setPlayer(null);
        assertEquals(red.r(), preview(2, 2).r(), 1e-9, "no player");
        state.setCurrentMap(null);
        assertEquals(red.r(), preview(2, 2).r(), 1e-9, "no map");
    }

    @Test
    void thePreviewIsOrangeWithoutTheIngredientsAndGreenWithThem() {
        manager.setSelectedBuildingType(BuildingType.CHEST);
        final Rgba orange = preview(2, 2);
        assertEquals(Rgba.ORANGE.g(), orange.g(), 1e-9);

        final BuildingRecipe recipe = recipes.getBuildingRecipeByType(BuildingType.CHEST).orElseThrow();
        give(BuildingType.CHEST);
        assertTrue(recipe.ingredients().size() > 0);

        final Rgba green = preview(2, 2);
        assertEquals(Rgba.LIME_GREEN.g(), green.g(), 1e-9);
        assertTrue(green.a() < 1.0, "the preview is translucent");
    }

    @Test
    void aDemolitionTellsTheViewsExactlyOnce() {
        final Tile water = map.getTile(SIZE - 1, 3);
        water.setFloor(BuildingFactory.create(BuildingType.BRIDGE, SIZE - 1, 3));

        manager.removeBuilding(SIZE - 1, 3);

        assertEquals(1, inventoryChanges);
    }

    @Test
    void aChestDemolitionTellsTheViewsExactlyOnceToo() {
        final Chest chest = (Chest) BuildingFactory.create(BuildingType.CHEST, 2, 2);
        chest.setItemAt(0, new Item(ItemType.STONE, 5));
        map.getTile(2, 2).setBuilding(chest);

        manager.removeBuilding(2, 2);

        assertEquals(1, inventoryChanges);
    }

    @Test
    void whatTheInventoryCannotHoldOfARefundIsDroppedAsAPile() {
        while (player.addItem(new Item(ItemType.STONE, ItemType.STONE.getMaxStackSize()))) {
            // keeps filling until no slot is left
        }
        map.getTile(2, 2).setBuilding(BuildingFactory.create(BuildingType.CHEST, 2, 2));

        manager.removeBuilding(2, 2);

        final int refunded = recipes.getBuildingRecipeByType(BuildingType.CHEST).orElseThrow().ingredients().stream()
                .mapToInt(CraftingIngredient::quantity).sum();
        assertEquals(refunded, state.getDroppedItems().stream().mapToInt(pile -> pile.getItem().getQuantity()).sum());
    }
}
