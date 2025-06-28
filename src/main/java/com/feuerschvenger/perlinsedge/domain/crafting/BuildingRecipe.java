package com.feuerschvenger.perlinsedge.domain.crafting;

import com.feuerschvenger.perlinsedge.domain.entities.buildings.BuildingType;

import java.util.Collections;
import java.util.List;

/**
 * Represents a recipe for constructing a building.
 * Defines the type of building that can be built and the required ingredients.
 */
public record BuildingRecipe(BuildingType buildingType, List<CraftingIngredient> ingredients) {

    public BuildingRecipe(BuildingType buildingType, List<CraftingIngredient> ingredients) {
        this.buildingType = buildingType;
        this.ingredients = Collections.unmodifiableList(ingredients);
    }

    public String getDisplayName() {
        return buildingType.getDisplayName();
    }

    public String getDescription() {
        return buildingType.getDescription();
    }

}
