package com.feuerschvenger.perlinsedge.domain.pathfinding;

/**
 * An extra cost for entering a tile, so a search prefers to go around the tile instead of through it without
 * forbidding it. The cost is never negative, which keeps the heuristic of the search admissible.
 */
@FunctionalInterface
public interface TilePenalty {

    /** No tile costs more than another. */
    TilePenalty NONE = (x, y) -> 0;

    /**
     * Prices the entry to a tile.
     *
     * @param x tile column
     * @param y tile row
     * @return the extra cost added to the step onto the tile, zero or more
     */
    double extraCost(int x, int y);
}
