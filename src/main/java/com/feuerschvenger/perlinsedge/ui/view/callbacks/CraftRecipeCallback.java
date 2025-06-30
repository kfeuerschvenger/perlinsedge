package com.feuerschvenger.perlinsedge.ui.view.callbacks;

import com.feuerschvenger.perlinsedge.domain.crafting.Recipe;

/**
 * Functional interface for handling crafting recipe selection.
 */
@FunctionalInterface
public interface CraftRecipeCallback {

    void craft(Recipe recipe);

}
