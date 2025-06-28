package com.feuerschvenger.perlinsedge.domain.pathfinding;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.*;

/**
 * A* pathfinding implementation with optimizations for isometric grid-based worlds.
 * Supports resource avoidance and efficient path reconstruction.
 */
public final class Pathfinder {
    // Movement directions (8-way movement)
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final double[] DIRECTION_COSTS = precomputeDirectionCosts();

    private final TileMap map;

    public Pathfinder(TileMap map) {
        this.map = map;
    }

    /**
     * Finds the optimal path between two points using A* algorithm.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     * @return List of nodes representing the path, or null if no path exists
     */
    public List<PathNode> findPath(int startX, int startY, int targetX, int targetY) {
        // Check valid start and end positions
        if (!isWalkable(startX, startY) || !isWalkable(targetX, targetY)) {
            return null;
        }

        final PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        final Map<PathNode, PathNode> cameFrom = new HashMap<>();
        final Map<PathNode, Double> gScores = new HashMap<>();

        final PathNode startNode = new PathNode(startX, startY);
        startNode.gCost = 0;
        startNode.hCost = heuristic(startX, startY, targetX, targetY);
        startNode.fCost = startNode.gCost + startNode.hCost;

        openSet.add(startNode);
        gScores.put(startNode, 0.0);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            // Skip if we found a better path to this node already
            if (current.gCost > gScores.getOrDefault(current, Double.POSITIVE_INFINITY)) {
                continue;
            }

            // Path found
            if (current.x == targetX && current.y == targetY) {
                return reconstructPath(cameFrom, current);
            }

            // Explore neighbors
            for (int i = 0; i < DX.length; i++) {
                int nx = current.x + DX[i];
                int ny = current.y + DY[i];

                // Skip unwalkable tiles
                if (!isWalkable(nx, ny)) continue;

                PathNode neighbor = new PathNode(nx, ny);
                double moveCost = current.gCost + DIRECTION_COSTS[i];

                // Found a better path to this neighbor
                if (moveCost < gScores.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    cameFrom.put(neighbor, current);
                    neighbor.gCost = moveCost;
                    neighbor.hCost = heuristic(nx, ny, targetX, targetY);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;

                    gScores.put(neighbor, moveCost);
                    openSet.add(neighbor);
                }
            }
        }

        return null; // No path found
    }

    /**
     * Checks if a tile is walkable considering resource avoidance.
     */
    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) {
            return false;
        }
        Tile tile = map.getTile(x, y);
        return tile != null && tile.isWalkable();
    }

    /**
     * Euclidean distance heuristic for 8-direction movement.
     */
    private double heuristic(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Reconstructs the path from the target node to the start.
     */
    private List<PathNode> reconstructPath(Map<PathNode, PathNode> cameFrom, PathNode current) {
        final LinkedList<PathNode> path = new LinkedList<>();
        while (current != null) {
            path.addFirst(current);
            current = cameFrom.get(current);
        }
        path.removeFirst(); // Remove start position
        return path;
    }

    /**
     * Precomputes movement costs for each direction.
     */
    private static double[] precomputeDirectionCosts() {
        double[] costs = new double[8];
        for (int i = 0; i < 8; i++) {
            costs[i] = Math.sqrt(DX[i] * DX[i] + DY[i] * DY[i]);
        }
        return costs;
    }

}