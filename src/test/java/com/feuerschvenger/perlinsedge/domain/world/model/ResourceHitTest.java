package com.feuerschvenger.perlinsedge.domain.world.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResourceHitTest {

    @Test
    void aResourceThatWasNeverStruckReportsInfinity() {
        assertEquals(Double.POSITIVE_INFINITY, new Resource(ResourceType.TREE, 1L).secondsSinceHit());
    }

    @Test
    void aStruckResourceReportsASmallPositiveElapsedTime() {
        Resource resource = new Resource(ResourceType.STONE, 2L);
        resource.startFlashing();

        double elapsed = resource.secondsSinceHit();

        assertTrue(elapsed >= 0.0);
        assertTrue(elapsed < 5.0);
    }

    @Test
    void aSecondHitRestartsTheClock() throws InterruptedException {
        Resource resource = new Resource(ResourceType.TREE, 3L);
        resource.startFlashing();
        Thread.sleep(30);
        double first = resource.secondsSinceHit();
        resource.startFlashing();

        assertTrue(resource.secondsSinceHit() < first);
    }
}
