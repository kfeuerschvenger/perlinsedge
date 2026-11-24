package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.generation.MapRequest;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

import java.util.Objects;

/**
 * What identifies one generated map for the test harness: the type, the seed and the side of the square map. It is the
 * request of the generator for a square map.
 *
 * @param type the kind of map
 * @param seed the generation seed
 * @param size the width and the height in tiles
 */
public record MapSpec(MapType type, long seed, int size) {

    /** Checks the type is set and the size is positive. */
    public MapSpec {
        Objects.requireNonNull(type, "type");
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1: " + size);
        }
    }

    /** @return the request of the generator for this map, as wide and as high as the size */
    public MapRequest toRequest() {
        return new MapRequest(type, seed, size, size);
    }

    /** @return a name safe to use in a file, such as {@code DESERT-1-150} */
    public String label() {
        return type + "-" + seed + "-" + size;
    }
}
