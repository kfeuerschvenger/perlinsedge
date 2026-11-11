package com.feuerschvenger.perlinsedge.domain.world.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(resource.secondsSinceHit(7.05), resource.secondsSinceHit(7.05), 0.0);
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
