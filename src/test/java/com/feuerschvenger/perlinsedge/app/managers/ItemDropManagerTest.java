package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Chest;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import com.feuerschvenger.perlinsedge.domain.entities.items.DroppedItem;
import com.feuerschvenger.perlinsedge.domain.entities.items.Item;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDropManagerTest {

    private GameStateManager state;
    private AtomicInteger inventoryRefreshes;
    private ItemDropManager drops;
    private TileMap map;
    private Player player;

    @BeforeEach
    void setUp() {
        state = new GameStateManager();
        inventoryRefreshes = new AtomicInteger();
        drops = new ItemDropManager(state, inventoryRefreshes::incrementAndGet);
        map = new TileMap(9, 9);
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        player = new Player(4, 4);
        state.setCurrentMap(map);
        state.setPlayer(player);
        player.setItemAt(0, new Item(ItemType.STONE, 5));
    }

    private DroppedItem onlyDrop() {
        assertEquals(1, state.getDroppedItems().size());
        return state.getDroppedItems().getFirst();
    }

    private void wallOffEveryNeighbour() {
        for (int x = 3; x <= 5; x++) {
            for (int y = 3; y <= 5; y++) {
                if (x != 4 || y != 4) {
                    map.getTile(x, y).setBuilding(new Wall(x, y));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Throwing
    // ------------------------------------------------------------------

    @Test
    void droppingOneTakesOneItemFromTheStackAndThrowsItInFrontOfThePlayer() {
        assertTrue(drops.dropFromSlot(player, 0, false));

        assertEquals(4, player.getItemAt(0).getQuantity());
        DroppedItem thrown = onlyDrop();
        assertEquals(1, thrown.getQuantity());
        assertEquals(ItemType.STONE, thrown.getItemType());
        assertEquals(DroppedItem.DropSource.PLAYER, thrown.getSource());
        assertEquals(4, thrown.getTileX());
        assertEquals(2, thrown.getTileY(), "the player faces up at the start, two tiles away");
        assertEquals(1, inventoryRefreshes.get());
    }

    @Test
    void droppingTheStackThrowsAllOfItAndEmptiesTheSlot() {
        assertTrue(drops.dropFromSlot(player, 0, true));

        assertNull(player.getItemAt(0));
        assertEquals(5, onlyDrop().getQuantity());
    }

    @Test
    void droppingTheLastItemOfAStackEmptiesTheSlot() {
        player.setItemAt(1, new Item(ItemType.COAL, 1));

        assertTrue(drops.dropFromSlot(player, 1, false));

        assertNull(player.getItemAt(1));
        assertEquals(1, onlyDrop().getQuantity());
    }

    @Test
    void thrownItemsFlyFromThePlayersTile() {
        drops.dropFromSlot(player, 0, false);

        DroppedItem thrown = onlyDrop();
        assertEquals(4.0, thrown.flightY(0.0), "the flight starts on the player's tile");
        assertEquals(2.0, thrown.flightY(DroppedItem.THROW_SECONDS + 1));
    }

    @Test
    void anEmptySlotThrowsNothing() {
        assertFalse(drops.dropFromSlot(player, 7, false));
        assertFalse(drops.dropFromSlot(player, 7, true));

        assertTrue(state.getDroppedItems().isEmpty());
        assertEquals(0, inventoryRefreshes.get());
    }

    @Test
    void aDeadPlayerThrowsNothing() {
        player.takeDamage(1000);

        assertFalse(drops.dropFromSlot(player, 0, true));

        assertEquals(5, player.getItemAt(0).getQuantity());
        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void withoutAPlayerOrAContainerNothingHappens() {
        state.setPlayer(null);
        assertFalse(drops.dropFromSlot(player, 0, true));

        state.setPlayer(player);
        assertFalse(drops.dropFromSlot(null, 0, true));
        assertEquals(5, player.getItemAt(0).getQuantity());
    }

    @Test
    void aWallRightInFrontMakesTheItemLandOnThePlayersOwnTile() {
        map.getTile(4, 3).setBuilding(new Wall(4, 3));

        drops.dropFromSlot(player, 0, false);

        DroppedItem thrown = onlyDrop();
        assertEquals(4, thrown.getTileX());
        assertEquals(4, thrown.getTileY());
    }

    @Test
    void anItemCanBeThrownOutOfAChest() {
        Chest chest = new Chest(0, 0, 9);
        chest.setItemAt(2, new Item(ItemType.WOOD_LOG, 3));

        assertTrue(drops.dropFromSlot(chest, 2, false));

        assertEquals(2, chest.getItemAt(2).getQuantity());
        assertEquals(ItemType.WOOD_LOG, onlyDrop().getItemType());
    }

    // ------------------------------------------------------------------
    //  Merging
    // ------------------------------------------------------------------

    @Test
    void dropsOfTheSameTypeLandingNearEachOtherMergeIntoOnePile() {
        drops.dropFromSlot(player, 0, false);
        drops.dropFromSlot(player, 0, false);
        drops.dropFromSlot(player, 0, false);

        assertEquals(3, onlyDrop().getQuantity());
        assertEquals(2, player.getItemAt(0).getQuantity());
    }

    @Test
    void aDropNextToAnExistingPlayerPileOfTheSameTypeMergesIntoIt() {
        state.getDroppedItems().add(new DroppedItem(5, 2, new Item(ItemType.STONE, 10), 0, DroppedItem.DropSource.PLAYER));

        drops.dropFromSlot(player, 0, true);

        assertEquals(15, onlyDrop().getQuantity());
    }

    @Test
    void aDropNextToResourceLootStaysSeparate() {
        state.getDroppedItems().add(new DroppedItem(5, 2, new Item(ItemType.STONE, 10), 0, DroppedItem.DropSource.RESOURCE));

        drops.dropFromSlot(player, 0, true);

        assertEquals(2, state.getDroppedItems().size());
        assertEquals(10, state.getDroppedItems().get(0).getQuantity());
    }

    @Test
    void aDropOfAnotherTypeNextToAPileStaysSeparate() {
        state.getDroppedItems().add(new DroppedItem(4, 2, new Item(ItemType.WOOD_LOG, 10), 0, DroppedItem.DropSource.RESOURCE));

        drops.dropFromSlot(player, 0, true);

        assertEquals(2, state.getDroppedItems().size());
    }

    @Test
    void whatDoesNotFitIntoTheNearbyPileBecomesANewPile() {
        state.getDroppedItems().add(new DroppedItem(4, 2, new Item(ItemType.STONE, 62), 0, DroppedItem.DropSource.PLAYER));

        drops.dropFromSlot(player, 0, true);

        assertEquals(2, state.getDroppedItems().size());
        assertEquals(64, state.getDroppedItems().get(0).getQuantity());
        assertEquals(3, state.getDroppedItems().get(1).getQuantity());
    }

    // ------------------------------------------------------------------
    //  Pickup delay
    // ------------------------------------------------------------------

    @Test
    void anItemThrownOntoThePlayersOwnTileIsNotCollectedAgainUntilTheDelayHasPassed() {
        wallOffEveryNeighbour();
        drops.dropFromSlot(player, 0, true);
        assertNull(player.getItemAt(0));

        drops.update();
        assertEquals(1, state.getDroppedItems().size(), "collected at once");
        state.incrementGameTime(1.9);
        drops.update();
        assertEquals(1, state.getDroppedItems().size(), "collected before the two seconds were over");

        state.incrementGameTime(0.1);
        drops.update();

        assertTrue(state.getDroppedItems().isEmpty());
        assertEquals(5, player.getItemCount(new Item(ItemType.STONE, 1)));
    }

    @Test
    void anItemLootedFromAResourceIsCollectedAtOnce() {
        state.getDroppedItems().add(new DroppedItem(4, 4, new Item(ItemType.STONE, 2), 0, DroppedItem.DropSource.RESOURCE));

        drops.update();

        assertTrue(state.getDroppedItems().isEmpty());
        assertEquals(7, player.getItemCount(new Item(ItemType.STONE, 1)));
        assertEquals(1, inventoryRefreshes.get());
    }

    @Test
    void aDeadPlayerCollectsNothing() {
        state.getDroppedItems().add(new DroppedItem(4, 4, new Item(ItemType.STONE, 2), 0, DroppedItem.DropSource.RESOURCE));
        player.takeDamage(1000);

        drops.update();

        assertEquals(1, state.getDroppedItems().size());
    }

    // ------------------------------------------------------------------
    //  Despawn
    // ------------------------------------------------------------------

    @Test
    void anItemDespawnsAfterFiveMinutesOfGameTime() {
        state.getDroppedItems().add(new DroppedItem(0, 0, new Item(ItemType.STONE, 2), 0, DroppedItem.DropSource.PLAYER));

        state.incrementGameTime(299);
        drops.update();
        assertEquals(1, state.getDroppedItems().size());

        state.incrementGameTime(2);
        drops.update();

        assertTrue(state.getDroppedItems().isEmpty());
    }

    @Test
    void aPausedGameNeverAgesItemsBecauseTheGameClockStandsStill() {
        state.getDroppedItems().add(new DroppedItem(0, 0, new Item(ItemType.STONE, 2), 0, DroppedItem.DropSource.PLAYER));
        state.incrementGameTime(100);

        // While paused the update loop returns before the clock advances, so only the unchanged clock is read
        for (int frame = 0; frame < 10_000; frame++) {
            drops.update();
        }

        assertNotNull(state.getDroppedItems().getFirst());
        assertEquals(1, state.getDroppedItems().size());
    }

    @Test
    void aMergedPileLivesAnotherFiveMinutesFromTheMerge() {
        state.getDroppedItems().add(new DroppedItem(4, 2, new Item(ItemType.STONE, 2), 0, DroppedItem.DropSource.PLAYER));
        state.incrementGameTime(250);

        drops.dropFromSlot(player, 0, false);
        state.incrementGameTime(100);
        drops.update();

        assertEquals(1, state.getDroppedItems().size(), "350 s after the first drop but 100 s after the merge");
        assertEquals(3, state.getDroppedItems().getFirst().getQuantity());
    }
}
