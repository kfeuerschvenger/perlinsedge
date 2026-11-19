package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.config.RecipeManager;
import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Building;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.ProcessingStation;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.light.BuildingLight;
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
 * The processing stations as buildings: placing and demolishing them, and loading them with a click.
 */
class BuildingManagerStationTest {
    private static final int STATION_X = 2;
    private static final int STATION_Y = 2;

    private GameStateManager state;
    private RecipeManager recipes;
    private BuildingManager manager;
    private Player player;
    private TileMap map;
    private final List<String> hints = new ArrayList<>();

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        recipes = new RecipeManager();
        manager = new BuildingManager(state, recipes, new ContainerManager(), new ItemDropManager(state, () -> { }),
                () -> { });
        manager.setHintSink(hints::add);
        map = new TileMap(6, 6);
        for (var x = 0; x < 6; x++) {
            for (var y = 0; y < 6; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(0, 0);
        state.setCurrentMap(map);
        state.setPlayer(player);
        manager.activateBuildingMode();
    }

    private ProcessingStation build(BuildingType type) {
        for (CraftingIngredient ingredient : recipes.getBuildingRecipeByType(type).orElseThrow().ingredients()) {
            player.addItem(new Item(ingredient.itemType(), ingredient.quantity()));
        }
        manager.setSelectedBuildingType(type);
        assertTrue(manager.placeBuilding(STATION_X, STATION_Y));
        return (ProcessingStation) map.getTile(STATION_X, STATION_Y).getBuilding();
    }

    /** Leaves one stack in the inventory, in the selected hotbar slot, so that it is what the player holds. */
    private void hold(ItemType type, int quantity) {
        for (var slot = 0; slot < player.getItems().size(); slot++) {
            player.setItemAt(slot, null);
        }
        player.setItemAt(player.getSelectedHotbarIndex(), new Item(type, quantity));
    }

    private int held(ItemType type) {
        return player.getItemCount(new Item(type, 1));
    }

    @Test
    void theStoneOvenIsBuiltFromSixStoneBricksAndTwoCoal() {
        final var ingredients = recipes.getBuildingRecipeByType(BuildingType.STONE_OVEN).orElseThrow().ingredients();

        assertEquals(List.of(new CraftingIngredient(ItemType.STONE_BRICK, 6), new CraftingIngredient(ItemType.COAL, 2)),
                ingredients);
    }

    @Test
    void everyStationThatIsPlacedJoinsTheTickingList() {
        final var oven = build(BuildingType.STONE_OVEN);

        assertEquals(List.of(oven), state.getStations());
    }

    @Test
    void aPieceThatIsNoStationDoesNotJoinTheTickingList() {
        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        manager.setSelectedBuildingType(BuildingType.FENCE);

        assertTrue(manager.placeBuilding(STATION_X, STATION_Y));

        assertTrue(state.getStations().isEmpty());
    }

    @Test
    void demolishingAStationRemovesItFromTheTickingListAndDropsWhatItHeld() {
        final var furnace = build(BuildingType.FURNACE);
        furnace.load(ItemType.IRON_ORE);
        furnace.load(ItemType.IRON_ORE);
        furnace.load(ItemType.COAL);
        state.getDroppedItems().clear();

        assertTrue(manager.removeBuilding(STATION_X, STATION_Y));

        assertTrue(state.getStations().isEmpty());
        final var piles = state.getDroppedItems();
        assertTrue(piles.stream().allMatch(pile -> pile.getTileX() == STATION_X && pile.getTileY() == STATION_Y));
        assertEquals(2, piles.stream().filter(pile -> pile.getItem().getType() == ItemType.IRON_ORE)
                .mapToInt(pile -> pile.getItem().getQuantity()).sum());
        assertEquals(1, piles.stream().filter(pile -> pile.getItem().getType() == ItemType.COAL)
                .mapToInt(pile -> pile.getItem().getQuantity()).sum());
    }

    @Test
    void demolishingAnEmptyStationDropsNothing() {
        build(BuildingType.CAMPFIRE);
        state.getDroppedItems().clear();

        manager.removeBuilding(STATION_X, STATION_Y);

        assertTrue(state.getDroppedItems().isEmpty());
        assertTrue(state.getStations().isEmpty());
    }

    @Test
    void aClickWithAnAcceptedItemLoadsOneUnitAndTakesItFromTheHand() {
        final var furnace = build(BuildingType.FURNACE);
        hold(ItemType.IRON_ORE, 5);

        assertTrue(manager.interactWithBuilding(STATION_X, STATION_Y));

        assertEquals(1, furnace.count(ItemType.IRON_ORE));
        assertEquals(4, held(ItemType.IRON_ORE));
        assertTrue(hints.isEmpty());
    }

    @Test
    void theLastUnitInTheHandEmptiesTheSlot() {
        final var oven = build(BuildingType.STONE_OVEN);
        hold(ItemType.WOOD_LOG, 1);

        manager.interactWithBuilding(STATION_X, STATION_Y);

        assertEquals(1, oven.count(ItemType.WOOD_LOG));
        assertNull(player.getActiveItem());
    }

    @Test
    void aClickWithAnotherItemShowsWhatTheStationHoldsAndLoadsNothing() {
        final var furnace = build(BuildingType.FURNACE);
        furnace.load(ItemType.IRON_ORE);
        furnace.load(ItemType.COAL);
        hold(ItemType.STONE, 3);

        assertTrue(manager.interactWithBuilding(STATION_X, STATION_Y));

        assertEquals(List.of("Input 1/20 (Iron ore 1), Fuel 1/20 (Coal 1)"), hints);
        assertEquals(3, held(ItemType.STONE));
    }

    @Test
    void aClickWithABareHandShowsWhatTheStationTakes() {
        build(BuildingType.CAMPFIRE);
        hold(ItemType.STONE, 1);
        player.setItemAt(player.getSelectedHotbarIndex(), null);

        manager.interactWithBuilding(STATION_X, STATION_Y);

        assertEquals(List.of("Empty, takes Raw meat, Wood log"), hints);
    }

    @Test
    void aClickWithAnItemThatIsAtItsCapSaysSoAndKeepsTheItem() {
        final var campfire = build(BuildingType.CAMPFIRE);
        for (var i = 0; i < campfire.capOf(ItemType.WOOD_LOG); i++) {
            campfire.load(ItemType.WOOD_LOG);
        }
        hold(ItemType.WOOD_LOG, 4);

        assertTrue(manager.interactWithBuilding(STATION_X, STATION_Y));

        assertEquals(List.of("Fuel is full, 10/10 (Wood log 10)"), hints);
        assertEquals(4, held(ItemType.WOOD_LOG));
    }

    @Test
    void aLogIsFuelInTheOven() {
        final var oven = build(BuildingType.STONE_OVEN);
        hold(ItemType.RAW_MEAT, 1);
        manager.interactWithBuilding(STATION_X, STATION_Y);
        assertFalse(oven.isWorking(), "meat without a log does not cook");

        hold(ItemType.WOOD_LOG, 1);
        manager.interactWithBuilding(STATION_X, STATION_Y);

        assertTrue(oven.isWorking());
    }

    @Test
    void aLogIsRawMaterialInTheFurnaceAndNeverFuel() {
        final var furnace = build(BuildingType.FURNACE);
        hold(ItemType.IRON_ORE, 1);
        manager.interactWithBuilding(STATION_X, STATION_Y);
        hold(ItemType.WOOD_LOG, 1);
        manager.interactWithBuilding(STATION_X, STATION_Y);

        assertTrue(furnace.isWorking(), "the log is raw material for charcoal");
        furnace.advance(AppConfig.getInstance().stations().getFurnaceCharcoalSeconds());
        assertEquals(1, furnace.count(ItemType.IRON_ORE), "the ore is still waiting for coal");
    }

    @Test
    void resettingTheGameStateForgetsTheStations() {
        build(BuildingType.FURNACE);

        state.resetGameState();

        assertTrue(state.getStations().isEmpty());
    }

    @Test
    void aPlacedLightPieceRegistersItsLightAndDemolishingItTakesTheLightOut() {
        final var torch = placeTorch();

        assertEquals(1, state.getLights().size());
        assertTrue(state.getLights().contains(torch.getLight()));

        assertTrue(manager.removeBuilding(STATION_X, STATION_Y));

        assertTrue(state.getLights().isEmpty());
    }

    @Test
    void aPieceThatGivesNoLightRegistersNone() {
        player.addItem(new Item(ItemType.WOOD_LOG, 2));
        manager.setSelectedBuildingType(BuildingType.FENCE);

        assertTrue(manager.placeBuilding(STATION_X, STATION_Y));

        assertTrue(state.getLights().isEmpty());
    }

    @Test
    void aStationLightFollowsTheStateOfTheStationWithoutBeingRegisteredAgain() {
        final var campfire = build(BuildingType.CAMPFIRE);
        final var light = (BuildingLight) campfire.getLight();
        assertEquals(1, state.getLights().size());
        assertFalse(light.isOn());

        campfire.load(ItemType.WOOD_LOG);

        assertTrue(light.isOn());
        assertEquals(1, state.getLights().size());
    }

    @Test
    void resettingTheGameClearsThePlacedLights() {
        placeTorch();

        state.resetGameState();

        assertTrue(state.getLights().isEmpty());
    }

    private Building placeTorch() {
        for (CraftingIngredient ingredient : recipes.getBuildingRecipeByType(BuildingType.STANDING_TORCH).orElseThrow()
                .ingredients()) {
            player.addItem(new Item(ingredient.itemType(), ingredient.quantity()));
        }
        manager.setSelectedBuildingType(BuildingType.STANDING_TORCH);
        assertTrue(manager.placeBuilding(STATION_X, STATION_Y));
        return map.getTile(STATION_X, STATION_Y).getBuilding();
    }
}
