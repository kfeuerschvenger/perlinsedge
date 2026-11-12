package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.*;

/**
 * A* pathfinding implementation optimized for isometric grid-based worlds.
 * Features 8-directional movement, efficient path reconstruction, and walkability validation.
 * A diagonal step is only allowed when both tiles it passes between are walkable, so paths never cut a corner.
 */
public final class Pathfinder {
    // Reuse direction constants from IsometricUtils to prevent duplication
    private static final int[][] MOVEMENT_DIRECTIONS = IsometricUtils.ALL_DIRECTIONS;
    private static final double[] DIRECTION_COSTS = precomputeDirectionCosts();

    private final TileMap map;
    private int lastExpandedNodes;
    /** Farthest a goal tile lies from the target of the search in progress, 0 when the target is the goal. */
    private double goalSlack;
    /** True while the search in progress ends on any tile next to the target instead of on the target. */
    private boolean goalIsNeighbour;

    /**
     * Constructs a Pathfinder instance for a specific game map.
     *
     * @param map The TileMap to perform pathfinding on
     */
    public Pathfinder(TileMap map) {
        this.map = Objects.requireNonNull(map, "TileMap must not be null");
    }

    // ==================================================================
    //  Pathfinding Core Algorithm
    // ==================================================================

    /**
     * Finds the optimal path between two points using A* algorithm.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     * @return List of nodes representing the path (excluding start position),
     *         or null if no valid path exists
     */
    public List<PathNode> findPath(int startX, int startY, int targetX, int targetY) {
        return findPath(startX, startY, targetX, targetY, Integer.MAX_VALUE);
    }

    /**
     * Finds a path like {@link #findPath(int, int, int, int)} but gives up once the search has expanded
     * {@code maxExpandedNodes} tiles. Searching for an unreachable target otherwise expands every reachable tile
     * of the map, so callers that search often bound the work with this cap.
     *
     * @param startX           Starting X coordinate
     * @param startY           Starting Y coordinate
     * @param targetX          Target X coordinate
     * @param targetY          Target Y coordinate
     * @param maxExpandedNodes largest number of tiles the search may expand, must be positive
     * @return the path excluding the start position, or null if none exists within the cap
     */
    public List<PathNode> findPath(int startX, int startY, int targetX, int targetY, int maxExpandedNodes) {
        return search(startX, startY, targetX, targetY, false, maxExpandedNodes);
    }

    /**
     * Finds the cheapest path to any walkable tile among the eight around a target, in one search. The target tile
     * itself is not a goal and does not need to be walkable, so this reaches something solid such as a building or a
     * resource from whichever side is cheapest.
     *
     * @param startX           Starting X coordinate
     * @param startY           Starting Y coordinate
     * @param targetX          X coordinate of the tile to get next to
     * @param targetY          Y coordinate of the tile to get next to
     * @param maxExpandedNodes largest number of tiles the search may expand, must be positive
     * @return the path excluding the start position (empty when the start is already next to the target), or null if
     *         no such tile can be reached within the cap
     */
    public List<PathNode> findPathNextTo(int startX, int startY, int targetX, int targetY, int maxExpandedNodes) {
        return search(startX, startY, targetX, targetY, true, maxExpandedNodes);
    }

    /**
     * A* search to a tile, or to any walkable tile next to it when {@code nextTo} is set. The heuristic of a
     * next-to search is the distance to the target minus the farthest a goal tile lies from it, which never
     * overestimates the cost of reaching the closest goal.
     */
    private List<PathNode> search(int startX, int startY, int targetX, int targetY,
                                  boolean nextTo, int maxExpandedNodes) {
        if (maxExpandedNodes <= 0) {
            throw new IllegalArgumentException("maxExpandedNodes must be positive");
        }
        lastExpandedNodes = 0;
        goalSlack = nextTo ? Math.sqrt(2) : 0;
        goalIsNeighbour = nextTo;

        // Validate start and target positions
        if (!isWalkable(startX, startY) || (!nextTo && !isWalkable(targetX, targetY))) {
            return null;
        }

        // Initialize data structures for A* algorithm
        final PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        final Map<PathNode, PathNode> cameFrom = new HashMap<>();
        final Map<PathNode, Double> gScores = new HashMap<>();

        // Configure starting node
        final PathNode startNode = new PathNode(startX, startY);
        initializeNode(startNode, 0, heuristic(startX, startY, targetX, targetY));
        openSet.add(startNode);
        gScores.put(startNode, 0.0);

        // Process nodes until path found or search exhausted
        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            // Skip outdated nodes that have been improved since being queued
            if (current.gCost > gScores.getOrDefault(current, Double.POSITIVE_INFINITY)) {
                continue;
            }

            // Check for path completion
            if (isTargetReached(current, targetX, targetY)) {
                return reconstructPath(cameFrom, current);
            }

            if (++lastExpandedNodes > maxExpandedNodes) {
                return null;
            }

            // Explore all possible neighbor directions
            exploreNeighbors(current, targetX, targetY, openSet, cameFrom, gScores);
        }

