package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.model.MapType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The recipe of every map type. The registry is filled by a switch over the map types that has no default branch, so a
 * new {@link MapType} without a recipe does not compile. The recipes hold only immutable passes, so one registry is
 * shared by every generation.
 */
public final class MapRecipes {

    private final Map<MapType, MapRecipe> recipes;

    /** Creates the registry with the recipe of every map type. */
    public MapRecipes() {
        this(MapRecipes::build);
    }

    /**
     * Creates a registry with recipes of its own, which lets a test supply a recipe that is broken on purpose.
     *
     * @param builder returns the recipe of a map type
     * @throws IllegalArgumentException when the recipe built for a type is for another type
     */
    public MapRecipes(Function<MapType, MapRecipe> builder) {
        final var byType = new EnumMap<MapType, MapRecipe>(MapType.class);
        for (var type : MapType.values()) {
            final var recipe = Objects.requireNonNull(builder.apply(type), "recipe");
            if (recipe.type() != type) {
                throw new IllegalArgumentException("the recipe built for " + type + " is for " + recipe.type());
            }
            byType.put(type, recipe);
        }
        this.recipes = Collections.unmodifiableMap(byType);
    }

    /**
     * Returns the recipe of a map type.
     *
     * @param type the map type
     * @return its recipe, whose type is the one asked for
     */
    public MapRecipe recipeFor(MapType type) {
        return recipes.get(Objects.requireNonNull(type, "type"));
    }

    /** Builds the recipe of a type. The switch is exhaustive and has no default on purpose. */
    private static MapRecipe build(MapType type) {
        return switch (type) {
            case DENSE_FOREST -> DenseForestRecipe.create();
            case SWAMP -> SwampRecipe.create();
            case ISLANDS -> IslandsRecipe.create();
            case DESERT -> DesertRecipe.create();
            case TUNDRA -> TundraRecipe.create();
            case RIVERS -> RiversRecipe.create();
            case MEDITERRANEAN -> MediterraneanRecipe.create();
            case MOUNTAINOUS -> MountainousRecipe.create();
        };
    }
}
