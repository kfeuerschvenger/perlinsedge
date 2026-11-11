package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;

/**
 * The heart icons of the health row: full, half or empty.
 */
public enum HeartState {
    FULL(AssetPaths.HEART_FULL),
    HALF(AssetPaths.HEART_HALF),
    EMPTY(AssetPaths.HEART_EMPTY);

    private final String path;

    /** Creates a heart state with the classpath location of its icon. */
    HeartState(String path) {
        this.path = path;
    }

    /** @return the classpath location of the icon */
    public String getPath() {
        return path;
    }

    /**
     * Picks the icons of a row of hearts for a health fraction.
     * Every heart is worth two halves. Any health above zero shows at least half a heart, so a hurt but living player
     * never sees an empty row.
     *
     * @param fraction current health divided by maximum health, values outside 0 to 1 are clamped
     * @param count    hearts in the row
     * @return one state per heart, from the first heart on
     */
    public static HeartState[] row(double fraction, int count) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        int halves = (int) Math.round(clamped * count * 2);
        if (clamped > 0 && halves == 0) {
            halves = 1;
        }
        HeartState[] states = new HeartState[count];
        for (int i = 0; i < count; i++) {
            int left = halves - 2 * i;
            states[i] = left >= 2 ? FULL : left == 1 ? HALF : EMPTY;
        }
        return states;
    }
}
