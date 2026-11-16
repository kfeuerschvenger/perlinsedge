package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An enemy holds the tile it stands on and the tile it moves to, and no other enemy may take either. */
class EnemyOccupancyTest {

    private final EnemyOccupancy occupancy = new EnemyOccupancy();

    @Test
    void anEnemyHoldsTheTileItStandsOnAndNoOtherEnemyMayTakeIt() {
        final Slime slime = new Slime(3, 4);
        final Skeleton skeleton = new Skeleton(8, 8);
        occupancy.sync(slime);
        occupancy.sync(skeleton);

        assertTrue(occupancy.isFreeFor(slime, 3, 4), "an enemy may stay on its own tile");
        assertFalse(occupancy.isFreeFor(skeleton, 3, 4));
        assertSame(slime, occupancy.ownerOf(3, 4));
        assertTrue(occupancy.isFreeFor(skeleton, 5, 5));
        assertNull(occupancy.ownerOf(5, 5));
    }

    @Test
    void aReservedTargetIsHeldUntilTheEnemyHasArrivedAndTheOldTileIsThenLetGo() {
        final Slime slime = new Slime(3, 4);
        occupancy.sync(slime);

        assertTrue(occupancy.tryReserve(slime, 4, 4));
        slime.setTargetTileX(4);
        slime.setTargetTileY(4);
        occupancy.sync(slime);

        assertSame(slime, occupancy.ownerOf(3, 4), "the old tile stays held while the enemy is on its way");
        assertSame(slime, occupancy.ownerOf(4, 4));

        slime.setCurrentTileX(4);
        slime.setCurrentTileY(4);
        occupancy.sync(slime);

        assertNull(occupancy.ownerOf(3, 4), "the old tile is let go on arrival");
        assertSame(slime, occupancy.ownerOf(4, 4));
        assertEquals(1, occupancy.heldTileCount());
    }

    @Test
    void aTileHeldByAnotherEnemyCannotBeReserved() {
        final Slime first = new Slime(3, 4);
        final Slime second = new Slime(4, 4);
        occupancy.sync(first);
        occupancy.sync(second);

        assertFalse(occupancy.tryReserve(first, 4, 4));
        assertTrue(occupancy.tryReserve(first, 3, 5));
        assertFalse(occupancy.tryReserve(second, 3, 5), "a reserved target is held too");
        assertSame(second, occupancy.ownerOf(4, 4));
    }

    @Test
    void reservingAnotherTargetLetsGoOfTheFirstOne() {
        final Slime slime = new Slime(3, 4);
        occupancy.sync(slime);

        occupancy.tryReserve(slime, 4, 4);
        occupancy.tryReserve(slime, 3, 5);

        assertNull(occupancy.ownerOf(4, 4));
        assertSame(slime, occupancy.ownerOf(3, 5));
        assertSame(slime, occupancy.ownerOf(3, 4));
    }

    @Test
    void aDeadEnemyHoldsNothingOnceReleasedAndIsFreeEvenBefore() {
        final Slime slime = new Slime(3, 4);
        final Skeleton skeleton = new Skeleton(8, 8);
        occupancy.sync(slime);
        occupancy.tryReserve(slime, 4, 4);

        slime.takeDamage(1_000);
        assertTrue(occupancy.isFreeFor(skeleton, 3, 4), "a dead enemy blocks nothing");
        assertNull(occupancy.ownerOf(4, 4));

        occupancy.release(slime);
        assertEquals(0, occupancy.heldTileCount());
    }

    @Test
    void aMoveMadeWithoutAReservationIsStillRecordedBySync() {
        final Slime slime = new Slime(3, 4);
        occupancy.sync(slime);

        slime.setCurrentTileX(9);
        slime.setCurrentTileY(9);
        slime.setTargetTileX(9);
        slime.setTargetTileY(9);
        occupancy.sync(slime);

        assertNull(occupancy.ownerOf(3, 4));
        assertSame(slime, occupancy.ownerOf(9, 9));
    }

    @Test
    void rebuildHoldsTheTilesOfTheLivingEnemiesOnly() {
        final Slime alive = new Slime(1, 1);
        final Slime dead = new Slime(2, 2);
        dead.takeDamage(1_000);
        occupancy.sync(new Slime(7, 7));

        occupancy.rebuild(List.of(alive, dead));

        assertSame(alive, occupancy.ownerOf(1, 1));
        assertNull(occupancy.ownerOf(2, 2));
        assertNull(occupancy.ownerOf(7, 7), "the enemies of the old map are forgotten");
    }

    @Test
    void theTilesOfOtherEnemiesCostMoreInASearchAndTheOwnTilesCostNothing() {
        final Slime slime = new Slime(3, 4);
        final Skeleton skeleton = new Skeleton(5, 5);
        occupancy.sync(slime);
        occupancy.sync(skeleton);

        final TilePenalty penalty = occupancy.penaltyFor(slime, 6.0);

        assertEquals(6.0, penalty.extraCost(5, 5));
        assertEquals(0.0, penalty.extraCost(3, 4));
        assertEquals(0.0, penalty.extraCost(9, 9));
    }
}
