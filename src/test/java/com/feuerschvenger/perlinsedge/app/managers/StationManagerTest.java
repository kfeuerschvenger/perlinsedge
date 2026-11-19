package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.config.AppConfig;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingFactory;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.ProcessingStation;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ticking of the stations from the game state, and where what they make lands.
 */
class StationManagerTest {
    private static final double SMELT = AppConfig.getInstance().stations().getFurnaceSmeltSeconds();

    private GameStateManager state;
    private StationManager manager;
    private TileMap map;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        manager = new StationManager(state, new ItemDropManager(state, () -> { }));
        map = new TileMap(7, 7);
        for (var x = 0; x < 7; x++) {
            for (var y = 0; y < 7; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        state.setCurrentMap(map);
    }

    private ProcessingStation furnaceAt(int x, int y, int rotation) {
        final var furnace = (ProcessingStation) BuildingFactory.create(BuildingType.FURNACE, x, y, rotation);
        map.getTile(x, y).setBuilding(furnace);
        state.getStations().add(furnace);
        furnace.load(ItemType.IRON_ORE);
        furnace.load(ItemType.COAL);
        return furnace;
    }

    private DroppedItem onlyDrop() {
        assertEquals(1, state.getDroppedItems().size());
        return state.getDroppedItems().get(0);
    }

    @Test
    void aProductDropsOnTheTileInFrontOfTheStationFollowingItsRotation() {
        final int[][] expectedFront = {{4, 3}, {3, 4}, {2, 3}, {3, 2}};
        for (var rotation = 0; rotation < expectedFront.length; rotation++) {
            state.getDroppedItems().clear();
            state.getStations().clear();
            final var furnace = furnaceAt(3, 3, rotation);

            manager.update(SMELT);

            final var drop = onlyDrop();
            assertEquals(ItemType.IRON_INGOT, drop.getItem().getType());
            assertEquals(expectedFront[rotation][0], drop.getTileX(), "rotation " + rotation);
            assertEquals(expectedFront[rotation][1], drop.getTileY(), "rotation " + rotation);
            assertEquals(0, furnace.count(ItemType.IRON_ORE));
        }
    }

    @Test
    void aProductIsAStationPileThatNeverDespawns() {
        furnaceAt(3, 3, 0);
        final var drops = new ItemDropManager(state, () -> { });

        manager.update(SMELT);
        state.incrementGameTime(DroppedItem.LIFETIME_SECONDS * 2);
        drops.update();

        assertEquals(DroppedItem.DropSource.STATION, onlyDrop().getSource());
    }

    @Test
    void whenTheFrontIsBlockedTheProductGoesToTheNextEdgeClockwise() {
        furnaceAt(3, 3, 0);
        map.getTile(4, 3).setBuilding(BuildingFactory.create(BuildingType.CHEST, 4, 3));

        manager.update(SMELT);

        assertEquals(3, onlyDrop().getTileX());
        assertEquals(4, onlyDrop().getTileY());
    }

    @Test
    void whenAllFourEdgesAreBlockedTheProductDropsOnTheStationTile() {
        furnaceAt(3, 3, 0);
        map.getTile(4, 3).setBuilding(BuildingFactory.create(BuildingType.CHEST, 4, 3));
        map.getTile(3, 4).setBuilding(BuildingFactory.create(BuildingType.CHEST, 3, 4));
        map.getTile(2, 3).setBuilding(BuildingFactory.create(BuildingType.CHEST, 2, 3));
        map.getTile(3, 2).setBuilding(BuildingFactory.create(BuildingType.CHEST, 3, 2));

        manager.update(SMELT);

        assertEquals(3, onlyDrop().getTileX());
        assertEquals(3, onlyDrop().getTileY());
    }

    @Test
    void aStationAtTheEdgeOfTheMapSkipsTheMissingFrontTile() {
        furnaceAt(6, 3, 0);

        manager.update(SMELT);

        assertEquals(6, onlyDrop().getTileX());
        assertEquals(4, onlyDrop().getTileY());
    }

    @Test
    void nothingDropsBeforeTheTimeIsUpAndEveryTickAdvancesTheStations() {
        furnaceAt(3, 3, 0);

        for (var i = 0; i < SMELT - 1; i++) {
            manager.update(1.0);
        }
        assertTrue(state.getDroppedItems().isEmpty());
        manager.update(1.0);

        assertEquals(1, state.getDroppedItems().size());
    }

    @Test
    void aStationThatLeftTheListIsNoLongerTicked() {
        final var furnace = furnaceAt(3, 3, 0);
        state.getStations().remove(furnace);

        manager.update(SMELT * 2);

        assertTrue(state.getDroppedItems().isEmpty());
        assertEquals(1, furnace.count(ItemType.IRON_ORE));
    }

    @Test
    void withoutAMapTheStationsStandStill() {
        final var furnace = furnaceAt(3, 3, 0);
        state.setCurrentMap(null);

        manager.update(SMELT);

        assertEquals(1, furnace.count(ItemType.IRON_ORE));
    }
}
