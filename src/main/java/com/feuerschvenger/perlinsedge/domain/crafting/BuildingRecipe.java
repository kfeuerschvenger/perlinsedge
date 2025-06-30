package com.feuerschvenger.perlinsedge.domain.crafting;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an immutable recipe for constructing a specific building type.
 * Contains the building type and the required crafting ingredients.
 *
 * <p>This record ensures:
 * <ul>
 *   <li>Complete immutability of recipe data</li>
 *   <li>Defensive copying of ingredients list</li>
 *   <li>Null safety through validation</li>
 *   <li>Consistent display information via BuildingType</li>
 * </ul>
 *
 * @param buildingType The type of building this recipe constructs
 * @param ingredients The unmodifiable list of required crafting ingredients
 */
public record BuildingRecipe(BuildingType buildingType, List<CraftingIngredient> ingredients) {

    /**
     * Compact constructor for validation and immutability enforcement.
     *
     * @param buildingType Building type (must not be null)
     * @param ingredients Ingredients list (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    public BuildingRecipe {
        Objects.requireNonNull(buildingType, "Building type must not be null");
        Objects.requireNonNull(ingredients, "Ingredients list must not be null");

        // Create defensive unmodifiable copy
        ingredients = Collections.unmodifiableList(ingredients);
    }

    // Derived properties --------------------------------------------------------

    /**
     * Gets the display name of the building from its type.
     *
     * @return Localized or formatted display name
     */
    public String getDisplayName() {
        return buildingType.getDisplayName();
    }

    /**
     * Gets the description of the building from its type.
     *
     * @return Localized or formatted description
     */
    public String getDescription() {
        return buildingType.getDescription();
    }

    // Equality and representation -----------------------------------------------

    /**
     * Returns a string representation of the building recipe.
     * Format: "BuildingRecipe[type={type}, ingredients={ingredients}]"
     */
    @Override
    public String toString() {
        return String.format("BuildingRecipe[type=%s, ingredients=%s]",
                buildingType, ingredients);
    }

}