package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.OccupiedTiles;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every enemy stands on a tile of its own, at every tick, in the open and in a corridor one tile wide, for two enemies
 * and for a horde, and no enemy ever takes a tile of the player.
 */
class EnemyCrowdTest {

    private static final double STEP = 1.0 / 60.0;
    private static final int OPEN_SIZE = 41;
    private static final int CORRIDOR_LENGTH = 60;
    private static final int CORRIDOR_ROW = 3;

    /** A map, a player who stands still and the enemies that chase them, updated through one shared occupancy. */
    private static final class Scene {
        final TileMap map;
        final Player player;
        final List<Enemy> enemies = new ArrayList<>();
        final EnemyOccupancy occupancy = new EnemyOccupancy();

        Scene(TileMap map, int playerX, int playerY) {
            this.map = map;
            this.player = new Player(playerX, playerY);
        }

        void add(Enemy enemy) {
            enemies.add(enemy);
            occupancy.sync(enemy);
            if (enemy instanceof Slime slime) {
                ((SlimeMovementStrategy) slime.getMovementStrategy()).alert();
            } else if (enemy instanceof Skeleton skeleton) {
                ((SkeletonMovementStrategy) skeleton.getMovementStrategy()).alert();
            }
        }

        /** Runs the scene and checks at every tick that no two enemies share a tile and none takes the player's. */
        void run(double seconds) {
            for (int tick = 0; tick < Math.round(seconds / STEP); tick++) {
                player.heal(1_000);
                for (Enemy enemy : enemies) {
                    enemy.update(STEP, map, player, occupancy);
                }
                assertNoSharedTile(tick);
            }
        }

        /**
         * Runs like {@link #run(double)} while the player is ordered to walk to the next of the given tiles every
         * {@code ticksPerStop} ticks, around the tiles the enemies hold and halting before a tile they step to, as
         * the game does. The player has a walk in progress and a target tile of its own that moves, so the crowd has to
         * follow a moving player.
         *
         * @return how many tiles the player walked on, which tells that the player really moved
         */
        int runWithWanderingPlayer(double seconds, int ticksPerStop, int[][] stops) {
            int next = 0;
            int stepsTaken = 0;
            int lastX = player.getCurrentTileX();
            int lastY = player.getCurrentTileY();
            for (int tick = 0; tick < Math.round(seconds / STEP); tick++) {
                if (tick % ticksPerStop == 0) {
                    final int[] stop = stops[next++ % stops.length];
                    player.moveTo(map, stop[0], stop[1], OccupiedTiles.of(enemies));
                }
                player.heal(1_000);
                haltBeforeEnemies();
                player.update(STEP);
                // Arriving on a tile makes the next tile of the walk the target at once, so it is vetted again before
                // the enemies move: a target on a tile an enemy holds would be a tile the enemy stands on
                haltBeforeEnemies();
                if (player.getCurrentTileX() != lastX || player.getCurrentTileY() != lastY) {
                    stepsTaken++;
                    lastX = player.getCurrentTileX();
                    lastY = player.getCurrentTileY();
                }
                for (Enemy enemy : enemies) {
                    enemy.update(STEP, map, player, occupancy);
                }
                assertNoSharedTile(tick);
            }
            return stepsTaken;
        }

        /** Ends the walk of the player when its next tile is held by an enemy. */
        private void haltBeforeEnemies() {
            final PathNode ahead = player.getNextTile();
            if (ahead != null && OccupiedTiles.contains(enemies, ahead.x, ahead.y)) {
                player.haltBefore(OccupiedTiles.of(enemies));
            }
        }

        private void assertNoSharedTile(int tick) {
            final Map<Long, Enemy> seen = new HashMap<>();
            for (Enemy enemy : enemies) {
                final long current = tileKey(enemy.getCurrentTileX(), enemy.getCurrentTileY());
                final long target = tileKey(enemy.getTargetTileX(), enemy.getTargetTileY());
                claim(seen, current, enemy, tick);
                if (target != current) {
                    claim(seen, target, enemy, tick);
                }
                assertFalse(isPlayerTile(enemy.getCurrentTileX(), enemy.getCurrentTileY()),
                        "an enemy stands on the player at tick " + tick);
                assertFalse(isPlayerTile(enemy.getTargetTileX(), enemy.getTargetTileY()),
                        "an enemy moves onto the player at tick " + tick);
            }
        }

