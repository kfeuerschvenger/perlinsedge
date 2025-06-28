package com.feuerschvenger.perlinsedge.domain.pathfinding;

/**
 * Represents a node in the pathfinding grid with position and cost information.
 * Implements Comparable for efficient priority queue operations.
 */
public final class PathNode implements Comparable<PathNode> {
    public final int x;
    public final int y;
    public double gCost;  // Cost from start node
    public double hCost;  // Heuristic cost to target
    public double fCost;  // Total cost (gCost + hCost)

    /**
     * Creates a new path node at the specified coordinates.
     */
    public PathNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost, other.fCost);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathNode pathNode = (PathNode) o;
        return x == pathNode.x && y == pathNode.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

}