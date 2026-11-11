package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

/**
 * The sizes and the shape of the soft ground shadows under entities, resources and buildings.
 * Shadows are a few pre-baked ellipses at fixed art sizes, so a shadow is always one of those sizes drawn at the
 * integer art scale and stays crisp. This class decides which size a thing gets and which pixels an ellipse covers,
 * and holds no framework type, so it can be tested without a window.
 */
public final class GroundShadow {

    /** Opacity of every shadow pixel: black at this alpha. */
    public static final double OPACITY = 0.30;

    /** Widths, in art pixels, of the baked ellipses, ascending. */
    private static final int[] WIDTHS = {6, 9, 12, 16, 22, 30, 40};

    /** Shadow width of the player, relative to its sprite width. */
    public static final double PLAYER_FRACTION = 0.6;

    /** Shadow width of an enemy drawn from a sprite, relative to its sprite width. */
    public static final double SPRITE_ENEMY_FRACTION = 0.6;

    /** Not instantiable: the class only holds static rules. */
    private GroundShadow() {}

    /** @return how many ellipse sizes there are */
    public static int bucketCount() {
        return WIDTHS.length;
    }

    /**
     * Returns the width of one ellipse size.
     *
     * @param bucket the size index, below {@link #bucketCount()}
     * @return the width in art pixels
     */
    public static int widthOf(int bucket) {
        return WIDTHS[bucket];
    }

    /**
     * Returns the height of an ellipse of a width: half of it, which is the slope of the isometric ground.
     *
     * @param width the width in art pixels
     * @return the height in art pixels, at least three
     */
    public static int heightOf(int width) {
        return Math.max(3, (width + 1) / 2);
    }

    /**
     * Picks the ellipse size nearest to a wanted width.
     *
     * @param widthArtPixels the wanted width in art pixels
     * @return the size index
     */
    public static int bucketFor(double widthArtPixels) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < WIDTHS.length; i++) {
            double distance = Math.abs(WIDTHS[i] - widthArtPixels);
            if (distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Tells whether a pixel of an ellipse image is covered, testing the pixel centre against the ellipse that fills the image.
     *
     * @param width  image width
     * @param height image height
     * @param px     pixel column
     * @param py     pixel row
     * @return true when the pixel is part of the shadow
     */
    public static boolean covers(int width, int height, int px, int py) {
        final double nx = (px + 0.5 - width / 2.0) / (width / 2.0);
        final double ny = (py + 0.5 - height / 2.0) / (height / 2.0);
        return nx * nx + ny * ny <= 1.0;
    }

    /**
     * Returns the shadow width under a resource.
     * A tree casts a narrower shadow than its canopy, a rock or an ore a nearly as wide one.
     *
     * @param type           the resource type
     * @param spriteArtWidth the width of its sprite in art pixels
     * @return the wanted shadow width in art pixels
     */
    public static double widthFor(ResourceType type, double spriteArtWidth) {
        return spriteArtWidth * switch (type) {
            case TREE -> 0.55;
            case STONE, IRON_ORE -> 0.9;
            case CRYSTALS -> 0.8;
        };
    }

    /**
     * Returns the shadow width under a building, which are drawn from shapes and have no sprite width.
     *
     * @param type the building type
     * @return the shadow width in art pixels, zero for the buildings that lie flat on the ground
     */
    public static double widthFor(BuildingType type) {
        return switch (type) {
            case BRIDGE, DOCK -> 0.0;
            case STANDING_TORCH -> 8.0;
            case FENCE, DOOR, ANVIL -> 16.0;
            case CHEST, CAMPFIRE -> 18.0;
            case FURNACE -> 22.0;
            case WORKBENCH, BED -> 26.0;
            case WALL -> 30.0;
        };
    }
}