        return null; // No valid path exists
    }

    /** @return number of tiles the most recent search expanded, 0 before any search */
    public int getLastExpandedNodes() {
        return lastExpandedNodes;
    }

    // ==================================================================
    //  Helper Methods
    // ==================================================================

    /**
     * Explores all neighbor directions from current position.
     */
    private void exploreNeighbors(PathNode current, int targetX, int targetY,
                                  PriorityQueue<PathNode> openSet,
                                  Map<PathNode, PathNode> cameFrom,
                                  Map<PathNode, Double> gScores) {
        for (int i = 0; i < MOVEMENT_DIRECTIONS.length; i++) {
            int nx = current.x + MOVEMENT_DIRECTIONS[i][0];
            int ny = current.y + MOVEMENT_DIRECTIONS[i][1];

            // Skip invalid or unwalkable positions
            if (!isWalkable(nx, ny)) continue;

            // A diagonal step may not cut the corner of a blocked tile
            if (isDiagonal(i) && !(isWalkable(current.x + MOVEMENT_DIRECTIONS[i][0], current.y)
                    && isWalkable(current.x, current.y + MOVEMENT_DIRECTIONS[i][1]))) {
                continue;
            }

            processNeighbor(current, nx, ny, targetX, targetY,
                    i, openSet, cameFrom, gScores);
        }
    }

    private static boolean isDiagonal(int directionIndex) {
        return MOVEMENT_DIRECTIONS[directionIndex][0] != 0 && MOVEMENT_DIRECTIONS[directionIndex][1] != 0;
    }

    /**
     * Processes a single neighbor position.
     */
    private void processNeighbor(PathNode current, int nx, int ny, int targetX, int targetY,
                                 int directionIndex,
                                 PriorityQueue<PathNode> openSet,
                                 Map<PathNode, PathNode> cameFrom,
                                 Map<PathNode, Double> gScores) {
        PathNode neighbor = new PathNode(nx, ny);
        double newGCost = current.gCost + DIRECTION_COSTS[directionIndex];

        // Update neighbor if we found a better path
        if (newGCost < gScores.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
            cameFrom.put(neighbor, current);
            initializeNode(neighbor, newGCost, heuristic(nx, ny, targetX, targetY));
            gScores.put(neighbor, newGCost);
            openSet.add(neighbor);
        }
    }

    /**
     * Initializes node costs for pathfinding.
     */
    private void initializeNode(PathNode node, double gCost, double hCost) {
        node.gCost = gCost;
        node.hCost = hCost;
        node.fCost = gCost + hCost;
    }

    /**
     * Checks if target position has been reached.
     */
    private boolean isTargetReached(PathNode node, int targetX, int targetY) {
        if (goalIsNeighbour) {
            return (node.x != targetX || node.y != targetY)
                    && Math.abs(node.x - targetX) <= 1 && Math.abs(node.y - targetY) <= 1;
        }
        return node.x == targetX && node.y == targetY;
    }

    /**
     * Validates if a tile is walkable and within map boundaries.
     */
    private boolean isWalkable(int x, int y) {
        if (!IsometricUtils.isWithinMapBounds(x, y, map)) {
            return false;
        }
        Tile tile = map.getTile(x, y);
        return tile != null && tile.isWalkable();
    }

    // ==================================================================
    //  Heuristic and Path Reconstruction
    // ==================================================================

    /**
     * Euclidean distance heuristic for 8-direction movement.
     * Consistent and admissible for A* algorithm.
     */
    private double heuristic(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.max(0, Math.sqrt(dx * dx + dy * dy) - goalSlack);
    }

    /**
     * Reconstructs path from target to start.
     *
     * @return Path from start to target (excluding start position)
     */
    private List<PathNode> reconstructPath(Map<PathNode, PathNode> cameFrom, PathNode current) {
        final LinkedList<PathNode> path = new LinkedList<>();

        // Backtrack from target to start
        while (current != null) {
            path.addFirst(current);
            current = cameFrom.get(current);
        }

        // Remove start position (already occupied)
        path.removeFirst();
        return path;
    }

    // ==================================================================
    //  Direction Cost Precomputation
    // ==================================================================

    /**
     * Precomputes movement costs for each direction.
     * Diagonal moves cost ≈1.414, cardinal moves cost 1.0.
     */
    private static double[] precomputeDirectionCosts() {
        double[] costs = new double[MOVEMENT_DIRECTIONS.length];
        for (int i = 0; i < MOVEMENT_DIRECTIONS.length; i++) {
            int dx = MOVEMENT_DIRECTIONS[i][0];
            int dy = MOVEMENT_DIRECTIONS[i][1];
            costs[i] = Math.sqrt(dx * dx + dy * dy);
        }
        return costs;
    }

}