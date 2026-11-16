package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Decides which shadows are worth casting. The drawing itself is done by {@link FxShadowPass}.
 */
public final class FxShadowHelper {

    /**
     * The share of full opacity below which a shadow or a sky tint cannot be seen, so nothing is cast or laid over the
     * world for it.
     */
    public static final double MIN_VISIBLE_STRENGTH = 0.004;

    /** Not instantiable: the class only holds static rules. */
    private FxShadowHelper() {}

    /**
     * Returns how far outside the canvas a caster can stand and still throw its shadow into it. A shadow reaches the
     * height of the caster times the length factor, and the width of the caster adds to it sideways.
     *
     * @param heightArt     height in art pixels of the tallest caster
     * @param halfWidthArt  half the width in art pixels of the widest caster
     * @param lengthFactor  how long shadows are as a multiple of the height of what casts them
     * @param artScale      screen pixels per art pixel
     * @return the margin in screen pixels
     */
    public static double reachMargin(double heightArt, double halfWidthArt, double lengthFactor, double artScale) {
        return (heightArt * lengthFactor + halfWidthArt) * artScale;
    }

    /**
     * Returns how far below the canvas a caster can stand and still throw its shadow into it. A shadow only extends
     * down the screen from the base of its caster, so a caster below the canvas reaches back up only by the sideways
     * spread of its width, which is half as tall on the screen as it is wide, and by one cell of rounding.
     *
     * @param halfWidthArt half the width in art pixels of the widest caster
     * @param artScale     screen pixels per art pixel
     * @return the margin below the canvas in screen pixels
     */
    public static double bottomMargin(double halfWidthArt, double artScale) {
        return (halfWidthArt / 2.0 + 1.0) * artScale;
    }

    /**
     * Tells whether a caster standing on a point can throw a shadow into the canvas. A caster above or to either side
     * can reach by the whole margin, and one below only by the smaller bottom margin, since shadows fall down the
     * screen.
     *
     * @param x            screen X of the ground contact point
     * @param y            screen Y of the ground contact point
     * @param canvasWidth  canvas width
     * @param canvasHeight canvas height
     * @param margin       how far above or beside the canvas a caster can stand, see {@link #reachMargin}
     * @param bottomMargin how far below the canvas a caster can stand, see {@link #bottomMargin}
     * @return true when the shadow may reach the canvas
     */
    public static boolean canReach(double x, double y, double canvasWidth, double canvasHeight, double margin,
                                   double bottomMargin) {
        return x > -margin && x < canvasWidth + margin && y > -margin && y < canvasHeight + bottomMargin;
    }

    /**
     * Returns how many tiles to walk beyond the visible tile bounds on the sides toward the top and the left of the
     * screen, where a caster can stand the whole reach away.
     *
     * @param reach          see {@link #reachMargin}
     * @param halfTileHeight half the height of a tile in screen pixels
     * @return the number of tiles to add on the low sides of the bounds
     */
    public static int nearRingTiles(double reach, double halfTileHeight) {
        return (int) Math.ceil(reach / halfTileHeight);
    }

    /**
     * Returns how many tiles to walk beyond the visible tile bounds on the sides toward the bottom and the right of the
     * screen. A tile coordinate grows with a displacement to the right by a quarter of the reach per half tile height
     * and with a displacement down by half of the bottom reach, so the far sides need far fewer tiles than the near
     * ones.
     *
     * @param reach          see {@link #reachMargin}
     * @param bottomReach    see {@link #bottomMargin}
     * @param halfTileHeight half the height of a tile in screen pixels
     * @return the number of tiles to add on the high sides of the bounds
     */
    public static int farRingTiles(double reach, double bottomReach, double halfTileHeight) {
        return (int) Math.ceil((reach / 4.0 + bottomReach / 2.0) / halfTileHeight) + 1;
    }
}
