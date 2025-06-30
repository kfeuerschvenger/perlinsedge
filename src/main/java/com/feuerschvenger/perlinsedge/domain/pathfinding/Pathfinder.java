package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.utils.IsometricUtils;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.*;

/**
 * A* pathfinding implementation optimized for isometric grid-based worlds.
 * Features 8-directional movement, efficient path reconstruction, and walkability validation.
 */
public final class Pathfinder {
    // Reuse direction constants from IsometricUtils to prevent duplication
    private static final int[][] MOVEMENT_DIRECTIONS = IsometricUtils.ALL_DIRECTIONS;
    private static final double[] DIRECTION_COSTS = precomputeDirectionCosts();

    private final TileMap map;

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
        // Validate start and target positions
        if (!isWalkable(startX, startY) || !isWalkable(targetX, targetY)) {
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

            // Explore all possible neighbor directions
            exploreNeighbors(current, targetX, targetY, openSet, cameFrom, gScores);
        }

        return null; // No valid path exists
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

            processNeighbor(current, nx, ny, targetX, targetY,
                    i, openSet, cameFrom, gScores);
        }
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
        return Math.sqrt(dx * dx + dy * dy);
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