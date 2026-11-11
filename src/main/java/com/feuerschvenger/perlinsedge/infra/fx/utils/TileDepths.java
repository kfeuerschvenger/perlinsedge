package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/**
 * The vertical layout of the ground, in art pixels below the top of a land tile.
 * Every tile is a column: land tops sit at zero, the sand bed under shallow, coast and river water one step lower and
 * the bed under deep water two steps lower. The water sheet lies half a step below the land tops, so the banks show a
 * waterline and the step between two depths shows its side faces. A block sprite carries {@link #FACE_PIXELS} of face,
 * and a face strip continues it by the same amount, so a side that a lower neighbour exposes is completed with whole
 * strips. The depth is a pure function of the tile type, so nothing is stored per tile and nothing is allocated.
 */
public final class TileDepths {

    /** Art pixels between one depth level and the next. */
    public static final int STEP_PIXELS = 10;

    /** Art pixels the water sheet lies below the land tops, half a step so the sheet is between the land and the bed. */
    public static final int SURFACE_PIXELS = STEP_PIXELS / 2;

    /** Art pixels of side face a tile sprite carries, and the height of one face strip. */
    public static final int FACE_PIXELS = 5;

    /** Not instantiable: the class only holds static rules. */
    private TileDepths() {}

    /**
     * Returns the depth level of a tile type.
     *
     * @param type the tile type
     * @return 0 for land, 1 for shallow, coast and river water, 2 for deep water
     */
    public static int level(TileType type) {
        if (!type.isWater()) {
            return 0;
        }
        return type == TileType.DEEP_WATER ? 2 : 1;
    }

    /**
     * Returns how far the top of a tile type lies below the land tops.
     *
     * @param type the tile type
     * @return the drop in art pixels
     */
    public static int sinkPixels(TileType type) {
        return level(type) * STEP_PIXELS;
    }

    /**
     * Returns how many face strips complete the side of a tile that its lower neighbour on that side exposes.
     * The sprite of the tile already covers {@link #FACE_PIXELS} of the drop, a neighbour at the same level or higher
     * exposes nothing, and a tile at the edge of the map has no neighbour to expose it.
     *
     * @param self     the type of the tile
     * @param neighbour the type of the neighbour in front of that side, or null outside the map
     * @return the number of strips, zero or more
     */
    public static int stripsExposed(TileType self, TileType neighbour) {
        if (neighbour == null) {
            return 0;
        }
        final int uncovered = sinkPixels(neighbour) - sinkPixels(self) - FACE_PIXELS;
        return uncovered <= 0 ? 0 : (uncovered + FACE_PIXELS - 1) / FACE_PIXELS;
    }
}
