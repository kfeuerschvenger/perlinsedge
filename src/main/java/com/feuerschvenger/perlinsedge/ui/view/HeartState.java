package com.feuerschvenger.perlinsedge.ui.view;

import com.feuerschvenger.perlinsedge.infra.fx.utils.AssetPaths;

/**
 * The heart icon that shows a health fraction: full while health is high, half while the player is hurt, empty at zero.
 */
public enum HeartState {
    FULL(AssetPaths.HEART_FULL),
    HALF(AssetPaths.HEART_HALF),
    EMPTY(AssetPaths.HEART_EMPTY);

    /** Health fraction above which the heart is full. */
    private static final double FULL_ABOVE = 0.66;

    private final String path;

    HeartState(String path) {
        this.path = path;
    }

    /** @return the classpath location of the icon */
    public String getPath() {
        return path;
    }

    /**
     * Picks the icon for a health fraction.
     *
     * @param fraction current health divided by maximum health, values outside 0 to 1 are clamped
     * @return the heart state
     */
    public static HeartState of(double fraction) {
        if (fraction > FULL_ABOVE) {
            return FULL;
        }
        return fraction > 0 ? HALF : EMPTY;
    }
}
