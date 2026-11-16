package com.feuerschvenger.perlinsedge.infra.fx.input;

import javafx.scene.input.KeyCode;

import java.util.EnumSet;
import java.util.Set;

/**
 * Drops the auto-repeat presses the operating system sends while a key is held. The game only has one-shot key
 * actions, so only the first press of a physical key is delivered until that key is released. Tracking is by physical
 * key code, so a modifier changing while the key is held does not count as a new press.
 */
final class KeyRepeatFilter {

    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);

    /**
     * Records a press of a key.
     *
     * @param code the physical key code
     * @return true for the first press since the key was last released, false for an auto-repeat
     */
    boolean firstPress(KeyCode code) {
        return code != null && held.add(code);
    }

    /**
     * Records the release of a key, so its next press counts as a first press.
     *
     * @param code the physical key code
     */
    void release(KeyCode code) {
        if (code != null) {
            held.remove(code);
        }
    }

    /** Forgets every held key, for when the window loses focus and the releases will never arrive. */
    void clear() {
        held.clear();
    }
}
