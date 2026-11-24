package com.feuerschvenger.perlinsedge.domain.pathfinding;

/**
 * Represents a node in the pathfinding grid with position and cost information.
 * Implements Comparable for efficient priority queue operations.
 */
public final class PathNode implements Comparable<PathNode> {
    public final int x;
    public final int y;
    private double gCost;  // Cost from start node
    private double fCost;  // Total cost (gCost plus the heuristic cost to the target)

    /**
     * Creates a new path node at the specified coordinates.
     */
    public PathNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the costs of the node for a search.
     *
     * @param gCost cost from the start node
     * @param hCost heuristic cost to the target
     */
    public void setCosts(double gCost, double hCost) {
        this.gCost = gCost;
        this.fCost = gCost + hCost;
    }

    /** @return the cost from the start node */
    public double getGCost() {
        return gCost;
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost, other.fCost);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var pathNode = (PathNode) o;
        return x == pathNode.x && y == pathNode.y;
    }

    @Override
    public int hashCode() {
        // x and y take separate halves of the hash, so no two tiles of a map up to 65536 wide collide. A sum such as
        // 31 * x + y gives a 300 x 300 map only about 9600 distinct values for 90000 tiles, which turns the buckets of
        // the search maps into trees ordered by the cost compare and makes an unreachable search take minutes
        return Integer.rotateLeft(x, Short.SIZE) ^ y;
    }

}
