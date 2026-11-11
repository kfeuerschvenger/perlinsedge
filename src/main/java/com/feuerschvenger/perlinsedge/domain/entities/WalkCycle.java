package com.feuerschvenger.perlinsedge.domain.entities;

/**
 * Pure frame selection for the player walk animation. The sheet rows are walk a, idle and walk b, so a stride that
 * passes through the standing pose between the two steps reads as a walk instead of a shuffle.
 */
public final class WalkCycle {

    /** Sheet row of the first step. */
    public static final int WALK_A = 0;
    /** Sheet row of the standing pose. */
    public static final int IDLE = 1;
    /** Sheet row of the second step. */
    public static final int WALK_B = 2;

    private static final int[] CYCLE = {WALK_A, IDLE, WALK_B, IDLE};

    private WalkCycle() {}

    /**
     * Returns the number of steps in one full walk cycle.
     *
     * @return the cycle length
     */
    public static int length() {
        return CYCLE.length;
    }

    /**
     * Returns the sheet row shown at a step of the cycle while the player moves.
     *
     * @param step the animation step, wrapped around the cycle length
     * @return the sheet row: walk a, idle, walk b, idle, then repeating
     */
    public static int frameAt(int step) {
        return CYCLE[Math.floorMod(step, CYCLE.length)];
    }
}
