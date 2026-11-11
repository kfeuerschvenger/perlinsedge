package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

import java.util.Objects;

/**
 * Factory to create instances of different types of map generators.
 */
public class MapGeneratorFactory {

    private final NoiseSourceFactory noiseFactory;

    /**
     * Creates a factory.
     *
     * @param noiseFactory builds the noise fields of every generator this factory creates
     */
    public MapGeneratorFactory(NoiseSourceFactory noiseFactory) {
        this.noiseFactory = Objects.requireNonNull(noiseFactory, "noiseFactory");
    }

    /**
     * Creates and returns a new instance of IMapGenerator based on the MapType.
     * Every map type is currently produced by the Perlin generator; this is where a different generator would be chosen.
     *
     * @param mapType The type of map for which a generator is needed.
     * @return An instance of IMapGenerator.
     */
    public IMapGenerator createGenerator(MapType mapType) {
        return new PerlinMapGenerator(noiseFactory);
    }

}
