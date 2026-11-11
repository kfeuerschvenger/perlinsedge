package com.feuerschvenger.perlinsedge.infra.fx.graphics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameStatsTest {

    @Test
    void reportsZeroUntilAWindowHasPassed() {
        FrameStats stats = new FrameStats();

        stats.record(0.125);

        assertEquals(0.0, stats.getFps());
        assertEquals(125.0, stats.getLastFrameMillis(), 1e-9);
    }

    @Test
    void averagesTheFramesOfAWindow() {
        FrameStats stats = new FrameStats();

        for (int i = 0; i < 4; i++) {
            stats.record(0.125);
        }

        assertEquals(8.0, stats.getFps(), 1e-9);
    }

    @Test
    void keepsTheLastFigureWhileTheNextWindowFills() {
        FrameStats stats = new FrameStats();
        for (int i = 0; i < 4; i++) {
            stats.record(0.125);
        }

        stats.record(0.0625);

        assertEquals(8.0, stats.getFps(), 1e-9);
        assertEquals(62.5, stats.getLastFrameMillis(), 1e-9);
    }

    @Test
    void ignoresNonPositiveFrames() {
        FrameStats stats = new FrameStats();

        stats.record(0);
        stats.record(-1);

        assertEquals(0.0, stats.getLastFrameMillis());
    }
}
