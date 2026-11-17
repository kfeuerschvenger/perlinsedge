package com.feuerschvenger.perlinsedge.config;

import com.feuerschvenger.perlinsedge.domain.crafting.CraftingIngredient;
import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;
import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeManagerTest {

    @Test
    void theHammerIsCraftedFromTwoSticksAndTwoStonesWithNoStation() {
        Recipe hammer = new RecipeManager().getAllItemRecipes().stream()
                .filter(recipe -> recipe.getOutputItemType() == ItemType.HAMMER)
                .findFirst()
                .orElseThrow();

        assertEquals(1, hammer.getOutputQuantity());
        assertEquals(List.of(new CraftingIngredient(ItemType.STICK, 2), new CraftingIngredient(ItemType.STONE, 2)),
                hammer.getIngredients());
    }

    @Test
    void theHammerHasItsIconFile() {
        assertTrue(getClass().getResource("/assets/items/" + ItemType.HAMMER.getSpritePath() + ".png") != null);
    }
}
