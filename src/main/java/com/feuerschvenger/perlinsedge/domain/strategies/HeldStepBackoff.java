package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;

import java.util.function.DoubleSupplier;

/**
 * The wait of an enemy for a step of its path that another enemy holds. It remembers the step and the wait it last
 * gave it: the wait for the same step doubles, up to {@link #MAX_WAIT_SECONDS}, each time the step is still held when
 * it is tried again, and a different step starts over with the first wait the caller chooses.
 */
final class HeldStepBackoff {

    /** Longest wait for a held step. */
    public static final double MAX_WAIT_SECONDS = 1.5;
    /** How much longer each repeated wait for the same held step is. */
    private static final double GROWTH_FACTOR = 2.0;
    /** Marks that no step is blocked. */
    private static final int NO_STEP = Integer.MIN_VALUE;

    private int stepX = NO_STEP;
    private int stepY = NO_STEP;
    private double waitSeconds;

    /**
     * Gives the wait for a step that is held now.
     *
     * @param step             the step of the path that cannot be taken
     * @param firstWaitSeconds supplies the first wait for a step that was not blocked before, asked only then
     * @return the wait, in seconds
     */
    double waitFor(PathNode step, DoubleSupplier firstWaitSeconds) {
        if (isBlockedAt(step)) {
            waitSeconds = Math.min(MAX_WAIT_SECONDS, waitSeconds * GROWTH_FACTOR);
        } else {
            stepX = step.x;
            stepY = step.y;
            waitSeconds = firstWaitSeconds.getAsDouble();
        }
        return waitSeconds;
    }

    /** @return true when the step is the one remembered as blocked, so it is still held after a wait */
    boolean isBlockedAt(PathNode step) {
        return step.x == stepX && step.y == stepY;
    }

    /** @return true when a step is remembered as blocked */
    boolean isBlocked() {
        return stepX != NO_STEP;
    }

    /**
     * Keeps the memory only when the path starts with the blocked step, and forgets it otherwise.
     *
     * @param firstStep the first step of the new path, null when there is none
     */
    void keepOnlyIfFirstStepOf(PathNode firstStep) {
        if (firstStep == null || !isBlockedAt(firstStep)) {
            clear();
        }
    }

    /** Forgets the blocked step and the wait it earned. */
    void clear() {
        stepX = NO_STEP;
        stepY = NO_STEP;
        waitSeconds = 0;
    }
}
