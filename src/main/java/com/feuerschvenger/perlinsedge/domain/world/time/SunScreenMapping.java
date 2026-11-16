package com.feuerschvenger.perlinsedge.domain.world.time;

/**
 * Maps the position of the sun on its arc to the direction shadows take on the isometric screen.
 * The sun rises in the east, which is the right of the screen, and sets in the west, the left. A shadow points away from
 * the sun, so a morning shadow points to the left, a noon shadow straight down the screen toward the viewer and an
 * evening shadow to the right. The direction is a unit vector in screen axes, with Y growing downward.
 */
public final class SunScreenMapping {

    /** Not instantiable: the class only holds static rules. */
    private SunScreenMapping() {}

    /**
     * Returns the angle of a shadow on the screen.
     *
     * @param sunProgress where the sun is on its arc, 0 at sunrise, 0.5 at noon and 1 at sunset, clamped to that range
     * @return the angle in radians: pi for left, pi / 2 for down, zero for right
     */
    public static double shadowAngle(double sunProgress) {
        return Math.PI * (1.0 - Math.max(0.0, Math.min(1.0, sunProgress)));
    }

    /**
     * Returns the horizontal part of the shadow direction.
     *
     * @param sunProgress where the sun is on its arc, 0 to 1
     * @return minus one when the shadow points left, zero straight down, one when it points right
     */
    public static double shadowDirectionX(double sunProgress) {
        return Math.cos(shadowAngle(sunProgress));
    }

    /**
     * Returns the vertical part of the shadow direction.
     *
     * @param sunProgress where the sun is on its arc, 0 to 1
     * @return zero for a shadow along the horizontal, one for a shadow pointing down the screen, never negative
     */
    public static double shadowDirectionY(double sunProgress) {
        return Math.sin(shadowAngle(sunProgress));
    }
}
