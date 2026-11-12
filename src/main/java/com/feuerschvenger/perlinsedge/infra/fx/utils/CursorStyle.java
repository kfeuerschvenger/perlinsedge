package com.feuerschvenger.perlinsedge.infra.fx.utils;

import java.util.Set;

/**
 * The rules of how the tile cursor looks: which ground is light enough to need a dark crosshair, how the crosshair
 * breathes while it rests and how the green or red answer to a click fades.
 * Every value is a pure function of the ground art or of the game clock, so the renderer allocates nothing per frame.
 */
public final class CursorStyle {

    /** Seconds one breath of the resting crosshair takes. */
    public static final double BREATH_PERIOD_SECONDS = 1.8;

    /** Opacity of the crosshair at the low point of its breath. */
    public static final double BREATH_MIN_ALPHA = 0.6;

    /** Opacity of the answer to a click when it is about to end. */
    public static final double FLASH_MIN_ALPHA = 0.35;

    /** The tile arts light enough that a light crosshair would not read on them. */
    private static final Set<String> LIGHT_GROUND = Set.of("sand", "dune", "snow", "ice", "savanna");

    /** Not instantiable: the class only holds static rules. */
    private CursorStyle() {}

    /**
     * Tells whether the ground drawn from an art is light.
     * Water answers false whatever its bed is, because the sheet over it is blue and dark.
     *
     * @param art the tile art name of the ground, or null
     * @return true when the cursor should be dark to stand out
     */
    public static boolean isLightGround(String art) {
        return art != null && LIGHT_GROUND.contains(art);
    }

    /**
     * Returns the opacity of the resting crosshair, which swells and fades once per {@link #BREATH_PERIOD_SECONDS}.
     *
     * @param gameTime the game time in seconds
     * @return an opacity between {@link #BREATH_MIN_ALPHA} and 1
     */
    public static double breathAlpha(double gameTime) {
        final double wave = 0.5 + 0.5 * Math.sin(2 * Math.PI * gameTime / BREATH_PERIOD_SECONDS);
        return BREATH_MIN_ALPHA + (1 - BREATH_MIN_ALPHA) * wave;
    }

    /**
     * Returns the opacity of the answer to a click.
     *
     * @param strength how much of the flash is left, 1 at the click and 0 at its end
     * @return an opacity between {@link #FLASH_MIN_ALPHA} and 1
     */
    public static double flashAlpha(double strength) {
        return FLASH_MIN_ALPHA + (1 - FLASH_MIN_ALPHA) * Math.max(0, Math.min(1, strength));
    }
}
