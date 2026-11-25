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

    /**
     * The shorter side of a map, in tiles, from which the invariants of the dense forest and the mountains are
     * promised, on a map that is at most twice as long as it is wide. Below it the water can leave too little land, or
     * too little room away from the rock, for the glade and the distance of the spawn from the rock. On a map longer
     * than that, or below it, the clearings still shrink to keep the trees in their share of the land, but one water
     * body can cut the land in two pieces that no trail joins, which the shares of the spawn region cannot allow for.
     */
    public static final int MIN_CONTENT_SIDE = 64;

    /**
     * How far, in tiles, the spawn of the mountains is from the nearest mountain at the least, from
     * {@link #MIN_CONTENT_SIDE} up. The clearings keep this clearance from rock and the spawn clearing is one of them.
     * On a map too small for any tile to have it the spawn pass places the player without a clearing, closer to rock.
     */
    public static final int MOUNTAINOUS_SPAWN_CLEARANCE = 4;

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
