package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.LinkedList;
import java.util.List;

/**
 * Movement of a skeleton: it stands still until the player comes within {@link #AGGRO_RANGE_TILES},
 * then walks the shortest path towards the player and stops next to them.
 * The path is searched again every {@link #REPATH_INTERVAL_SECONDS} so the skeleton follows a moving player.
 */
public class SkeletonMovementStrategy implements EnemyMovementStrategy {
    /** Distance in tiles at which a skeleton notices the player. */
    public static final double AGGRO_RANGE_TILES = 10.0;

    private static final double MOVE_SPEED = 2.5; // Tiles per second
    private static final double STOP_DISTANCE_TILES = 1.0;
    private static final double REPATH_INTERVAL_SECONDS = 0.5;

    private final LinkedList<PathNode> path = new LinkedList<>();
    private final PathPlanner pathPlanner = new PathPlanner();
    private double secondsUntilRepath;

    @Override
    public void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player) {
        enemy.snapshot();
        pathPlanner.tick(deltaTime);
        secondsUntilRepath -= deltaTime;

        if (player.isDead() || distanceToPlayer(enemy, player) > AGGRO_RANGE_TILES) {
            path.clear();
            return;
        }
        if (distanceToPlayer(enemy, player) <= STOP_DISTANCE_TILES) {
            path.clear();
            return;
        }

        if (path.isEmpty() || secondsUntilRepath <= 0) {
            replan(enemy, map, player);
        }
        follow(enemy, deltaTime);
    }

    private void replan(Enemy enemy, TileMap map, Player player) {
        secondsUntilRepath = REPATH_INTERVAL_SECONDS;
        List<PathNode> found = pathPlanner.findPath(
                map,
                enemy.getCurrentTileX(), enemy.getCurrentTileY(),
                player.getCurrentTileX(), player.getCurrentTileY());
        if (found != null) {
            path.clear();
            path.addAll(found);
        }
    }

    private void follow(Enemy enemy, double deltaTime) {
        double remaining = MOVE_SPEED * deltaTime;
        while (remaining > 0 && !path.isEmpty()) {
            PathNode next = path.peek();
            double dx = next.x - enemy.getPosX();
            double dy = next.y - enemy.getPosY();
            double distance = Math.hypot(dx, dy);

            if (distance <= remaining) {
                enemy.setPosX(next.x);
                enemy.setPosY(next.y);
                enemy.setCurrentTileX(next.x);
                enemy.setCurrentTileY(next.y);
                enemy.setTargetTileX(next.x);
                enemy.setTargetTileY(next.y);
                path.poll();
                remaining -= distance;
            } else {
                enemy.setPosX(enemy.getPosX() + dx / distance * remaining);
                enemy.setPosY(enemy.getPosY() + dy / distance * remaining);
                remaining = 0;
            }
        }
    }

    private static double distanceToPlayer(Enemy enemy, Player player) {
        return Math.hypot(player.getCurrentTileX() - enemy.getCurrentTileX(),
                player.getCurrentTileY() - enemy.getCurrentTileY());
    }
}
