package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Mimic;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnemyBehaviourTest {

    private static final double STEP = 1.0 / 60.0;

    private static TileMap grass(int size) {
        TileMap map = new TileMap(size, size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    /** A map where the 3x3 block around the given tile is walled in, so nothing outside can reach it. */
    private static void wallIn(TileMap map, int cx, int cy) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    map.getTile(cx + dx, cy + dy).setBuilding(new Wall(cx + dx, cy + dy));
                }
            }
        }
    }

    // ------------------------------------------------------------------ pathfinder cap

    @Test
    void searchGivesUpAtTheNodeCapWhenTheTargetIsUnreachable() {
        TileMap map = grass(60);
        wallIn(map, 50, 50);
        Pathfinder bounded = new Pathfinder(map);

        assertNull(bounded.findPath(5, 5, 50, 50, 200));
        assertEquals(201, bounded.getLastExpandedNodes(), "stops right after the cap instead of flooding the map");

        Pathfinder unbounded = new Pathfinder(map);
        assertNull(unbounded.findPath(5, 5, 50, 50));
        assertTrue(unbounded.getLastExpandedNodes() > 3000, "without a cap every reachable tile is expanded");
    }

    @Test
    void reachableTargetWithinTheCapStillGetsAPath() {
        List<PathNode> path = new Pathfinder(grass(60)).findPath(5, 5, 20, 20, 5000);

        assertNotNull(path);
        assertEquals(15, path.size());
    }

    // ------------------------------------------------------------------ planner backoff

    @Test
    void plannerBacksOffAfterAFailedSearchAndDoublesTheWaitUpToTheLimit() {
        TileMap map = grass(60);
        wallIn(map, 50, 50);
        PathPlanner planner = new PathPlanner();

        assertNull(planner.findPath(map, 5, 5, 50, 50));
        assertTrue(planner.isBackingOff());

        planner.tick(PathPlanner.INITIAL_BACKOFF_SECONDS - 0.01);
        assertTrue(planner.isBackingOff());
        planner.tick(0.02);
        assertFalse(planner.isBackingOff());

        assertNull(planner.findPath(map, 5, 5, 50, 50));
        planner.tick(PathPlanner.INITIAL_BACKOFF_SECONDS + 0.01);
        assertTrue(planner.isBackingOff(), "the second wait is longer than the first");

        for (int i = 0; i < 10; i++) {
            planner.tick(PathPlanner.MAX_BACKOFF_SECONDS);
            planner.findPath(map, 5, 5, 50, 50);
        }
        planner.tick(PathPlanner.MAX_BACKOFF_SECONDS);
        assertFalse(planner.isBackingOff(), "the wait never exceeds the limit");
    }

    @Test
    void plannerDoesNotSearchWhileBackingOffAndResetsAfterSuccess() {
        TileMap map = grass(60);
        wallIn(map, 50, 50);
        PathPlanner planner = new PathPlanner();
        planner.findPath(map, 5, 5, 50, 50);

        assertNull(planner.findPath(map, 5, 5, 6, 6), "a reachable target is not searched during the backoff");

        planner.tick(PathPlanner.MAX_BACKOFF_SECONDS);
        assertNotNull(planner.findPath(map, 5, 5, 6, 6));
        assertFalse(planner.isBackingOff());
    }

    // ------------------------------------------------------------------ mimic

    @Test
    void mimicStaysStillWhenThePlayerIsFarAway() {
        TileMap map = grass(30);
        Mimic mimic = new Mimic(10, 10);
        Player player = new Player(25, 25);

        for (int i = 0; i < 120; i++) {
            mimic.update(STEP, map, player);
        }

        assertEquals(10, mimic.getCurrentTileX());
        assertEquals(10.0, mimic.getPosX());
        assertFalse(((MimicMovementStrategy) mimic.getMovementStrategy()).isAggroed());
    }

    @Test
    void mimicActivatesAndChasesWhenThePlayerApproaches() {
        TileMap map = grass(30);
        Mimic mimic = new Mimic(10, 10);
        Player player = new Player(10, 12);

        mimic.update(STEP, map, player);

        assertTrue(((MimicMovementStrategy) mimic.getMovementStrategy()).isAggroed());
        double before = Math.hypot(player.getCurrentTileX() - mimic.getPosX(), player.getCurrentTileY() - mimic.getPosY());
        for (int i = 0; i < 30; i++) {
            mimic.update(STEP, map, player);
        }
        double after = Math.hypot(player.getCurrentTileX() - mimic.getPosX(), player.getCurrentTileY() - mimic.getPosY());
        assertTrue(after < before, "the active mimic closes the distance");
    }

    @Test
    void mimicWakesWhenHitFromOutsideItsRange() {
        Mimic mimic = new Mimic(10, 10);

        mimic.takeDamage(1);

        assertTrue(((MimicMovementStrategy) mimic.getMovementStrategy()).isAggroed());
    }

    @Test
    void activeMimicStrikesAnAdjacentPlayer() {
        TileMap map = grass(30);
        Mimic mimic = new Mimic(10, 10);
        Player player = new Player(10, 11);
        double before = player.getHealth();

        mimic.update(STEP, map, player);
        mimic.update(STEP, map, player);

        assertTrue(player.getHealth() < before);
    }

    // ------------------------------------------------------------------ skeleton

    @Test
    void skeletonIgnoresAPlayerBeyondItsAggroRange() {
        TileMap map = grass(40);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(30, 30);

        for (int i = 0; i < 120; i++) {
            skeleton.update(STEP, map, player);
        }

        assertEquals(5, skeleton.getCurrentTileX());
        assertEquals(5, skeleton.getCurrentTileY());
    }

    @Test
    void skeletonWalksTowardsANearbyPlayer() {
        TileMap map = grass(40);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(12, 5);

        for (int i = 0; i < 60 * 10; i++) {
            skeleton.update(STEP, map, player);
        }

        double distance = Math.hypot(player.getCurrentTileX() - skeleton.getCurrentTileX(),
                player.getCurrentTileY() - skeleton.getCurrentTileY());
        assertTrue(distance <= 1.5, "the skeleton ends up next to the player, was " + distance);
    }

    @Test
    void skeletonStrikesInMeleeOncePerCooldown() {
        TileMap map = grass(40);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(6, 5);
        double start = player.getHealth();

        skeleton.update(STEP, map, player);
        double afterFirst = player.getHealth();
        skeleton.update(STEP, map, player);

        assertEquals(start - skeleton.getAttackDamage(), afterFirst, 1e-9);
        assertEquals(afterFirst, player.getHealth(), 1e-9, "the cooldown blocks the second strike");
    }

    @Test
    void skeletonCannotStrikeFromFarAway() {
        TileMap map = grass(40);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(9, 5);
        double start = player.getHealth();

        skeleton.update(STEP, map, player);

        assertEquals(start, player.getHealth(), 1e-9);
    }

    @Test
    void skeletonSurvivesAnUnreachablePlayerWithoutFloodingTheMap() {
        TileMap map = grass(40);
        wallIn(map, 12, 5);
        Skeleton skeleton = new Skeleton(5, 5);
        Player player = new Player(12, 5);

        for (int i = 0; i < 300; i++) {
            skeleton.update(STEP, map, player);
        }

        assertTrue(skeleton.getCurrentTileX() <= 11);
    }
}
