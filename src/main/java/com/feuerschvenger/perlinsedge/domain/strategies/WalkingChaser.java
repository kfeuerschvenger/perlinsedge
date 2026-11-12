package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.config.AppConfig.EnemyConfig.ChaseRange;
import com.feuerschvenger.perlinsedge.domain.entities.Player;
import com.feuerschvenger.perlinsedge.domain.entities.enemies.Enemy;
import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.LinkedList;
import java.util.List;

/**
 * The walking chase shared by the enemies that follow the player on foot: it walks the shortest path to the player
 * and stops on a tile next to them, diagonals included, and it gives up under the rules of {@link ChaseLeash}.
 * The enemy only ever walks tile centre to tile centre along a path that the pathfinder built from walkable tiles
 * without cutting a corner, so it never enters water, a resource or a building.
 * The path is searched again every {@link #REPATH_INTERVAL_SECONDS} so the enemy follows a moving player, but only
 * once it stands on a tile centre: a search from the middle of a step would start from the tile it left and could
 * send it back across the corner of a blocked tile.
 * The enemy never enters the tile of the player.
 */
final class WalkingChaser {

    private static final double REPATH_INTERVAL_SECONDS = 0.5;
    private static final double AT_TILE_CENTRE_EPSILON = 1e-9;

    private final double speedTilesPerSecond;
    private final ChaseLeash leash;
    private final LinkedList<PathNode> path = new LinkedList<>();
    private final PathPlanner pathPlanner = new PathPlanner();
    private double secondsUntilRepath;

    /**
     * Creates a chaser that is not chasing.
     *
     * @param speedTilesPerSecond walking speed
     * @param range               aggro, leash and lose distances
     */
    WalkingChaser(double speedTilesPerSecond, ChaseRange range) {
        this.speedTilesPerSecond = speedTilesPerSecond;
        this.leash = new ChaseLeash(range);
    }

    /**
     * Runs one update of the chase and walks the enemy along its path.
     *
     * @param enemy     the enemy to move
     * @param deltaTime seconds since the previous update
     * @param map       the map to search and walk
     * @param player    the player being chased
     */
    void update(Enemy enemy, double deltaTime, TileMap map, Player player) {
        pathPlanner.tick(deltaTime);
        secondsUntilRepath -= deltaTime;

        final boolean chasing = leash.update(distanceToPlayer(enemy, player), player.isDead(), deltaTime);
        if (!chasing || isNextToPlayer(enemy, player)) {
            finishCurrentStepOnly(enemy);
        } else if (!isMidStep(enemy) && (path.isEmpty() || secondsUntilRepath <= 0)) {
            replan(enemy, map, player);
        }
        follow(enemy, deltaTime, map, player);
    }

    /** Starts a chase regardless of distance. */
    void alert() {
        leash.alert();
    }

    /** Ends the chase, the enemy finishes the step it is on. */
    void release() {
        leash.release();
    }

    /** @return true while the enemy is chasing the player */
    boolean isChasing() {
        return leash.isChasing();
    }

    private void replan(Enemy enemy, TileMap map, Player player) {
        secondsUntilRepath = REPATH_INTERVAL_SECONDS;
        final List<PathNode> found = pathPlanner.findPath(
                map,
                enemy.getCurrentTileX(), enemy.getCurrentTileY(),
                player.getCurrentTileX(), player.getCurrentTileY());
        if (found != null) {
            path.clear();
            path.addAll(found);
            // The path ends on the tile of the player, the enemy stops one tile short of it
            if (!path.isEmpty() && isPlayerTile(path.getLast(), player)) {
                path.removeLast();
            }
        }
    }

    private void follow(Enemy enemy, double deltaTime, TileMap map, Player player) {
        enemy.snapshot();
        double remaining = speedTilesPerSecond * deltaTime;
        while (remaining > 0 && !path.isEmpty()) {
            final PathNode next = path.peek();
            if (isPlayerTile(next, player)) {
                // The player stepped onto the tile the enemy is walking to: go back to the tile it left and wait
                turnBackToTileCentre(enemy);
                return;
            }
            if (!isWalkable(map, next)) {
                // Something was built on the path after it was searched: stop and search again from where it stands
                path.clear();
                secondsUntilRepath = 0;
                return;
            }

            final double dx = next.x - enemy.getPosX();
            final double dy = next.y - enemy.getPosY();
            final double distance = Math.hypot(dx, dy);

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
                enemy.setTargetTileX(next.x);
                enemy.setTargetTileY(next.y);
                remaining = 0;
            }
        }
    }

    /** Drops the rest of the path but keeps the step in progress, so the enemy never stops between two tiles. */
    private void finishCurrentStepOnly(Enemy enemy) {
        if (path.isEmpty()) {
            return;
        }
        if (isMidStep(enemy)) {
            final PathNode inProgress = path.peek();
            path.clear();
            path.add(inProgress);
        } else {
            path.clear();
        }
    }

    private void turnBackToTileCentre(Enemy enemy) {
        path.clear();
        enemy.setTargetTileX(enemy.getCurrentTileX());
        enemy.setTargetTileY(enemy.getCurrentTileY());
        if (isMidStep(enemy)) {
            path.add(new PathNode(enemy.getCurrentTileX(), enemy.getCurrentTileY()));
        }
    }

    private static boolean isWalkable(TileMap map, PathNode node) {
        if (node.x < 0 || node.y < 0 || node.x >= map.getWidth() || node.y >= map.getHeight()) {
            return false;
        }
        final Tile tile = map.getTile(node.x, node.y);
        return tile != null && tile.isWalkable();
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
