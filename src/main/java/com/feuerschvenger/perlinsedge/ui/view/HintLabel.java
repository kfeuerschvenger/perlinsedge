package com.feuerschvenger.perlinsedge.ui.view;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Short on-screen hint that tells the player why an action did nothing.
 * It fades in, stays for a moment and fades out; showing a new hint while one is visible restarts the hold from the
 * opacity the label has at that moment, so the label never jumps to full opacity when it is shown again mid fade.
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
        getStyleClass().add(UiTheme.PILL);

        // Both fades have fixed end points, so the opacity is a function of the time inside the cycle alone
        FadeTransition in = new FadeTransition(Duration.millis(FADE_IN_MS), this);
        in.setFromValue(0);
        in.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(VISIBLE_MS));
        FadeTransition out = new FadeTransition(Duration.millis(FADE_OUT_MS), this);
        out.setFromValue(1);
        out.setToValue(0);
        cycle = new SequentialTransition(in, hold, out);
    }

    /**
     * Shows a hint, replacing the one on screen if there is any.
     *
     * @param message text to show
     */
    public void show(String message) {
        setText(UiTheme.caps(message));
        double opacity = getOpacity();
        if (opacity > 0) {
            cycle.stop();
            cycle.jumpTo(Duration.millis(resumePositionMs(opacity)));
            setOpacity(opacity);
            cycle.play();
            return;
        }
        cycle.playFromStart();
    }

    /**
     * Position inside the cycle at which the opacity equals the given one on the way up, which is where a label that
     * is shown again continues from. A label that is already fully visible continues at the start of the hold.
     *
     * @param opacity current opacity of the label
     * @return milliseconds from the start of the cycle
     */
    static double resumePositionMs(double opacity) {
        return Math.max(0, Math.min(1, opacity)) * FADE_IN_MS;
    }
}
