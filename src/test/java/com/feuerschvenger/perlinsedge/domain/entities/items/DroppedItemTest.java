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
        assertFalse(item.canBePickedUp(4, 4, 10.5));
        assertFalse(item.canBePickedUp(4, 4, 11.9));
        assertTrue(item.canBePickedUp(4, 4, 12.0), "no need to leave and come back once the delay has passed");
    }

    @Test
    void theDelayBelongsToEachItemAndNeverHoldsUpAnother() {
        DroppedItem recentPlayerDrop = drop(DroppedItem.DropSource.PLAYER, 100);
        DroppedItem oldDrop = drop(DroppedItem.DropSource.PLAYER, 10);
        DroppedItem lootDrop = drop(DroppedItem.DropSource.ENEMY, 100);

        assertFalse(recentPlayerDrop.canBePickedUp(4, 4, 101.9));
        assertTrue(oldDrop.canBePickedUp(4, 4, 101.9));
        assertTrue(lootDrop.canBePickedUp(4, 4, 101.9));
    }

    @Test
    void playerDropOnThePlayersOwnTileStillWaitsTheFullDelay() {
        // The drop position falls back to the player's tile when no neighbour is free
        DroppedItem item = drop(DroppedItem.DropSource.PLAYER, 50);

        assertFalse(item.canBePickedUp(4, 4, 50.0));
        assertFalse(item.canBePickedUp(4, 4, 51.99));
        assertTrue(item.canBePickedUp(4, 4, 52.0));
    }

    @Test
    void pickupRangeStaysOnTheSameTile() {
        DroppedItem item = drop(DroppedItem.DropSource.RESOURCE, 0);

        assertTrue(item.canBePickedUp(4, 4, 1));
        assertFalse(item.canBePickedUp(5, 4, 1));
        assertFalse(item.canBePickedUp(5, 5, 1));
    }

    @Test
    void delayIsTwoSecondsForPlayerDropsAndZeroForTheRest() {
        assertEquals(2.0, drop(DroppedItem.DropSource.PLAYER, 0).pickupDelaySeconds());
        assertEquals(0.0, drop(DroppedItem.DropSource.ENEMY, 0).pickupDelaySeconds());
        assertEquals(0.0, drop(DroppedItem.DropSource.OTHER, 0).pickupDelaySeconds());
    }

    @Test
    void anItemDespawnsAfterFiveMinutesOfGameTime() {
        DroppedItem item = drop(DroppedItem.DropSource.RESOURCE, 100);

        assertFalse(item.isExpired(100));
        assertFalse(item.isExpired(399.9));
        assertFalse(item.isExpired(400.0), "exactly five minutes is not past the lifetime yet");
        assertTrue(item.isExpired(400.1));
    }

    @Test
    void theLifetimeIsFiveMinutes() {
        assertEquals(300.0, DroppedItem.LIFETIME_SECONDS);
    }

    @Test
    void absorbingUnitsAddsThemAndStartsTheDelayAndTheLifetimeAgain() {
        DroppedItem pile = new DroppedItem(4, 4, new Item(ItemType.STONE, 3), 10, DroppedItem.DropSource.PLAYER);

        pile.absorb(5, 100);

        assertEquals(8, pile.getQuantity());
        assertEquals(DroppedItem.DropSource.PLAYER, pile.getSource());
        assertFalse(pile.canBePickedUp(4, 4, 101.9));
        assertTrue(pile.canBePickedUp(4, 4, 102.0));
        assertFalse(pile.isExpired(399.9));
        assertTrue(pile.isExpired(400.1));
    }

    @Test
    void absorbingUnitsDoesNotRestartTheFlightWhichIsMeasuredFromTheThrow() {
        DroppedItem pile = new DroppedItem(0, 0, 4, 4, new Item(ItemType.STONE, 3), 10, DroppedItem.DropSource.PLAYER);

        pile.absorb(1, 10.2);

        assertEquals(0.5, pile.flightProgress(10 + DroppedItem.THROW_SECONDS / 2), 1e-9,
                "the pile flies on from the original throw time while its timers started again");
        assertEquals(1.0, pile.flightProgress(10 + DroppedItem.THROW_SECONDS), 1e-9);
    }

    @Test
    void theAgeIsMeasuredFromTheDespawnTimerLikeTheExpiry() {
        DroppedItem pile = new DroppedItem(4, 4, new Item(ItemType.STONE, 3), 10, DroppedItem.DropSource.PLAYER);
        assertEquals(90, pile.ageSeconds(100), 1e-9);

        pile.absorb(1, 100);

        assertEquals(20, pile.ageSeconds(120), 1e-9, "the age starts again with the timer");
    }

    @Test
    void aPileOnlyTakesUnitsUpToTheMaximumStackSize() {
        DroppedItem pile = new DroppedItem(4, 4, new Item(ItemType.STONE, 60), 0, DroppedItem.DropSource.PLAYER);

        assertEquals(4, pile.roomLeft());
        assertThrows(IllegalArgumentException.class, () -> pile.absorb(5, 1));
        assertThrows(IllegalArgumentException.class, () -> pile.absorb(0, 1));
        pile.absorb(4, 1);
        assertEquals(0, pile.roomLeft());
    }

    @Test
    void anItemThatNeverFlewIsAlreadyLanded() {
        DroppedItem item = drop(DroppedItem.DropSource.RESOURCE, 10);

        assertEquals(1.0, item.flightProgress(10));
        assertEquals(4.0, item.flightX(10));
        assertEquals(4.0, item.flightY(10));
    }

    @Test
    void aThrownItemFliesFromTheOriginToTheLandingTileAndSlowsDown() {
        DroppedItem item = new DroppedItem(2, 2, 4, 2, new Item(ItemType.STONE, 1), 10, DroppedItem.DropSource.PLAYER);

        assertEquals(0.0, item.flightProgress(10));
        assertEquals(2.0, item.flightX(10));
        assertEquals(2.0, item.flightY(10));
        double half = item.flightX(10 + DroppedItem.THROW_SECONDS / 2);
        assertTrue(half > 3.0 && half < 4.0, "an ease out covers more than half the distance by half the time: " + half);
        assertEquals(1.0, item.flightProgress(10 + DroppedItem.THROW_SECONDS));
        assertEquals(4.0, item.flightX(10 + DroppedItem.THROW_SECONDS), 1e-9);
        assertEquals(4.0, item.flightX(500), "it stays on the landing tile once landed");
    }

    @Test
    void aGameTimeBeforeTheThrowKeepsTheItemAtTheOrigin() {
        DroppedItem item = new DroppedItem(2, 2, 4, 2, new Item(ItemType.STONE, 1), 10, DroppedItem.DropSource.PLAYER);

        assertEquals(0.0, item.flightProgress(0));
        assertEquals(2.0, item.flightX(0));
    }

    @Test
    void absorbingUnitsKeepsTheFlightOfAPileInTheAir() {
        DroppedItem item = new DroppedItem(2, 2, 4, 2, new Item(ItemType.STONE, 1), 10, DroppedItem.DropSource.PLAYER);
        double before = item.flightX(10.1);

        item.absorb(1, 10.1);

        assertEquals(2, item.getQuantity());
        assertTrue(item.flightProgress(10.1) < 1.0, "the pile has not landed yet");
        assertEquals(before, item.flightX(10.1), 1e-9, "the pile does not jump to its landing tile");
        assertEquals(4.0, item.flightX(10 + DroppedItem.THROW_SECONDS), 1e-9);
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
