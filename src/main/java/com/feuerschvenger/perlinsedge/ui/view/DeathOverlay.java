package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.app.managers.RespawnCountdown;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Locale;

/**
 * Full-screen death screen: a dark translucent backdrop with "YOU DIED" and the seconds left until the respawn.
 * It is hidden until the player dies. It takes mouse events, so a click on it never reaches the world behind it.
 */
public class DeathOverlay extends VBox {
    private static final double SPACING = 16;

    /** Node id of the countdown label, so a caller can look it up without relying on child order. */
    static final String COUNTDOWN_ID = "death-countdown";

    private final Label countdown = new Label();
    private int shownSeconds = -1;

    /** Builds the overlay hidden. */
    public DeathOverlay() {
        setAlignment(Pos.CENTER);
        setSpacing(SPACING);
        getStyleClass().add(UiTheme.BACKDROP);

        Label title = new Label("YOU DIED");
        title.getStyleClass().add(UiTheme.TITLE_DANGER);
        countdown.getStyleClass().add(UiTheme.HEADING);
        countdown.setId(COUNTDOWN_ID);
        getChildren().addAll(title, countdown);

        hide();
    }

    /**
     * Shows or hides the overlay for the current frame. Showing it again with the same seconds changes nothing.
     *
     * @param dead         whether the player is dead
     * @param timerSeconds the respawn timer, in seconds
     */
    public void update(boolean dead, double timerSeconds) {
        if (!dead) {
            hide();
            return;
        }
        int seconds = RespawnCountdown.secondsRemaining(timerSeconds);
        if (seconds != shownSeconds) {
            shownSeconds = seconds;
            countdown.setText(RespawnCountdown.label(timerSeconds).toUpperCase(Locale.ROOT));
        }
        if (!isVisible()) {
            setVisible(true);
        }
    }

    /** Hides the overlay and forgets the seconds it showed. */
    public void hide() {
        shownSeconds = -1;
        setVisible(false);
    }
}
