package com.feuerschvenger.perlinsedge.app.managers;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Skeleton;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Slime;
import com.feuerschvenger.perlinsedge.domain.strategies.SkeletonMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.strategies.SlimeMovementStrategy;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A crowd of enemies follows a player who is ordered around the map, with the player and the enemies advanced in the
 * order the game advances them. No enemy ever shares a tile with another or with the tile the player stands on or
 * steps to, at any tick.
 */
class WanderingPlayerCrowdTest {

    private static final double STEP = 1.0 / 60.0;
    private static final int SIZE = 41;
    private static final int[][] STOPS = {{23, 18}, {17, 22}, {22, 24}, {18, 17}, {20, 20}};
    private static final int TICKS_PER_STOP = 150;

    private static List<Enemy> ring(boolean slimes, int count, int radius) {
        final List<Enemy> enemies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final double angle = 2 * Math.PI * i / count;
            final int x = SIZE / 2 + (int) Math.round(radius * Math.cos(angle));
            final int y = SIZE / 2 + (int) Math.round(radius * Math.sin(angle));
            final Enemy enemy = slimes ? new Slime(x, y) : new Skeleton(x, y);
            if (enemy instanceof Slime slime) {
                ((SlimeMovementStrategy) slime.getMovementStrategy()).alert();
            } else {
                ((SkeletonMovementStrategy) enemy.getMovementStrategy()).alert();
            }
            enemies.add(enemy);
        }
        return enemies;
    }

    private static void runCrowd(boolean slimes, int radius) {
        final GameStateManager state = new GameStateManager();
        final TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
        final Player player = new Player(SIZE / 2, SIZE / 2);
        state.setCurrentMap(map);
        state.setPlayer(player);
        final List<Enemy> enemies = ring(slimes, 10, radius);
        state.setEnemies(enemies);
        final PlayerManager players = new PlayerManager(state, null, null);
        final CombatManager combat = new CombatManager(state);

        int next = 0;
        int steps = 0;
        int lastX = player.getCurrentTileX();
        int lastY = player.getCurrentTileY();
        for (int tick = 0; tick < 40 * 60; tick++) {
            if (tick % TICKS_PER_STOP == 0) {
                final int[] stop = STOPS[next++ % STOPS.length];
                players.movePlayer(map, stop[0], stop[1]);
            }
            player.heal(1_000);
            players.advancePlayer(STEP);
            if (player.getCurrentTileX() != lastX || player.getCurrentTileY() != lastY) {
                steps++;
                lastX = player.getCurrentTileX();
                lastY = player.getCurrentTileY();
            }
            combat.updateEnemies(STEP);
            assertClear(enemies, player, tick);
        }
        assertTrue(steps >= 20, "the player walked on only " + steps + " tiles");
    }

    private static void assertClear(List<Enemy> enemies, Player player, int tick) {
        final Set<String> held = new HashSet<>();
        for (Enemy enemy : enemies) {
            final String current = enemy.getCurrentTileX() + "," + enemy.getCurrentTileY();
            final String target = enemy.getTargetTileX() + "," + enemy.getTargetTileY();
            assertTrue(held.add(current), "two enemies share the tile " + current + " at tick " + tick);
            if (!target.equals(current)) {
                assertTrue(held.add(target), "two enemies share the tile " + target + " at tick " + tick);
            }
            for (String tile : new String[]{current, target}) {
                assertFalse(tile.equals(player.getCurrentTileX() + "," + player.getCurrentTileY())
                                || tile.equals(player.getTargetTileX() + "," + player.getTargetTileY()),
                        "an enemy holds the tile " + tile + " the player stands on or steps to at tick " + tick);
            }
        }
    }

    @Test
    void aCrowdOfSlimesFollowingAPlayerWhoKeepsMovingNeverSharesATile() {
        runCrowd(true, 6);
    }

    @Test
    void aCrowdOfSkeletonsFollowingAPlayerWhoKeepsMovingNeverSharesATile() {
        runCrowd(false, 8);
    }
}
