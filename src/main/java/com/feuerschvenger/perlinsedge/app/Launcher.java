package com.feuerschvenger.perlinsedge.app;

import javafx.application.Application;

/**
 * Entry point of the runnable fat jar.
 *
 * <p>The shaded jar puts JavaFX on the classpath. The JVM launcher refuses to start a main class that extends
 * {@link Application} unless {@code javafx.graphics} is in the boot module layer, so the jar's {@code Main-Class}
 * is this plain class instead of {@link GameApplication}. It only delegates to {@link Application#launch}.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts the game.
     *
     * @param args command line arguments, forwarded to the JavaFX application
     */
    public static void main(String[] args) {
        Application.launch(GameApplication.class, args);
    }
}
