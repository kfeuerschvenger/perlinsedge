package com.feuerschvenger.perlinsedge.infra.fx.graphics;

/**
 * Smooths frame times into a readable frames-per-second figure for the debug overlay.
 * The figure is the average over a window of {@value #WINDOW_SECONDS} seconds, so it does not flicker every frame.
 */
public final class FrameStats {

    private static final double WINDOW_SECONDS = 0.5;

    private double windowTime;
    private int windowFrames;
    private double fps;
    private double lastFrameMillis;

    /**
     * Records one rendered frame.
     *
     * @param deltaSeconds duration of the frame in seconds, non-positive values are ignored
     */
    public void record(double deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }
        lastFrameMillis = deltaSeconds * 1000.0;
        windowTime += deltaSeconds;
        windowFrames++;

        if (windowTime >= WINDOW_SECONDS) {
            fps = windowFrames / windowTime;
            windowTime = 0;
            windowFrames = 0;
        }
    }

    /** @return the average frames per second of the last full window, 0 until one window has passed */
    public double getFps() {
        return fps;
    }

    /** @return the duration of the most recent frame in milliseconds */
    public double getLastFrameMillis() {
        return lastFrameMillis;
    }
}
