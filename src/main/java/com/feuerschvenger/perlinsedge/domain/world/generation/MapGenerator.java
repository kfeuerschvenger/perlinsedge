package com.feuerschvenger.perlinsedge.domain.world.generation;

import com.feuerschvenger.perlinsedge.domain.world.generation.recipe.MapRecipes;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;

import java.util.Objects;

/**
 * The entry point of map generation: runs the recipe of the requested map type over a new context and turns the result
 * into a map. The generator keeps no state of a generation, so one instance serves any number of requests from any
 * number of threads, and the same request always gives the same map.
 */
public final class MapGenerator {

    private final NoiseSourceFactory noiseFactory;
    private final MapRecipes recipes;
    private final GenerationPipeline pipeline = new GenerationPipeline();

    /**
     * Creates a generator with the standard recipes.
     *
     * @param noiseFactory builds the noise sources, which keeps the noise library outside the domain
     */
    public MapGenerator(NoiseSourceFactory noiseFactory) {
        this(noiseFactory, new MapRecipes());
    }

    /**
     * Creates a generator with its own recipes.
     *
     * @param noiseFactory builds the noise sources
     * @param recipes      the recipe of every map type
     */
    public MapGenerator(NoiseSourceFactory noiseFactory, MapRecipes recipes) {
        this.noiseFactory = Objects.requireNonNull(noiseFactory, "noiseFactory");
        this.recipes = Objects.requireNonNull(recipes, "recipes");
    }

    /**
     * Generates a map.
     *
     * @param request the type, the seed and the size
     * @return the map, with the spawn point of the player
     * @throws IllegalStateException when the recipe of the type does not choose a spawn point
     */
    public TileMap generate(MapRequest request) {
        return generate(request, PassObserver.NONE);
    }

    /**
     * Generates a map and tells an observer how long each pass took.
     *
     * @param request  the type, the seed and the size
     * @param observer told the name and the time of each pass after it ran
     * @return the map, with the spawn point of the player
     * @throws IllegalStateException when the recipe of the type does not choose a spawn point
     */
    public TileMap generate(MapRequest request, PassObserver observer) {
        Objects.requireNonNull(request, "request");
        final var context = new GenerationContext(request, noiseFactory);
        pipeline.run(context, recipes.recipeFor(request.type()).passes(), observer);
        if (context.spawnIndex() == GenerationContext.NONE) {
            throw new IllegalStateException("the recipe of " + request.type() + " chose no spawn point");
        }
        return TileMapAssembler.materialize(context);
    }
}
