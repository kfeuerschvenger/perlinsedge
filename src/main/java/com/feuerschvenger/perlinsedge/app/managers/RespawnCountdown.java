package com.feuerschvenger.perlinsedge.app.managers;

/**
 * Pure rules for the respawn countdown shown on the death screen. The value comes from the real respawn timer, so the
 * screen and the respawn always agree.
 */
public final class RespawnCountdown {

    /** Not instantiable: the class only holds static rules. */
    private RespawnCountdown() {}

    /**
     * Returns the whole seconds left until the respawn, rounded up so the screen reads 5, 4, 3, 2, 1 for a 5 second
     * timer. A dead player never shows less than 1: the timer is only at or below zero for the instant between the
     * death and the respawn.
     *
     * @param timerSeconds the respawn timer, in seconds
     * @return the seconds to show, at least 1
     */
    public static int secondsRemaining(double timerSeconds) {
        return Math.max(1, (int) Math.ceil(timerSeconds));
    }

    /**
     * Returns the countdown line of the death screen.
     *
     * @param timerSeconds the respawn timer, in seconds
     * @return the text, for example "Respawning in 3"
     */
    public static String label(double timerSeconds) {
        return "Respawning in " + secondsRemaining(timerSeconds);
    }
}
