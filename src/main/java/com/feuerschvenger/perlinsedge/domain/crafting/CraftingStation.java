package com.feuerschvenger.perlinsedge.domain.crafting;

import lombok.Getter;

/**
 * The places an item can be crafted at. Every item recipe belongs to exactly one of them: the inventory crafts the
 * basic recipes anywhere, and each crafting station building crafts its own list, which is not repeated at the others.
 */
@Getter
public enum CraftingStation {
    /** The player inventory, which crafts the basic recipes without any building. */
    INVENTORY("Inventory"),
    /** The workbench building: stone tools, bricks and potions. */
    WORKBENCH("Workbench"),
    /** The anvil building: iron tools. */
    ANVIL("Anvil");

    /** Name shown as the title of the crafting view. */
    private final String displayName;

    CraftingStation(String displayName) {
        this.displayName = displayName;
    }
}
