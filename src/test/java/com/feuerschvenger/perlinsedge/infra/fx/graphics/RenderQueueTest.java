package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderQueueTest {

    private static void fill(RenderQueue queue, int count) {
        for (int i = 0; i < count; i++) {
            queue.add(i, i * 2.0, count - i, "object" + i, null, i % 2 == 0, null);
        }
    }

    @Test
    void sortsByDepthAscending() {
        RenderQueue queue = new RenderQueue();
        queue.add(0, 0, 30, "far", null, false, null);
        queue.add(0, 0, 10, "near", null, false, null);
        queue.add(0, 0, 20, "middle", null, false, null);

        queue.sortByDepth();

        assertEquals("near", queue.get(0).object());
        assertEquals("middle", queue.get(1).object());
        assertEquals("far", queue.get(2).object());
    }

    @Test
    void equalDepthsKeepTheirInsertionOrder() {
        RenderQueue queue = new RenderQueue();
        queue.add(0, 0, 5, "first", null, false, null);
        queue.add(0, 0, 5, "second", null, false, null);
        queue.add(0, 0, 5, "third", null, false, null);

        queue.sortByDepth();

        assertEquals("first", queue.get(0).object());
        assertEquals("second", queue.get(1).object());
        assertEquals("third", queue.get(2).object());
    }

    @Test
    void steadyFramesReuseTheSameInstances() {
        RenderQueue queue = new RenderQueue();
        fill(queue, 50);
        int allocatedAfterFirstFrame = queue.allocatedInstances();
        assertEquals(50, allocatedAfterFirstFrame);

        for (int frame = 0; frame < 100; frame++) {
            queue.clear();
            fill(queue, 50);
            queue.sortByDepth();
        }

        assertEquals(allocatedAfterFirstFrame, queue.allocatedInstances(), "no new instances after the first frame");
        assertEquals(50, queue.size());
    }

    @Test
    void aBiggerFrameGrowsThePoolOnlyByTheDifference() {
        RenderQueue queue = new RenderQueue();
        fill(queue, 10);
        queue.clear();

        fill(queue, 14);

        assertEquals(14, queue.allocatedInstances());
    }

    @Test
    void clearEmptiesTheQueueAndRecycledItemsAreRefilled() {
        RenderQueue queue = new RenderQueue();
        queue.add(1, 2, 3, "a", null, true, null);
        Renderable first = queue.get(0);

        queue.clear();
        assertEquals(0, queue.size());
        queue.add(7, 8, 9, "b", null, false, null);

        assertSame(first, queue.get(0), "the instance is recycled");
        assertEquals(7, queue.get(0).screenX());
        assertEquals(8, queue.get(0).screenY());
        assertEquals(9, queue.get(0).drawDepth());
        assertEquals("b", queue.get(0).object());
        assertFalse(queue.get(0).isFlashing());
    }

    @Test
    void sortingOneFrameDoesNotDisturbTheNextFramesRecycling() {
        RenderQueue queue = new RenderQueue();
        fill(queue, 20);
        queue.sortByDepth();
        queue.clear();

        fill(queue, 20);

        assertEquals(20, queue.allocatedInstances());
        assertTrue(queue.size() == 20);
        for (int i = 0; i < 20; i++) {
            assertEquals("object" + i, queue.get(i).object());
        }
    }
}
