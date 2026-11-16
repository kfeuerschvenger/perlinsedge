package com.feuerschvenger.perlinsedge.domain.world.model;

import com.feuerschvenger.perlinsedge.domain.entities.items.ItemType;

/**
 * Enum representing different types of resources in the game world.
 */
public enum ResourceType {
    STONE,
    /** Crystals in rocky ground, only a pickaxe held in hand can mine them. */
    CRYSTALS(ItemType.PICKAXE),
    TREE,
    /** Iron ore in rocky ground, only a pickaxe held in hand can mine it. */
    IRON_ORE(ItemType.PICKAXE);

    private final ItemType requiredTool;

    ResourceType() {
        this(null);
    }

    ResourceType(ItemType requiredTool) {
        this.requiredTool = requiredTool;
    }

    /**
     * Returns the tool the player must hold in hand to harvest this resource.
     *
     * @return the required tool, or null when the resource can be harvested by hand
     */
    public ItemType getRequiredTool() {
        return requiredTool;
    }

    /**
     * Returns the first letter of the resource type name.
     * @return The first letter of the resource type name.
     */
    public String getInitial() {
        return name().charAt(0) + "";
    }

}
