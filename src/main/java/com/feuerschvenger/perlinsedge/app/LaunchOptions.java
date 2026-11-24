package com.feuerschvenger.perlinsedge.app;

import java.util.List;

/**
 * Options read once from the program arguments at launch. The record is immutable, so a choice made at startup cannot
 * change while the game runs.
 * <p>
 * The debug keys are turned on by the exact, case-sensitive program argument {@code --debug}: {@code --DEBUG},
 * {@code --debug=true} and {@code -debug} are ignored. The argument is unrelated to the system property
 * {@code -Dperlinsedge.debug}, which only turns on the JavaFX and Prism logging and never enables a key.
 *
 * @param debugKeys whether the debug keys F1 to F5 do anything
 */
public record LaunchOptions(boolean debugKeys) {

    /** Program argument that turns the debug keys on. */
    static final String DEBUG_FLAG = "--debug";

    /** The options of a launch with no arguments: every debug feature is off. */
    public static final LaunchOptions DEFAULT = new LaunchOptions(false);

    /**
     * Reads the options from the program arguments. Arguments that are not known are ignored.
     *
     * @param args the raw program arguments, null meaning none
     * @return the options of the launch
     */
    public static LaunchOptions parse(List<String> args) {
        if (args == null) {
            return DEFAULT;
        }
        return new LaunchOptions(args.contains(DEBUG_FLAG));
    }
}
