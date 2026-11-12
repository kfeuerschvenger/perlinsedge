package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.Collections;
import java.util.List;

/**
 * Plans how the player gets within reach of something to interact with.
 * Reach is the eight tiles around the target and the target itself. When the player is not in reach the plan is the
 * cheapest walk to one of the walkable tiles around the target.
 */
public final class InteractionPlanner {

    /** Largest distance in tiles, on either axis, from which the player can interact with a tile. */
    public static final int REACH = 1;

    /** Bound on the tiles one search may expand, so a target walled in by water does not search the whole map. */
    private static final int SEARCH_LIMIT = 6000;

    /** Not instantiable: the class only holds static rules. */
    private InteractionPlanner() {}

    /**
     * Tells whether a player standing on one tile can interact with another.
     *
     * @param playerX player column
     * @param playerY player row
     * @param targetX target column
     * @param targetY target row
     * @return true when the target is on the same tile or one of the eight around it
     */
    public static boolean inReach(int playerX, int playerY, int targetX, int targetY) {
        return Math.abs(playerX - targetX) <= REACH && Math.abs(playerY - targetY) <= REACH;
    }

    /**
     * Plans the walk that brings the player within reach of a tile.
     *
     * @param map     the map
     * @param fromX   player column
     * @param fromY   player row
     * @param targetX target column
     * @param targetY target row
     * @return an empty list when the player is already in reach, the cheapest path to a walkable tile around the target
     *         otherwise, or null when no such tile can be reached
     */
    public static List<PathNode> pathToReach(TileMap map, int fromX, int fromY, int targetX, int targetY) {
        if (inReach(fromX, fromY, targetX, targetY)) {
            return Collections.emptyList();
        }
        final Pathfinder pathfinder = new Pathfinder(map);
        List<PathNode> best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        for (int[] direction : IsometricUtils.ALL_DIRECTIONS) {
            final int standX = targetX + direction[0];
            final int standY = targetY + direction[1];
            final Tile stand = map.getTile(standX, standY);
            if (stand == null || !stand.isWalkable()) {
                continue;
            }
            final List<PathNode> path = pathfinder.findPath(fromX, fromY, standX, standY, SEARCH_LIMIT);
            if (path == null || path.isEmpty()) {
                continue;
            }
            final double cost = cost(path, fromX, fromY);
            if (cost < bestCost) {
                best = path;
                bestCost = cost;
            }
        }
        return best;
    }

    /** The length of a path in tiles, a diagonal step counting its true length. */
    private static double cost(List<PathNode> path, int fromX, int fromY) {
        double total = 0;
        int x = fromX;
        int y = fromY;
        for (PathNode node : path) {
            total += Math.hypot(node.x - x, node.y - y);
            x = node.x;
            y = node.y;
        }
        return total;
    }
}
