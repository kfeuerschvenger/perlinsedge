package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.entities.creatures.CreatureContext;
import com.feuerschvenger.perlinsedge.domain.pathfinding.CreatureOccupancy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.OccupiedTiles;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The walking chase follows a moving player and recovers when the tile it walks to becomes blocked. */
class WalkingChaserTest {

    private static final double STEP = 1.0 / 60.0;

    /** Shared by every update of a test, as in the game, where one occupancy lives as long as the map. */
    private final CreatureOccupancy occupancy = new CreatureOccupancy();

    private static TileMap grass(int width, int height) {
        TileMap map = new TileMap(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    private void run(Skeleton enemy, TileMap map, Player player, double seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            enemy.update(STEP, new CreatureContext(map, player, occupancy));
        }
    }

    private static boolean isNextTo(Skeleton enemy, Player player) {
        return Math.max(Math.abs(enemy.getCurrentTileX() - player.getCurrentTileX()),
                Math.abs(enemy.getCurrentTileY() - player.getCurrentTileY())) <= 1;
    }

    @Test
    void aTileBlockedUnderAStepInProgressSendsTheEnemyBackAndTheChaseResumes() {
        TileMap map = grass(30, 20);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(13, 5);
        run(skeleton, map, player, 0.1);
        assertTrue(skeleton.getPosX() > 5.0 && skeleton.getPosX() < 6.0, "precondition: between two tiles");
        assertEquals(6, skeleton.getTargetTileX(), "precondition: walking to the tile that gets blocked");

        map.getTile(6, 5).setWorldResource(new Resource(ResourceType.STONE, 1));
        skeleton.update(STEP, new CreatureContext(map, player, occupancy));

        assertFalse(OccupiedTiles.of(List.of(skeleton)).contains(new PathNode(6, 5)),
                "the blocked tile is no longer reported as the one it steps to");

        boolean wasOnATileCentre = false;
        for (int i = 0; i < 60 * 5; i++) {
            skeleton.update(STEP, new CreatureContext(map, player, occupancy));
            assertFalse(Math.round(skeleton.getPosX()) == 6 && Math.round(skeleton.getPosY()) == 5,
                    "drawn over the blocked tile");
            wasOnATileCentre |= skeleton.getPosX() == skeleton.getCurrentTileX()
                    && skeleton.getPosY() == skeleton.getCurrentTileY();
        }
        assertTrue(wasOnATileCentre, "it came back to a tile centre");
        assertTrue(skeleton.getCurrentTileX() >= 8, "and went on towards the player: " + skeleton.getCurrentTileX());
    }

    @Test
    void aTileBlockedAheadOfAnEnemyOnATileCentreSendsItAroundIt() {
        TileMap map = grass(30, 20);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(13, 5);
        map.getTile(6, 5).setWorldResource(new Resource(ResourceType.STONE, 1));

        run(skeleton, map, player, 4.0);

        assertTrue(skeleton.getCurrentTileX() >= 8);
    }

    @Test
    void anEnemyNeverEntersTheTileThePlayerIsSteppingTo() {
        TileMap map = grass(30, 20);
        Skeleton skeleton = new Skeleton(8, 10);
        Player player = new Player(10, 10);
        assertTrue(player.walkAlong(List.of(new PathNode(9, 10))), "precondition: the player steps toward the enemy");

        for (int i = 0; i < 60; i++) {
            skeleton.update(STEP, new CreatureContext(map, player, occupancy));
            assertEquals(8.0, skeleton.getPosX(), 1e-9, "stepped toward the player's tile at update " + i);
            assertEquals(8, skeleton.getTargetTileX());
        }
    }

    @Test
    void aPlayerChangingTileSoonAfterASearchDoesNotStartAnotherOneBeforeTheSpacingHasPassed() {
        TileMap map = grass(40, 30);
        Skeleton skeleton = new Skeleton(10, 10);
        Player player = new Player(20, 10);
        run(skeleton, map, player, 0.2);

        player.setPosition(10, 17);
        run(skeleton, map, player, 0.15);
        assertEquals(10, skeleton.getTargetTileY(), "still on the path to the old tile, no new search yet");

        run(skeleton, map, player, 1.0);
        assertTrue(skeleton.getTargetTileY() > 10, "the new position is followed once the spacing has passed");
    }

    @Test
    void aPlayerThatMovesMidChaseIsFollowedWithoutWaitingForTheOldPathToEnd() {
        TileMap map = grass(40, 30);
        Skeleton skeleton = new Skeleton(10, 10);
        Player player = new Player(10, 3);
        run(skeleton, map, player, 0.4);

        player.setPosition(17, 10);
        run(skeleton, map, player, 3.0);

        assertTrue(isNextTo(skeleton, player),
                "the skeleton is at " + skeleton.getCurrentTileX() + "," + skeleton.getCurrentTileY());
    }

    /** A corridor one tile wide, a chasing skeleton at its start, a motionless slime right ahead of it and the player far on. */
    private static final class Corridor {
        final TileMap map = grass(30, 1);
        final CreatureOccupancy occupancy = new CreatureOccupancy();
        final Skeleton skeleton = new Skeleton(5, 0);
        final Slime blocker = new Slime(6, 0);
        final Player player = new Player(13, 0);
        final SkeletonMovementStrategy movement = (SkeletonMovementStrategy) skeleton.getMovementStrategy();

        Corridor() {
            occupancy.sync(skeleton);
            occupancy.sync(blocker);
            movement.alert();
        }

        void run(double seconds) {
            for (int i = 0; i < Math.round(seconds / STEP); i++) {
                player.heal(1_000);
                skeleton.update(STEP, new CreatureContext(map, player, occupancy));
            }
        }

        void runTicks(int ticks) {
            for (int i = 0; i < ticks; i++) {
                player.heal(1_000);
                skeleton.update(STEP, new CreatureContext(map, player, occupancy));
            }
        }
    }

    @Test
    void aStepHeldByAnotherEnemyIsWaitedOutWithoutASearchAndTheWaitGrowsWhileItStaysHeld() {
        final Corridor corridor = new Corridor();

        corridor.run(0.1);
        assertEquals(1, corridor.movement.pathSearchCount(), "one search found the path");
        corridor.run(0.14);
        assertEquals(1, corridor.movement.pathSearchCount(), "no search while the first wait runs");

        corridor.run(10);

        assertEquals(5, corridor.skeleton.getCurrentTileX(), "the held tile is never entered");
        // Waits of 0.15 s to 0.45 s without a backoff would search about three times a second
        assertTrue(corridor.movement.pathSearchCount() <= 10,
                "searched " + corridor.movement.pathSearchCount() + " times in ten seconds");
    }

    @Test
    void theChaseResumesOnceTheHeldStepIsLetGo() {
        final Corridor corridor = new Corridor();
        corridor.run(3);
        assertEquals(5, corridor.skeleton.getCurrentTileX(), "precondition: held back");

        corridor.occupancy.release(corridor.blocker);
        corridor.run(8);

        assertTrue(isNextTo(corridor.skeleton, corridor.player),
                "the skeleton is at " + corridor.skeleton.getCurrentTileX());
    }

    @Test
    void aPlayerChangingTileDoesNotMakeABlockedEnemySearchBeforeItsWaitHasEnded() {
        final Corridor corridor = new Corridor();
        // Past the doubling: the wait is at its longest, 1.5 s, whatever the first random wait was
        corridor.run(6);
        final int searchesBefore = corridor.movement.pathSearchCount();
        for (int i = 0; i < 180 && corridor.movement.pathSearchCount() == searchesBefore; i++) {
            corridor.runTicks(1);
        }
        assertTrue(corridor.movement.pathSearchCount() > searchesBefore, "precondition: a search just ran");

        corridor.player.setPosition(12, 0);
        final int searches = corridor.movement.pathSearchCount();
        corridor.runTicks(86);

        assertEquals(searches, corridor.movement.pathSearchCount(), "searched before the wait of 1.5 s had ended");
        corridor.runTicks(10);
        assertTrue(corridor.movement.pathSearchCount() > searches, "the moved player is searched for once the wait ends");
    }

    @Test
    void aTileTakenWhileTheEnemyWalksToItSendsTheEnemyBackToItsTileCentre() {
        final Corridor corridor = new Corridor();
        corridor.occupancy.release(corridor.blocker);
        corridor.run(0.1);
        assertTrue(corridor.skeleton.getPosX() > 5.0 && corridor.skeleton.getPosX() < 6.0,
                "precondition: between two tiles");
        assertEquals(6, corridor.skeleton.getTargetTileX(), "precondition: walking to tile 6");

        // The tile is now held by the slime, as if the claim of the skeleton had been lost
        corridor.occupancy.clear();
        corridor.occupancy.sync(corridor.blocker);
        double farthest = 0;
        for (int i = 0; i < 60 * 3; i++) {
            corridor.runTicks(1);
            farthest = Math.max(farthest, corridor.skeleton.getPosX());
        }

        assertEquals(5, corridor.skeleton.getCurrentTileX());
        assertTrue(farthest < 6.0, "walked onto the tile the slime holds: " + farthest);
        assertEquals(5.0, corridor.skeleton.getPosX(), 1e-9, "stands on a tile centre");
    }
}
