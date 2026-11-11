package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.List;

/**
 * Bounds the cost of an enemy chasing the player.
 * Each search is capped, and after a search that finds nothing the planner refuses to search again for a growing
 * delay. Without that, an enemy aggroed on a player it cannot reach would expand every reachable tile of the map
 * on every step.
 */
final class PathPlanner {

    /** Largest number of tiles one search may expand. */
    static final int MAX_EXPANDED_NODES = 1_500;
    /** Wait after the first failed search, in seconds. */
    static final double INITIAL_BACKOFF_SECONDS = 0.5;
    /** Longest wait between searches, in seconds. */
    static final double MAX_BACKOFF_SECONDS = 4.0;

    private double backoffSeconds;
    private double secondsUntilNextSearch;

    /**
     * Advances the retry timer.
     *
     * @param deltaTime time since the previous call in seconds
     */
    void tick(double deltaTime) {
        secondsUntilNextSearch = Math.max(0, secondsUntilNextSearch - deltaTime);
    }

    /** @return true while a recent failed search keeps new searches from running */
    boolean isBackingOff() {
        return secondsUntilNextSearch > 0;
    }

    /**
     * Searches for a path unless the planner is backing off.
     *
     * @param map     map to search
     * @param startX  start tile X
     * @param startY  start tile Y
     * @param targetX target tile X
     * @param targetY target tile Y
     * @return the path, or null when none was found or the planner is backing off
     */
    List<PathNode> findPath(TileMap map, int startX, int startY, int targetX, int targetY) {
        if (isBackingOff()) {
            return null;
        }

        List<PathNode> path = new Pathfinder(map).findPath(startX, startY, targetX, targetY, MAX_EXPANDED_NODES);
        if (path == null) {
            backoffSeconds = backoffSeconds == 0
                    ? INITIAL_BACKOFF_SECONDS
                    : Math.min(MAX_BACKOFF_SECONDS, backoffSeconds * 2);
            secondsUntilNextSearch = backoffSeconds;
        } else {
            backoffSeconds = 0;
        }
        return path;
    }
}
