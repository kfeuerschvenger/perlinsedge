package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

import java.util.Objects;

/**
 * Everything that varies between two generations: which kind of map, the seed and the size. All the other settings are
 * constants of the recipe of the type.
 *
 * @param type   the kind of map
 * @param seed   the generation seed
 * @param width  the number of columns, at least 1
 * @param height the number of rows, at least 1
 */
public record MapRequest(MapType type, long seed, int width, int height) {

    /** Checks the type is set, both sides are positive and the number of tiles fits in an int. */
    public MapRequest {
        Objects.requireNonNull(type, "type");
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("the size must be at least 1x1: " + width + "x" + height);
        }
        if ((long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("the size has more tiles than an array can index: " + width + "x"
                    + height);
        }
    }

    /**
     * Returns the number of tiles, which is the length of every array of a context.
     *
     * @return width times height, which the constructor checked cannot overflow
     */
    public int tileCount() {
        return width * height;
    }
}
