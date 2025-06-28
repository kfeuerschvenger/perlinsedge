package com.feuerschvenger.perlinsedge.domain.crafting;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a crafting recipe, defining the input ingredients and the output item.
 * This class encapsulates the logic for creating an item from its ingredients,
 * including validation of the recipe's properties.
 */
public class Recipe {
    private final String name;
    private final String description;
    private final ItemType outputItemType;
    private final int outputQuantity;
    private final List<CraftingIngredient> ingredients;

    /**
     * Constructs a new Recipe instance.
     *
     * @param outputItemType The type of item produced by this recipe.
     * @param outputQuantity The quantity of the output item produced.
     * @param ingredients A list of ingredients required for crafting the item.
     * @throws IllegalArgumentException if outputItemType is null, outputQuantity is not positive,
     *                                  ingredients is null or empty, or any ingredient has a non-positive quantity.
     */
    public Recipe(ItemType outputItemType, int outputQuantity, List<CraftingIngredient> ingredients) {
        this.outputItemType = Objects.requireNonNull(outputItemType, "The output item type cannot be null.");
        this.name = outputItemType.getDisplayName();
        this.description = outputItemType.getDescription();
        if (outputQuantity <= 0) {
            throw new IllegalArgumentException("The output quantity must be positive.");
        }
        this.outputQuantity = outputQuantity;
        this.ingredients = Collections.unmodifiableList(Objects.requireNonNull(ingredients, "The list of ingredients cannot be null."));
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipes must have at least one ingredient.");
        }
    }

    /**
     * Returns a formatted string of the required ingredients.
     * Example: "Requires: 2 x Wooden Log, 1 x Stone"
     * @return A formatted string listing the ingredients.
     */
    public String getIngredientsString() {
        return "Requires: " + ingredients.stream()
                .map(CraftingIngredient::toString)
                .collect(Collectors.joining(", "));
    }

    // ==================================================================
    //  GETTERS FOR RECIPE PROPERTIES
    // ==================================================================

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ItemType getOutputItemType() {
        return outputItemType;
    }

    public int getOutputQuantity() {
        return outputQuantity;
    }

    public List<CraftingIngredient> getIngredients() {
        return ingredients;
    }

}