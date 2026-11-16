package com.feuerschvenger.perlinsedge.domain.world.light;

/**
 * How the light of a source fades with the distance from it. The fade is cut into a few rings, each with one flat
 * level, so the light has the crisp bands of classic pixel art instead of a smooth gradient: full strength in the
 * innermost ring and one step less in each ring beyond it, down to nothing outside the radius.
 */
public final class LightFalloff {

    /** Not instantiable: the class only holds static rules. */
    private LightFalloff() {}

    /**
     * Returns the level of the ring a distance falls in.
     *
     * @param distanceShare how far from the source as a share of its radius, 0 at the source and 1 at the radius
     * @param rings         the number of rings the light is cut into, above zero
     * @return {@code 1} in the innermost ring and {@code 1 / rings} in the outermost, and 0 at or beyond the radius, so
     *         at most {@code rings + 1} distinct values come out
     */
    public static double level(double distanceShare, int rings) {
        if (!(distanceShare < 1.0)) {
            return 0.0;
        }
        final int ring = (int) Math.floor(Math.max(0.0, distanceShare) * rings);
        return (rings - ring) / (double) rings;
    }
}
