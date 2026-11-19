package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;

/**
 * The opacity of the ground shadows and the width of the ellipse shadow of the things that are drawn from shapes
 * instead of from a sprite. A thing with a sprite casts the silhouette of that sprite, see {@link Silhouette}, and needs
 * no width. This class holds no framework type, so it can be tested without a window.
 */
public final class GroundShadow {

    /** Opacity of the shadow layer where a shadow lies: black at this alpha, scaled by the strength of the day. */
    public static final double OPACITY = 0.30;

    /** Shadow width of the stand-in drawn for a player without art, relative to the width of that stand-in. */
    public static final double PLAYER_FRACTION = 0.6;

    /** Shadow width of a tree, relative to the width of its stand-in. */
    private static final double TREE_SHADOW_RATIO = 0.55;

    /** Shadow width of a rock or an ore, relative to the width of its stand-in. */
    private static final double ROCK_SHADOW_RATIO = 0.9;

    /** Shadow width of a crystal cluster, relative to the width of its stand-in. */
    private static final double CRYSTAL_SHADOW_RATIO = 0.8;

    /** Ellipse shadow widths of the buildings drawn from shapes, in art pixels. */
    private static final double TORCH_SHADOW_WIDTH = 8.0;
    private static final double LOW_PIECE_SHADOW_WIDTH = 16.0;
    private static final double MID_PIECE_SHADOW_WIDTH = 18.0;
    private static final double FURNACE_SHADOW_WIDTH = 22.0;
    private static final double WIDE_PIECE_SHADOW_WIDTH = 26.0;

    /** Not instantiable: the class only holds static rules. */
    private GroundShadow() {}

    /**
     * Returns the ellipse shadow width under a resource drawn without art.
     * A tree casts a narrower shadow than its canopy, a rock or an ore a nearly as wide one.
     *
     * @param type           the resource type
     * @param spriteArtWidth the width of its stand-in in art pixels
     * @return the wanted shadow width in art pixels
     */
    public static double widthFor(ResourceType type, double spriteArtWidth) {
        return spriteArtWidth * switch (type) {
            case TREE -> TREE_SHADOW_RATIO;
            case STONE, IRON_ORE -> ROCK_SHADOW_RATIO;
            case CRYSTALS -> CRYSTAL_SHADOW_RATIO;
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
            case BRIDGE, SLEEPING_BAG -> 0.0;
            case STANDING_TORCH -> TORCH_SHADOW_WIDTH;
            case FENCE, FENCE_GATE, ANVIL -> LOW_PIECE_SHADOW_WIDTH;
            case CHEST, CAMPFIRE -> MID_PIECE_SHADOW_WIDTH;
            case FURNACE -> FURNACE_SHADOW_WIDTH;
            case WORKBENCH, BED -> WIDE_PIECE_SHADOW_WIDTH;
        };
    }
}