        private void claim(Map<Long, Enemy> seen, long key, Enemy enemy, int tick) {
            final Enemy other = seen.put(key, enemy);
            assertTrue(other == null || other == enemy,
                    "two enemies share the tile (" + (key >> 32) + ", " + (int) key + ") at tick " + tick);
        }

        private boolean isPlayerTile(int x, int y) {
            return (x == player.getCurrentTileX() && y == player.getCurrentTileY())
                    || (x == player.getTargetTileX() && y == player.getTargetTileY());
        }

        int adjacentToPlayer() {
            int count = 0;
            for (Enemy enemy : enemies) {
                if (Math.max(Math.abs(enemy.getCurrentTileX() - player.getCurrentTileX()),
                        Math.abs(enemy.getCurrentTileY() - player.getCurrentTileY())) <= 1) {
                    count++;
                }
            }
            return count;
        }
    }

    private static long tileKey(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static TileMap openGround() {
        final TileMap map = new TileMap(OPEN_SIZE, OPEN_SIZE);
        for (int x = 0; x < OPEN_SIZE; x++) {
            for (int y = 0; y < OPEN_SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        return map;
    }

    /** Water everywhere but a straight corridor one tile wide. */
    private static TileMap corridor() {
        final TileMap map = new TileMap(CORRIDOR_LENGTH, 2 * CORRIDOR_ROW + 1);
        for (int x = 0; x < CORRIDOR_LENGTH; x++) {
            for (int y = 0; y < 2 * CORRIDOR_ROW + 1; y++) {
                map.setTile(x, y, new Tile(x, y, y == CORRIDOR_ROW ? TileType.GRASS : TileType.DEEP_WATER));
            }
        }
        return map;
    }

    /** Puts the enemies on a ring around the middle of the open ground, evenly spread. */
    private static List<int[]> ring(int count, int radius) {
        final List<int[]> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final double angle = 2 * Math.PI * i / count;
            tiles.add(new int[]{OPEN_SIZE / 2 + (int) Math.round(radius * Math.cos(angle)),
                    OPEN_SIZE / 2 + (int) Math.round(radius * Math.sin(angle))});
        }
        return tiles;
    }

    private static Scene openScene(boolean slimes, int count, int radius) {
        final Scene scene = new Scene(openGround(), OPEN_SIZE / 2, OPEN_SIZE / 2);
        for (int[] tile : ring(count, radius)) {
            scene.add(slimes ? new Slime(tile[0], tile[1]) : new Skeleton(tile[0], tile[1]));
        }
        return scene;
    }

    private static Scene corridorScene(boolean slimes, int count) {
        final Scene scene = new Scene(corridor(), 30, CORRIDOR_ROW);
        for (int i = 0; i < count; i++) {
            final int x = 29 - i;
            scene.add(slimes ? new Slime(x, CORRIDOR_ROW) : new Skeleton(x, CORRIDOR_ROW));
        }
        return scene;
    }

    @Test
    void twoSlimesChasingThePlayerInTheOpenNeverShareATile() {
        final Scene scene = new Scene(openGround(), 20, 20);
        scene.add(new Slime(15, 20));
        scene.add(new Slime(15, 21));

        scene.run(20);

        assertEquals(2, scene.adjacentToPlayer(), "both slimes reach the player");
    }

    @Test
    void tenSlimesChasingThePlayerInTheOpenNeverShareATile() {
        final Scene scene = openScene(true, 10, 6);

        scene.run(30);

        assertTrue(scene.adjacentToPlayer() >= 7, "the horde spreads around the player: " + scene.adjacentToPlayer());
    }

    @Test
    void twoSlimesChasingThePlayerThroughACorridorNeverShareATile() {
        final Scene scene = corridorScene(true, 2);

        scene.run(40);

        assertTrue(scene.adjacentToPlayer() >= 1, "the first slime reaches the player");
    }

    @Test
    void tenSlimesChasingThePlayerThroughACorridorNeverShareATile() {
        final Scene scene = corridorScene(true, 10);

        scene.run(60);

        assertTrue(scene.adjacentToPlayer() >= 1, "the first slime reaches the player");
    }

    @Test
    void twoSkeletonsChasingThePlayerInTheOpenNeverShareATile() {
        final Scene scene = new Scene(openGround(), 20, 20);
        scene.add(new Skeleton(15, 20));
        scene.add(new Skeleton(15, 21));

        scene.run(15);

        assertEquals(2, scene.adjacentToPlayer(), "both skeletons reach the player");
    }

    @Test
    void tenSkeletonsChasingThePlayerInTheOpenNeverShareATile() {
        final Scene scene = openScene(false, 10, 8);

        scene.run(30);

        assertTrue(scene.adjacentToPlayer() >= 7, "the horde spreads around the player: " + scene.adjacentToPlayer());
    }

    @Test
    void tenSkeletonsChasingThePlayerThroughACorridorNeverShareATile() {
        final Scene scene = corridorScene(false, 10);

        scene.run(40);

        assertTrue(scene.adjacentToPlayer() >= 1, "the first skeleton reaches the player");
    }

    @Test
    void aCrowdFollowingAPlayerWhoKeepsMovingNeverSharesATile() {
        final Scene scene = openScene(true, 10, 6);
        final Scene walkers = openScene(false, 10, 8);

        final int[][] stops = {{23, 18}, {17, 22}, {22, 24}, {18, 17}, {20, 20}};
        final int slimeSteps = scene.runWithWanderingPlayer(40, 150, stops);
        final int walkerSteps = walkers.runWithWanderingPlayer(40, 150, stops);

        assertTrue(slimeSteps >= 20, "the player walked on only " + slimeSteps + " tiles");
        assertTrue(walkerSteps >= 20, "the player walked on only " + walkerSteps + " tiles");
    }

    @Test
    void aMixedCrowdOfSlimesAndSkeletonsNeverSharesATile() {
        final Scene scene = new Scene(openGround(), 20, 20);
        final List<int[]> tiles = ring(12, 7);
        for (int i = 0; i < tiles.size(); i++) {
            scene.add(i % 2 == 0 ? new Slime(tiles.get(i)[0], tiles.get(i)[1])
                    : new Skeleton(tiles.get(i)[0], tiles.get(i)[1]));
        }

        scene.run(30);

        assertTrue(scene.adjacentToPlayer() >= 6, "the crowd spreads around the player: " + scene.adjacentToPlayer());
    }

    @Test
    void slimesWithDifferentSpawnTilesRestForDifferentTimesBetweenHops() {
        final double first = ((SlimeMovementStrategy) new Slime(3, 4).getMovementStrategy()).restBetweenHops();
        final double second = ((SlimeMovementStrategy) new Slime(4, 4).getMovementStrategy()).restBetweenHops();
        final double again = ((SlimeMovementStrategy) new Slime(3, 4).getMovementStrategy()).restBetweenHops();

        assertNotEquals(first, second, "neighbours do not hop in lockstep");
        assertEquals(first, again, "the rest follows from the spawn tile");
    }

    private static double restAt(int x, int y) {
        return ((SlimeMovementStrategy) new Slime(x, y).getMovementStrategy()).restBetweenHops();
    }

    @Test
    void theRestsOfSlimesSpawnedOnARowAndOnABlockAreSpreadApart() {
        final List<double[]> rests = new ArrayList<>();
        for (int x = 0; x < 20; x++) {
            rests.add(new double[]{x, 0, restAt(x, 0)});
        }
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                rests.add(new double[]{100 + x, 50 + y, restAt(100 + x, 50 + y)});
            }
        }

        int pairs = 0;
        int apart = 0;
        for (int i = 0; i < rests.size(); i++) {
            for (int j = i + 1; j < rests.size(); j++) {
                pairs++;
                if (Math.abs(rests.get(i)[2] - rests.get(j)[2]) >= 0.005) {
                    apart++;
                }
            }
        }
        assertTrue(apart >= 0.9 * pairs, apart + " of " + pairs + " pairs of tiles differ by 5 ms or more");
    }

