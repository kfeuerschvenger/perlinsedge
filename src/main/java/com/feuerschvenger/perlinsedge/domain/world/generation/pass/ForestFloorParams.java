package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Where the forest floor lies around the trees.
 *
 * @param reach  how far from a tree, in tiles of Chebyshev distance, the floor reaches; 0 floors only the tile of the
 *               tree itself
 * @param floors the terrains that turn into forest floor, copied
 */
public record ForestFloorParams(int reach, Set<TileType> floors) {

    /** Checks the reach is not negative and copies the floors. */
    public ForestFloorParams {
        if (reach < 0) {
            throw new IllegalArgumentException("the reach cannot be negative: " + reach);
        }
        Objects.requireNonNull(floors, "floors");
        floors = copyOf(floors);
    }

    /**
     * Returns the terrains that turn into forest floor.
     *
     * @return a copy of the set
     */
    @Override
    public Set<TileType> floors() {
        return copyOf(floors);
    }

    private static Set<TileType> copyOf(Set<TileType> types) {
        final var copy = EnumSet.noneOf(TileType.class);
        copy.addAll(types);
        return copy;
    }
}
