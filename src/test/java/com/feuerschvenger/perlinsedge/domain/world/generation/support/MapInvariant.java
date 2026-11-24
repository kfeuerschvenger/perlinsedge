package com.feuerschvenger.perlinsedge.domain.world.generation.support;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/** One property a generated map must have, checked for a map and the spec it came from. */
@FunctionalInterface
public interface MapInvariant {

    /**
     * Checks the map.
     *
     * @param map  the generated map
     * @param spec what it was generated from
     * @throws AssertionError with a message saying what is wrong when the map breaks the invariant
     */
    void check(TileMap map, MapSpec spec);
}
