package com.feuerschvenger.perlinsedge.domain.crafting;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a complete crafting recipe, defining the required ingredients and resulting output.
 * Immutable by design to ensure recipe consistency throughout the crafting system.
 *
 * <p>Key features:
 * <ul>
 *   <li>Full validation of all recipe components</li>
 *   <li>Immutable state guarantees thread safety</li>
 *   <li>Derived display properties from output item</li>
 *   <li>Human-readable ingredient formatting</li>
 * </ul>
 */
public class Recipe {
    // Recipe metadata
    private final String name;
    private final String description;

    // Output configuration
    private final ItemType outputItemType;
    private final int outputQuantity;

    // Input requirements
    private final List<CraftingIngredient> ingredients;

    /**
     * Constructs a validated crafting recipe.
     *
     * @param outputItemType Type of item produced (must not be null)
     * @param outputQuantity Quantity produced (must be positive)
     * @param ingredients Required ingredients (must not be null/empty with valid quantities)
     * @throws NullPointerException if outputItemType or ingredients is null
     * @throws IllegalArgumentException for invalid quantities or empty ingredients
     */
    public Recipe(ItemType outputItemType, int outputQuantity, List<CraftingIngredient> ingredients) {
        // Validate core parameters
        this.outputItemType = Objects.requireNonNull(outputItemType, "Output item type must not be null");
        this.ingredients = validateIngredients(ingredients);

        // Validate and set output quantity
        if (outputQuantity <= 0) {
            throw new IllegalArgumentException("Output quantity must be positive. Received: " + outputQuantity);
        }
        this.outputQuantity = outputQuantity;

        // Derive display properties from output item
        this.name = outputItemType.getDisplayName();
        this.description = outputItemType.getDescription();
    }

    /**
     * Validates and prepares the ingredients list.
     *
     * @return Unmodifiable list of validated ingredients
     * @throws NullPointerException if ingredients is null
     * @throws IllegalArgumentException if ingredients list is empty
     */
    private List<CraftingIngredient> validateIngredients(List<CraftingIngredient> ingredients) {
        Objects.requireNonNull(ingredients, "Ingredients list must not be null");

        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipes must contain at least one ingredient");
        }

        return Collections.unmodifiableList(ingredients);
    }

    // Display Methods -----------------------------------------------------------

    /**
     * Formats ingredients into a human-readable string.
     * Example: "Requires: 2 × Wood, 1 × Stone"
     *
     * @return Formatted ingredient requirements
     */
    public String getIngredientsString() {
        String formatted = ingredients.stream()
                .map(CraftingIngredient::toString)
                .collect(Collectors.joining(", "));

        return "Requires: " + formatted;
    }

    // Accessor Methods ----------------------------------------------------------

    /**
     * Gets the display name of the recipe (derived from output item).
     * @return Recipe display name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description of the recipe (derived from output item).
     * @return Recipe description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the type of item produced by this recipe.
     * @return Output item type
     */
    public ItemType getOutputItemType() {
        return outputItemType;
    }

    /**
     * Gets the quantity of items produced per craft.
     * @return Output quantity
     */
    public int getOutputQuantity() {
        return outputQuantity;
    }

    /**
     * Gets the immutable list of required ingredients.
     * @return Unmodifiable list of ingredients
     */
    public List<CraftingIngredient> getIngredients() {
        return ingredients;
    }

    // Object Contract -----------------------------------------------------------

    /**
     * Returns a string representation of the recipe.
     * Format: "Recipe[name='Stone Axe', output=STONE_AXE×1, ingredients=...]"
     */
    @Override
    public String toString() {
        return String.format("Recipe[name='%s', output=%s×%d, ingredients=%s]",
                name, outputItemType, outputQuantity, ingredients);
    }

    /**
     * Generated equals method considering all significant fields.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return outputQuantity == recipe.outputQuantity &&
                Objects.equals(name, recipe.name) &&
                Objects.equals(description, recipe.description) &&
                outputItemType == recipe.outputItemType &&
                Objects.equals(ingredients, recipe.ingredients);
    }

    /**
     * Generated hashCode method consistent with equals implementation.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, description, outputItemType, outputQuantity, ingredients);
    }

}