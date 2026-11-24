package com.feuerschvenger.perlinsedge.domain.world.generation.pass;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;

/** Decides the terrain of a tile from its height and climate. One rule serves a whole map, chosen by its recipe. */
@FunctionalInterface
public interface GroundRule {

    /**
     * Classifies a tile.
     *
     * @param height      the height of the tile, about -1 to 1, with the dither of the ground pass already added
     * @param temperature the temperature, 0 to 1
     * @param moisture    the moisture, 0 to 1
     * @return the terrain of the tile
     */
    TileType classify(float height, float temperature, float moisture);
}