    @Test
    void slimesOnNeighbouringTilesNeverRestWithinATickOfEachOther() {
        for (int x = -30; x <= 30; x++) {
            for (int y = -30; y <= 30; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            assertTrue(Math.abs(restAt(x, y) - restAt(x + dx, y + dy)) > STEP,
                                    "tiles (" + x + ", " + y + ") and (" + (x + dx) + ", " + (y + dy) + ") rest alike");
                        }
                    }
                }
            }
        }
    }

    @Test
    void twoSlimesStartingTogetherTakeOffForTheirSecondHopAtDifferentTimes() {
        final TileMap map = openGround();
        final int[] takeOffTick = new int[2];
        final Slime[] slimes = {new Slime(10, 10), new Slime(10, 30)};
        final Player[] players = {new Player(20, 10), new Player(20, 30)};
        for (int i = 0; i < 2; i++) {
            final EnemyOccupancy occupancy = new EnemyOccupancy();
            ((SlimeMovementStrategy) slimes[i].getMovementStrategy()).alert();
            final SlimeMovementStrategy movement = (SlimeMovementStrategy) slimes[i].getMovementStrategy();
            int takeOffs = 0;
            boolean wasJumping = false;
            for (int tick = 0; tick < 600 && takeOffs < 2; tick++) {
                slimes[i].update(STEP, map, players[i], occupancy);
                if (movement.isJumping() && !wasJumping) {
                    takeOffs++;
                    takeOffTick[i] = tick;
                }
                wasJumping = movement.isJumping();
            }
            assertEquals(2, takeOffs, "slime " + i + " made two hops");
        }

        assertNotEquals(takeOffTick[0], takeOffTick[1], "the second hops are out of step");
    }

    /** The eight tiles around the player and then the tiles further out, 30 enemies in all, so the ring is full. */
    private static Scene packedScene(boolean slimes) {
        final Scene scene = new Scene(openGround(), OPEN_SIZE / 2, OPEN_SIZE / 2);
        int count = 0;
        for (int distance = 1; distance <= 3 && count < 30; distance++) {
            for (int dx = -distance; dx <= distance && count < 30; dx++) {
                for (int dy = -distance; dy <= distance && count < 30; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) == distance) {
                        final int x = OPEN_SIZE / 2 + dx;
                        final int y = OPEN_SIZE / 2 + dy;
                        scene.add(slimes ? new Slime(x, y) : new Skeleton(x, y));
                        count++;
                    }
                }
            }
        }
        return scene;
    }

    private static int totalSearches(Scene scene) {
        int total = 0;
        for (Enemy enemy : scene.enemies) {
            total += enemy.getMovementStrategy() instanceof SlimeMovementStrategy slime
                    ? slime.pathSearchCount()
                    : ((SkeletonMovementStrategy) enemy.getMovementStrategy()).pathSearchCount();
        }
        return total;
    }

    @Test
    void slimesBlockedByAFullRingAroundThePlayerBackOffInsteadOfSearchingAtEveryReplanWait() {
        final Scene scene = packedScene(true);

        scene.run(30);

        // The 22 slimes outside the ring are blocked all along. A search per 0.15 s to 0.45 s of wait would run into
        // the thousands over the 30 s; waiting on the held step and backing off to 1.5 s stays far below
        final int searches = totalSearches(scene);
        assertTrue(searches <= 22 * 30 / 1.5 + 60, "30 slimes searched " + searches + " times in 30 s");
    }

    @Test
    void skeletonsBlockedByAFullRingAroundThePlayerBackOffInsteadOfSearchingAtEveryReplanWait() {
        final Scene scene = packedScene(false);

        scene.run(30);

        final int searches = totalSearches(scene);
        assertTrue(searches <= 22 * 30 / 1.5 + 60, "30 skeletons searched " + searches + " times in 30 s");
    }

    @Test
    void aBlockedCorridorHordeSearchesFarLessOftenThanOncePerShortestWait() {
        final Scene slimes = corridorScene(true, 10);
        final Scene walkers = corridorScene(false, 10);

        slimes.run(30);
        walkers.run(30);

        assertTrue(totalSearches(slimes) <= 10 * 30 / 1.5 + 30, "slimes searched " + totalSearches(slimes));
        assertTrue(totalSearches(walkers) <= 10 * 30 / 1.5 + 30, "skeletons searched " + totalSearches(walkers));
    }
}
