package com.feuerschvenger.perlinsedge.infra.fx.utils;

import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Where the foot of a resource sprite touches the ground.
 * Every sprite is drawn with the bottom row of its canvas on the tile centre, but low sprites such as rocks, ore and bushes
 * leave a few empty rows under their outline, so they float above the ground. A low sprite is moved down by that padding
 * plus a small contact depth, which puts its lowest painted pixel a little below the tile centre like a boulder sunk into
 * the ground. Tall sprites such as trees end on their last row already and are left alone. The ground shadow is centred on
 * the same contact point.
 */
public final class ResourceAnchor {

    /** Art pixels below the tile centre where a low sprite touches the ground. */
    public static final int CONTACT_DEPTH_ART_PIXELS = 2;

    /** The art kinds that are low: they sit on the ground rather than stand on a trunk. */
    private static final Set<String> LOW_KINDS = Set.of("bush", "shrub", "fern", "reed", "rock", "iron");

    private ResourceAnchor() {}

    /**
     * Tells whether an art kind is drawn as a low sprite.
     *
     * @param kind the art kind, the file name without its variant number
     * @return true for rocks, ore, bushes and the other low kinds
     */
    public static boolean isLow(String kind) {
        return LOW_KINDS.contains(kind);
    }

    /**
     * Counts the empty rows under the lowest painted pixel of a sprite.
     *
     * @param height     the sprite height in pixels
     * @param rowIsEmpty tells whether a row, counted from the top, holds no painted pixel
     * @return the number of empty rows at the bottom, the whole height for an empty sprite
     */
    public static int bottomPadding(int height, IntPredicate rowIsEmpty) {
        int padding = 0;
        while (padding < height && rowIsEmpty.test(height - 1 - padding)) {
            padding++;
        }
        return padding;
    }

    /**
     * Returns how far a sprite is moved down from the default anchor, which puts its canvas bottom on the tile centre.
     *
     * @param kind          the art kind
     * @param bottomPadding the empty rows under the sprite's lowest painted pixel
     * @return the shift in art pixels, zero for a tall kind
     */
    public static int dropArtPixels(String kind, int bottomPadding) {
        return isLow(kind) ? bottomPadding + CONTACT_DEPTH_ART_PIXELS : 0;
    }

    /**
     * Returns how far below the tile centre the lowest painted pixel of a sprite lies after the shift.
     *
     * @param kind the art kind
     * @return the depth in art pixels, which is also the offset of the ground shadow, zero for a tall kind
     */
    public static int contactArtPixels(String kind) {
        return isLow(kind) ? CONTACT_DEPTH_ART_PIXELS : 0;
    }
}
