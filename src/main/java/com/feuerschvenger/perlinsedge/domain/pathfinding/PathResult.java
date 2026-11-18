package com.feuerschvenger.perlinsedge.domain.pathfinding;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * The outcome of a path search. Whether a target can be reached and what the route to it is are two separate facts:
 * a start that already is the goal is reachable by an empty route, while a target the search cannot get to has no
 * route at all. An empty list therefore cannot stand for both, which is why the outcome is not an optional list.
 *
 * @param nodes     the tiles to walk, excluding the start; empty when the search did not find a route or when the
 *                  start already is the goal
 * @param reachable true when the search found the goal, false when it did not within its limits
 */
public record PathResult(List<PathNode> nodes, boolean reachable) {

    private static final PathResult UNREACHABLE = new PathResult(List.of(), false);

    /**
     * Wraps the route of a successful search.
     *
     * @param nodes the tiles to walk, excluding the start
     * @return a reachable outcome carrying the route
     */
    public static PathResult found(List<PathNode> nodes) {
        return new PathResult(nodes, true);
    }

    /**
     * Gives the outcome of a search that found no route.
     *
     * @return the shared unreachable outcome
     */
    public static PathResult unreachable() {
        return UNREACHABLE;
    }

    /**
     * Gives the route of a search that is expected to have succeeded.
     *
     * @return the tiles to walk, excluding the start
     * @throws NoSuchElementException when the search found no route
     */
    public List<PathNode> requireNodes() {
        if (!reachable) {
            throw new NoSuchElementException("The search found no route");
        }
        return nodes;
    }
}
