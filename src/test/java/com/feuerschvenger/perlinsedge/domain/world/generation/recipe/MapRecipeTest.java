package com.feuerschvenger.perlinsedge.domain.world.generation.recipe;

import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationContext;
import com.feuerschvenger.perlinsedge.domain.world.generation.GenerationPass;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapRecipeTest {

    /** A pass that does nothing. */
    private static final GenerationPass IDLE = new GenerationPass() {
        @Override
        public String name() {
            return "idle";
        }

        @Override
        public void apply(GenerationContext context) {
            // Does nothing on purpose
        }
    };

    @Test
    void keepsTheTypeAndThePassesInOrder() {
        final var recipe = new MapRecipe(MapType.ISLANDS, List.of(IDLE));

        assertEquals(MapType.ISLANDS, recipe.type());
        assertEquals(List.of(IDLE), recipe.passes());
    }

    @Test
    void copiesTheListSoLaterChangesDoNotReachTheRecipe() {
        final var source = new ArrayList<GenerationPass>(List.of(IDLE));
        final var recipe = new MapRecipe(MapType.ISLANDS, source);

        source.clear();

        final var passes = recipe.passes();

        assertEquals(1, passes.size());
        assertThrows(UnsupportedOperationException.class, () -> passes.add(IDLE));
    }

    @Test
    void refusesAMissingTypeOrList() {
        final var passes = List.of(IDLE);

        assertThrows(NullPointerException.class, () -> new MapRecipe(null, passes));
        assertThrows(NullPointerException.class, () -> new MapRecipe(MapType.ISLANDS, null));
    }
}
