package com.feuerschvenger.perlinsedge.ui.view;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Short on-screen hint that tells the player why an action did nothing.
 * It fades in, stays for a moment and fades out; showing a new hint while one is visible restarts the cycle.
 * The label never takes mouse events, so it cannot block clicks on the world behind it.
 */
public class HintLabel extends Label {
    private static final double FADE_IN_MS = 120;
    private static final double VISIBLE_MS = 1600;
    private static final double FADE_OUT_MS = 500;

    private final SequentialTransition cycle;

    public HintLabel() {
        setMouseTransparent(true);
        setOpacity(0);
        setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"
                + " -fx-background-color: rgba(0,0,0,0.65); -fx-background-radius: 8; -fx-padding: 6 14 6 14;");

        FadeTransition in = new FadeTransition(Duration.millis(FADE_IN_MS), this);
        in.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(VISIBLE_MS));
        FadeTransition out = new FadeTransition(Duration.millis(FADE_OUT_MS), this);
        out.setToValue(0);
        cycle = new SequentialTransition(in, hold, out);
    }

    /**
     * Shows a hint, replacing the one on screen if there is any.
     *
     * @param message text to show
     */
    public void show(String message) {
        cycle.stop();
        setText(message);
        setOpacity(0);
        cycle.playFromStart();
    }
}
