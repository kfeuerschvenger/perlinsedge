package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.generation.pass.SpawnPass;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRecipesTest {

    private static final GenerationPass NOTHING = new GenerationPass() {
        @Override
        public String name() {
            return "nothing";
        }

        @Override
        public void apply(GenerationContext context) {
            // Does nothing on purpose: the test only needs a recipe that is well formed
        }
    };

    @ParameterizedTest
    @EnumSource(MapType.class)
    void everyMapTypeHasARecipeOfItsOwnTypeThatEndsWithTheSpawn(MapType type) {
        final var recipe = new MapRecipes().recipeFor(type);

        assertEquals(type, recipe.type());
        assertInstanceOf(SpawnPass.class, recipe.passes().getLast());
    }

    @ParameterizedTest
    @EnumSource(MapType.class)
    void everyRecipeStartsWithTheNoiseAndHasGroundBeforeAnyResource(MapType type) {
        final var names = new MapRecipes().recipeFor(type).passes().stream().map(GenerationPass::name).toList();

        assertEquals("noise-fields", names.getFirst());
        assertEquals(1, names.stream().filter("ground"::equals).count());
        assertTrue(names.indexOf("ground") < names.indexOf("rock-resources"));
        assertTrue(names.indexOf("rivers") < names.indexOf("rock-resources"));
    }

    @ParameterizedTest
    @EnumSource(MapType.class)
    void everyRecipeMakesRockyZonesBeforeTheOreAndBalancesTheResourcesLast(MapType type) {
        final var names = new MapRecipes().recipeFor(type).passes().stream().map(GenerationPass::name).toList();

        assertTrue(names.indexOf("rocky-zones") < names.indexOf("rock-resources"));
        assertTrue(names.indexOf("rock-resources") < names.indexOf("stone"));
        assertTrue(names.indexOf("stone") < names.indexOf("balance"));
        assertTrue(names.indexOf("balance") < names.indexOf("spawn"));
        if (names.contains("forest-floor")) {
            assertTrue(names.indexOf("vegetation") < names.indexOf("balance"));
            assertTrue(names.indexOf("balance") < names.indexOf("forest-floor"));
            assertTrue(names.indexOf("forest-floor") < names.indexOf("spawn"));
        }
    }

    @Test
    void theSameRegistryAlwaysHandsOutTheSameRecipe() {
        final var recipes = new MapRecipes();

        assertSame(recipes.recipeFor(MapType.DESERT), recipes.recipeFor(MapType.DESERT));
    }

    @Test
    void aRecipeBuiltForAnotherTypeIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new MapRecipes((MapType type) -> new MapRecipe(MapType.DESERT, List.of(NOTHING))));
    }

    @Test
    void aMissingRecipeOrTypeIsRefused() {
        assertThrows(NullPointerException.class, () -> new MapRecipes((MapType type) -> null));
        final var recipes = new MapRecipes();
        assertThrows(NullPointerException.class, () -> recipes.recipeFor(null));
    }
}
