package com.feuerschvenger.perlinsedge.domain.pathfinding;

/**
 * Represents a node in the pathfinding grid with position and cost information.
 * Implements Comparable for efficient priority queue operations.
 */
public final class PathNode implements Comparable<PathNode> {
    /** Odd 32-bit golden ratio constant that spreads consecutive coordinates over the bits of a hash. */
    private static final int HASH_MULTIPLIER = 0x9E3779B1;

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
        // The hash of a HashMap is spread with h ^ (h >>> 16) and masked to a power-of-two table, so the low bits of
        // the hash alone choose the bucket. Packing x and y into separate halves, or summing them as in 31 * x + y,
        // leaves only a few hundred or a few thousand distinct buckets on a 300 x 300 map and turns each bucket into a
        // tree ordered by the cost compare. The golden ratio multiplier mixes the bits of x into every bucket bit
        return x * HASH_MULTIPLIER ^ y;
    }

}
