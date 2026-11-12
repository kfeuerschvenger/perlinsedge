package com.feuerschvenger.perlinsedge.domain.entities.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropMergerTest {

    private List<DroppedItem> drops;

    @BeforeEach
    void setUp() {
        drops = new ArrayList<>();
    }

    private DroppedItem pile(ItemType type, int quantity, int x, int y) {
        DroppedItem pile = new DroppedItem(x, y, new Item(type, quantity), 0, DroppedItem.DropSource.PLAYER);
        drops.add(pile);
        return pile;
    }

    @Test
    void aDropMergesIntoAPileOnTheSameTile() {
        DroppedItem pile = pile(ItemType.STONE, 10, 4, 4);

        int left = DropMerger.mergeInto(drops, ItemType.STONE, 5, 4, 4, 20);

        assertEquals(0, left);
        assertEquals(15, pile.getQuantity());
    }

    @Test
    void aDropMergesIntoAPileOneTileAwayIncludingDiagonals() {
        DroppedItem orthogonal = pile(ItemType.STONE, 1, 5, 4);
        DroppedItem diagonal = pile(ItemType.WOOD_LOG, 1, 3, 3);

        assertEquals(0, DropMerger.mergeInto(drops, ItemType.STONE, 2, 4, 4, 20));
        assertEquals(0, DropMerger.mergeInto(drops, ItemType.WOOD_LOG, 2, 4, 4, 20));

        assertEquals(3, orthogonal.getQuantity());
        assertEquals(3, diagonal.getQuantity());
    }

    @Test
    void aPileTwoTilesAwayDoesNotMerge() {
        DroppedItem far = pile(ItemType.STONE, 1, 6, 4);

        int left = DropMerger.mergeInto(drops, ItemType.STONE, 2, 4, 4, 20);

        assertEquals(2, left);
        assertEquals(1, far.getQuantity());
    }

    @Test
    void aDifferentTypeDoesNotMerge() {
        DroppedItem logs = pile(ItemType.WOOD_LOG, 1, 4, 4);

        int left = DropMerger.mergeInto(drops, ItemType.STONE, 2, 4, 4, 20);

        assertEquals(2, left);
        assertEquals(1, logs.getQuantity());
    }

    @Test
    void aPileNeverGrowsPastTheMaximumStackSizeAndTheRestIsReturned() {
        DroppedItem pile = pile(ItemType.STONE, 60, 4, 4);

        int left = DropMerger.mergeInto(drops, ItemType.STONE, 10, 4, 4, 20);

        assertEquals(64, pile.getQuantity());
        assertEquals(6, left);
    }

    @Test
    void aFullPileIsSkippedAndTheNextOneTakesTheDrop() {
        DroppedItem full = pile(ItemType.STONE, 64, 4, 4);
        DroppedItem open = pile(ItemType.STONE, 10, 5, 4);

        int left = DropMerger.mergeInto(drops, ItemType.STONE, 3, 4, 4, 20);

        assertEquals(0, left);
        assertEquals(64, full.getQuantity());
        assertEquals(13, open.getQuantity());
    }

    @Test
    void theClosestPileIsFilledFirstAndTheRestSpillsOverToTheNext() {
        DroppedItem near = pile(ItemType.STONE, 62, 4, 4);
        DroppedItem farther = pile(ItemType.STONE, 10, 5, 5);

        int left = DropMerger.mergeInto(drops, ItemType.STONE, 5, 4, 4, 20);

        assertEquals(0, left);
        assertEquals(64, near.getQuantity());
        assertEquals(13, farther.getQuantity());
    }

    @Test
    void nothingNearbyLeavesTheWholeDropUnmerged() {
        assertEquals(7, DropMerger.mergeInto(drops, ItemType.STONE, 7, 4, 4, 20));
    }

    @Test
    void aMergedPileStartsItsPickupDelayAndLifetimeAgain() {
        DroppedItem pile = pile(ItemType.STONE, 1, 4, 4);

        DropMerger.mergeInto(drops, ItemType.STONE, 1, 4, 4, 100);

        assertFalse(pile.canBePickedUp(4, 4, 101.9));
        assertTrue(pile.canBePickedUp(4, 4, 102.0));
        assertFalse(pile.isExpired(399.9));
        assertTrue(pile.isExpired(400.1));
    }

    @Test
    void lootFromResourcesAndEnemiesIsNeverMergedInto() {
        DroppedItem resource = new DroppedItem(4, 4, new Item(ItemType.STONE, 3), 0, DroppedItem.DropSource.RESOURCE);
        DroppedItem enemy = new DroppedItem(5, 4, new Item(ItemType.STONE, 3), 0, DroppedItem.DropSource.ENEMY);
        drops.add(resource);
        drops.add(enemy);

        int left = DropMerger.mergeInto(drops, ItemType.STONE, 2, 4, 4, 100);

        assertEquals(2, left);
        assertEquals(3, resource.getQuantity());
        assertEquals(3, enemy.getQuantity());
        assertEquals(DroppedItem.DropSource.RESOURCE, resource.getSource());
        assertTrue(resource.canBePickedUp(4, 4, 100), "the loot keeps its own timers");
    }

    @Test
    void nonStackableItemsNeverMerge() {
        DroppedItem sword = pile(ItemType.SWORD, 1, 4, 4);

        int left = DropMerger.mergeInto(drops, ItemType.SWORD, 1, 4, 4, 20);

        assertEquals(1, left);
        assertEquals(1, sword.getQuantity());
    }
}
