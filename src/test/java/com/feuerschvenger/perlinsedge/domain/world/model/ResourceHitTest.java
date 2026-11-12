package com.feuerschvenger.perlinsedge.domain.world.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResourceHitTest {

    @Test
    void aResourceThatWasNeverStruckReportsInfinity() {
        assertEquals(Double.POSITIVE_INFINITY, new Resource(ResourceType.TREE, 1L).secondsSinceHit(10.0));
    }

    @Test
    void aStruckResourceReportsTheGameTimeSinceTheHit() {
        Resource resource = new Resource(ResourceType.STONE, 2L);
        resource.startFlashing(4.0);

        assertEquals(0.0, resource.secondsSinceHit(4.0), 1e-9);
        assertEquals(0.1, resource.secondsSinceHit(4.1), 1e-9);
    }

    @Test
    void theElapsedTimeStandsStillWhileTheGameClockDoes() {
        Resource resource = new Resource(ResourceType.TREE, 3L);
        resource.startFlashing(7.0);

        double before = resource.secondsSinceHit(7.25);
        double after = resource.secondsSinceHit(7.25);

        assertEquals(0.25, before, 0.0);
        assertEquals(before, after, 0.0);
        assertEquals(0.5, resource.secondsSinceHit(7.5), 0.0);
        assertTrue(resource.isFlashing(7.1));
        assertFalse(resource.isFlashing(7.2));
    }

    @Test
    void aHitAheadOfTheClockMeansTheGameWasResetSoNothingFlashesOrShakes() {
        Resource resource = new Resource(ResourceType.TREE, 4L);
        resource.startFlashing(120.0);

        assertEquals(Double.POSITIVE_INFINITY, resource.secondsSinceHit(0.0));
        assertFalse(resource.isFlashing(0.0));
    }

    @Test
    void aResourceThatWasNeverStruckDoesNotFlash() {
        assertFalse(new Resource(ResourceType.TREE, 5L).isFlashing(1.0));
    }

    @Test
    void aSecondHitRestartsTheClock() {
        Resource resource = new Resource(ResourceType.TREE, 3L);
        resource.startFlashing(1.0);
        double first = resource.secondsSinceHit(1.5);
        resource.startFlashing(1.5);

        assertEquals(0.5, first, 1e-9);
        assertEquals(0.0, resource.secondsSinceHit(1.5), 1e-9);
    }
}
