package com.feuerschvenger.perlinsedge.domain.world.model;

/**
 * Enum representing different types of resources in the game world.
 */
public enum ResourceType {
    STONE,
    CRYSTALS,
    TREE;

    /**
     * Returns the first letter of the resource type name.
     * @return The first letter of the resource type name.
     */
    public String getInitial() {
        return name().charAt(0) + "";
    }

}
