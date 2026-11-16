package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

/**
 * Where the shadow of a sprite is anchored, so that it stands on the same point as the sprite it belongs to. A sprite is
 * drawn with its left edge at {@code rint(centreX - width / 2)} and its top edge at {@code rint(groundY - groundRow)},
 * all in screen pixels, and the shadow is anchored on the base point that results from those same roundings instead of on
 * the unrounded contact point. The two then move together by whole pixels while the camera pans or the object walks.
 */
final class ShadowAnchor {

    /** Not instantiable: the class only holds static rules. */
    private ShadowAnchor() {}

    /**
     * Returns the screen X of the base of a sprite as it is drawn: the middle of its width, counted from its rounded left
     * edge. A sprite drawn an odd number of screen pixels wide has its middle on the middle of a pixel, and the result
     * keeps that half pixel, which the shadow layer resolves.
     *
     * @param centreX  screen X of the contact point, where the middle of the sprite would stand unrounded
     * @param widthArt width of the sprite as it is drawn in art pixels, which is a whole number of screen pixels
     * @param artScale screen pixels per art pixel
     * @return the screen X of the middle of the drawn sprite
     */
    static double baseX(double centreX, double widthArt, double artScale) {
        // A drawn width is a whole number of screen pixels, so a width given as a share of an art pixel is taken as that
        final double half = Math.rint(widthArt * artScale) / 2.0;
        return Math.rint(centreX - half) + half;
    }

    /**
     * Returns the screen Y of the base of a sprite as it is drawn: the row that touches the ground, counted from its
     * rounded top edge.
     *
     * @param groundY   screen Y of the contact point, where the ground row would lie unrounded
     * @param groundRow the row of the sprite that touches the ground, counted from its top, in art pixels as it is drawn,
     *                  which is a whole number of screen pixels
     * @param artScale  screen pixels per art pixel
     * @return the screen Y of the ground row of the drawn sprite
     */
    static double baseY(double groundY, double groundRow, double artScale) {
        // A drawn height is a whole number of screen pixels, so a height given in art pixels is taken as that
        final double rows = Math.rint(groundRow * artScale);
        return Math.rint(groundY - rows) + rows;
    }

    /**
     * Converts a screen coordinate to a coordinate on the shadow layer, whose grid is one cell per art pixel, starts one
     * cell before the canvas and is aligned to the camera.
     *
     * @param screen   the screen coordinate
     * @param phase    the screen coordinate of the first cell boundary at or after the canvas edge, from the camera
     * @param artScale screen pixels per art pixel
     * @return the coordinate in cells, which may fall inside a cell
     */
    static double cell(double screen, double phase, double artScale) {
        return (screen - phase) / artScale + 1;
    }

    /**
     * Converts a coordinate on the shadow layer back to the screen.
     *
     * @param cell     the coordinate in cells
     * @param phase    the same phase given to {@link #cell}
     * @param artScale screen pixels per art pixel
     * @return the screen coordinate
     */
    static double screen(double cell, double phase, double artScale) {
        return (cell - 1) * artScale + phase;
    }
}
