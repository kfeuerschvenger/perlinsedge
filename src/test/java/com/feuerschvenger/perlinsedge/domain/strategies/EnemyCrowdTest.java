package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.pathfinding.EnemyOccupancy;
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
         * Runs like {@link #run(double)} while the player jumps to the next of the given tiles every
         * {@code ticksPerStop} ticks, skipping a tile an enemy holds, so the crowd has to follow a moving player.
         */
        void runWithWanderingPlayer(double seconds, int ticksPerStop, int[][] stops) {
            int next = 0;
            for (int tick = 0; tick < Math.round(seconds / STEP); tick++) {
                if (tick > 0 && tick % ticksPerStop == 0) {
                    final int[] stop = stops[next++ % stops.length];
                    if (occupancy.ownerOf(stop[0], stop[1]) == null) {
                        player.setPosition(stop[0], stop[1]);
                    }
                }
                player.heal(1_000);
                for (Enemy enemy : enemies) {
                    enemy.update(STEP, map, player, occupancy);
                }
                assertNoSharedTile(tick);
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
        scene.runWithWanderingPlayer(40, 150, stops);
        walkers.runWithWanderingPlayer(40, 150, stops);
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

    @Test
    void twoSlimesStartingTogetherTakeOffForTheirSecondHopAtDifferentTimes() {
        final TileMap map = openGround();
        final int[] takeOffTick = new int[2];
        final Slime[] slimes = {new Slime(10, 10), new Slime(10, 30)};
        final Player[] players = {new Player(20, 10), new Player(20, 30)};
        for (int i = 0; i < 2; i++) {
            ((SlimeMovementStrategy) slimes[i].getMovementStrategy()).alert();
            final SlimeMovementStrategy movement = (SlimeMovementStrategy) slimes[i].getMovementStrategy();
            int takeOffs = 0;
            boolean wasJumping = false;
            for (int tick = 0; tick < 600 && takeOffs < 2; tick++) {
                slimes[i].update(STEP, map, players[i]);
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

    @Test
    void aBlockedHordeSearchesAtMostOncePerReplanWaitNotOnEveryTick() {
        final Scene scene = corridorScene(true, 10);
        final int ticks = 60 * 30;

        scene.run(30);

        for (Enemy enemy : scene.enemies) {
            final int searches = ((SlimeMovementStrategy) enemy.getMovementStrategy()).pathSearchCount();
            assertTrue(searches <= ticks / 9, "a slime searched " + searches + " times in " + ticks + " ticks");
        }
    }

    @Test
    void aBlockedWalkingHordeSearchesAtMostOncePerReplanWaitNotOnEveryTick() {
        final Scene scene = corridorScene(false, 10);
        final int ticks = 60 * 30;

        scene.run(30);

        for (Enemy enemy : scene.enemies) {
            final int searches = ((SkeletonMovementStrategy) enemy.getMovementStrategy()).pathSearchCount();
            assertTrue(searches <= ticks / 9, "a skeleton searched " + searches + " times in " + ticks + " ticks");
        }
    }
}
