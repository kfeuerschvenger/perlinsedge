package com.feuerschvenger.perlinsedge.domain.strategies;

import com.feuerschvenger.perlinsedge.domain.pathfinding.PathNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The wait for a held step doubles while the same step stays held, and starts over when the memory is cleared. */
class HeldStepBackoffTest {

    private static final double FIRST_WAIT = 0.2;
    private static final PathNode STEP = new PathNode(4, 7);
    private static final PathNode OTHER_STEP = new PathNode(5, 7);

    private final HeldStepBackoff backoff = new HeldStepBackoff();

    @Test
    void theWaitDoublesWhileTheSameStepStaysHeldUpToTheLongestWait() {
        assertEquals(FIRST_WAIT, backoff.waitFor(STEP, () -> FIRST_WAIT), 1e-12);
        assertEquals(2 * FIRST_WAIT, backoff.waitFor(STEP, () -> FIRST_WAIT), 1e-12);
        assertEquals(4 * FIRST_WAIT, backoff.waitFor(STEP, () -> FIRST_WAIT), 1e-12);
        assertEquals(HeldStepBackoff.MAX_WAIT_SECONDS, backoff.waitFor(STEP, () -> FIRST_WAIT), 1e-12);
        assertEquals(HeldStepBackoff.MAX_WAIT_SECONDS, backoff.waitFor(STEP, () -> FIRST_WAIT), 1e-12);
    }

    @Test
    void theFirstWaitIsAskedOnlyForAStepThatWasNotBlockedBefore() {
        final AtomicInteger asked = new AtomicInteger();

        backoff.waitFor(STEP, () -> asked.incrementAndGet() * FIRST_WAIT);
        backoff.waitFor(STEP, () -> asked.incrementAndGet() * FIRST_WAIT);
        assertEquals(1, asked.get(), "a repeated step grows the wait without asking for a first one");

        backoff.waitFor(OTHER_STEP, () -> asked.incrementAndGet() * FIRST_WAIT);
        assertEquals(2, asked.get());
    }

    @Test
    void aDifferentStepStartsOverWithAFirstWait() {
        backoff.waitFor(STEP, () -> FIRST_WAIT);
        backoff.waitFor(STEP, () -> FIRST_WAIT);

        assertEquals(FIRST_WAIT, backoff.waitFor(OTHER_STEP, () -> FIRST_WAIT), 1e-12);
        assertFalse(backoff.isBlockedAt(STEP));
        assertTrue(backoff.isBlockedAt(OTHER_STEP));
    }

    @Test
    void clearingTheMemoryStartsTheSameStepOverWithAFirstWait() {
        backoff.waitFor(STEP, () -> FIRST_WAIT);
        backoff.waitFor(STEP, () -> FIRST_WAIT);
        backoff.waitFor(STEP, () -> FIRST_WAIT);

        backoff.clear();

        assertFalse(backoff.isBlocked());
        assertFalse(backoff.isBlockedAt(STEP));
        assertEquals(FIRST_WAIT, backoff.waitFor(STEP, () -> FIRST_WAIT), 1e-12);
    }

    @Test
    void aNewPathKeepsTheMemoryOnlyWhenItStartsWithTheBlockedStep() {
        backoff.waitFor(STEP, () -> FIRST_WAIT);

        backoff.keepOnlyIfFirstStepOf(STEP);
        assertTrue(backoff.isBlockedAt(STEP), "the same first step keeps the memory");

        backoff.keepOnlyIfFirstStepOf(OTHER_STEP);
        assertFalse(backoff.isBlocked(), "another first step forgets it");

        backoff.waitFor(STEP, () -> FIRST_WAIT);
        backoff.keepOnlyIfFirstStepOf(null);
        assertFalse(backoff.isBlocked(), "an empty path forgets it");
    }
}
