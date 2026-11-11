package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.LinkedList;
import java.util.List;

/**
 * Movement of a skeleton: it stands still until the player comes within {@link #AGGRO_RANGE_TILES},
 * then walks the shortest path towards the player and stops on a tile next to them, diagonals included.
 * The path is searched again every {@link #REPATH_INTERVAL_SECONDS} so the skeleton follows a moving player,
 * but only once the skeleton stands on a tile centre: a search from the middle of a step would start from the
 * tile it left and could send it back across the corner of a blocked tile.
 * The skeleton never enters the tile of the player.
 */
public class SkeletonMovementStrategy implements EnemyMovementStrategy {
    /** Distance in tiles at which a skeleton notices the player. */
    public static final double AGGRO_RANGE_TILES = 10.0;

    private static final double MOVE_SPEED = 2.5; // Tiles per second
    private static final double REPATH_INTERVAL_SECONDS = 0.5;
    private static final double AT_TILE_CENTRE_EPSILON = 1e-9;

    private final LinkedList<PathNode> path = new LinkedList<>();
    private final PathPlanner pathPlanner = new PathPlanner();
    private double secondsUntilRepath;

    @Override
    public void updateMovement(Enemy enemy, double deltaTime, TileMap map, Player player) {
        enemy.snapshot();
        pathPlanner.tick(deltaTime);
        secondsUntilRepath -= deltaTime;

        if (!shouldChase(enemy, player)) {
            finishCurrentStepOnly(enemy);
            follow(enemy, deltaTime, player);
            return;
        }

        if (!isMidStep(enemy) && (path.isEmpty() || secondsUntilRepath <= 0)) {
            replan(enemy, map, player);
        }
        follow(enemy, deltaTime, player);
    }

    private boolean shouldChase(Enemy enemy, Player player) {
        return !player.isDead()
                && distanceToPlayer(enemy, player) <= AGGRO_RANGE_TILES
                && !isNextToPlayer(enemy, player);
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
            // The path ends on the tile of the player, the skeleton stops one tile short of it
            if (!path.isEmpty() && isPlayerTile(path.getLast(), player)) {
                path.removeLast();
            }
        }
    }

    private void follow(Enemy enemy, double deltaTime, Player player) {
        double remaining = MOVE_SPEED * deltaTime;
        while (remaining > 0 && !path.isEmpty()) {
            PathNode next = path.peek();
            if (isPlayerTile(next, player)) {
                turnBackToTileCentre(enemy);
                next = path.peek();
                if (next == null) {
                    return;
                }
            }

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

    /** Drops the rest of the path but keeps the step in progress, so the skeleton never stops between two tiles. */
    private void finishCurrentStepOnly(Enemy enemy) {
        if (path.isEmpty()) {
            return;
        }
        if (isMidStep(enemy)) {
            PathNode inProgress = path.peek();
            path.clear();
            path.add(inProgress);
        } else {
            path.clear();
        }
    }

    /** The player stepped onto the tile the skeleton is walking to: go back to the tile it left and wait there. */
    private void turnBackToTileCentre(Enemy enemy) {
        path.clear();
        if (isMidStep(enemy)) {
            path.add(new PathNode(enemy.getCurrentTileX(), enemy.getCurrentTileY()));
        }
    }

    private static boolean isPlayerTile(PathNode node, Player player) {
        return node.x == player.getCurrentTileX() && node.y == player.getCurrentTileY();
    }

    private static boolean isMidStep(Enemy enemy) {
        return Math.abs(enemy.getPosX() - enemy.getCurrentTileX()) > AT_TILE_CENTRE_EPSILON
                || Math.abs(enemy.getPosY() - enemy.getCurrentTileY()) > AT_TILE_CENTRE_EPSILON;
    }

    /** Chebyshev distance of one tile or less: the eight tiles around the player, and the player's own tile. */
    private static boolean isNextToPlayer(Enemy enemy, Player player) {
        return Math.max(Math.abs(player.getCurrentTileX() - enemy.getCurrentTileX()),
                Math.abs(player.getCurrentTileY() - enemy.getCurrentTileY())) <= 1;
    }

    private static double distanceToPlayer(Enemy enemy, Player player) {
        return Math.hypot(player.getCurrentTileX() - enemy.getCurrentTileX(),
                player.getCurrentTileY() - enemy.getCurrentTileY());
    }
}
