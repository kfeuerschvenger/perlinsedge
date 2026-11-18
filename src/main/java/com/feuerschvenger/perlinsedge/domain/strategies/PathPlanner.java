package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import com.feuerschvenger.perlinsedge.domain.pathfinding.Pathfinder;
import com.feuerschvenger.perlinsedge.domain.pathfinding.TilePenalty;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.List;
import java.util.Optional;

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
    private int searches;

    /** @return how many searches have run, not counting the calls refused while backing off */
    int searchCount() {
        return searches;
    }

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
     * @return the path, or empty when none was found or the planner is backing off
     */
    Optional<List<PathNode>> findPath(TileMap map, int startX, int startY, int targetX, int targetY) {
        return findPath(map, startX, startY, targetX, targetY, TilePenalty.NONE);
    }

    /**
     * Searches for a path that avoids costly tiles, unless the planner is backing off.
     *
     * @param map     map to search
     * @param startX  start tile X
     * @param startY  start tile Y
     * @param targetX target tile X
     * @param targetY target tile Y
     * @param penalty extra cost of entering a tile, such as one another enemy holds
     * @return the path, or empty when none was found or the planner is backing off
     */
    Optional<List<PathNode>> findPath(TileMap map, int startX, int startY, int targetX, int targetY,
                                      TilePenalty penalty) {
        if (isBackingOff()) {
            return Optional.empty();
        }

        searches++;
        Optional<List<PathNode>> path = new Pathfinder(map, null, penalty)
                .findPath(startX, startY, targetX, targetY, MAX_EXPANDED_NODES);
        if (path.isEmpty()) {
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
