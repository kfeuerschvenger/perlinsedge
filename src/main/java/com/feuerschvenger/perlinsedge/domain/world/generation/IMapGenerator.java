package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

/**
 * Interface for map generation strategies.
 * Implementations should provide a way to generate a TileMap based on the provided context.
 */
public interface IMapGenerator {

    /**
     * Generates a TileMap based on the specified width, height, and context.
     *
     * @param width   The width of the map.
     * @param height  The height of the map.
     * @param context The context containing parameters for map generation.
     * @return A TileMap generated according to the specified parameters.
     */
    TileMap generateMap(int width, int height, MapGenerationContext context);

    /**
     * Returns the seed used for random number generation in this map generator.
     * @return The seed value.
     */
    long getSeed();

}
