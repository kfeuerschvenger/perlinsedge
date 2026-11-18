package com.feuerschvenger.perlinsedge.domain.world.light;

/**
 * How the light of a source fades with the distance from it: a smooth, continuous gradient that is brightest at the
 * source, falls ever more slowly toward the middle and reaches exactly nothing at the radius, with no step anywhere. It
 * takes the square of the distance, so no square root is needed for a cell.
 */
public final class LightFalloff {

    /** Not instantiable: the class only holds static rules. */
    private LightFalloff() {}

    /**
     * Returns the share of the light that reaches a distance.
     *
     * @param shareSquared the square of how far from the source as a share of its radius, 0 at the source and 1 at the
     *                     radius
     * @return 1 at the source, falling without a jump to 0 at the radius and 0 beyond it; the value is
     *         {@code (1 - shareSquared)^2}
     */
    public static double level(double shareSquared) {
        if (Double.isNaN(shareSquared) || shareSquared >= 1.0) {
            return 0.0;
        }
        final double left = 1.0 - Math.max(0.0, shareSquared);
        return left * left;
    }
}
