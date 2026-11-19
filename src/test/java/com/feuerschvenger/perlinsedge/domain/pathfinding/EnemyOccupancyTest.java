package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    @Test
    void anEnemyThatLostATileOnSyncTakesItWhenTheHolderLeaves() {
        final Slime first = new Slime(3, 4);
        final Slime second = new Slime(3, 4);
        occupancy.rebuild(List.of(first, second));
        assertSame(first, occupancy.ownerOf(3, 4), "precondition: the first enemy won the tile");

        first.setCurrentTileX(6);
        first.setCurrentTileY(6);
        first.setTargetTileX(6);
        first.setTargetTileY(6);
        occupancy.sync(first);
        assertNull(occupancy.ownerOf(3, 4), "precondition: the tile is free for a moment");
        occupancy.sync(second);

        assertSame(second, occupancy.ownerOf(3, 4));
        assertSame(first, occupancy.ownerOf(6, 6));
    }

    @Test
    void aLostTargetIsTakenOnALaterSyncOnceItIsFree() {
        final Slime holder = new Slime(5, 5);
        final Slime mover = new Slime(1, 1);
        occupancy.sync(holder);
        mover.setTargetTileX(5);
        mover.setTargetTileY(5);
        occupancy.sync(mover);
        assertSame(holder, occupancy.ownerOf(5, 5), "precondition: the holder keeps the tile");

        holder.setCurrentTileX(8);
        holder.setCurrentTileY(8);
        holder.setTargetTileX(8);
        holder.setTargetTileY(8);
        occupancy.sync(holder);
        occupancy.sync(mover);

        assertSame(mover, occupancy.ownerOf(5, 5));
    }

    @Test
    void manyEnemiesAreHeldAndReleasedWithoutLosingAnyOther() {
        final List<Slime> slimes = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            final Slime slime = new Slime(i % 20, i / 20);
            slimes.add(slime);
            occupancy.sync(slime);
        }
        assertEquals(300, occupancy.heldTileCount());

        for (int i = 0; i < 300; i += 2) {
            occupancy.release(slimes.get(i));
        }

        assertEquals(150, occupancy.heldTileCount());
        for (int i = 0; i < 300; i++) {
            assertSame(i % 2 == 0 ? null : slimes.get(i), occupancy.ownerOf(i % 20, i / 20), "tile " + i);
        }
    }

    @Test
    void thePriceListOfAnEnemyIsReusedByEverySearch() {
        final Slime slime = new Slime(3, 4);
        final Skeleton skeleton = new Skeleton(5, 5);
        occupancy.sync(slime);
        occupancy.sync(skeleton);

        final TilePenalty first = occupancy.penaltyFor(slime, 6.0);
        final TilePenalty second = occupancy.penaltyFor(slime, 2.0);

        assertSame(first, second);
        assertEquals(2.0, second.extraCost(5, 5), "the latest penalty applies");
    }

    @Test
    void negativeAndLargeCoordinatesAreHeldApart() {
        final int[][] tiles = {{-1, -1}, {-1, 0}, {0, -1}, {-5, -7}, {Integer.MAX_VALUE, Integer.MAX_VALUE},
                {Integer.MIN_VALUE, Integer.MIN_VALUE}, {Integer.MAX_VALUE, Integer.MIN_VALUE},
                {Integer.MIN_VALUE, Integer.MAX_VALUE}, {1_000_000, -1_000_000}, {-1_000_000, 1_000_000}};
        final List<Skeleton> skeletons = new ArrayList<>();
        for (int[] tile : tiles) {
            final Skeleton skeleton = new Skeleton(tile[0], tile[1]);
            skeletons.add(skeleton);
            occupancy.sync(skeleton);
        }

        assertEquals(tiles.length, occupancy.heldTileCount());
        for (int i = 0; i < tiles.length; i++) {
            assertSame(skeletons.get(i), occupancy.ownerOf(tiles[i][0], tiles[i][1]), "tile " + i);
        }
        assertNull(occupancy.ownerOf(1, 1));
        assertNull(occupancy.ownerOf(Integer.MAX_VALUE, 0));

        for (int i = 0; i < tiles.length; i += 2) {
            occupancy.release(skeletons.get(i));
        }
        for (int i = 0; i < tiles.length; i++) {
            assertSame(i % 2 == 0 ? null : skeletons.get(i), occupancy.ownerOf(tiles[i][0], tiles[i][1]), "tile " + i);
        }
    }

    /** The slot the occupancy table gives a tile, repeated here to build tiles that crowd into one probe run. */
    private static int slotOf(int x, int y, int mask) {
        final long key = ((long) x << 32) | (y & 0xFFFF_FFFFL);
        return (int) ((key * 0x9E37_79B9_7F4A_7C15L) >>> 32) & mask;
    }

    @Test
    void tilesThatCollideInTheTableAreHeldAndLetGoWithoutLosingTheOthers() {
        // The same slot for every table size from 64 to 256 slots, so the cluster survives the table growing
        final int mask = 255;
        final List<int[]> cluster = new ArrayList<>();
        for (int x = 0; cluster.size() < 40 && x < 100_000; x++) {
            if (slotOf(x, 3, mask) == 7) {
                cluster.add(new int[]{x, 3});
            }
        }
        assertEquals(40, cluster.size(), "precondition: enough colliding tiles");
        final List<Skeleton> skeletons = new ArrayList<>();
        for (int[] tile : cluster) {
            final Skeleton skeleton = new Skeleton(tile[0], tile[1]);
            skeletons.add(skeleton);
            occupancy.sync(skeleton);
        }

        // Let go from the middle of the run, the end and the start: each backward shift has to keep the rest findable
        final int[] releaseOrder = {20, 0, 39, 10, 30, 5, 25, 15, 35, 1};
        final boolean[] released = new boolean[cluster.size()];
        for (int index : releaseOrder) {
            occupancy.release(skeletons.get(index));
            released[index] = true;
            for (int i = 0; i < cluster.size(); i++) {
                assertSame(released[i] ? null : skeletons.get(i), occupancy.ownerOf(cluster.get(i)[0], cluster.get(i)[1]),
                        "tile " + i + " after releasing " + index);
            }
        }
        assertEquals(cluster.size() - releaseOrder.length, occupancy.heldTileCount());
    }

    @Test
    void aRandomSequenceOfMovesAndReleasesAgreesWithAHashMapOracle() {
        final int enemyCount = 20;
        final int side = 8;
        final Random random = new Random(20_251_117L);
        final List<Skeleton> enemies = new ArrayList<>();
        for (int i = 0; i < enemyCount; i++) {
            enemies.add(new Skeleton(0, 0));
        }
        final Map<Long, Skeleton> oracle = new HashMap<>();
        final Map<Skeleton, List<Long>> held = new HashMap<>();

        for (int step = 0; step < 3_000; step++) {
            final Skeleton enemy = enemies.get(random.nextInt(enemyCount));
            if (random.nextInt(4) == 0) {
                occupancy.release(enemy);
                oracleRelease(oracle, held, enemy);
            } else {
                moveWhenFree(enemy, random, side, oracle, held);
            }
            assertAgreesWith(oracle, side, step);
        }
    }

    /** Moves the enemy to a random tile and target when both are free in the oracle, and leaves it where it is otherwise. */
    private void moveWhenFree(Skeleton enemy, Random random, int side, Map<Long, Skeleton> oracle,
                              Map<Skeleton, List<Long>> held) {
        final int cx = random.nextInt(side) - side / 2;
        final int cy = random.nextInt(side) - side / 2;
        final int tx = random.nextBoolean() ? cx : random.nextInt(side) - side / 2;
        final int ty = tx == cx ? cy : random.nextInt(side) - side / 2;
        if (!isFreeInOracle(oracle, enemy, cx, cy) || !isFreeInOracle(oracle, enemy, tx, ty)) {
            return;
        }
        oracleRelease(oracle, held, enemy);
        enemy.setCurrentTileX(cx);
        enemy.setCurrentTileY(cy);
        enemy.setTargetTileX(tx);
        enemy.setTargetTileY(ty);
        occupancy.sync(enemy);
        final List<Long> tiles = new ArrayList<>();
        for (long key : new long[]{tileKey(cx, cy), tileKey(tx, ty)}) {
            if (!tiles.contains(key)) {
                tiles.add(key);
                oracle.put(key, enemy);
            }
        }
        held.put(enemy, tiles);
    }

    private static long tileKey(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFF_FFFFL);
    }

    private static boolean isFreeInOracle(Map<Long, Skeleton> oracle, Skeleton enemy, int x, int y) {
        final Skeleton owner = oracle.get(tileKey(x, y));
        return owner == null || owner == enemy;
    }

    private static void oracleRelease(Map<Long, Skeleton> oracle, Map<Skeleton, List<Long>> held, Skeleton enemy) {
        final List<Long> tiles = held.remove(enemy);
        if (tiles != null) {
            tiles.forEach(oracle::remove);
        }
    }

    private void assertAgreesWith(Map<Long, Skeleton> oracle, int side, int step) {
        assertEquals(oracle.size(), occupancy.heldTileCount(), "held tiles at step " + step);
        for (int x = -side / 2; x < side / 2; x++) {
            for (int y = -side / 2; y < side / 2; y++) {
                assertSame(oracle.get(tileKey(x, y)), occupancy.ownerOf(x, y), "tile " + x + "," + y + " at step " + step);
            }
        }
    }
}
