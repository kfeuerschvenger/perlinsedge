package com.feuerschvenger.perlinsedge.app;

import javafx.animation.AnimationTimer;

import java.util.function.DoubleConsumer;

/**
 * Fixed-timestep game loop built on a JavaFX {@link AnimationTimer}.
 * Logic runs in steps of 1/60 s from an accumulator, then one render call receives the interpolation factor.
 * A single timer instance is reused across start and stop calls.
 */
public final class GameLoop {

    /** Duration of one logic step in seconds. */
    public static final double FIXED_TIME_STEP = 1.0 / 60.0;

    private static final double MAX_FRAME_DELTA = 0.25;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    /** Receives the interpolation factor and the frame time once per rendered frame. */
    @FunctionalInterface
    public interface FrameRenderer {
        /**
         * Draws one frame.
         *
         * @param alpha     fraction of a logic step left in the accumulator, from 0.0 to 1.0
         * @param deltaTime time since the previous frame in seconds, clamped to avoid a spiral of death
         */
        void render(double alpha, double deltaTime);
    }

    private final DoubleConsumer updateStep;
    private final FrameRenderer frameRenderer;
    private final AnimationTimer timer = new FixedStepTimer();

    private long lastUpdate;
    private double accumulator;

    /**
     * Creates a loop.
     *
     * @param updateStep    called zero or more times per frame with {@link #FIXED_TIME_STEP}
     * @param frameRenderer called once per frame after the logic steps
     */
    public GameLoop(DoubleConsumer updateStep, FrameRenderer frameRenderer) {
        this.updateStep = updateStep;
        this.frameRenderer = frameRenderer;
    }

    /** Starts the loop, or resumes it after {@link #stop()}. */
    public void start() {
        timer.start();
    }

    /** Stops the loop. Nothing is updated or rendered until {@link #start()} is called again. */
    public void stop() {
        timer.stop();
    }

    private final class FixedStepTimer extends AnimationTimer {
        @Override
        public void handle(long now) {
            if (lastUpdate == 0) {
                lastUpdate = now;
                return;
            }

            double deltaTime = (now - lastUpdate) / NANOS_PER_SECOND;
            lastUpdate = now;
            deltaTime = Math.min(deltaTime, MAX_FRAME_DELTA);

            accumulator += deltaTime;
            while (accumulator >= FIXED_TIME_STEP) {
                updateStep.accept(FIXED_TIME_STEP);
                accumulator -= FIXED_TIME_STEP;
            }

            frameRenderer.render(accumulator / FIXED_TIME_STEP, deltaTime);
        }
    }
}
