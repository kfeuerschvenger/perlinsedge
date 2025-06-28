package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

/**
 * Factory to create instances of different types of map generators.
 */
public class MapGeneratorFactory {

    public MapGeneratorFactory() { }

    /**
     * Creates and returns a new instance of IMapGenerator based on the MapType.
     * If is required to have other generators, the logic to choose which one to create would go here.
     * @param mapType The type of map for which a generator is needed.
     * @return An instance of IMapGenerator.
     */
    public IMapGenerator createGenerator(MapType mapType) {
        // For now, we always return a PerlinMapGenerator,
        // but this is where you could add logic for different types of generators.
        // switch (mapType) {
        //     case PERLIN: return new PerlinMapGenerator();
        //     case OTHER_GENERATOR: return new SomeOtherMapGenerator();
        //     default: return new PerlinMapGenerator();
        // }
        return new PerlinMapGenerator();
    }

    /**
     * Returns the current map generator.
     * @return The map generator used in the last generation or a default generator.
     */
    public IMapGenerator getCurrentMapGenerator() {
        return new PerlinMapGenerator();
    }

}