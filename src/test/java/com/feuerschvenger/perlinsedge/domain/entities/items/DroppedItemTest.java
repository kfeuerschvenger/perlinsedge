package com.feuerschvenger.perlinsedge.domain.entities.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroppedItemTest {

    private static DroppedItem drop(DroppedItem.DropSource source, double dropTime) {
        return new DroppedItem(4, 4, new Item(ItemType.STONE, 3), dropTime, source);
    }

    @Test
    void resourceAndEnemyDropsAreCollectableAtOnce() {
        assertTrue(drop(DroppedItem.DropSource.RESOURCE, 10).canBePickedUp(4, 4, 10));
        assertTrue(drop(DroppedItem.DropSource.ENEMY, 10).canBePickedUp(4, 4, 10));
    }

    @Test
    void playerDropWaitsItsOwnDelayAndNeedsNoWalkAway() {
        DroppedItem item = drop(DroppedItem.DropSource.PLAYER, 10);

        assertFalse(item.canBePickedUp(4, 4, 10.0));
        assertFalse(item.canBePickedUp(4, 4, 10.4));
        assertTrue(item.canBePickedUp(4, 4, 10.5), "no need to leave and come back once the delay has passed");
    }

    @Test
    void theDelayBelongsToEachItemAndNeverHoldsUpAnother() {
        DroppedItem recentPlayerDrop = drop(DroppedItem.DropSource.PLAYER, 100);
        DroppedItem oldDrop = drop(DroppedItem.DropSource.PLAYER, 10);
        DroppedItem lootDrop = drop(DroppedItem.DropSource.ENEMY, 100);

        assertFalse(recentPlayerDrop.canBePickedUp(4, 4, 100.1));
        assertTrue(oldDrop.canBePickedUp(4, 4, 100.1));
        assertTrue(lootDrop.canBePickedUp(4, 4, 100.1));
    }

    @Test
    void pickupRangeStaysOnTheSameTile() {
        DroppedItem item = drop(DroppedItem.DropSource.RESOURCE, 0);

        assertTrue(item.canBePickedUp(4, 4, 1));
        assertFalse(item.canBePickedUp(5, 4, 1));
        assertFalse(item.canBePickedUp(5, 5, 1));
    }

    @Test
    void delayIsHalfASecondForPlayerDropsAndZeroForTheRest() {
        assertEquals(0.5, drop(DroppedItem.DropSource.PLAYER, 0).pickupDelaySeconds());
        assertEquals(0.0, drop(DroppedItem.DropSource.ENEMY, 0).pickupDelaySeconds());
        assertEquals(0.0, drop(DroppedItem.DropSource.OTHER, 0).pickupDelaySeconds());
    }

    @Test
    void rejectsNullAndEmptyItems() {
        assertThrows(IllegalArgumentException.class,
                () -> new DroppedItem(0, 0, null, 0, DroppedItem.DropSource.PLAYER));
        Item empty = new Item(ItemType.STONE, 1);
        empty.setQuantity(0);
        assertThrows(IllegalArgumentException.class,
                () -> new DroppedItem(0, 0, empty, 0, DroppedItem.DropSource.PLAYER));
    }
}
